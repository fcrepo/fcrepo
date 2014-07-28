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

import static com.google.common.base.Throwables.propagate;
import static java.net.URLDecoder.decode;
import static java.util.UUID.randomUUID;
import static org.fcrepo.kernel.RdfLexicon.COULD_NOT_STORE_PROPERTY;
import static org.fcrepo.kernel.RdfLexicon.LDP_NAMESPACE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.UnsupportedEncodingException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import com.hp.hpl.jena.rdf.model.AnonId;
import org.apache.commons.lang.StringUtils;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.JcrRdfTools;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.listeners.StatementListener;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
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

    private NodePropertiesTools propertiesTools = new NodePropertiesTools();

    private Model problems;

    private final IdentifierTranslator subjects;

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
            final IdentifierTranslator subjects, final Session session, final Model problemModel) {
        return new JcrPropertyStatementListener(subjects, session, problemModel);
    }

    /**
     * Construct a statement listener within the given session
     *
     * @param subjects
     * @param session
     */
    private JcrPropertyStatementListener(final IdentifierTranslator subjects,
            final Session session, final Model problems) {
        super();
        this.session = session;
        this.subjects = subjects;
        this.problems = problems;
        this.jcrRdfTools = JcrRdfTools.withContext(subjects, session);
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
            if (!subjects.isFedoraGraphSubject(subject) && !subject.isAnon()) {
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
                String path = null;
                try {
                    path = decode(subjects.getPathFromSubject(subject), "UTF-8");
                } catch ( UnsupportedEncodingException ex ) {
                    LOGGER.warn("Required encoding (UTF-8) not supported, trying undecoded path",ex);
                    path = subjects.getPathFromSubject(subject);
                }
                subjectNode = session.getNode(path);
            }

            // special logic for handling rdf:type updates.
            // if the object is an already-existing mixin, update
            // the node's mixins. If it isn't, just treat it normally.
            if (s.getPredicate().equals(RDF.type)
                    && s.getObject().isResource()) {
                final Resource mixinResource = s.getObject().asResource();

                if (mixinResource.getNameSpace().equals(LDP_NAMESPACE)) {
                    return;
                }

                try {
                    final String namespacePrefix = session.getNamespacePrefix(mixinResource.getNameSpace());
                    final String mixinName = namespacePrefix + ":" + mixinResource.getLocalName();

                    if (FedoraTypesUtils.getNodeTypeManager(subjectNode).hasNodeType(mixinName)) {

                        if (subjectNode.canAddMixin(mixinName)) {
                            subjectNode.addMixin(mixinName);
                        } else {
                            problems.add(subject, COULD_NOT_STORE_PROPERTY, s.getPredicate().getURI());
                        }

                        return;

                    }
                } catch (final NamespaceException e) {
                    LOGGER.trace("Unable to resolve registered namespace for resource {}: {}", mixinResource, e);
                }
            }

            // extract the JCR propertyName from the predicate
            final String propertyName =
                    jcrRdfTools.getPropertyNameFromPredicate(subjectNode,
                                                             s.getPredicate(),
                                                             s.getModel()
                                                                     .getNsPrefixMap());

            if (validateModificationsForPropertyName(
                    subject, subjectNode, s.getPredicate())) {
                final Value v =
                    jcrRdfTools.createValue(subjectNode, s.getObject(),
                            propertiesTools.getPropertyType(subjectNode,
                                    propertyName));
                propertiesTools.appendOrReplaceNodeProperty(subjects,
                        subjectNode, propertyName, v);
            }
        } catch (final RepositoryException e) {
            throw propagate(e);
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
            if (!subjects.isFedoraGraphSubject(subject)) {
                return;
            }

            final Node subjectNode = session.getNode(subjects.getPathFromSubject(subject));

            // special logic for handling rdf:type updates.
            // if the object is an already-existing mixin, update
            // the node's mixins. If it isn't, just treat it normally.
            if (s.getPredicate().equals(RDF.type)
                    && s.getObject().isResource()) {
                final Resource mixinResource = s.getObject().asResource();
                final String errorPrefix = "Error removing node type";
                try {
                    final String namespacePrefix = session.getNamespacePrefix(mixinResource.getNameSpace());
                    final String mixinName = namespacePrefix + ":" + mixinResource.getLocalName();

                    if (FedoraTypesUtils.getNodeTypeManager(subjectNode).hasNodeType(mixinName)) {
                        try {
                            subjectNode.removeMixin(mixinName);
                        } catch (final RepositoryException e) {
                            LOGGER.info(
                                    "problem with removing <{}> <{}> <{}>: {}",
                                    subject.getURI(),
                                    RdfLexicon.COULD_NOT_STORE_PROPERTY,
                                    s.getPredicate().getURI(),
                                    e);
                            String errorMessage = e.getMessage();
                            if (StringUtils.isNotBlank(errorMessage)) {
                                errorMessage = errorPrefix + " '" + mixinName +
                                        "': \n" + e.getClass().getName() + ": " + errorMessage;
                            } else {
                                errorMessage = errorPrefix + " '" + mixinName +
                                        "': \n" + e.getClass().getName();
                            }
                            problems.add(subject, RdfLexicon.COULD_NOT_STORE_PROPERTY, errorMessage);
                        }
                        return;
                    }

                } catch (final NamespaceException e) {
                    LOGGER.trace("Unable to resolve registered namespace for resource {}: {}", mixinResource, e);

                    String errorMessage = e.getMessage();
                    if (StringUtils.isNotBlank(errorMessage)) {
                        errorMessage = errorPrefix + " " +
                                e.getClass().getName() + ": "  + errorMessage;
                    } else {
                        errorMessage = errorPrefix + ": " + e.getClass().getName();
                    }
                    problems.add(subject, RdfLexicon.COULD_NOT_STORE_PROPERTY, errorMessage);
                }

            }

            final String propertyName =
                jcrRdfTools.getPropertyNameFromPredicate(subjectNode, s.getPredicate());


            // if the property doesn't exist, we don't need to worry about it.
            if (subjectNode.hasProperty(propertyName) &&
                    validateModificationsForPropertyName(subject, subjectNode,
                                                            s.getPredicate())) {
                final Value v =
                    jcrRdfTools.createValue(subjectNode, s.getObject(),
                            propertiesTools.getPropertyType(subjectNode,
                                    propertyName));
                propertiesTools.removeNodeProperty(subjects, subjectNode,
                        propertyName, v);
            }

        } catch (final RepositoryException e) {
            throw propagate(e);
        }

    }

    private boolean validateModificationsForPropertyName(
            final Resource subject, final Node subjectNode, final Resource predicate) {
        if (jcrRdfTools.isInternalProperty(subjectNode, predicate)) {
            LOGGER.debug("problem with <{}> <{}> <{}>",
                         subject.getURI(),
                         RdfLexicon.COULD_NOT_STORE_PROPERTY,
                         predicate.getURI());
            problems.add(subject, RdfLexicon.COULD_NOT_STORE_PROPERTY, predicate.getURI());
            return false;
        }

        return true;
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
