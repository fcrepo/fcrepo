
package org.fcrepo;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkArgument;
import static com.yammer.metrics.MetricRegistry.name;
import static org.fcrepo.services.RepositoryService.metrics;
import static org.fcrepo.services.ServiceHelpers.getObjectSize;
import static org.fcrepo.utils.FedoraTypesUtils.isOwned;
import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.nodetype2name;
import static org.fcrepo.utils.FedoraTypesUtils.value2string;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Calendar;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.yammer.metrics.Timer;

/**
 * An abstraction that represents a Fedora Object backed by
 * a JCR node.
 * 
 * @author ajs6f
 *
 */
public class FedoraObject extends JcrTools implements FedoraJcrTypes {

    static final Logger logger = getLogger(FedoraObject.class);

    /**
     * Timer for the time to create/initialize a FedoraObject
     */
    static final Timer timer = metrics.timer(name(FedoraObject.class,
            "FedoraObject"));

    private Node node;

    /**
     * Construct a FedoraObject from an existing JCR Node
     * @param n an existing JCR node to treat as an fcrepo object
     */
    public FedoraObject(final Node n) {
        if (node != null) {
            logger.debug("Supporting a FedoraObject with null backing Node!");
        }
        node = n;
    }

    /**
     * Create or find a FedoraObject at the given path
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     * @throws RepositoryException
     */
    public FedoraObject(final Session session, final String path)
            throws RepositoryException {

        checkArgument(session != null, "null cannot create a Fedora object!");
        checkArgument(path != null, "Cannot create a Fedora object at null!");

        final Timer.Context context = timer.time();

        try {
            node = findOrCreateNode(session, path, NT_FOLDER);
            if (node.isNew()) {
                node.addMixin(FEDORA_OBJECT);
                node.addMixin(FEDORA_OWNED);
                node.setProperty(FEDORA_OWNERID, session.getUserID());
                node.setProperty(JCR_LASTMODIFIED, Calendar.getInstance());
                node.setProperty(DC_IDENTIFIER, new String[] {
                        node.getIdentifier(), node.getName()});
            }
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

    /**
     * Get the purported owner of this object
     * @return the owner id
     * @throws RepositoryException
     */
    public String getOwnerId() throws RepositoryException {
        if (isOwned.apply(node)) {
            return node.getProperty(FEDORA_OWNERID).getString();
        } else {
            return null;
        }
    }

    /**
     * Set the owner id for the object
     * @param ownerId
     * @throws RepositoryException
     */
    public void setOwnerId(final String ownerId) throws RepositoryException {
        if (isOwned.apply(node)) {
            node.setProperty(FEDORA_OWNERID, ownerId);
        } else {
            node.addMixin(FEDORA_OWNED);
            node.setProperty(FEDORA_OWNERID, ownerId);
        }
    }

    /**
     * Get the "label" (read: administrative title) of the object
     * @return object label
     * @throws RepositoryException
     */
    public String getLabel() throws RepositoryException {
        if (node.hasProperty(DC_TITLE)) {
            final Property dcTitle = node.getProperty(DC_TITLE);
            if (!dcTitle.isMultiple()) {
                return dcTitle.getString();
            } else {
                return on('/').join(map(dcTitle.getValues(), value2string));
            }
        }
        return null;
    }

    /**
     * Set the "label" (read: administrative title) for the object
     * @param label
     * @throws RepositoryException
     */
    public void setLabel(final String label) throws RepositoryException {
        node.setProperty(DC_TITLE, label);
    }

    /**
     * Get the date this object was created
     * @return
     * @throws RepositoryException
     */
    public String getCreated() throws RepositoryException {
        return node.getProperty(JCR_CREATED).getString();
    }

    /**
     * Get the date this object was last modified (whatever that means)
     * @return
     * @throws RepositoryException
     */
    public String getLastModified() throws RepositoryException {
        return node.getProperty(JCR_LASTMODIFIED).getString();
    }

    /**
     * Get the total size of this object and its datastreams
     * @return size in bytes
     * @throws RepositoryException
     */
    public long getSize() throws RepositoryException {
        return getObjectSize(node);
    }

    /**
     * Get the mixins this object uses
     * @return a collection of mixin names
     * @throws RepositoryException
     */
    public Collection<String> getModels() throws RepositoryException {
        return map(node.getMixinNodeTypes(), nodetype2name);
    }

}
