/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.utils;

import static com.google.common.base.Throwables.propagate;
import static org.fcrepo.utils.JcrRdfTools.createValue;
import static org.fcrepo.utils.JcrRdfTools.getNodeFromGraphSubject;
import static org.fcrepo.utils.JcrRdfTools.getPropertyNameFromPredicate;
import static org.fcrepo.utils.JcrRdfTools.isFedoraGraphSubject;
import static org.fcrepo.utils.NodePropertiesTools.appendOrReplaceNodeProperty;
import static org.fcrepo.utils.NodePropertiesTools.getPropertyType;
import static org.fcrepo.utils.NodePropertiesTools.removeNodeProperty;
import static org.modeshape.jcr.JcrI18n.couldNotStoreProperty;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.fcrepo.rdf.GraphSubjects;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.listeners.StatementListener;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Listen to Jena statement events, and when the statement is changed in the
 * graph store, make the change within JCR as well.
 */
public class JcrPropertyStatementListener extends StatementListener {

    private Problems problems;

    private static final Logger LOGGER =
            getLogger(JcrPropertyStatementListener.class);

    private final GraphSubjects subjects;

    private final Session session;

    /**
     * Construct a statement listener within the given session
     * 
     * @param subjects
     * @param session
     * @throws RepositoryException
     */
    public JcrPropertyStatementListener(final GraphSubjects subjects,
            final Session session) throws RepositoryException {
        this.session = session;
        this.subjects = subjects;
        this.problems = new SimpleProblems();
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
            if (!isFedoraGraphSubject(subjects, subject)) {
                return;
            }

            final Node subjectNode =
                    getNodeFromGraphSubject(subjects, getSession(), subject);

            // extract the JCR propertyName from the predicate
            final String propertyName =
                    getPropertyNameFromPredicate(subjectNode, s.getPredicate());

            if (validateModificationsForPropertyName(subjectNode, propertyName)) {
                final Value v =
                        createValue(subjectNode, s.getObject(),
                                getPropertyType(subjectNode, propertyName));
                appendOrReplaceNodeProperty(subjectNode, propertyName, v);
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
            if (!isFedoraGraphSubject(subjects, subject)) {
                return;
            }

            final Node subjectNode =
                    getNodeFromGraphSubject(subjects, getSession(), subject);

            final String propertyName =
                    getPropertyNameFromPredicate(subjectNode, s.getPredicate());

            // if the property doesn't exist, we don't need to worry about it.
            if (subjectNode.hasProperty(propertyName) &&
                    validateModificationsForPropertyName(subjectNode,
                            propertyName)) {
                final Value v =
                        createValue(subjectNode, s.getObject(),
                                getPropertyType(subjectNode, propertyName));
                removeNodeProperty(subjectNode, propertyName, v);
            }

        } catch (final RepositoryException e) {
            throw propagate(e);
        }

    }

    private boolean validateModificationsForPropertyName(
            final Node subjectNode, final String propertyName) {
        if (propertyName.startsWith("jcr:") ||
                propertyName.startsWith("fedora:")) {
            problems.addError(couldNotStoreProperty, "", subjectNode,
                    propertyName);
            return false;
        }

        return true;
    }

    /**
     * Get a list of any problems from trying to apply the statement changes to
     * the node's properties
     * 
     * @return
     */
    public Problems getProblems() {
        return problems;
    }

    private Session getSession() {
        return this.session;
    }
}
