/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.utils;

import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Joiner;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.MalformedRdfException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.slf4j.Logger;

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

    private final IdentifierConverter<Resource, FedoraResource> idTranslator;

    private final List<String> exceptions;

    /**
     * Construct a statement listener within the given session
     *
     * @param idTranslator
     * @param session
     */
    public JcrPropertyStatementListener(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                        final Session session) {
        this(idTranslator, new JcrRdfTools(idTranslator, session));
    }

    /**
     * Construct a statement listener within the given session
     *
     * @param idTranslator
     */
    public JcrPropertyStatementListener(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                        final JcrRdfTools jcrRdfTools) {
        super();
        this.idTranslator = idTranslator;
        this.jcrRdfTools = jcrRdfTools;
        this.exceptions = new ArrayList<>();
    }

    /**
     * When a statement is added to the graph, serialize it to a JCR property
     *
     * @param input
     */
    @Override
    public void addedStatement(final Statement input) {
        LOGGER.debug(">> added statement {}", input);

        try {
            final Resource subject = input.getSubject();

            // if it's not about a node, ignore it.
            if (!idTranslator.inDomain(subject) && !subject.isAnon()) {
                return;
            }

            final Statement s = jcrRdfTools.skolemize(idTranslator, input);

            final FedoraResource resource = idTranslator.convert(s.getSubject());

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
        } catch (final RepositoryException | RepositoryRuntimeException e) {
            exceptions.add(e.getMessage());
        }

    }

    /**
     * When a statement is removed, remove it from the JCR properties
     *
     * @param s
     */
    @Override
    public void removedStatement(final Statement s) {
        LOGGER.trace(">> removed statement {}", s);

        try {
            final Resource subject = s.getSubject();

            // if it's not about a node, we don't care.
            if (!idTranslator.inDomain(subject)) {
                return;
            }

            final FedoraResource resource = idTranslator.convert(subject);

            // special logic for handling rdf:type updates.
            // if the object is an already-existing mixin, update
            // the node's mixins. If it isn't, just treat it normally.
            final Property property = s.getPredicate();
            final RDFNode objectNode = s.getObject();

            if (property.equals(RDF.type) && objectNode.isResource()) {
                final Resource mixinResource = objectNode.asResource();
                try {
                    jcrRdfTools.removeMixin(resource, mixinResource, s.getModel().getNsPrefixMap());
                } catch (final RepositoryException e) {
                    // TODO
                }
                return;
            }

            jcrRdfTools.removeProperty(resource, property, objectNode, s.getModel().getNsPrefixMap());

        } catch (final RepositoryException | RepositoryRuntimeException e) {
            exceptions.add(e.getMessage());
        }

    }

    /**
     * Assert that no exceptions were thrown while this listener was processing change
     */
    public void assertNoExceptions() throws MalformedRdfException {
        if (!exceptions.isEmpty()) {
            throw new MalformedRdfException(Joiner.on("\n").join(exceptions));
        }
    }
}
