package org.fcrepo.binary;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.List;
import org.modeshape.jcr.value.binary.StrategyHint;

public class PolicyDecisionPoint {
	private List<Policy> policies;

	private static PolicyDecisionPoint instance = null;


	protected PolicyDecisionPoint() {
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
		this.policies = policies;
	}
}
