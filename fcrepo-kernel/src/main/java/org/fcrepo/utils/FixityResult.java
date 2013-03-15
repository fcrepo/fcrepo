package org.fcrepo.utils;

import java.net.URI;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DatastreamFixityStatus")
public class FixityResult {

    @XmlElement
    public String storeIdentifier;

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
	
	@XmlElement
	public boolean validChecksum;
	
	@XmlElement
	public boolean validSize;
    public FixityResult() {

    }

    public FixityResult(long size, URI checksum) {
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
}
