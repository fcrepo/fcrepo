
package org.fcrepo;

import static com.google.common.base.Joiner.on;
import static org.fcrepo.utils.FedoraJcrTypes.DC_TITLE;
import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.value2string;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.modeshape.jcr.api.JcrTools;

/**
 * An abstraction that represents a Fedora Object backed by
 * a JCR node.
 * 
 * @author ajs6f
 *
 */
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

    /**
     * @return The JCR name of the node that backs this object.
     * @throws RepositoryException
     */
    public String getName() throws RepositoryException {
        return node.getName();
    }

    /**
     * @return The JCR node that backs this object.
     */
    public Node getNode() {
        return node;
    }


    /**
     * @return A label associated with this object. Normally stored in a String-valued dc:title property.
     * @throws RepositoryException
     */
    public String getLabel() throws RepositoryException {
        if (node.hasProperty(DC_TITLE)) {

            Property labels = node.getProperty(DC_TITLE);
            String label;
            if (!labels.isMultiple()) {
                label = node.getProperty(DC_TITLE).getString();
            } else {
                label = on('/').join(map(labels.getValues(), value2string));
            }
            return label;
        } else {
            return "";
        }

    }

    public void setLabel(String label) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        node.setProperty(DC_TITLE, label);
        node.getSession().save();
    }

}
