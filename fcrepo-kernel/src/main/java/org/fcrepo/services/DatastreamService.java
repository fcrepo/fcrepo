
package org.fcrepo.services;

import static org.fcrepo.services.ObjectService.getObjectNode;
import static org.fcrepo.utils.FedoraJcrTypes.DC_IDENTIFER;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OWNED;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Datastream;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatastreamService {

    private static final Logger logger = LoggerFactory
            .getLogger(DatastreamService.class);

    @Inject
    private Repository repo;

    private Session readOnlySession;

    private static JcrTools jcrTools = new JcrTools(false);

    public static Node createDatastreamNode(final Session session,
            final String dsPath, final String contentType,
            final InputStream requestBodyStream) throws RepositoryException,
            IOException {

        final Node ds = jcrTools.findOrCreateNode(session, dsPath, NT_FILE);
        ds.addMixin(FEDORA_DATASTREAM);

        final Node contentNode =
                jcrTools.findOrCreateChild(ds, JCR_CONTENT, NT_RESOURCE);
        logger.debug("Created content node at path: " + contentNode.getPath());
        /*
         * This next line of code deserves explanation. If we chose for the
         * simpler line:
         * Property dataProperty = contentNode.setProperty(JCR_DATA,
         * requestBodyStream);
         * then the JCR would not block on the stream's completion, and we would
         * return to the requester before the mutation to the repo had actually
         * completed. So instead we use createBinary(requestBodyStream), because
         * its contract specifies:
         * "The passed InputStream is closed before this method returns either
         * normally or because of an exception."
         * which lets us block and not return until the job is done! The simpler
         * code may still be useful to us for an asynchronous method that we
         * develop later.
         */
        Property dataProperty =
                contentNode.setProperty(JCR_DATA, session.getValueFactory()
                        .createBinary(requestBodyStream));
        logger.debug("Created data property at path: " + dataProperty.getPath());

        ds.setProperty("fedora:contentType", contentType);

        ds.addMixin(FEDORA_OWNED);
        ds.setProperty("fedora:ownerId", session.getUserID());

        if (!ds.hasProperty("fedora:created")) {
            ds.setProperty("fedora:created", Calendar.getInstance());
        }
        ds.setProperty("jcr:lastModified", Calendar.getInstance());

        // TODO: I guess we should also have the PID + DSID..
        ds.setProperty(DC_IDENTIFER, new String[] {ds.getIdentifier(),
                ds.getParent().getName() + "/" + ds.getName()});

        return ds;
    }

    public static Node getDatastreamNode(final String pid, final String dsId)
            throws PathNotFoundException, RepositoryException {
        logger.trace("Executing getDatastreamNode() with pid: " + pid +
                " and dsId: " + dsId);
        final Node objNode = getObjectNode(pid);
        logger.trace("Retrieved object node: " + objNode.getName());
        final Node dsNode = objNode.getNode(dsId);
        logger.trace("Retrieved datastream node: " + dsNode.getName());
        return dsNode;
    }

    public static Datastream getDatastream(final String pid, final String dsId)
            throws PathNotFoundException, RepositoryException {
        return new Datastream(getObjectNode(pid).getNode(dsId));
    }

    @PostConstruct
    public void getSession() {
        try {
            readOnlySession = repo.login();
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    @PreDestroy
    public void logoutSession() {
        readOnlySession.logout();
    }
}
