
package org.fcrepo;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

public class Datastream {

    Node node;

    public Datastream(Node n) {
        this.node = n;
    }

    public Node getNode() {
        return node;
    }
    
    public InputStream getContent() throws ValueFormatException, PathNotFoundException, RepositoryException {
        return node.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary()
                .getStream();
    }

    public String getMimeType() throws ValueFormatException,
            PathNotFoundException, RepositoryException {
        return node.hasProperty("fedora:contentType") ? node.getProperty(
                "fedora:contentType").getString() : "application/octet-stream";
    }

}
