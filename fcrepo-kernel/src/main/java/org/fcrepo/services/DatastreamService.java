
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

    public Node createDatastreamNode(final Session session,
            final String dsPath, final String contentType,
            final InputStream requestBodyStream) throws RepositoryException,
            IOException, InvalidChecksumException {

        return createDatastreamNode(session, dsPath, contentType,
                requestBodyStream, null, null);
    }

    public Node createDatastreamNode(final Session session,
            final String dsPath, final String contentType,
            final InputStream requestBodyStream, String checksumType,
            String checksum) throws RepositoryException, IOException,
            InvalidChecksumException {

        Datastream ds = new Datastream(session, dsPath);
        Node result = ds.getNode();
        ds.setContent(requestBodyStream, contentType, checksumType, checksum);
        return result;
    }

    public Node getDatastreamNode(final String pid, final String dsId)
            throws RepositoryException {
        logger.trace("Executing getDatastreamNode() with pid: {} and dsId: {}", pid, dsId);
        final Node dsNode = getDatastream(pid, dsId).getNode();
        logger.trace("Retrieved datastream node: {}", dsNode.getName());
        return dsNode;
    }

    public Datastream getDatastream(final String pid, final String dsId)
            throws RepositoryException {
        return new Datastream(readOnlySession, pid, dsId);
    }

    public void purgeDatastream(final Session session, final String pid,
            final String dsId) throws RepositoryException {
    	Datastream ds = new Datastream(session, pid, dsId);
    	ds.purge();
    }

    public DatastreamIterator getDatastreamsFor(final String pid,
            final Session session) throws RepositoryException {
        return new DatastreamIterator(
        		new FedoraObject(session, getObjectJcrNodePath(pid)).getNode()
                .getNodes());
    }

    public DatastreamIterator getDatastreamsFor(final String pid)
            throws RepositoryException {
        return getDatastreamsFor(pid, readOnlySession);
    }
    
    public boolean exists(String pid, String dsId) throws RepositoryException {
    	return exists(pid, dsId, readOnlySession);
    }
    
    public boolean exists(String pid, String dsId, Session session) throws RepositoryException {
    	String dspath = getDatastreamJcrNodePath(pid, dsId);
    	return session.nodeExists(dspath);
    }

}
