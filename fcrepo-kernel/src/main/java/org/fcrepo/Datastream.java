
package org.fcrepo;

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

    public String getMimeType() throws ValueFormatException,
            PathNotFoundException, RepositoryException {
        return node.hasProperty("fedora:contentType") ? node.getProperty(
                "fedora:contentType").getString() : "application/octet-stream";
    }

}
