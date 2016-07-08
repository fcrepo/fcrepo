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

import org.fcrepo.kernel.api.models.FedoraResource;

/**
 * A binary storage policy definition and evaluation machinery
 * @author cbeer
 * @since Apr 25, 2013
 */
public interface StoragePolicy {

    /**
     * Evaluate the policy; if the policy matches, return the
     * binary storage hint. If not, return null.
     * @param resource the resource
     * @return the binary storage hint
     */
    String evaluatePolicy(final FedoraResource resource);
}
