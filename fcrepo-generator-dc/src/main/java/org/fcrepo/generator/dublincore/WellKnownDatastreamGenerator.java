
package org.fcrepo.generator.dublincore;

import org.slf4j.Logger;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class WellKnownDatastreamGenerator implements DCGenerator {

    private static final Logger logger = getLogger(WellKnownDatastreamGenerator.class);

    private String wellKnownDsid;

    @Override
    public InputStream getStream(final Node node) {

        try {
            return getContentInputStream(node);
        } catch (RepositoryException e) {

            logger.warn("logged exception", e);

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
