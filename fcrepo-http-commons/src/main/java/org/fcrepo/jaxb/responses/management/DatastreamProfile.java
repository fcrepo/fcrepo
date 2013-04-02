
package org.fcrepo.jaxb.responses.management;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
    public static String convertDateToXSDString(long date) {
        final DateTime dt = new DateTime(date);
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        return fmt.print(dt);
    }

}
