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

package org.fcrepo.storage.policy;

import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.services.policy.StoragePolicy;
import org.slf4j.Logger;

import javax.jcr.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Service that evaluates a set of storage policies for an object and provides
 * storage hints for a binary stream
 * 
 * @author cbeer
 * @date Apr 25, 2013
 */
public class StoragePolicyDecisionPointImpl implements StoragePolicyDecisionPoint {

    private static final Logger LOGGER = getLogger(StoragePolicyDecisionPointImpl.class);

    private List<StoragePolicy> policies;

    /**
     * Initialize the policy storage machinery
     */
    public StoragePolicyDecisionPointImpl() {
        LOGGER.debug("Initializing binary StoragePolicyDecisionPointImpl");
        policies = new ArrayList<StoragePolicy>();
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint#addPolicy(org.fcrepo.kernel.services.policy.StoragePolicy)
     */
    @Override
    public void addPolicy(final org.fcrepo.kernel.services.policy.StoragePolicy p) {
        policies.add(p);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint#evaluatePolicies(javax.jcr.Node)
     */
    @Override
    public String evaluatePolicies(final Node n) {
        for (final StoragePolicy p : policies) {
            final String h = p.evaluatePolicy(n);
            if (h != null) {
                return h;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint#addPolicy(org.fcrepo.kernel.services.policy.StoragePolicy)
     */
    @Override
    public void removePolicy(final StoragePolicy p) {
        policies.remove(p);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint#addPolicy(org.fcrepo.kernel.services.policy.StoragePolicy)
     */
    @Override
    public void removeAll() {
        policies.clear();
    }

    /**
     * For now: simple contains implementation
     * 
     * @param p
     */
    @Override
    public boolean contains(StoragePolicy p) {
        return policies.contains(p);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint#setPolicies(java.util.List)
     */
    @Override
    public void setPolicies(final List<StoragePolicy> policies) {
        this.policies = policies;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "policies=" + policies;
    }

    /**
     * @return policies size
     */
    @Override
    public int size() {
        return policies.size();
    }
}
