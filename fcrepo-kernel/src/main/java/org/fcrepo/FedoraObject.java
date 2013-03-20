
package org.fcrepo;

import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OWNED;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OWNERID;
import static org.fcrepo.utils.FedoraTypesUtils.isOwned;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import org.fcrepo.services.RepositoryService;
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

    final static Timer timer = RepositoryService.metrics.timer(MetricRegistry.name(FedoraObject.class, "FedoraObject"));

    public FedoraObject(Node n) {
        this.node = n;
    }

    public FedoraObject(Session session, String path)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {

        final Timer.Context context = timer.time();

        try {
        this.node = findOrCreateNode(session, path, NT_FOLDER);
        node.addMixin("fedora:object");
        node.addMixin("fedora:owned");
        node.setProperty("fedora:ownerId", session.getUserID());
        node.setProperty("jcr:lastModified", Calendar.getInstance());
        node.setProperty("dc:identifier", new String[] {node.getIdentifier(),
                node.getName()});
        } finally {
            context.stop();
        }
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

    public String getOwnerId() throws RepositoryException {
        if (isOwned.apply(node))
            return node.getProperty(FEDORA_OWNERID).getString();
        else
            return null;
    }

    public void setOwnerId(String ownerId) throws RepositoryException {
        if (isOwned.apply(node))
            node.setProperty(FEDORA_OWNERID, ownerId);
        else {
            node.addMixin(FEDORA_OWNED);
            node.setProperty(FEDORA_OWNERID, ownerId);
        }
    }

}
