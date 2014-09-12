/**
 * Copyright 2014 DuraSpace, Inc.
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.fcrepo.kernel.services.policy.StoragePolicy;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * Service that evaluates a set of storage policies for an object and provides
 * storage hints for a binary stream
 *
 * @author cbeer
 * @since Apr 25, 2013
 */
public class StoragePolicyDecisionPointImpl extends ArrayList<StoragePolicy> implements StoragePolicyDecisionPoint {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = getLogger(StoragePolicyDecisionPointImpl.class);

    /**
     * Initialize the policy storage machinery
     */
    public StoragePolicyDecisionPointImpl() {
        LOGGER.debug("Initializing binary StoragePolicyDecisionPointImpl");
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint#evaluatePolicies(javax.jcr.Node)
     */
    @Override
    public String evaluatePolicies(final Node n) {
        for (final StoragePolicy p : this) {
            final String h = p.evaluatePolicy(n);
            if (h != null) {
                return h;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint#setPolicies(java.util.List)
     */
    @Override
    public void setPolicies(final List<StoragePolicy> policies) {
        clear();
        addAll(policies);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final ToStringHelper helper = MoreObjects.toStringHelper(this);
        for (final StoragePolicy p : this) {
            helper.add(p.getClass().getName(), p);
        }
        return "policies=" + helper.toString();
    }

}
