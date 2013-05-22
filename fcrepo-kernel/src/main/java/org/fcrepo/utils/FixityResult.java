
package org.fcrepo.utils;

import static java.util.Objects.hash;

import java.net.URI;
import java.util.EnumSet;

public class FixityResult {

    public static enum FixityState {
        SUCCESS, REPAIRED, BAD_CHECKSUM, BAD_SIZE
    }

    public EnumSet<FixityState> status = EnumSet.noneOf(FixityState.class);

    public long computedSize;

    public URI computedChecksum;

    private final LowLevelCacheEntry entry;

    public FixityResult() {
        entry = null;
    }

    public FixityResult(final LowLevelCacheEntry entry) {
        this.entry = entry;
    }

    public FixityResult(final long size, final URI checksum) {
        entry = null;
        computedSize = size;
        computedChecksum = checksum;
    }

    public FixityResult(final LowLevelCacheEntry entry, final long size,
            final URI checksum) {
        this.entry = entry;
        computedSize = size;
        computedChecksum = checksum;
    }

    public String getStoreIdentifier() {
        return entry.getExternalIdentifier();
    }

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

    @Override
    public int hashCode() {
        return hash(computedSize, computedChecksum);
    }

    @Override
    public String toString() {
        return "Fixity: checksum: " + computedChecksum.toString() + " / " +
                Long.toString(computedSize);
    }

    public LowLevelCacheEntry getEntry() {
        return entry;
    }

    public boolean matches(final URI checksum) {
        return computedChecksum.equals(checksum);
    }

    public boolean matches(final long size) {
        return computedSize == size;
    }

    public boolean matches(final long size, final URI checksum) {
        return matches(size) && matches(checksum);
    }

    public boolean isSuccess() {
        return status.contains(FixityState.SUCCESS);
    }

    public void setRepaired() {
        status.add(FixityState.REPAIRED);
    }
}
