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

package org.fcrepo.storage.policy;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.services.policy.Policy;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * A binary storage policy based on the mime type of the node
 * 
 * @author cbeer
 * @date Apr 25, 2013
 */
public class MimeTypePolicy implements Policy {

    private static final Logger LOGGER = getLogger(MimeTypePolicy.class);

    private final String mimeType;

    private final String hint;

    /**
     * Register a mime type => binary storage hint mapping
     */
    public MimeTypePolicy(final String mimeType, final String hint) {
        this.mimeType = mimeType;
        this.hint = hint;
    }

    @Override
    public String toString() {
        return "MimeTypePolicy [mimeType=" + mimeType + ", hint=" + hint + "]";
    }

    /**
     * Evaluate the mime type policy. If the content node's mime type matches
     * this policy's mime type, return the hint.
     */
    @Override
    public String evaluatePolicy(final Node n) {
        LOGGER.debug("Evaluating MimeTypePolicy ({} -> {}) for {} ", mimeType,
            hint, n);
        try {
            final String nodeMimeType =
                n.getNode(JCR_CONTENT).getProperty(JCR_MIME_TYPE).getString();

            LOGGER.trace("Found mime type {}", nodeMimeType);

            if (nodeMimeType.equals(mimeType)) {
                LOGGER.trace("{} matched this mime type."
                    + "Returning hint {} ", mimeType, hint);
                return hint;
            }
        } catch (final RepositoryException e) {
            LOGGER.warn("Got Exception evaluating policy: {}", e);
            return null;
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (hint == null ? 0 : hint.hashCode());
        result = prime * result + (mimeType == null ? 0 : mimeType.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MimeTypePolicy other = (MimeTypePolicy) obj;
        if (hint == null) {
            if (other.hint != null) {
                return false;
            }
        } else if (!hint.equals(other.hint)) {
            return false;
        }
        if (mimeType == null) {
            if (other.mimeType != null) {
                return false;
            }
        } else if (!mimeType.equals(other.mimeType)) {
            return false;
        }
        return true;
    }
}
