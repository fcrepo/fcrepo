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
package org.fcrepo.binary;

import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @todo Add Documentation.
 * @author cbeer
 * @date Apr 25, 2013
 */
public class MimeTypePolicy implements Policy {

    private static final Logger logger = getLogger(MimeTypePolicy.class);

    private final String mimeType;

    private final String hint;

    /**
     * @todo Add Documentation.
     */
    public MimeTypePolicy(final String mimeType, final String hint) {
        this.mimeType = mimeType;
        this.hint = hint;
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public String evaluatePolicy(final Node n) {
        logger.debug("Evaluating MimeTypePolicy for {} -> {}", mimeType, hint);
        try {
            final String nodeMimeType =
                    n.getNode(JCR_CONTENT).getProperty(JCR_MIME_TYPE)
                            .getString();
            logger.debug("Found mime type {}", nodeMimeType);
            if (nodeMimeType.equals(mimeType)) {
                return hint;
            }
        } catch (RepositoryException e) {
            logger.warn("Got Exception evaluating policy: {}", e);
            return null;
        }

        return null;
    }
}
