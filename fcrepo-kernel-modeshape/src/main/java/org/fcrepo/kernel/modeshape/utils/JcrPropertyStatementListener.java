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
import org.fcrepo.kernel.api.functions.Converter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.rdf.JcrRdfTools;

import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.listeners.StatementListener;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.vocabulary.RDF;

import java.util.ArrayList;
import java.util.List;

/**
 * Listen to Jena statement events, and when the statement is changed in the
 * graph store, make the change within JCR as well.
 *
 * @author awoods
 */
public class JcrPropertyStatementListener extends StatementListener {

    private static final Logger LOGGER = getLogger(JcrPropertyStatementListener.class);

    private final JcrRdfTools jcrRdfTools;

    private final Converter<Resource, String> idTranslator;

    private final List<Exception> exceptions;

    private final Node topic;

    /**
     * Construct a statement listener within the given session
     *
     * @param idTranslator the id translator
     * @param session the session
     * @param topic the topic of the RDF statement
     */
    public JcrPropertyStatementListener(final Converter<Resource, String> idTranslator,
                                        final Session session, final Node topic) {
        this(idTranslator, new JcrRdfTools(idTranslator, session), topic);
    }

    /**
     * Construct a statement listener within the given session
     *
     * @param idTranslator the id translator
     * @param jcrRdfTools the jcr rdf tools
     * @param topic the topic of the RDF statement
     */
    public JcrPropertyStatementListener(final Converter<Resource, String> idTranslator,
                                        final JcrRdfTools jcrRdfTools, final Node topic) {
        super();
        this.idTranslator = idTranslator;
        this.jcrRdfTools = jcrRdfTools;
        this.exceptions = new ArrayList<>();
        this.topic = topic;
    }

    /**
     * When a statement is added to the graph, serialize it to a JCR property
     *
     * @param input the input statement
     */
    @Override
    public void addedStatement(final Statement input) {

        final Resource subject = input.getSubject();
        try {
            validateSubject(subject);
            LOGGER.debug(">> adding statement {}", input);

            final Statement s = jcrRdfTools.skolemize(input);

            final FedoraResource resource = jcrRdfTools.resourceTranslator().apply(s.getSubject());

            // special logic for handling rdf:type updates.
            // if the object is an already-existing mixin, update
            // the node's mixins. If it isn't, just treat it normally.
            final Property property = s.getPredicate();
            final RDFNode objectNode = s.getObject();
            if (property.equals(RDF.type) && objectNode.isResource()) {
                final Resource mixinResource = objectNode.asResource();
                jcrRdfTools.addMixin(resource, mixinResource, input.getModel().getNsPrefixMap());
                return;
            }

            jcrRdfTools.addProperty(resource, property, objectNode, input.getModel().getNsPrefixMap());
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
        try {
            // if it's not about the right kind of node, ignore it.
            final Resource subject = s.getSubject();
            validateSubject(subject);
            LOGGER.trace(">> removing statement {}", s);

            final FedoraResource resource = jcrRdfTools.resourceTranslator().apply(subject);

            // special logic for handling rdf:type updates.
            // if the object is an already-existing mixin, update
            // the node's mixins. If it isn't, just treat it normally.
            final Property property = s.getPredicate();
            final RDFNode objectNode = s.getObject();

            if (property.equals(RDF.type) && objectNode.isResource()) {
                final Resource mixinResource = objectNode.asResource();
                jcrRdfTools.removeMixin(resource, mixinResource, s.getModel().getNsPrefixMap());
                return;
            }

            jcrRdfTools.removeProperty(resource, property, objectNode, s.getModel().getNsPrefixMap());
        } catch (final ConstraintViolationException e) {
            throw e;
        } catch (final RepositoryException | RepositoryRuntimeException e) {
            exceptions.add(e);
        }

    }

    /**
     * If it's not the right kind of node, throw an appropriate unchecked exception.
     *
     * @param subject
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
