
package org.fcrepo;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkArgument;
import static com.yammer.metrics.MetricRegistry.name;
import static org.fcrepo.services.PathService.getDatastreamJcrNodePath;
import static org.fcrepo.services.RepositoryService.metrics;
import static org.fcrepo.services.ServiceHelpers.getNodePropertySize;
import static org.fcrepo.utils.FedoraTypesUtils.getBinary;
import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.value2string;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;

import org.fcrepo.binary.PolicyDecisionPoint;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.utils.ContentDigest;
import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.yammer.metrics.Histogram;

/**
 * Abstraction for a Fedora datastream backed by a JCR node.
 * 
 * @author ajs6f
 *
 */
public class Datastream extends JcrTools implements FedoraJcrTypes {

    private static final Logger logger = getLogger(Datastream.class);

    static final Histogram contentSizeHistogram = metrics.histogram(name(
            Datastream.class, "content-size"));

    private Node node;

    /**
     * The JCR node for this datastream
     * @param n an existing {@link Node}
     */
    public Datastream(final Node n) {
        // turn off debug logging
        super(false);
        if (node != null) {
            logger.debug("Supporting a Fedora Datastream with null backing Node!");
        }
        node = n;
        try {
        if (!hasMixin(node)) {
            logger.debug("Setting fedora:datastream properties on a nt:file node...");
            node.addMixin(FEDORA_DATASTREAM);
            node.addMixin(FEDORA_OWNED);
            node.setProperty(FEDORA_OWNERID, node.getSession().getUserID());

            node.setProperty("jcr:lastModified", Calendar.getInstance());

            // TODO: I guess we should also have the PID + DSID..
            String[] ids = (node.getParent() != null)
                  ? new String[]{node.getIdentifier(), node.getParent().getName() + "/" + node.getName()}
                  : new String[]{node.getIdentifier()};
            node.setProperty(DC_IDENTIFIER, ids);
            Node contentNode = node.getNode(JCR_CONTENT);
            decorateContentNode(contentNode);
        }
        } catch (RepositoryException ex) {
            logger.warn("Could not decorate jcr:content with fedora:datastream properties: " + ex.getMessage());
        }
    }

    /**
     * Find or create a Datastream from a pid and dsid
     * @param session
     * @param pid object persistent identifier
     * @param dsId datastream identifier
     * @throws RepositoryException
     */
    public Datastream(final Session session, final String pid, final String dsId)
            throws RepositoryException {
        this(session, getDatastreamJcrNodePath(pid, dsId));
    }

    /**
     * Find or create a Datastream object at the given JCR path
     * @param session
     * @param dsPath the absolute path for the datastream
     * @throws RepositoryException
     */
    public Datastream(final Session session, final String dsPath)
            throws RepositoryException {
        super(false);
        checkArgument(session != null,
                "null cannot create a Fedora Datastream!");
        checkArgument(dsPath != null,
                "A Fedora Datastream cannot be created at null path!");

        node = findOrCreateNode(session, dsPath, NT_FILE);
        if (node.isNew()) {
            logger.debug("Setting fedora:datastream properties on a new DS node...");
            node.addMixin(FEDORA_DATASTREAM);
            node.addMixin(FEDORA_OWNED);
            node.setProperty(FEDORA_OWNERID, session.getUserID());

            node.setProperty("jcr:lastModified", Calendar.getInstance());

            // TODO: I guess we should also have the PID + DSID..
            node.setProperty(DC_IDENTIFIER, new String[] {node.getIdentifier(),
                    node.getParent().getName() + "/" + node.getName()});
            if (node.hasNode(JCR_CONTENT)) {
                decorateContentNode(node.getNode(JCR_CONTENT));
            }
        } else if (!hasMixin(node)) {
            logger.debug("Setting fedora:datastream properties on a nt:file node...");
            node.addMixin(FEDORA_DATASTREAM);
            node.addMixin(FEDORA_OWNED);
            node.setProperty(FEDORA_OWNERID, session.getUserID());

            node.setProperty("jcr:lastModified", Calendar.getInstance());

            // TODO: I guess we should also have the PID + DSID..
            String[] ids = (node.getParent() != null)
                  ? new String[]{node.getIdentifier(), node.getParent().getName() + "/" + node.getName()}
                  : new String[]{node.getIdentifier()};
            node.setProperty(DC_IDENTIFIER, ids);
            Node contentNode = node.getNode(JCR_CONTENT);
            decorateContentNode(contentNode);
        }
        
    }

    /**
     * @return The backing JCR node.
     */
    public Node getNode() {
        return node;
    }

    /**
     * @return The InputStream of content associated with this datastream.
     * @throws RepositoryException
     */
    public InputStream getContent() throws RepositoryException {
        final Node contentNode = node.getNode(JCR_CONTENT);
        logger.trace("Retrieved datastream content node.");
        return contentNode.getProperty(JCR_DATA).getBinary().getStream();
    }

    /**
     * Sets the content of this Datastream.
     * 
     * @param content
     * @throws RepositoryException
     */
    public void setContent(final InputStream content,
            final String checksumType, final String checksum)
            throws RepositoryException, InvalidChecksumException {

        final Node contentNode =
                findOrCreateChild(node, JCR_CONTENT, NT_RESOURCE);

        if (contentNode.canAddMixin(FEDORA_CHECKSUM)) {
            contentNode.addMixin(FEDORA_CHECKSUM);
        }

        logger.debug("Created content node at path: " + contentNode.getPath());

        /*
         * https://docs.jboss.org/author/display/MODE/Binary+values#Binaryvalues-
         * ExtendedBinaryinterface
         * promises: "All javax.jcr.Binary values returned by ModeShape will
         * implement this public interface, so feel free to cast the values to
         * gain access to the additional methods."
         */
        final Binary binary = (Binary) getBinary(node, content, PolicyDecisionPoint.getInstance().evaluatePolicies(node));

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
        final Property dataProperty = contentNode.setProperty(JCR_DATA, binary);

        final String dsChecksum = binary.getHexHash();
        if (checksum != null && !checksum.equals("") &&
                !checksum.equals(binary.getHexHash())) {
            logger.debug("Failed checksum test");
            throw new InvalidChecksumException("Checksum Mismatch of " +
                    dsChecksum + " and " + checksum);
        }

        contentSizeHistogram.update(dataProperty.getLength());

        contentNode.setProperty(CONTENT_SIZE, dataProperty.getLength());
        contentNode.setProperty(DIGEST_VALUE, dsChecksum);
        contentNode.setProperty(DIGEST_ALGORITHM, "SHA-1");

        logger.debug("Created data property at path: " + dataProperty.getPath());

    }

    /**
     * Set the datastream's binary content payload
     * @param content binary payload
     * @throws RepositoryException
     * @throws InvalidChecksumException
     */
    public void setContent(final InputStream content)
            throws RepositoryException, InvalidChecksumException {
        setContent(content, null, null);
    }

    /**
     * Set the datastream's bianry content payload, and check it against a provided digest
     * @param content binary paylod
     * @param mimeType the mimetype given for the content inputstream
     * @param checksumType one of: SHA-1
     * @param checksum the digest of content
     * @throws RepositoryException
     * @throws InvalidChecksumException
     */
    public void setContent(final InputStream content, final String mimeType,
            final String checksumType, final String checksum)
            throws RepositoryException, InvalidChecksumException {
		node.setProperty(FEDORA_CONTENTTYPE, mimeType);
        setContent(content, checksumType, checksum);
    }

    /**
     * @return The size in bytes of content associated with this datastream.
     * @throws RepositoryException
     */
	public long getContentSize() {
		try {
			return node.getNode(JCR_CONTENT).getProperty(CONTENT_SIZE)
					.getLong();
		} catch (RepositoryException e) {
			logger.error("Could not get contentSize() - " + e.getMessage());
		}
		// TODO Size is not stored, recalculate size?
		return 0L;
	}

    /**
     * Get the pre-calculated content digest for the binary payload
     * @return a URI with the format algorithm:value
     * @throws RepositoryException
     */
    public URI getContentDigest() throws RepositoryException {
        final Node contentNode = node.getNode(JCR_CONTENT);
        try {
	        return ContentDigest
	                .asURI(contentNode.getProperty(DIGEST_ALGORITHM).getString(),
	                        contentNode.getProperty(DIGEST_VALUE).getString());
        } catch (RepositoryException e) {
        	logger.error("Could not get content digest - " + e.getMessage());
        }
        //TODO checksum not stored. recalculating checksum, 
        //however, this would defeat the purpose validating against the checksum
        Binary binary = (Binary) contentNode.getProperty(JCR_DATA)
        					.getBinary();
        String dsChecksum = binary.getHexHash();

        return ContentDigest.asURI("SHA-1",dsChecksum);
    }

    /**
     * Get the digest algorithm used to calculate the primary digest of the binary payload
     * @return
     * @throws RepositoryException
     */
    public String getContentDigestType() {
    	try {
    		return node.getNode(JCR_CONTENT).getProperty(DIGEST_ALGORITHM)
    				.getString();
    	} catch (RepositoryException e) {
    		logger.error("Could not get content digest type - " + e.getMessage());
    	}
    	//only supporting sha-1
    	logger.debug("Using default digest type of SHA-1");
        return "SHA-1";
    }

    /**
     * @return The ID of this datastream, unique within an object. Normally just the name of the backing JCR node.
     * @throws RepositoryException
     */
    public String getDsId() throws RepositoryException {
        return node.getName();
    }

    /**
     * @return the FedoraObject to which this datastream belongs.
     * @throws RepositoryException
     */
    public FedoraObject getObject() throws RepositoryException {
        return new FedoraObject(node.getParent());
    }

    /**
     * @return The MimeType of content associated with this datastream.
     * @throws RepositoryException
     */
    public String getMimeType() throws RepositoryException {
        return node.hasProperty(FEDORA_CONTENTTYPE) ? node.getProperty(
                FEDORA_CONTENTTYPE).getString() : "application/octet-stream";
    }

    /**
     * @return A label associated with this datastream. Normally stored in a String-valued dc:title property.
     * @throws RepositoryException
     */
    public String getLabel() throws RepositoryException {
        if (node.hasProperty(DC_TITLE)) {

            final Property labels = node.getProperty(DC_TITLE);
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

    /**
     * @return Owner of this datastream stored in the fedora:ownerId property.
     * @throws RepositoryException
     */
    public String getOwnerId() throws RepositoryException {
        if (node.hasProperty(FEDORA_OWNERID)) {
            final Property ownerIds = node.getProperty(FEDORA_OWNERID);
            String ownerId;
            if (!ownerIds.isMultiple()) {
                ownerId = node.getProperty(FEDORA_OWNERID).getString();
            } else {
                ownerId = on('/').join(map(ownerIds.getValues(), value2string));
            }
            return ownerId;
        } else {
            return "";
        }
    }

    /**
     * Set an administrative label for this object
     * @param label
     * @throws RepositoryException
     */
    public void setLabel(final String label) throws RepositoryException {
        node.setProperty(DC_TITLE, label);
    }

    /**
     * Get the date this datastream was created
     * @return
     * @throws RepositoryException
     */
    public Date getCreatedDate() throws RepositoryException {
        return new Date(node.getProperty(JCR_CREATED).getDate()
                .getTimeInMillis());
    }

    /**
     * Get the date this datastream was last modified
     * @return
     * @throws RepositoryException
     */
    public Date getLastModifiedDate() {
    	//TODO no modified date stored
    	//attempt to set as created date?
    	try {
	        return new Date(node.getProperty(JCR_LASTMODIFIED).getDate()
	                .getTimeInMillis());
    	} catch (RepositoryException e) {
    		logger.error("Could not get last modified date");
    	}
    	logger.debug("Setting modified date");
    	try {
    		Date createdDate = getCreatedDate();
    		node.setProperty(JCR_LASTMODIFIED, createdDate.toString());
    		node.getSession().save();
    		return createdDate;
    	} catch(RepositoryException e) {
    		logger.error("Could not set new modified date - " + e.getMessage());
    	}
    	return null;
    }

    /**
     * Delete this datastream's underlying node
     */
    public void purge() throws RepositoryException {
        node.remove();
    }

    /**
     * Return the calculated size of the DS node
     * @return
     * @throws RepositoryException
     */
    public long getSize() throws RepositoryException {
        return getNodePropertySize(node) + getContentSize();

    }
    
    private void decorateContentNode(Node contentNode) throws RepositoryException {
        if (contentNode == null) {
            logger.warn("{}/jcr:content appears to be null!");
            return;
        }
        if (contentNode.canAddMixin(FEDORA_CHECKSUM)) {
            contentNode.addMixin(FEDORA_CHECKSUM);
        }

        final Property dataProperty = contentNode.getProperty(JCR_DATA);
        Binary binary = (Binary) dataProperty.getBinary();
        final String dsChecksum = binary.getHexHash();

        contentSizeHistogram.update(dataProperty.getLength());

        contentNode.setProperty(CONTENT_SIZE, dataProperty.getLength());
        contentNode.setProperty(DIGEST_VALUE, dsChecksum);
        contentNode.setProperty(DIGEST_ALGORITHM, "SHA-1");

        logger.debug("Decorated data property at path: " + dataProperty.getPath());
    }
    
    public static boolean hasMixin(Node node) throws RepositoryException {
        NodeType[] nodeTypes = node.getMixinNodeTypes();
        for (NodeType nodeType: nodeTypes) {
            if (FEDORA_DATASTREAM.equals(nodeType.getName())) {
                return true;
            }
        }
        return false;
    }

}
