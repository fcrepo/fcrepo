/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.utils;

import org.fcrepo.kernel.utils.CacheEntry;
import org.fcrepo.kernel.utils.FixityResult;

import javax.jcr.RepositoryException;

import static java.util.Objects.hash;

import java.net.URI;
import java.util.EnumSet;
import java.util.Set;

/**
 * Structure for presenting the results of a fixity check
 * (and any repair operations that may have occurred)
 *
 * @author bbpennel
 */
public class FixityResultImpl implements FixityResult {

    /**
     * the size computed by the fixity check
     * @todo make this private
     */
    private final long computedSize;

    /**
     * the checksum computed by the fixity check
     * @todo make this private
     */
    private final URI computedChecksum;

    private final String storeIdentifier;

    /**
     * Prepare a fixity result given the computed checksum and size
     * @param size the given size
     * @param checksum the given checksum
     */
    public FixityResultImpl(final long size, final URI checksum) {
        this("comparison-only-identifier", size, checksum);
    }

    /**
     * Prepare a fixity result with the expected size and checksum
     * @param entry the entry
     * @param size the expected size
     * @param checksum the checksum
     * @throws RepositoryException if repository exception occurred
     */
    public FixityResultImpl(final CacheEntry entry, final long size,
                        final URI checksum) throws RepositoryException {
        this(entry.getExternalIdentifier(), size, checksum);
    }

    /**
     *
     * @param storeIdentifier the store identifier
     * @param size the size
     * @param checksum the checksum
     */
    public FixityResultImpl(final String storeIdentifier, final long size, final URI checksum) {
        this.storeIdentifier = storeIdentifier;
        computedSize = size;
        computedChecksum = checksum;
    }

    /**
     * Get the identifier for the entry's store
     * @return the store identifier
     */
    @Override
    public String getStoreIdentifier() {
        return storeIdentifier;
    }

    @Override
    public boolean equals(final Object obj) {

        boolean result = false;
        if (obj instanceof FixityResult) {
            final FixityResult that = (FixityResult) obj;
            result =
                    computedSize == that.getComputedSize() &&
                            computedChecksum.equals(that.getComputedChecksum());
        }

        return result;
    }

    @Override
    public int hashCode() {
        return hash(computedSize, computedChecksum);
    }

    @Override
    public String toString() {
        return "Fixity: checksum: " + computedChecksum + " / " + computedSize;
    }

    /**
     * Check if the fixity result matches the given checksum URI
     * @param checksum the checksum uri
     * @return true if the checksums match
     */
    @Override
    public boolean matches(final URI checksum) {
        return computedChecksum.equals(checksum);
    }

    /**
     * Check if the fixity result matches the given size
     * @param size the size
     * @return true if fixity result matches the given size
     */
    @Override
    public boolean matches(final long size) {
        return computedSize == size;
    }

    /**
     * Does the fixity entry match the given size and checksum?
     * @param size bitstream size in bytes
     * @param checksum checksum URI in the form urn:DIGEST:RESULT
     * @return true if both conditions matched
     */
    @Override
    public boolean matches(final long size, final URI checksum) {
        return matches(size) && matches(checksum);
    }

    /**
     * @return the status
     */
    @Override
    public Set<FixityState> getStatus(final long size, final URI checksum) {

        final Set<FixityState> status = EnumSet.noneOf(FixityState.class);


        if (matches(size, checksum)) {
            status.add(FixityState.SUCCESS);
        } else {
            if (!matches(size)) {
                status.add(FixityState.BAD_SIZE);
            }

            if (!matches(checksum)) {
                status.add(FixityState.BAD_CHECKSUM);
            }
        }

        return status;
    }

    /**
     * @return the computedSize
     */
    @Override
    public long getComputedSize() {
        return computedSize;
    }

    /**
     * @return the computedChecksum
     */
    @Override
    public URI getComputedChecksum() {
        return computedChecksum;
    }

}
