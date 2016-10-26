/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * Service Interface implementation for managing and using {@link org.fcrepo.kernel.api.services.policy.StoragePolicy}
 * @author osmandin
 * @since Aug 14, 2013
 *
 */
public interface StoragePolicyDecisionPoint extends List<StoragePolicy> {

    /**
     * Given a fedora resource (likely a fedora:Binary resource), determine which storage
     * policy should apply
     *
     * @param resource the resource
     * @return storage policy
     */
    String evaluatePolicies(final FedoraResource resource);

    /**
     * Explicitly set the policies this PDP should use
     *
     * @param policies the policies
     */
    void setPolicies(final List<StoragePolicy> policies);

}
