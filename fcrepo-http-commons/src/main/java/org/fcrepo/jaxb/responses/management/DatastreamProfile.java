
package org.fcrepo.jaxb.responses.management;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.fcrepo.Datastream;
import org.fcrepo.utils.LowLevelCacheEntry;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

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
    public String dsCreateDate;

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
     * @param date Instance of java.util.Date.
     * @return the lexical form of the XSD dateTime value, e.g.
     *         "2006-11-13T09:40:55.001Z".
     */
    public static String convertDateToXSDString(final long date) {
        final DateTime dt = new DateTime(date);
        final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        return fmt.print(dt);
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
    public List<String> storeIdentifiers ;

    public DSStores(){
       this.storeIdentifiers = new ArrayList();
    }

    public DSStores(Datastream datastream, Set cacheEntries)  {
        this.storeIdentifiers = new ArrayList();
        for (Iterator it = cacheEntries.iterator(); it.hasNext();) {
            LowLevelCacheEntry cacheEntry = (LowLevelCacheEntry) it.next();
            storeIdentifiers.add(cacheEntry.getExternalIdentifier());
            }
        }
    }

}
