package org.fcrepo.jaxb.responses;

import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "objectDatastreams", namespace = "http://www.fedora.info/definitions/1/0/access/")
public class ObjectDatastreams {

	public Set<DatastreamElement> datastreams;

	@XmlType(name = "datastream")
	public static class DatastreamElement {

		public DatastreamElement(String dsid, String label, String mimeType) {
			this.dsid = dsid;
			this.label = label;
			this.mimeType = mimeType;
		}

		@XmlAttribute
		public String dsid;

		@XmlAttribute
		public String label;

		@XmlAttribute
		public String mimeType;

		public DatastreamElement() {
		}
	}
}
