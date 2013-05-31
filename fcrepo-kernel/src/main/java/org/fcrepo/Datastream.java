/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo;

import static com.codahale.metrics.MetricRegistry.name;
import static org.fcrepo.services.ServiceHelpers.getNodePropertySize;
import static org.fcrepo.utils.FedoraTypesUtils.getBinary;
import static org.fcrepo.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.fcrepo.metrics.RegistryService.getMetrics;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.binary.PolicyDecisionPoint;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.utils.ContentDigest;
import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.JcrConstants;
import org.slf4j.Logger;

import com.codahale.metrics.Histogram;

/**
 * Abstraction for a Fedora datastream backed by a JCR node.
 *
 * @author ajs6f
 * @date Feb 21, 2013
 */
public class Datastream extends FedoraResource implements FedoraJcrTypes {

    private static final Logger LOGGER = getLogger(Datastream.class);

    static final Histogram contentSizeHistogram = getMetrics().histogram(name(Datastream.class, "content-size"));

    /**
     * The JCR node for this datastream
     * @param n an existing {@link Node}
     */
    public Datastream(final Node n) {
        super(n);
    }

    /**
     * Create or find a FedoraDatastream at the given path
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     * @throws RepositoryException
     */
    public Datastream(final Session session, final String path,
                      final String nodeType) throws RepositoryException {
        super(session, path, nodeType);
        mixinTypeSpecificCrap();
    }

    /**
     * Create or find a FedoraDatastream at the given path
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     * @throws RepositoryException
     */
    public Datastream(final Session session, final String path)
        throws RepositoryException {
        this(session, path, JcrConstants.NT_FILE);
    }

    private void mixinTypeSpecificCrap() {
        try {
            if (node.isNew() || !hasMixin(node)) {
                LOGGER.debug("Setting {} properties on a {} node...",
                             FEDORA_DATASTREAM, JcrConstants.NT_FILE);
                node.addMixin(FEDORA_DATASTREAM);

                if (node.hasNode(JCR_CONTENT)) {
                    Node contentNode = node.getNode(JCR_CONTENT);
                    decorateContentNode(contentNode);
                }
            }
        } catch (RepositoryException ex) {
            LOGGER.warn("Could not decorate {} with {} properties: {}" ,
                        JCR_CONTENT, FEDORA_DATASTREAM, ex);
        }
    }

    /**
     * @return The InputStream of content associated with this datastream.
     * @throws RepositoryException
     */
    public InputStream getContent() throws RepositoryException {
        final Node contentNode = node.getNode(JCR_CONTENT);
        LOGGER.trace("Retrieved datastream content node.");
        return contentNode.getProperty(JCR_DATA).getBinary().getStream();
    }

    /**
     * Sets the content of this Datastream.
     *
     * @param content
     * @throws RepositoryException
     */
    public void setContent(final InputStream content, final String contentType,
                           final String checksumType, final String checksum,
                           PolicyDecisionPoint storagePolicyDecisionPoint)
        throws RepositoryException, InvalidChecksumException {

        final Node contentNode =
            findOrCreateChild(node, JCR_CONTENT, NT_RESOURCE);

        if (contentNode.canAddMixin(FEDORA_BINARY)) {
            contentNode.addMixin(FEDORA_BINARY);
        }

        if (contentType != null) {
            contentNode.setProperty(JCR_MIME_TYPE, contentType);
        }


        LOGGER.debug("Created content node at path: {}", contentNode.getPath());

        String hint = null;


        if (storagePolicyDecisionPoint != null) {
            hint = storagePolicyDecisionPoint.evaluatePolicies(node);
        }

        Binary binary = (Binary) getBinary(node, content, hint);

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
            LOGGER.debug("Failed checksum test");
            throw new InvalidChecksumException("Checksum Mismatch of " +
                                               dsChecksum + " and " + checksum);
        }

        decorateContentNode(contentNode);

        LOGGER.debug("Created data property at path: {}",
                     dataProperty.getPath());

    }

    /**
     * @todo Add Documentation.
     */
    public void setContent(InputStream content) throws InvalidChecksumException,
                                                       RepositoryException {
        setContent(content, null, null, null, null);
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
            LOGGER.error("Could not get contentSize() - " + e.getMessage());
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
            return new URI(contentNode.getProperty(CONTENT_DIGEST).getString());
        } catch (RepositoryException e) {
            LOGGER.error("Could not get content digest: ", e);
        } catch (URISyntaxException e) {
            LOGGER.error("Could not get content digest: {}", e);
        }
        //TODO checksum not stored. recalculating checksum,
        //however, this would defeat the purpose validating against the checksum
        Binary binary = (Binary) contentNode.getProperty(JCR_DATA)
            .getBinary();
        String dsChecksum = binary.getHexHash();

        return ContentDigest.asURI("SHA-1",dsChecksum);
    }

    /**
     * @return The ID of this datastream, unique within an object. Normally just
     *         the name of the backing JCR node.
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
        return node.hasNode(JCR_CONTENT) &&
            node.getNode(JCR_CONTENT).hasProperty(JCR_MIME_TYPE) ?
            node.getNode(JCR_CONTENT).getProperty(JCR_MIME_TYPE).getString() :
            "application/octet-stream";
    }

    /**
     * Return the calculated size of the DS node
     * @return
     * @throws RepositoryException
     */
    @Override
    public long getSize() throws RepositoryException {
        return getNodePropertySize(node) + getContentSize();

    }

    /**
     * @todo Add Documentation.
     */
    private void decorateContentNode(Node contentNode)
        throws RepositoryException {
        if (contentNode == null) {
            LOGGER.warn("{}/{} appears to be null!", JCR_CONTENT);
            return;
        }
        if (contentNode.canAddMixin(FEDORA_BINARY)) {
            contentNode.addMixin(FEDORA_BINARY);
        }

        final Property dataProperty = contentNode.getProperty(JCR_DATA);
        Binary binary = (Binary) dataProperty.getBinary();
        final String dsChecksum = binary.getHexHash();

        contentSizeHistogram.update(dataProperty.getLength());

        contentNode.setProperty(CONTENT_SIZE, dataProperty.getLength());
        contentNode.setProperty(CONTENT_DIGEST, ContentDigest.
                                asURI("SHA-1", dsChecksum).toString());

        LOGGER.debug("Decorated data property at path: " +
                     dataProperty.getPath());
    }

    /**
     * @todo Add Documentation.
     */
    public static boolean hasMixin(Node node) throws RepositoryException {
        return isFedoraDatastream.apply(node);
    }

}
