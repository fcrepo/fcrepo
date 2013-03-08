
package org.fcrepo.generator.dublincore;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class WellKnownDatastreamGenerator implements DCGenerator {

    private String wellKnownDsid;

    @Override
    public InputStream getStream(final Node node) {

        try {
            return getContentInputStream(node);
        } catch (RepositoryException e) {
            e.printStackTrace();

            return null;
        }
    }

    private InputStream getContentInputStream(final Node node)
            throws RepositoryException {
        if (node.hasNode(this.wellKnownDsid)) {
            final Node dc = node.getNode(this.wellKnownDsid);

            Binary binary =
                    dc.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary();

            return binary.getStream();
        } else {
            return null;
        }
    }

    public void setWellKnownDsid(String wellKnownDsid) {
        this.wellKnownDsid = wellKnownDsid;
    }
}
