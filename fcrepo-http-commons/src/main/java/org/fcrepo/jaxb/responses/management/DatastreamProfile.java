
package org.fcrepo.jaxb.responses.management;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fcrepo.Datastream;
import org.fcrepo.utils.LowLevelCacheEntry;

@XmlRootElement(name = "datastreamProfile", namespace = "http://www.fedora.info/definitions/1/0/management/")
public class DatastreamProfile {

    @XmlAttribute
    public String pid;

    @XmlAttribute
    public String dsID;

    @XmlElement
    public String dsLabel;

    @XmlElement
    public String dsOwnerId;

    @XmlElement
    public String dsVersionID;

    @XmlElement
    public Date dsLastModifiedDate;

    @XmlElement
    public Date dsCreateDate;

    @XmlElement
    public DatastreamStates dsState;

    @XmlElement
    public String dsMIME;

    @XmlElement
    public URI dsFormatURI;

    @XmlElement
    public DatastreamControlGroup dsControlGroup;

    @XmlElement
    public long dsSize;

    @XmlElement
    public String dsVersionable;

    @XmlElement
    public String dsInfoType;

    @XmlElement
    public String dsLocation;

    @XmlElement
    public String dsLocationType;

    @XmlElement
    public String dsChecksumType;

    @XmlElement
    public URI dsChecksum;

    @XmlElement
    public DSStores dsStores;

    public static enum DatastreamControlGroup {
        M, E, R
    }

    public static enum DatastreamStates {
        A, D, I
    }

    /**
     * adds the datastream store information to the datastream profile output.
     * 
     * datastream profile output in fcrepo4 no longer matches output from
     * fcrepo3.x 
     * 
     */
    public static class DSStores {

        @XmlElement(name = "dsStore")
        public List<String> storeIdentifiers;

        public DSStores() {
            this.storeIdentifiers = new ArrayList<String>();
        }

        public DSStores(final Datastream datastream, final Set<?> cacheEntries) {
            this.storeIdentifiers = new ArrayList<String>();
            for (final Object name : cacheEntries) {
                final LowLevelCacheEntry cacheEntry =
                        (LowLevelCacheEntry) name;
                storeIdentifiers.add(cacheEntry.getExternalIdentifier());
            }
        }
    }

}
