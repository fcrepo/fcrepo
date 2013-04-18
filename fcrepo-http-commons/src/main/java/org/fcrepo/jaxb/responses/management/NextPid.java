
package org.fcrepo.jaxb.responses.management;

import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "pids", namespace = "")
public class NextPid {

    @XmlElement(name = "pid", namespace = "")
    public Set<String> pids;

    public NextPid(final Set<String> pids) {
        this.pids = pids;
    }

    public NextPid() {
    }

}
