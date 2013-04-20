
package org.fcrepo.utils;

import static java.util.Objects.hash;

import java.net.URI;
import java.util.EnumSet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DatastreamFixityStatus")
public class FixityResult {

    public static enum FixityState {
        SUCCESS, REPAIRED, BAD_CHECKSUM, BAD_SIZE
    }

    @XmlElement
    public String storeIdentifier;

    @XmlAttribute
    public EnumSet<FixityState> status = EnumSet.noneOf(FixityState.class);

    @XmlElement
    public long computedSize;

    @XmlElement
    public URI computedChecksum;

    @XmlElement
    public long dsSize;

    @XmlElement
    public String dsChecksumType;

    @XmlElement
    public URI dsChecksum;

    private final LowLevelCacheEntry entry;

    public FixityResult() {
        entry = null;
    }

    public FixityResult(final LowLevelCacheEntry entry) {
        this.entry = entry;
        storeIdentifier = entry.getExternalIdentifier();
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
}
