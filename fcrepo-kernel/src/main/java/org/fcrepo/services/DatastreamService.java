
package org.fcrepo.services;

import static org.fcrepo.services.ObjectService.getObjectNode;
import static org.fcrepo.utils.FedoraJcrTypes.DC_IDENTIFER;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OWNED;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OWNERID;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Datastream;
import org.fcrepo.exception.InvalidChecksumException;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for creating and retrieving Datastreams without using the JCR API.
 * 
 * @author cbeer
 *
 */
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
            IOException, InvalidChecksumException {

        return createDatastreamNode(session, dsPath, contentType, requestBodyStream, null, null);
    }
    
    public static Node createDatastreamNode(final Session session,
            final String dsPath, final String contentType,
            final InputStream requestBodyStream, 
            String checksumType, String checksum) throws RepositoryException,
            IOException, InvalidChecksumException {

        final Node ds = jcrTools.findOrCreateNode(session, dsPath, NT_FILE);
        ds.addMixin(FEDORA_DATASTREAM);
        new Datastream(ds).setContent(requestBodyStream, contentType, checksumType, checksum);     
        ds.addMixin(FEDORA_OWNED);
        ds.setProperty(FEDORA_OWNERID, session.getUserID());

        ds.setProperty("jcr:lastModified", Calendar.getInstance());

        // TODO: I guess we should also have the PID + DSID..
        ds.setProperty(DC_IDENTIFER, new String[] {ds.getIdentifier(),
                ds.getParent().getName() + "/" + ds.getName()});

        return ds;
    }

    public static Node getDatastreamNode(final String pid, final String dsId)
            throws RepositoryException {
        logger.trace("Executing getDatastreamNode() with pid: " + pid +
                " and dsId: " + dsId);
        final Node objNode = getObjectNode(pid);
        logger.trace("Retrieved object node: " + objNode.getName());
        final Node dsNode = objNode.getNode(dsId);
        logger.trace("Retrieved datastream node: " + dsNode.getName());
        return dsNode;
    }

    public static Datastream getDatastream(final String pid, final String dsId)
            throws RepositoryException {
        return new Datastream(getDatastreamNode(pid, dsId));
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
