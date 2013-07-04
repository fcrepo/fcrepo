/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.binary;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service that evaluates a set of storage policies for an object and provides
 * storage hints for a binary stream
 * 
 * @author cbeer
 * @date Apr 25, 2013
 */
public class PolicyDecisionPoint {

    private static final Logger LOGGER = getLogger(MimeTypePolicy.class);

    private List<Policy> policies;

    /**
     * Initialize the policy storage machinery
     */
    public PolicyDecisionPoint() {
        LOGGER.debug("Initializing binary PolicyDecisionPoint");
        policies = new ArrayList<Policy>();
    }

    /**
     * Add a new storage policy
     * 
     * @param p
     */
    public void addPolicy(final Policy p) {
        policies.add(p);
    }

    /**
     * Given a JCR node (likely a jcr:content node), determine which storage
     * policy should apply
     * 
     * @param n
     * @return
     */
    public String evaluatePolicies(final Node n) {
        for (final Policy p : policies) {
            final String h = p.evaluatePolicy(n);
            if (h != null) {
                return h;
            }
        }
        return null;
    }

    /**
     * Explicitly set the policies this PDP should use
     * 
     * @param policies
     */
    public void setPolicies(final List<Policy> policies) {
        LOGGER.debug("Adding policies to " + "PolicyDecisionPoint: {}",
                policies.toString());
        this.policies = policies;
    }
}
