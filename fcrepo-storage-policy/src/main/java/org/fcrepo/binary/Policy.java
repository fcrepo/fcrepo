package org.fcrepo.binary;

import org.modeshape.jcr.value.binary.StrategyHint;

import javax.jcr.Node;

public interface Policy {
	StrategyHint evaluatePolicy(Node n);
}
