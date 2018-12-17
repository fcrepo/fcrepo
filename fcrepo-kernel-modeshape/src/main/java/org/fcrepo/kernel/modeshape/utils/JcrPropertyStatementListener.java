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
package org.fcrepo.kernel.modeshape.utils;

import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.IncorrectTripleSubjectException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.OutOfDomainSubjectException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.rdf.JcrRdfTools;

import org.slf4j.Logger;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.listeners.StatementListener;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.RDF;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listen to Jena statement events, and when the statement is changed in the
 * graph store, make the change within JCR as well.
 *
 * @author awoods
 */
public class JcrPropertyStatementListener extends StatementListener {

    private static final Logger LOGGER = getLogger(JcrPropertyStatementListener.class);

    private enum Operation {
        ADD, REMOVE
    }

    /*
     * map statement hashcodes to the last successful operation on the statement
     * to prevent large, redundant SPARQL solution sets from exhausting the heap
     */
    private final Map<Statement,Operation> statements;

    private final JcrRdfTools jcrRdfTools;

    private final IdentifierConverter<Resource, FedoraResource> idTranslator;

    private final List<Exception> exceptions;

    private final Node topic;

    /**
     * Construct a statement listener within the given session
     *
     * @param idTranslator the id translator
     * @param session the session
     * @param topic the topic of the RDF statement
     */
    public JcrPropertyStatementListener(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                        final Session session, final Node topic) {
        this(idTranslator, new JcrRdfTools(idTranslator, session), topic);
    }

    /**
     * Construct a statement listener within the given session
     * This listener is not reusable across requests.
     *
     * @param idTranslator the id translator
     * @param jcrRdfTools the jcr rdf tools
     * @param topic the topic of the RDF statement
     */
    public JcrPropertyStatementListener(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                        final JcrRdfTools jcrRdfTools, final Node topic) {
        super();
        this.idTranslator = idTranslator;
        this.jcrRdfTools = jcrRdfTools;
        this.exceptions = new ArrayList<>();
        this.topic = topic;
        this.statements = new ConcurrentHashMap<>();
    }


    /**
     * When a statement is added to the graph, serialize it to a JCR property
     *
     * @param input the input statement
     */
    @Override
    public void addedStatement(final Statement input) {
        if (Operation.ADD == statements.get(input)) {
            return;
        }
        try {
            final Resource subject = input.getSubject();
            validateSubject(subject);
            LOGGER.debug(">> adding statement {}", input);

            final Statement s = jcrRdfTools.skolemize(idTranslator, input, topic.toString());

            final FedoraResource resource = idTranslator.convert(s.getSubject());
            final FedoraResource description = resource.getDescription();

            // special logic for handling rdf:type updates.
            // if the object is an already-existing mixin, update
            // the node's mixins. If it isn't, just treat it normally.
            final Property property = s.getPredicate();
            final RDFNode objectNode = s.getObject();
            if (property.equals(RDF.type) && objectNode.isResource()) {
                final Resource mixinResource = objectNode.asResource();
                jcrRdfTools.addMixin(description, mixinResource, input.getModel().getNsPrefixMap());
                statements.put(input, Operation.ADD);
                return;
            }

            jcrRdfTools.addProperty(description, property, objectNode, input.getModel().getNsPrefixMap());
            statements.put(input, Operation.ADD);
        } catch (final ConstraintViolationException e) {
            throw e;
        } catch (final javax.jcr.AccessDeniedException e) {
            throw new AccessDeniedException(e);
        } catch (final RepositoryException | RepositoryRuntimeException e) {
            exceptions.add(e);
        }

    }

    /**
     * When a statement is removed, remove it from the JCR properties
     *
     * @param s the given statement
     */
    @Override
    public void removedStatement(final Statement s) {
        if (Operation.REMOVE == statements.get(s)) {
            return;
        }
        try {
            // if it's not about the right kind of node, ignore it.
            final Resource subject = s.getSubject();
            validateSubject(subject);
            LOGGER.trace(">> removing statement {}", s);

            final FedoraResource resource = idTranslator.convert(subject);
            final FedoraResource description = resource.getDescription();

            // special logic for handling rdf:type updates.
            // if the object is an already-existing mixin, update
            // the node's mixins. If it isn't, just treat it normally.
            final Property property = s.getPredicate();
            final RDFNode objectNode = s.getObject();

            if (property.equals(RDF.type) && objectNode.isResource()) {
                final Resource mixinResource = objectNode.asResource();
                jcrRdfTools.removeMixin(description, mixinResource, s.getModel().getNsPrefixMap());
                statements.put(s, Operation.REMOVE);
                return;
            }

            jcrRdfTools.removeProperty(description, property, objectNode, s.getModel().getNsPrefixMap());
            statements.put(s, Operation.REMOVE);
        } catch (final ConstraintViolationException e) {
            throw e;
        } catch (final RepositoryException | RepositoryRuntimeException e) {
            exceptions.add(e);
        }

    }

    /**
     * If it's not the right kind of node, throw an appropriate unchecked exception.
     *
     * @param subject to validate
     */
    private void validateSubject(final Resource subject) {
        final String subjectURI = subject.getURI();
        // blank nodes are okay
        if (!subject.isAnon()) {
            // hash URIs with the same base as the topic are okay
            final int hashIndex = subjectURI.lastIndexOf("#");
            if (!(hashIndex > 0 && topic.getURI().equals(subjectURI.substring(0, hashIndex)))) {
                // the topic itself is okay
                if (!topic.equals(subject.asNode())) {
                    // it's a bad subject, but it could still be in-domain
                    if (idTranslator.inDomain(subject)) {
                        LOGGER.error("{} is not in the topic of this RDF, which is {}.", subject, topic);
                        throw new IncorrectTripleSubjectException(subject +
                                " is not in the topic of this RDF, which is " + topic);
                    }
                    // it's not even in the right domain!
                    LOGGER.error("subject ({}) is not in repository domain.", subject);
                    throw new OutOfDomainSubjectException(subject.asNode());
                }
            }
        }
    }

    /**
     * Assert that no exceptions were thrown while this listener was processing change
     */
    public void assertNoExceptions() {
        if (!exceptions.isEmpty()) {
            throw new MalformedRdfException(exceptions.stream().map(Exception::getMessage).collect(joining("\n")));
        }
    }
}
