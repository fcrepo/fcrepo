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

package org.fcrepo.kernel.services.policy;

import javax.jcr.Node;

import java.util.List;

/**
 * Service Interface implementation for managing and using org.fcrepo.kernel.services.policy.Policy
 * @author osmandin
 * @date Aug 14, 2013
 *
 */
public interface StoragePolicyDecisionPoint {

    /**
     * Add a new storage policy
     * 
     * @param p org.fcrepo.kernel.services.policy object
     */
    public abstract void addPolicy(Policy p);

    /**
     * Given a JCR node (likely a jcr:content node), determine which storage
     * policy should apply
     * 
     * @param n
     * @return
     */
    public abstract String evaluatePolicies(Node n);

    /**
     * Remove a storage policy
     * 
     * @param p org.fcrepo.kernel.services.policy object
     */
    public abstract void removePolicy(Policy p);

    /**
     * Explicitly set the policies this PDP should use
     * 
     * @param policies
     */
    public abstract void setPolicies(List<Policy> policies);

    /**
     * @param policy
     */
    public abstract boolean contains(Policy policy);

    /**
     * clear all policies
     */
    public void  removeAll();

    /**
     * @return policies size
     */
    public abstract int size();
}