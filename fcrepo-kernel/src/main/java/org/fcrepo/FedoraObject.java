
package org.fcrepo;

import static com.yammer.metrics.MetricRegistry.name;
import static org.fcrepo.services.RepositoryService.metrics;
import static org.fcrepo.utils.FedoraTypesUtils.isOwned;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.jcr.api.JcrTools;

import com.yammer.metrics.Timer;

/**
 * An abstraction that represents a Fedora Object backed by
 * a JCR node.
 * 
 * @author ajs6f
 *
 */
public class FedoraObject extends JcrTools implements FedoraJcrTypes {

    private Node node;

    final static Timer timer = metrics.timer(name(FedoraObject.class,
            "FedoraObject"));

    public FedoraObject(Node n) {
        this.node = n;
    }

    public FedoraObject(Session session, String path)
            throws RepositoryException {

        final Timer.Context context = timer.time();

        try {
            this.node = findOrCreateNode(session, path, NT_FOLDER);
            node.addMixin(FEDORA_OBJECT);
            node.addMixin(FEDORA_OWNED);
            node.setProperty(FEDORA_OWNERID, session.getUserID());
            node.setProperty(JCR_LASTMODIFIED, Calendar.getInstance());
            node.setProperty(DC_IDENTIFIER, new String[] {node.getIdentifier(),
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
