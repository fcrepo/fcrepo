/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.api.services.policy;

import java.util.List;

import javax.jcr.Node;

/**
 * Service Interface implementation for managing and using {@link org.fcrepo.kernel.api.services.policy.StoragePolicy}
 * @author osmandin
 * @since Aug 14, 2013
 *
 */
public interface StoragePolicyDecisionPoint extends List<StoragePolicy> {

    /**
     * Given a JCR node (likely a jcr:content node), determine which storage
     * policy should apply
     *
     * @param n the node
     * @return storage policy
     */
    String evaluatePolicies(final Node n);

    /**
     * Explicitly set the policies this PDP should use
     *
     * @param policies the policies
     */
    void setPolicies(final List<StoragePolicy> policies);

}
