/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api.services;

import static org.fcrepo.http.api.services.HttpRdfService.checkTripleForDisallowed;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.QuadAcc;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateData;
import org.apache.jena.sparql.modify.request.UpdateDataDelete;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.sparql.modify.request.UpdateDeleteWhere;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.modify.request.UpdateVisitorBase;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.MultipleConstraintViolationException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;
import org.fcrepo.kernel.api.exception.ServerManagedTypeException;
import org.slf4j.Logger;

/**
 * A special UpdateVisitor to translate Fedora URIs to internal FedoraIDs.
 * @author whikloj
 */
public class SparqlTranslateVisitor extends UpdateVisitorBase {

    private static final Logger LOGGER = getLogger(SparqlTranslateVisitor.class);

    private List<Update> newUpdates = new ArrayList<>();

    private HttpIdentifierConverter idTranslator;

    public SparqlTranslateVisitor(final HttpIdentifierConverter identifierConverter) {
        idTranslator = identifierConverter;
    }

    private List<ConstraintViolationException> exceptions = new ArrayList<>();

    @Override
    public void visit(final UpdateDataInsert update) {
        translateUpdate(update);
    }

    @Override
    public void visit(final UpdateDataDelete update) {
        translateUpdate(update);
    }

    @Override
    public void visit(final UpdateDeleteWhere update) {
        translateUpdate(update);
    }

    @Override
    public void visit(final UpdateModify update) {
        translateUpdate(update);
    }

    /**
     * Get the new UpdateRequest based on the parsed Updates.
     * @return the new update request object.
     */
    public UpdateRequest getTranslatedRequest() {
        final UpdateRequest newRequest = UpdateFactory.create();
        newUpdates.forEach(newRequest::add);
        return newRequest;
    }

    /**
     * Perform a translation of all the triples in an Update adding them to the internal list.
     * @param update the update request to translate.
     */
    private void translateUpdate(final Update update) {
        final List<Quad> sourceQuads;
        if (update instanceof UpdateDeleteWhere) {
            sourceQuads = ((UpdateDeleteWhere)update).getQuads();
        } else {
            sourceQuads = ((UpdateData) update).getQuads();
        }
        final List<Quad> newQuads = translateQuads(sourceQuads);
        assertNoExceptions();
        final Update newUpdate = makeUpdate(update.getClass(), newQuads);
        newUpdates.add(newUpdate);
    }

    /**
     * Perform a translation of all the triples in an UpdateModify request.
     * @param update the update request to translate
     */
    private void translateUpdate(final UpdateModify update) {
        final UpdateModify newUpdate = new UpdateModify();
        final List<Quad> insertQuads = (update.hasInsertClause() ? translateQuads(update.getInsertQuads()) :
                Collections.emptyList());
        final List<Quad> deleteQuads = (update.hasDeleteClause() ? translateQuads(update.getDeleteQuads()) :
                Collections.emptyList());
        assertNoExceptions();

        insertQuads.forEach(q -> newUpdate.getInsertAcc().addQuad(q));
        deleteQuads.forEach(q -> newUpdate.getDeleteAcc().addQuad(q));

        final Element where = update.getWherePattern();
        final Element newElement = processElements(where);
        newUpdate.setElement(newElement);
        newUpdates.add(newUpdate);
    }

    private void assertNoExceptions() {
        if (!exceptions.isEmpty()) {
            throw new MultipleConstraintViolationException(exceptions);
        }
    }

    /**
     * Process triples inside the Element or return the element.
     * @param element the element to translate.
     * @return the translated or original element.
     */
    private Element processElements(final Element element) {
        if (element instanceof ElementGroup) {
            final ElementGroup group = new ElementGroup();
            ((ElementGroup) element).getElements().forEach(e -> group.addElement(processElements(e)));
            return group;
        } else if (element instanceof ElementPathBlock) {
            final BasicPattern basicPattern = new BasicPattern();
            final var tripleIter = ((ElementPathBlock) element).patternElts();
            tripleIter.forEachRemaining(t -> {
                if (t.isTriple()) {
                    try {
                        basicPattern.add(translateTriple(t.asTriple()));
                    } catch (final ServerManagedPropertyException | ServerManagedTypeException exc) {
                        exceptions.add(exc);
                    }
                }
            });
            return new ElementPathBlock(basicPattern);
        }
        return element;
    }

    /**
     * Perform the translation to a list of quads.
     * @param quadsList the quads
     * @return the translated list of quads.
     */
    private List<Quad> translateQuads(final List<Quad> quadsList) {
        final List<Quad> newQuads = new ArrayList<>();
        for (final Quad q : quadsList) {
            try {
                final Node subject = translateId(q.getSubject());
                final Node object = translateId(q.getObject());
                checkTripleForDisallowed(q.asTriple());
                final Quad quad = new Quad(q.getGraph(), subject, q.getPredicate(), object);
                LOGGER.trace("Translated quad is: {}", quad);
                newQuads.add(quad);
            } catch (final ServerManagedPropertyException | ServerManagedTypeException exc) {
                // Swallow these exceptions to throw together later.
                exceptions.add(exc);
            }
        }
        return newQuads;
    }

    /**
     * Translate the subject and object of a triple from external URIs to internal IDs.
     * @param triple the triple to translate
     * @return the translated triple.
     */
    private Triple translateTriple(final Triple triple) {
        final Node subject = translateId(triple.getSubject());
        final Node object = translateId(triple.getObject());
        checkTripleForDisallowed(triple);
        return Triple.create(subject, triple.getPredicate(), object);
    }

    /**
     * Quads insert/delete data statements don't contain variables and use QuadDataAcc to accumulate,
     * insert {} delete {} where {} statements can't contain variables and use QuadAcc to accumulate. This function
     * simplifies the creation of the eventual Update.
     * @param updateClass the class of Update we are starting with.
     * @param quadList the list of Quads to generate the above class with.
     * @return a subclass of Update with the provided Quads.
     */
    private Update makeUpdate(final Class<? extends Update> updateClass, final List<Quad> quadList) {
        try {
            if (updateClass.equals(UpdateDeleteWhere.class)) {
                final QuadAcc quadAcc = new QuadAcc();
                quadList.forEach(quadAcc::addQuad);
                return new UpdateDeleteWhere(quadAcc);
            } else {
                final QuadDataAcc quadsAcc = new QuadDataAcc();
                quadList.forEach(quadsAcc::addQuad);
                final Constructor<? extends Update> update = updateClass.getConstructor(QuadDataAcc.class);
                return update.newInstance(quadsAcc);
            }
        } catch (final ReflectiveOperationException exc) {
            LOGGER.warn("Could not find constructor UpdateRequest");
            throw new RepositoryRuntimeException("Could not find constructor UpdateRequest", exc);
        }
    }

    /**
     * If the node is a URI with the external domain translate it, otherwise leave it alone
     * @param externalNode the node to translate
     * @return the original or translated node.
     */
    private Node translateId(final Node externalNode) {
        if (externalNode.isURI()) {
            final String externalId = externalNode.getURI();
            if (idTranslator.inExternalDomain(externalId)) {
                return NodeFactory.createURI(idTranslator.toInternalId(externalId));
            }
        }
        return externalNode;
    }

}
