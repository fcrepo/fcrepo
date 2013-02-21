
package org.fcrepo;

import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.modeshape.jcr.api.JcrTools;

public class FedoraObject extends JcrTools {

    private Node node;

    public FedoraObject(Node n) {
        this.node = n;
    }

    public FedoraObject(Session session, String path)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        this.node = findOrCreateNode(session, path, NT_FOLDER);
        node.addMixin("fedora:object");
        node.addMixin("fedora:owned");
        node.setProperty("fedora:ownerId", session.getUserID());
        node.setProperty("jcr:lastModified", Calendar.getInstance());
        node.setProperty("dc:identifier", new String[] {node.getIdentifier(),
                node.getName()});
    }

    public String getName() throws RepositoryException {
        return node.getName();
    }

    public Node getNode() {
        return node;
    }

}
