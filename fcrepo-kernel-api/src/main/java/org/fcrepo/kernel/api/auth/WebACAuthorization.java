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
package org.fcrepo.kernel.api.auth;

import java.net.URI;
import java.util.Set;

/**
 * @author whikloj
 * @author acoburn
 * @since 2015-08-25
 */
public interface WebACAuthorization {

    /**
     * Get the set of acl:agents, empty set if none.
     *
     * @return set of acl:agents
     */
    Set<String> getAgents();

    /**
     * Get the set of acl:agentClasses, empty set if none.
     * 
     * @return set of acl:agentClasses
     */
    Set<String> getAgentClasses();

    /**
     * Get the set of acl:modes, empty set if none.
     *
     * @return set of acl:modes
     */
    Set<URI> getModes();

    /**
     * Get the set of strings directly linked from this ACL, empty set if none.
     *
     * @return set of String
     */
    Set<String> getAccessToURIs();

    /**
     * Get the set of strings describing the rdf:types for this ACL, empty set if none.
     *
     * @return set of Strings
     */
    Set<String> getAccessToClassURIs();

    /**
     * Get the set of strings describing the agent groups for this ACL, empty set if none.
     *
     * @return set of Strings
     */
    Set<String> getAgentGroups();

    /**
     * Get the set of strings describing the defaults for this ACL, empty set if none.
     *
     * @return set of Strings
     */
    Set<String> getDefaults();
}
