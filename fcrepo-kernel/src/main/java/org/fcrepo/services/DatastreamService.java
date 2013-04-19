
package org.fcrepo.services;

import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.fcrepo.services.PathService.getObjectJcrNodePath;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.utils.DatastreamIterator;
import org.slf4j.Logger;

/**
 * Service for creating and retrieving Datastreams without using the JCR API.
 * 
 * @author cbeer
 *
 */
public class DatastreamService extends RepositoryService {

    private static final Logger logger = getLogger(DatastreamService.class);

    /**
     * Create a new Datastream node in the JCR store
     * @param session the jcr session to use
     * @param dsPath the absolute path to put the datastream
     * @param contentType the mime-type for the requestBodyStream
     * @param requestBodyStream binary payload for the datastream
     * @return
     * @throws RepositoryException
     * @throws IOException
     * @throws InvalidChecksumException
     */
    public Node createDatastreamNode(final Session session,
            final String dsPath, final String contentType,
            final InputStream requestBodyStream) throws RepositoryException,
            IOException, InvalidChecksumException {

        return createDatastreamNode(session, dsPath, contentType,
                requestBodyStream, null, null);
    }

    /**
     * Create a new Datastream node in the JCR store
     * @param session the jcr session to use
     * @param dsPath the absolute path to put the datastream
     * @param contentType the mime-type for the requestBodyStream
     * @param requestBodyStream binary payload for the datastream
     * @param checksumType digest algorithm used to calculate the checksum
     * @param checksum the digest for the binary payload
     * @return
     * @throws RepositoryException
     * @throws IOException
     * @throws InvalidChecksumException
     */
    public Node createDatastreamNode(final Session session,
            final String dsPath, final String contentType,
            final InputStream requestBodyStream, final String checksumType,
            final String checksum) throws RepositoryException, IOException,
            InvalidChecksumException {

        final Datastream ds = new Datastream(session, dsPath);
        final Node result = ds.getNode();
        ds.setContent(requestBodyStream, contentType, checksumType, checksum);
        return result;
    }

    /**
     * retrieve the JCR node for a Datastream by pid and dsid
     * @param pid object persistent identifier
     * @param dsId datastream identifier
     * @return
     * @throws RepositoryException
     */
    public Node getDatastreamNode(final String pid, final String dsId)
            throws RepositoryException {
        logger.trace("Executing getDatastreamNode() with pid: {} and dsId: {}",
                pid, dsId);
        final Node dsNode = getDatastream(pid, dsId).getNode();
        logger.trace("Retrieved datastream node: {}", dsNode.getName());
        return dsNode;
    }

    /**
     * Retrieve a Datastream instance by pid and dsid
     * @param pid object persistent identifier
     * @param dsId datastream identifier
     * @return
     * @throws RepositoryException
     */
    public Datastream getDatastream(final String pid, final String dsId)
            throws RepositoryException {
        return new Datastream(readOnlySession, pid, dsId);
    }

    /**
     * Delete a Datastream
     * @param session jcr session
     * @param pid object persistent identifier
     * @param dsId datastream identifier
     * @throws RepositoryException
     */
    public void purgeDatastream(final Session session, final String pid,
            final String dsId) throws RepositoryException {
        new Datastream(session, pid, dsId).purge();
    }

    /**
     * @param pid object persistent identifier
     * @param session jcr session
     * @return an iterator of the Datastream objects for a FedoraObject
     * @throws RepositoryException
     */
    public DatastreamIterator getDatastreamsFor(final String pid,
            final Session session) throws RepositoryException {
        return new DatastreamIterator(new FedoraObject(session,
                getObjectJcrNodePath(pid)).getNode().getNodes());
    }

    /**
     * Get
     * @param pid FedoraObject persistent identifier
     * @return a read-only iterator of Datastream objects for a FedoraObject
     * @throws RepositoryException
     */
    public DatastreamIterator getDatastreamsFor(final String pid)
            throws RepositoryException {
        return getDatastreamsFor(pid, readOnlySession);
    }

    /**
     * Check if a datastream exists in the repository
     * @param pid object persistent identifier
     * @param dsId datastream identifier
     * @return
     * @throws RepositoryException
     */
    public boolean exists(final String pid, final String dsId)
            throws RepositoryException {
        return exists(pid, dsId, readOnlySession);
    }

    /**
     * Check if a datastream exists in the repository
     * @param pid object persistent identifier
     * @param dsId datastream identifier
     * @param session jcr session
     * @return
     * @throws RepositoryException
     */
    public boolean exists(final String pid, final String dsId,
            final Session session) throws RepositoryException {
        return session.nodeExists(getDatastreamJcrNodePath(pid, dsId));
    }

}
