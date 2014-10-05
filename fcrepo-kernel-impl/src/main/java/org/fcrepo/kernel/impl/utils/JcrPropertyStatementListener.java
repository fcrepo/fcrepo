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

import com.google.common.annotations.VisibleForTesting;
import com.hp.hpl.jena.rdf.model.AnonId;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.listeners.StatementListener;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.vocabulary.RDF;

import java.util.HashMap;

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

    private Model problems;

    private final IdentifierConverter<Resource,Node> subjects;

    private final Session session;

    private static final JcrTools jcrTools = new JcrTools();

    /**
     * Return a Listener given the subject factory and JcrSession.
     * @param subjects
     * @param session
     * @param problemModel
     * @return JcrPropertyStatementListener for the given subject factory and JcrSession
     */
    public static JcrPropertyStatementListener getListener(
            final IdentifierConverter<Resource,Node> subjects, final Session session, final Model problemModel) {
        return new JcrPropertyStatementListener(subjects,
                session,
                problemModel,
                JcrRdfTools.withContext(subjects, session));
    }

    /**
     * Return a Listener given the subject factory and JcrSession.
     * @param subjects
     * @param session
     * @param problemModel
     * @return JcrPropertyStatementListener for the given subject factory and JcrSession
     */
    @VisibleForTesting
    public static JcrPropertyStatementListener getListener(final IdentifierConverter<Resource,Node> subjects,
                                                           final Session session,
                                                           final Model problemModel,
                                                           final JcrRdfTools tools) {
        return new JcrPropertyStatementListener(subjects, session, problemModel, tools);
    }

    /**
     * Construct a statement listener within the given session
     *
     * @param subjects
     * @param session
     */
    private JcrPropertyStatementListener(final IdentifierConverter<Resource,Node> subjects,
            final Session session, final Model problems, final JcrRdfTools tools) {
        super();
        this.session = session;
        this.subjects = subjects;
        this.problems = problems;
        this.jcrRdfTools = tools;
        this.skolemizedBnodeMap = new HashMap<>();
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
            throw new RepositoryRuntimeException(e);
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
            throw new RepositoryRuntimeException(e);
        }

    }

    /**
     * Get a list of any problems from trying to apply the statement changes to
     * the node's properties
     *
     * @return model containing any problems from applying the statement changes
     */
    public Model getProblems() {
        return problems;
    }

}
