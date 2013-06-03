/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utils;

import static java.util.Objects.hash;

import java.net.URI;
import java.util.EnumSet;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date Mar 12, 2013
 */
public class FixityResult {

    public static enum FixityState {
        SUCCESS, REPAIRED, BAD_CHECKSUM, BAD_SIZE
    }

    /**
     * This is a little weird here, and is vestigal from when
     * this was a JAX-B model as well.
     *
     * The "state" of the fixity object is one of:
     * - SUCCESS: the fixity check was declared successful
     * - BAD_CHECKSUM and/or BAD_SIZE: either the checksum or the size of the
     *       bitstream didn't match the stored size
     * - REPAIRED and BAD_*: the checksum or size failed to match, but it
     *       was automatically recovered from a different copy
     */
    public EnumSet<FixityState> status = EnumSet.noneOf(FixityState.class);

    public long computedSize;

    public URI computedChecksum;

    private final LowLevelCacheEntry entry;

    /**
     * @todo Add Documentation.
     */
    public FixityResult() {
        entry = null;
    }

    /**
     * @todo Add Documentation.
     */
    public FixityResult(final LowLevelCacheEntry entry) {
        this.entry = entry;
    }

    /**
     * @todo Add Documentation.
     */
    public FixityResult(final long size, final URI checksum) {
        entry = null;
        computedSize = size;
        computedChecksum = checksum;
    }

    /**
     * @todo Add Documentation.
     */
    public FixityResult(final LowLevelCacheEntry entry, final long size,
                        final URI checksum) {
        this.entry = entry;
        computedSize = size;
        computedChecksum = checksum;
    }

    /**
     * @todo Add Documentation.
     */
    public String getStoreIdentifier() {
        return entry.getExternalIdentifier();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public boolean equals(final Object obj) {

        boolean result = false;
        if (obj instanceof FixityResult) {
            final FixityResult that = (FixityResult) obj;
            result =
                computedSize == that.computedSize &&
                computedChecksum.equals(that.computedChecksum);
        }

        return result;
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public int hashCode() {
        return hash(computedSize, computedChecksum);
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public String toString() {
        return "Fixity: checksum: " + computedChecksum.toString() + " / " +
            Long.toString(computedSize);
    }

    /**
     * @todo Add Documentation.
     */
    public LowLevelCacheEntry getEntry() {
        return entry;
    }

    /**
     * @todo Add Documentation.
     */
    public boolean matches(final URI checksum) {
        return computedChecksum.equals(checksum);
    }

    /**
     * @todo Add Documentation.
     */
    public boolean matches(final long size) {
        return computedSize == size;
    }

    /**
     * Does the fixity entry match the given size and checksum?
     * @param size bitstream size in bytes
     * @param checksum checksum URI in the form urn:DIGEST:RESULT
     * @return true if both conditions matched
     */
    public boolean matches(final long size, final URI checksum) {
        return matches(size) && matches(checksum);
    }

    /**
     * @todo Add Documentation.
     */
    public boolean isSuccess() {
        return status.contains(FixityState.SUCCESS);
    }

    /**
     * @todo Add Documentation.
     */
    public void setRepaired() {
        status.add(FixityState.REPAIRED);
    }
}
