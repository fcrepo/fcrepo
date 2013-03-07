
package org.fcrepo;

import static com.google.common.base.Joiner.on;
import static org.fcrepo.utils.FedoraJcrTypes.DC_TITLE;
import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.value2string;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;

import java.io.InputStream;
import java.net.URI;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.fcrepo.utils.ContentDigest;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstraction for a Fedora datastream backed by a JCR node.
 * 
 * @author ajs6f
 *
 */
public class Datastream extends JcrTools {

    private final static String CONTENT_SIZE = "fedora:size";

    private final static String DIGEST_VALUE = "fedora:digest";

    private final static String DIGEST_ALGORITHM = "fedora:digestAlgorithm";

    private final static Logger logger = LoggerFactory
            .getLogger(Datastream.class);

    Node node;

    public Datastream(Node n) {
        this.node = n;
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
        return node.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary()
                .getStream();
    }

    /**
     * Sets the content of this Datastream.
     * 
     * @param content
     * @throws RepositoryException
     */
    public void setContent(InputStream content) throws RepositoryException {
        final Node contentNode =
                findOrCreateChild(node, JCR_CONTENT, NT_RESOURCE);

        if (contentNode.canAddMixin("fedora:checksum")) {
            contentNode.addMixin("fedora:checksum");
        }

        logger.debug("Created content node at path: " + contentNode.getPath());

        /*
         * https://docs.jboss.org/author/display/MODE/Binary+values#Binaryvalues-
         * ExtendedBinaryinterface
         * promises: "All javax.jcr.Binary values returned by ModeShape will
         * implement this public interface, so feel free to cast the values to
         * gain access to the additional methods."
         */
        Binary binary =
                (Binary) node.getSession().getValueFactory().createBinary(
                        content);

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
        Property dataProperty = contentNode.setProperty(JCR_DATA, binary);

        contentNode.setProperty(CONTENT_SIZE, dataProperty.getLength());
        contentNode.setProperty(DIGEST_VALUE, binary.getHexHash());
        contentNode.setProperty(DIGEST_ALGORITHM, "SHA-1");

        logger.debug("Created data property at path: " + dataProperty.getPath());

    }

    public void setContent(InputStream content, String mimeType)
            throws RepositoryException {
        setContent(content);
        node.setProperty("fedora:contentType", mimeType);
    }

    /**
     * @return The size in bytes of content associated with this datastream.
     * @throws RepositoryException
     */
    public long getContentSize() throws RepositoryException {
        return node.getNode(JCR_CONTENT).getProperty(CONTENT_SIZE).getLong();
    }

    public URI getContentDigest() throws RepositoryException {
        final Node contentNode = node.getNode(JCR_CONTENT);
        return ContentDigest
                .asURI(contentNode.getProperty(DIGEST_ALGORITHM).getString(),
                        contentNode.getProperty(DIGEST_VALUE).getString());
    }

    public String getContentDigestType() throws RepositoryException {
        return node.getNode(JCR_CONTENT).getProperty(DIGEST_ALGORITHM)
                .getString();
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
        return node.hasProperty("fedora:contentType") ? node.getProperty(
                "fedora:contentType").getString() : "application/octet-stream";
    }

    /**
     * @return A label associated with this datastream. Normally stored in a String-valued dc:title property.
     * @throws RepositoryException
     */
    public String getLabel() throws RepositoryException {
        if (node.hasProperty(DC_TITLE)) {

            Property labels = node.getProperty(DC_TITLE);
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

    public void setLabel(String label) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        node.setProperty(DC_TITLE, label);
    }

    public Date getCreatedDate() throws RepositoryException {
        return new Date(node.getProperty("jcr:created").getDate()
                .getTimeInMillis());
    }

}
