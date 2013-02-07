package org.fcrepo.modeshape.indexer.dublincore;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class IndexFromWellKnownPath extends AbstractIndexer {

    private String wellKnownPath;

    @Override
    public InputStream getStream(Node node) {

        try {
            return getContentInputStream(node);
        } catch (RepositoryException e) {
            e.printStackTrace();

            return null;
        }
    }

    private InputStream getContentInputStream(Node node) throws RepositoryException {
        if(node.hasNode(this.wellKnownPath)) {
            final Node dc = node.getNode(this.wellKnownPath);

            Binary binary = dc.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary();

            return binary.getStream();
        } else {
            return null;
        }
    }

    public void setWellKnownPath(String wellKnownPath) {
        this.wellKnownPath = wellKnownPath;
    }
}
