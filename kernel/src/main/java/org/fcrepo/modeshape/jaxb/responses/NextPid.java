package org.fcrepo.modeshape.jaxb.responses;

import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "pids", namespace = "")
public class NextPid {

	@XmlElement(name = "pid")
	Set<String> pids;

	public NextPid(Set<String> pids) {
		this.pids = pids;
	}

	public NextPid() {
	}

}
