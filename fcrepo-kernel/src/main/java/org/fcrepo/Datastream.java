
package org.fcrepo;

import static com.google.common.base.Joiner.on;
import static org.fcrepo.utils.FedoraJcrTypes.DC_TITLE;
import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.value2string;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * Abstraction for Fedora datastreams.
 * 
 * @author ajs6f
 *
 */
public class Datastream {

    Node node;

    public Datastream(Node n) {
        this.node = n;
    }

    public Node getNode() {
        return node;
    }

    public InputStream getContent() throws RepositoryException {
        return node.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary()
                .getStream();
    }

    public long getContentSize() throws RepositoryException {
        return node.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary()
                .getSize();
    }

    public String getDsId() throws RepositoryException {
        return node.getName();
    }

    public FedoraObject getObject() throws RepositoryException {
        return new FedoraObject(node.getParent());
    }

    public String getMimeType() throws RepositoryException {
        return node.hasProperty("fedora:contentType") ? node.getProperty(
                "fedora:contentType").getString() : "application/octet-stream";
    }

    public String getLabel() throws RepositoryException {
        if (node.hasProperty(DC_TITLE)) {

            Property labels = node.getProperty(DC_TITLE);
            String label;
            if (!labels.isMultiple())
                label = node.getProperty(DC_TITLE).getString();
            else {
                label = on('/').join(map(labels.getValues(), value2string));
            }
            return label;
        } else
            return "";

    }

    public void setLabel(String label) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        node.setProperty(DC_TITLE, label);
        node.getSession().save();
    }

}
