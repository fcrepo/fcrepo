package org.fcrepo.binary;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.List;
import org.modeshape.jcr.value.binary.StrategyHint;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class PolicyDecisionPoint {

	private static final Logger logger = getLogger(MimeTypePolicy.class);

	private List<Policy> policies;

	private static PolicyDecisionPoint instance = null;


	protected PolicyDecisionPoint() {
		logger.debug("Initializing binary PolicyDecisionPoint");
		policies = new ArrayList<Policy>();
	}


	public static PolicyDecisionPoint getInstance() {
		if(instance == null) {
			instance = new PolicyDecisionPoint();
		}
		return instance;
	}

	public void addPolicy(Policy p) {
		policies.add(p);
	}

	public StrategyHint evaluatePolicies(Node n) {
		for(Policy p : policies) {
			StrategyHint h = p.evaluatePolicy(n);
			if(h != null) {
				return h;
			}
		}

		return null;
	}


	public void setPolicies(List<Policy> policies) {
		logger.debug("Adding policies to binary PolicyDecisionPoint: {}", policies.toString());
		this.policies = policies;
	}
}
