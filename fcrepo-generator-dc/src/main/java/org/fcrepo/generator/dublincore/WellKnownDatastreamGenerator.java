/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.generator.dublincore;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;

/**
 * Retrieve a Dublin Core document from a well-known datastream
 * ( e.g. "DC")
 */
public class WellKnownDatastreamGenerator implements DCGenerator {

    private static final Logger LOGGER =
            getLogger(WellKnownDatastreamGenerator.class);

    private String wellKnownDsid;

    @Override
    public InputStream getStream(final Node node) {

        try {
            return getContentInputStream(node);
        } catch (final RepositoryException e) {

            LOGGER.warn("logged exception", e);

            return null;
        }
    }

    private InputStream getContentInputStream(final Node node)
        throws RepositoryException {
        if (node.hasNode(wellKnownDsid)) {
            final Node dc = node.getNode(wellKnownDsid);

            final Binary binary =
                    dc.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary();

            return binary.getStream();
        }
        return null;
    }

    /**
     * Set the well-known datastream to retrieve content from
     * @param wellKnownDsid
     */
    public void setWellKnownDsid(final String wellKnownDsid) {
        this.wellKnownDsid = wellKnownDsid;
    }
}
