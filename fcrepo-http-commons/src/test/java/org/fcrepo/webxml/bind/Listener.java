package org.fcrepo.webxml.bind;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(namespace="http://java.sun.com/xml/ns/javaee", name="listener")
public class Listener extends Displayable {
	
	public Listener() {}
	
	public Listener(String displayName, String className) {
		this.displayName = displayName;
		this.className = className;
	}

	@XmlElement(namespace="http://java.sun.com/xml/ns/javaee", name="listener-class")
	String className;

	public String className() {
		return this.className;
	}
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof Listener) {
			Listener that = (Listener)object;
			boolean className = (this.className == null) ? that.className == null :
				this.className.equals(that.className);
			boolean displayName = (this.displayName == null) ? that.displayName == null :
				this.displayName.equals(that.displayName);
			return className && displayName;
		}
		return false;
	}

}
