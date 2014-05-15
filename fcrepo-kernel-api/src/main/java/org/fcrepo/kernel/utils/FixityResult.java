/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.utils;

import javax.jcr.RepositoryException;
import java.net.URI;
import java.util.Set;

/**
 * @author bbpennel
 * @since Feb 18, 2014
 */
public interface FixityResult {

    /**
     * The possible fixity states (which may be ORed together later)
     */
    public static enum FixityState {
        SUCCESS, REPAIRED, BAD_CHECKSUM, BAD_SIZE, MISSING_STORED_FIXITY
    }

    /**
     * Get the identifier for the entry's store
     * @return String
     */
    String getStoreIdentifier() throws RepositoryException;

    /**
     * Check if the fixity result matches the given checksum URI
     *
     * @param checksum
     * @return fixity result matches the given checksum URI
     */
    boolean matches(URI checksum);

    /**
     * Check if the fixity result matches the given size
     *
     * @param size
     * @return fixity result matches the given size
     */
    boolean matches(long size);

    /**
     * Does the fixity entry match the given size and checksum?
     *
     * @param size bitstream size in bytes
     * @param checksum checksum URI
     * @return true if both conditions matched
     */
    boolean matches(long size, URI checksum);

    /**
     * Was the fixity declared a success
     * @return boolean
     */
    boolean isSuccess();

    /**
     * Mark the fixity result as been automatically repaired
     */
    void setRepaired();

    /**
     * @return the status
     */
    Set<FixityState> getStatus();

    /**
     * @return the computed size
     */
    long getComputedSize();

    /**
     * @return the computed checksum
     */
    URI getComputedChecksum();

}