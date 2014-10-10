/**
 * Copyright 2014 DuraSpace, Inc.
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

import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Joiner;
import com.hp.hpl.jena.rdf.model.AnonId;
import org.fcrepo.kernel.exception.MalformedRdfException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.fcrepo.kernel.impl.services.ObjectServiceImpl;
import org.fcrepo.kernel.services.ObjectService;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.listeners.StatementListener;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.vocabulary.RDF;

import java.util.ArrayList;
import java.util.HashMap;
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

    private final HashMap<AnonId, Node> skolemizedBnodeMap;

    private final IdentifierConverter<Resource,Node> subjects;

    private final Session session;

    private static final JcrTools jcrTools = new JcrTools();

    private final List<String> exceptions;

    private static final ObjectService objectService = new ObjectServiceImpl();

    /**
     * Construct a statement listener within the given session
     *
     * @param subjects
     * @param session
     */
    public JcrPropertyStatementListener(final IdentifierConverter<Resource,Node> subjects, final Session session) {
        this(subjects, session, new JcrRdfTools(subjects, session));
    }

    /**
     * Construct a statement listener within the given session
     *
     * @param subjects
     * @param session
     */
    public JcrPropertyStatementListener(final IdentifierConverter<Resource,Node> subjects,
                                        final Session session,
                                        final JcrRdfTools jcrRdfTools) {
        super();
        this.session = session;
        this.subjects = subjects;
        this.jcrRdfTools = jcrRdfTools;
        this.skolemizedBnodeMap = new HashMap<>();
        this.exceptions = new ArrayList<>();
    }

    /**
     * When a statement is added to the graph, serialize it to a JCR property
     *
     * @param s
     */
    @Override
    public void addedStatement(final Statement s) {
        LOGGER.debug(">> added statement {}", s);

        try {
            final Resource subject = s.getSubject();

            // if it's not about a node, ignore it.
            if (!subjects.inDomain(subject) && !subject.isAnon()) {
                return;
            }

            final Node subjectNode;

            if (subject.isAnon()) {
                if (skolemizedBnodeMap.containsKey(subject.getId())) {
                    subjectNode = skolemizedBnodeMap.get(subject.getId());
                } else {
                    subjectNode = jcrTools.findOrCreateNode(session, "/.well-known/genid/" + randomUUID().toString());
                    subjectNode.addMixin("fedora:blanknode");
                    skolemizedBnodeMap.put(subject.getId(), subjectNode);
                }
            } else if (subject.getURI().contains("#")) {
                final String absPath = subjects.asString(subject);
                subjectNode = objectService.findOrCreateObject(session, absPath).getNode();
            } else {
                subjectNode = subjects.convert(subject);
            }

            // special logic for handling rdf:type updates.
            // if the object is an already-existing mixin, update
            // the node's mixins. If it isn't, just treat it normally.
            final Property property = s.getPredicate();
            final RDFNode objectNode = s.getObject();
            if (property.equals(RDF.type) && objectNode.isResource()) {
                final Resource mixinResource = objectNode.asResource();
                jcrRdfTools.addMixin(subjectNode, mixinResource, s.getModel().getNsPrefixMap());
                return;
            }

            jcrRdfTools.addProperty(subjectNode, property, objectNode, s.getModel().getNsPrefixMap());
        } catch (final RepositoryException e) {
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
            if (!subjects.inDomain(subject)) {
                return;
            }

            final Node subjectNode = subjects.convert(subject);

            // special logic for handling rdf:type updates.
            // if the object is an already-existing mixin, update
            // the node's mixins. If it isn't, just treat it normally.
            final Property property = s.getPredicate();
            final RDFNode objectNode = s.getObject();

            if (property.equals(RDF.type) && objectNode.isResource()) {
                final Resource mixinResource = objectNode.asResource();
                try {
                    jcrRdfTools.removeMixin(subjectNode, mixinResource, s.getModel().getNsPrefixMap());
                } catch (final RepositoryException e) {
                    // TODO
                }
                return;
            }

            jcrRdfTools.removeProperty(subjectNode, property, objectNode, s.getModel().getNsPrefixMap());

        } catch (final RepositoryException e) {
            exceptions.add(e.getMessage());
        }

    }

    /**
     * Assert that no exceptions were thrown while this listener was processing change
     */
    public void assertNoExceptions() {
        if (!exceptions.isEmpty()) {
            throw new MalformedRdfException(Joiner.on("\n").join(exceptions));
        }
    }
}
