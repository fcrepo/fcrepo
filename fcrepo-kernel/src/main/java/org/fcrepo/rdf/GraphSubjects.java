/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.rdf;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @todo Add Documentation.
 * @author barmintor
 * @date May 15, 2013
 */
public interface GraphSubjects {
    /**
     * Translate a JCR node into an RDF Resource
     * @param node
     * @return an RDF URI resource
     * @throws RepositoryException
     */
    Resource getGraphSubject(final Node node) throws RepositoryException;

    /**
     * Translate an RDF resource into a JCR node
     * @param session
     * @param subject an RDF URI resource
     * @return a JCR node, or null if one couldn't be found
     * @throws RepositoryException
     */
    Node getNodeFromGraphSubject(final Session session, final Resource subject)
        throws RepositoryException;

    /**
     * Predicate for determining whether this {@link Node} is a Fedora object.
     */
    boolean isFedoraGraphSubject(final Resource subject);

}
