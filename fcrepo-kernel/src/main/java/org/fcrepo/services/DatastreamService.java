
package org.fcrepo.services;

import static org.fcrepo.utils.FedoraTypesUtils.getBinary;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.binary.PolicyDecisionPoint;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.utils.DatastreamIterator;
import org.modeshape.jcr.api.Binary;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service for creating and retrieving Datastreams without using the JCR API.
 * 
 * @author cbeer
 *
 */
public class DatastreamService extends RepositoryService {

	@Autowired(required=false)
	PolicyDecisionPoint storagePolicyDecisionPoint;

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
		ds.setContent(requestBodyStream, contentType, checksumType, checksum, getStoragePolicyDecisionPoint());
		return ds.getNode();
	}

	/**
	 * retrieve the JCR node for a Datastream by pid and dsid
	 * @param path
	 * @return
	 * @throws RepositoryException
	 */
	public Node getDatastreamNode(final Session session, final String path)
			throws RepositoryException {
		logger.trace("Executing getDatastreamNode() with path: {}",
							path);
		final Node dsNode = getDatastream(session, path).getNode();
		logger.trace("Retrieved datastream node: {}", dsNode.getName());
		return dsNode;
	}

	/**
	 * Retrieve a Datastream instance by pid and dsid
	 * @param path jcr path to the datastream
	 * @return
	 * @throws RepositoryException
	 */
	public Datastream getDatastream(final Session session, final String path)
			throws RepositoryException {
		return new Datastream(session, path);
	}

    /**
     * Delete a Datastream
     * @param session jcr session
     * @param path
     * @throws RepositoryException
     */
    public void purgeDatastream(final Session session, final String path) throws RepositoryException {
    	new Datastream(session, path).purge();
    }

    /**
     *
	 * @param session jcr session
	 * @param path path to the DS node
	 * @return an iterator of the Datastream objects for a FedoraObject
     * @throws RepositoryException
     */
    public DatastreamIterator getDatastreamsForPath(final Session session, final String path) throws RepositoryException {
        return new DatastreamIterator(new FedoraObject(session,
                path).getNode().getNodes());
    }

    /**
     * Check if a datastream exists in the repository
     *
	 * @param session jcr session
	 * @param path
	 * @return
     * @throws RepositoryException
     */
    public boolean exists(final Session session, final String path) throws RepositoryException {
        return session.nodeExists(path);
    }

	public void setStoragePolicyDecisionPoint(PolicyDecisionPoint pdp) {
		this.storagePolicyDecisionPoint = pdp;
	}


	/**
	 * Get the Policy Decision Point for this service. Initialize it if Spring didn't wire it in for us.
	 * @return a PolicyDecisionPoint
	 */
	private PolicyDecisionPoint getStoragePolicyDecisionPoint() {
		if(storagePolicyDecisionPoint == null) {
			storagePolicyDecisionPoint = new PolicyDecisionPoint();
		}

		return storagePolicyDecisionPoint;
	}
}
