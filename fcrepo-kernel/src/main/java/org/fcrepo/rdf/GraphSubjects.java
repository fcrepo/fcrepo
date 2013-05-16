package org.fcrepo.rdf;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.hp.hpl.jena.rdf.model.Resource;

public interface GraphSubjects {
    /**
     * Translate a JCR node into an RDF Resource
     * @param node
     * @return an RDF URI resource
     * @throws RepositoryException
     */
    public Resource getGraphSubject(final Node node) throws RepositoryException;
    
    /**
     * Translate an RDF resource into a JCR node
     * @param session
     * @param subject an RDF URI resource
     * @return a JCR node, or null if one couldn't be found
     * @throws RepositoryException
     */
    public Node getNodeFromGraphSubject(final Session session, final Resource subject)
    		throws RepositoryException;
    
    /**
     * Predicate for determining whether this {@link Node} is a Fedora object.
     */
    public boolean isFedoraGraphSubject(final Resource subject);

}
