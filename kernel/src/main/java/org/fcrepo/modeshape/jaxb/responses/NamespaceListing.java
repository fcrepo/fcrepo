package org.fcrepo.modeshape.jaxb.responses;

import java.net.URI;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "namespaceRegistry", namespace = "http://www.fedora.info/definitions/1/0/management/")
public class NamespaceListing {

	@XmlElement(name = "namespace")
	public Set<Namespace> namespaces;

	public NamespaceListing(Set<Namespace> nses) {
		this.namespaces = nses;
	}

	public NamespaceListing() {
	}

	public static class Namespace {

		@XmlAttribute
		public String prefix;

		@XmlAttribute(name = "URI")
		public URI uri;

		public Namespace(String prefix, URI uri) {
			this.prefix = prefix;
			this.uri = uri;
		}

		public Namespace() {
		}
	}

}
