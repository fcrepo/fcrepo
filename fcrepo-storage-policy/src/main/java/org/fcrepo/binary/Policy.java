package org.fcrepo.binary;

import javax.jcr.Node;

public interface Policy {
    String evaluatePolicy(Node n);
}
