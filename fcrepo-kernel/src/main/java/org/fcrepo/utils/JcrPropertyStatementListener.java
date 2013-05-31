/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import static org.fcrepo.utils.JcrRdfTools.getNodeFromGraphSubject;
import static org.fcrepo.utils.JcrRdfTools.getPropertyNameFromPredicate;
import static org.fcrepo.utils.JcrRdfTools.isFedoraGraphSubject;
import static org.fcrepo.utils.NodePropertiesTools.appendOrReplaceNodeProperty;
import static org.fcrepo.utils.NodePropertiesTools.getPropertyType;
import static org.fcrepo.utils.NodePropertiesTools.removeNodeProperty;
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
 * @todo Add Documentation.
 * @author Chris Beer
 * @date May 3, 2013
 */
public class JcrPropertyStatementListener extends StatementListener {

    private Problems problems;

    private static final Logger LOGGER =
        getLogger(JcrPropertyStatementListener.class);

    private final GraphSubjects subjects;

    private final Session session;

    /**
     * @todo Add Documentation.
     */
    public JcrPropertyStatementListener(final GraphSubjects subjects,
                                        final Session session)
        throws RepositoryException {
        this.session = session;
        this.subjects = subjects;
        this.problems = new SimpleProblems();
    }

    /**
     * When a statement is added to the graph, serialize it to a JCR property
     * @param s
     */
    @Override
    public void addedStatement( Statement s ) {
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
                    JcrRdfTools.createValue(subjectNode,
                                            s.getObject(),
                                            getPropertyType(subjectNode,
                                                            propertyName));
                appendOrReplaceNodeProperty(subjectNode, propertyName, v);
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * When a statement is removed, remove it from the JCR properties
     * @param s
     */
    @Override
    public void removedStatement( Statement s ) {
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
                    JcrRdfTools.createValue(subjectNode,
                                            s.getObject(),
                                            getPropertyType(subjectNode,
                                                            propertyName));
                removeNodeProperty(subjectNode, propertyName, v);
            }

        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

    }

    private boolean validateModificationsForPropertyName(final Node subjectNode,
                                                         final String propertyName) {
        if (propertyName.startsWith("jcr:") ||
            propertyName.startsWith("fedora:")) {
            problems.addError(org.modeshape.jcr.JcrI18n.couldNotStoreProperty,
                              "", subjectNode, propertyName);
            return false;
        }

        return true;
    }

    /**
     * Get a list of any problems from trying to apply the statement changes to
     * the node's properties
     * @return
     */
    public Problems getProblems() {
        return problems;
    }

    private Session getSession() {
        return this.session;
    }
}
