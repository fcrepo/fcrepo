/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.rdf.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static org.fcrepo.utils.FedoraJcrTypes.FCR_CONTENT;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.jcr.api.JcrConstants;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * @todo Add Documentation.
 * @author barmintor
 * @date May 15, 2013
 */
public class DefaultGraphSubjects implements GraphSubjects {

    /**
     * @todo Add Documentation.
     */
    @Override
    public Resource getGraphSubject(Node node) throws RepositoryException {
        final String absPath = node.getPath();

        if (absPath.endsWith(JcrConstants.JCR_CONTENT)) {
            return ResourceFactory
                .createResource("info:fedora" +
                                absPath.replace(JcrConstants.JCR_CONTENT,
                                                FedoraJcrTypes.FCR_CONTENT));
        } else {
            return ResourceFactory.createResource("info:fedora" + absPath);
        }
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public Node getNodeFromGraphSubject(Session session, Resource subject)
        throws RepositoryException {
        if (!isFedoraGraphSubject(subject)) {
            return null;
        }

        final String absPath =
            subject.getURI().substring("info:fedora".length());

        if (absPath.endsWith(FCR_CONTENT)) {
            return session.getNode(absPath.replace(FedoraJcrTypes.FCR_CONTENT,
                                                   JcrConstants.JCR_CONTENT));
        } else if (session.nodeExists(absPath)) {
            return session.getNode(absPath);
        } else {
            return null;
        }
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public boolean isFedoraGraphSubject(Resource subject) {
        checkArgument(subject != null, "null cannot be a Fedora object!");
        assert(subject != null);

        return subject.isURIResource() &&
            subject.getURI().startsWith("info:fedora/");
    }

}
