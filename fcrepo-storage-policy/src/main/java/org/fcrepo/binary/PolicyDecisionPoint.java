package org.fcrepo.binary;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class PolicyDecisionPoint {

    private static final Logger logger = getLogger(MimeTypePolicy.class);

    private List<Policy> policies;

    public PolicyDecisionPoint() {
        logger.debug("Initializing binary PolicyDecisionPoint");
        policies = new ArrayList<Policy>();
    }

    /**
     * Add a new storage policy
     * @param p
     */
    public void addPolicy(final Policy p) {
        policies.add(p);
    }

    /**
     * Given a JCR node (likely a jcr:content node), determine which storage policy should apply
     * @param n
     * @return
     */
    public String evaluatePolicies(final Node n) {
        for(Policy p : policies) {
            String h = p.evaluatePolicy(n);
            if(h != null) {
                return h;
            }
        }

        return null;
    }

    public void setPolicies(final List<Policy> policies) {
        logger.debug("Adding policies to binary PolicyDecisionPoint: {}", policies.toString());
        this.policies = policies;
    }
}
