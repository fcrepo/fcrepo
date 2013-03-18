package org.fcrepo.utils;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DatastreamFixityStatus")
public class FixityResult {
	
	public static final int SUCCESS = 0;
	
	public static final int REPAIRED = 1;
	
	public static final int BAD_CHECKSUM = 2;
	
	public static final int BAD_SIZE = 4;

    @XmlElement
    public String storeIdentifier;
    
    @XmlAttribute
    public int status;

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
        this.entry = null;
    }

    public FixityResult(LowLevelCacheEntry entry) {
        this.entry = entry;
        this.storeIdentifier = entry.getExternalIdentifier();
    }

    public FixityResult(long size, URI checksum) {
        this.entry = null;
        this.computedSize = size;
        this.computedChecksum = checksum;
    }

    public FixityResult(LowLevelCacheEntry entry, long size, URI checksum) {
        this.entry = entry;
        this.computedSize = size;
        this.computedChecksum = checksum;
    }
    
    public boolean equals(Object obj) {

        boolean result = false;
        if (obj instanceof FixityResult) {
            FixityResult that = (FixityResult) obj;
            result = this.computedSize == that.computedSize && this.computedChecksum.equals(that.computedChecksum);

        }

        return result;
    }

    public String toString() {
        return "Fixity: checksum: " + computedChecksum.toString() + " / " + Long.toString(computedSize);
    }

    public LowLevelCacheEntry getEntry() {
        return entry;
    }
}
