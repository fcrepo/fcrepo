
package org.fcrepo.services;

import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.fcrepo.services.PathService.getObjectJcrNodePath;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
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

    public Node createDatastreamNode(final Session session,
            final String dsPath, final String contentType,
            final InputStream requestBodyStream) throws RepositoryException,
            IOException, InvalidChecksumException {

        return createDatastreamNode(session, dsPath, contentType, requestBodyStream, null, null);
    }
    
    public Node createDatastreamNode(final Session session,
            final String dsPath, final String contentType,
            final InputStream requestBodyStream, 
            String checksumType, String checksum) throws RepositoryException,
            IOException, InvalidChecksumException {

        Datastream ds = new Datastream(session, dsPath);
        ds.setContent(requestBodyStream, contentType, checksumType, checksum);     
        return ds.getNode();
    }

    public Node getDatastreamNode(final String pid, final String dsId)
            throws RepositoryException {
        logger.trace("Executing getDatastreamNode() with pid: " + pid +
                " and dsId: " + dsId);
        final Node dsNode = getDatastream(pid, dsId).getNode();
        logger.trace("Retrieved datastream node: " + dsNode.getName());
        return dsNode;
    }

    public Datastream getDatastream(final String pid, final String dsId)
            throws RepositoryException {
        return new Datastream(readOnlySession, pid, dsId);
    }
    
    public void purgeDatastream(final Session session,
    		final String pid, 
    		final String dsId) throws RepositoryException {
    	final String dsPath = getDatastreamJcrNodePath(pid, dsId);
		new Datastream(session, dsPath).purge();
    }
    
    public NodeIterator getDatastreamsFor(String pid, Session session) throws RepositoryException {
    	return new FedoraObject(session, getObjectJcrNodePath(pid)).getNode().getNodes();
    }

    public NodeIterator getDatastreamsFor(String pid) throws RepositoryException {
    	return getDatastreamsFor(pid, readOnlySession);
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

    public void setRepository(Repository repository) {
            if(readOnlySession != null) {
                logoutSession();
            }
            repo = repository;

            getSession();
    }

}
