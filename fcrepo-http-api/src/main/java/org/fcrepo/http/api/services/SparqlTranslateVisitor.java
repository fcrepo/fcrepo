/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api.services;

import static org.fcrepo.config.ServerManagedPropsMode.RELAXED;
import static org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper.checkTripleForDisallowed;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.MultipleConstraintViolationException;
import org.fcrepo.kernel.api.exception.RelaxableServerManagedPropertyException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;
import org.fcrepo.kernel.api.exception.ServerManagedTypeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.modify.request.QuadAcc;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateData;
import org.apache.jena.sparql.modify.request.UpdateDataDelete;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.sparql.modify.request.UpdateDeleteWhere;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.modify.request.UpdateVisitorBase;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;

/**
 * A special UpdateVisitor to translate Fedora URIs to internal FedoraIDs.
 * @author whikloj
 */
public class SparqlTranslateVisitor extends UpdateVisitorBase {

    private static final Logger LOGGER = getLogger(SparqlTranslateVisitor.class);

    private static final String CUSTOM_SPARQL_VARIABLE = "fedoraBinaryFix";

    private static final Node CUSTOM_SPARQL_VAR_NODE = NodeFactory.createVariable(CUSTOM_SPARQL_VARIABLE);

    private List<Update> newUpdates = new ArrayList<>();

    private HttpIdentifierConverter idTranslator;

    private boolean isRelaxedMode;

    private FedoraId resourceId;

    private boolean resourceVariableUpdate = false;

    public SparqlTranslateVisitor(final HttpIdentifierConverter identifierConverter, final FedoraPropsConfig config,
                                  final FedoraId id) {
        idTranslator = identifierConverter;
        isRelaxedMode = config.getServerManagedPropsMode().equals(RELAXED);
        resourceId = id;
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
        final boolean isUpdateData = (update instanceof UpdateData);
        final boolean isDelete = (update instanceof UpdateDeleteWhere || update instanceof UpdateDataDelete);
        final List<Quad> sourceQuads;
        if (update instanceof UpdateDeleteWhere) {
            sourceQuads = ((UpdateDeleteWhere)update).getQuads();
        } else {
            sourceQuads = ((UpdateData) update).getQuads();
        }
        final List<Quad> newQuads = translateQuads(sourceQuads, isDelete, isUpdateData);
        assertNoExceptions();
        if (resourceId.isDescription() && update instanceof UpdateDataDelete) {
            // This is a NonRdfSourceDescription and an UpdateData block so add a second delete for the ID ending in
            // /fcr:metadata as per FCREPO-3820
            final var tempQuads = new ArrayList<>(newQuads);
            newQuads.addAll(translateQuads(tempQuads, isDelete, isUpdateData));
        }
        final Update newUpdate = makeUpdate(update.getClass(), newQuads);
        newUpdates.add(newUpdate);
    }

    /**
     * Perform a translation of all the triples in an UpdateModify request.
     * @param update the update request to translate
     */
    private void translateUpdate(final UpdateModify update) {
        final UpdateModify newUpdate = new UpdateModify();
        final List<Quad> insertQuads = (update.hasInsertClause() ? translateQuads(update.getInsertQuads(), false) :
                Collections.emptyList());
        final List<Quad> deleteQuads = (update.hasDeleteClause() ? translateQuads(update.getDeleteQuads(), true) :
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
     * Generate the variable definition of ?fedoraBinaryFix -> binary & binary description IDs
     * @return The elementData block.
     */
    private ElementData getVariableBlock() {
        final var descNode = NodeFactory.createURI(resourceId.getFullId());
        final var binaryNode = NodeFactory.createURI(resourceId.getFullDescribedId());
        final var elementVar = Var.alloc(CUSTOM_SPARQL_VARIABLE);

        final var elementData = new ElementData();
        // Bind the variable to the values
        elementData.add(elementVar);
        elementData.add(BindingBuilder.create().add(elementVar, binaryNode).build());
        elementData.add(BindingBuilder.create().add(elementVar, descNode).build());
        return elementData;
    }

    /**
     * Process triples inside the Element or return the element.
     * @param element the element to translate.
     * @return the translated or original element.
     */
    private Element processElements(final Element element) {
        if (element instanceof ElementGroup) {
            final ElementGroup group = new ElementGroup();
            ((ElementGroup)element).getElements().stream().map(this::processElements).forEach(group::addElement);
            if (resourceVariableUpdate) {
                group.addElement(getVariableBlock());
            }
            return group;
        } else if (element instanceof ElementPathBlock) {
            final BasicPattern basicPattern = new BasicPattern();
            final var tripleIter = ((ElementPathBlock) element).patternElts();
            tripleIter.forEachRemaining(t -> {
                if (t.isTriple()) {
                    try {
                        checkTripleForDisallowed(t.asTriple());
                    } catch (final ServerManagedPropertyException | ServerManagedTypeException exc) {
                        if (!isRelaxedMode) {
                            exceptions.add(exc);
                            return;
                        }
                    }
                    if (resourceId.isDescription() && t.getSubject().isURI()) {
                        // If this is a binary description resource and the subject is a URI.
                        // Get an internal ID based on the subject.
                        final var subjId = idTranslator.translateUri(t.getSubject().getURI());
                        // If it matches the binary ID or binary description ID.
                        if (resourceId.getFullDescribedId().equals(subjId) || resourceId.getFullId().equals(subjId)) {
                            // Substitute the variable for the URI.
                            basicPattern.add(translateTriple(Triple.create(
                                    Var.alloc(CUSTOM_SPARQL_VARIABLE),
                                    t.getPredicate(),
                                    t.getObject()
                            )));
                            resourceVariableUpdate = true;
                        } else {
                            basicPattern.add(translateTriple(t.asTriple()));
                        }
                    } else {
                        basicPattern.add(translateTriple(t.asTriple()));
                    }
                }
            });
            return new ElementPathBlock(basicPattern);
        }
        return element;
    }

    private List<Quad> translateQuads(final List<Quad> quadsList, final boolean isDelete) {
        return translateQuads(quadsList, isDelete, false);
    }

    /**
     * Perform the translation to a list of quads.
     * @param quadsList the quads
     * @param isDelete is this part of a delete block or statement
     * @param isUpdateBlock is this an INSERT|DELETE DATA update.
     * @return the translated list of quads.
     */
    private List<Quad> translateQuads(final List<Quad> quadsList,  final boolean isDelete,
                                      final boolean isUpdateBlock) {
        final List<Quad> newQuads = new ArrayList<>();
        for (final Quad q : quadsList) {
            try {
                checkTripleForDisallowed(q.asTriple());
            } catch (final RelaxableServerManagedPropertyException exc) {
                if (!isRelaxedMode) {
                    // Swallow these exceptions to throw together later.
                    exceptions.add(exc);
                    continue;
                }
            } catch (final ServerManagedTypeException | ServerManagedPropertyException exc) {
                exceptions.add(exc);
                continue;
            }
            final Quad quad;
            if (resourceId.isDescription()) {
                quad = translateBinaryQuad(q, isDelete, isUpdateBlock);
            } else {
                quad = translateContainerQuad(q);
            }
            LOGGER.trace("Translated quad is: {}", quad);
            newQuads.add(quad);
        }
        return newQuads;
    }

    private Quad translateContainerQuad(final Quad quad) {
        final Node subject = translateId(quad.getSubject());
        final Node object = translateId(quad.getObject());
        return Quad.create(quad.getGraph(), subject, quad.getPredicate(), object);
    }

    private Quad translateBinaryQuad(final Quad quad, final boolean isDelete, final boolean isUpdateBlock) {
        Node subject = translateId(quad.getSubject());
        final Node object = translateId(quad.getObject());
        if (resourceId.isDescription() && (
                subject.getURI().equals(resourceId.getFullDescribedId()) ||
                        subject.getURI().equals(resourceId.getFullId()))) {
            // Deletes use variables to ensure we catch triples with both binary and description as subject.
            if (isDelete) {
                // We are not processing an UpdateData block (i.e INSERT DATA {} or DELETE DATA {}), use variables
                if (!isUpdateBlock) {
                    subject = Var.alloc(CUSTOM_SPARQL_VARIABLE);
                    resourceVariableUpdate = true;
                } else if (subject.getURI().equals(resourceId.getFullDescribedId())) {
                    // We are processing an UpdateData block, so we must include two delete statements, one for
                    // the binary and one for the description.
                    subject = NodeFactory.createURI(resourceId.getFullId());
                } else {
                    subject = NodeFactory.createURI(resourceId.getFullDescribedId());
                }
            } else {
                // For insert just make sure to use the binary's ID.
                subject = NodeFactory.createURI(resourceId.getFullDescribedId());
            }
        }
        return Quad.create(quad.getGraph(), subject, quad.getPredicate(), object);
    }

    /**
     * Translate the subject and object of a triple from external URIs to internal IDs.
     * @param triple the triple to translate
     * @return the translated triple.
     */
    private Triple translateTriple(final Triple triple) {
        final Node subject = translateId(triple.getSubject());
        final Node object = translateId(triple.getObject());
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
            final String newUri = idTranslator.translateUri(externalId);
            return NodeFactory.createURI(newUri);
        }
        return externalNode;
    }

}
