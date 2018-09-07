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
package org.fcrepo.auth.webac;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author whikloj
 * @author acoburn
 * @since 2015-08-25
 */
public class WebACAuthorization {

    private final Set<String> agents = new HashSet<>();

    private final Set<String> agentClasses = new HashSet<>();

    private final Set<URI> modes = new HashSet<>();

    private final Set<String> accessTo = new HashSet<>();

    private final Set<String> accessToClass = new HashSet<>();

    private final Set<String> agentGroups = new HashSet<>();

    private final Set<String> defaults = new HashSet<>();

    /**
     * Constructor
     *
     * @param agents The acl:agent values
     * @param agentClasses the acl:agentClass values
     * @param modes the acl:mode values
     * @param accessTo the acl:accessTo values
     * @param accessToClass the acl:accessToClass values
     * @param agentGroups the acl:agentGroup values
     * @param defaults the acl:default values
     */
    public WebACAuthorization(final Collection<String> agents, final Collection<String> agentClasses,
            final Collection<URI> modes, final Collection<String> accessTo, final Collection<String> accessToClass,
            final Collection<String> agentGroups, final Collection<String> defaults) {
        this.agents.addAll(agents);
        this.agentClasses.addAll(agentClasses);
        this.modes.addAll(modes);
        this.accessTo.addAll(accessTo);
        this.accessToClass.addAll(accessToClass);
        this.agentGroups.addAll(agentGroups);
        this.defaults.addAll(defaults);
    }

    /**
     * Get the set of acl:agents, empty set if none.
     *
     * @return set of acl:agents
     */
    public Set<String> getAgents() {
        return agents;
    }

    /**
     * Get the set of acl:agentClasses, empty set if none.
     * 
     * @return set of acl:agentClasses
     */
    public Set<String> getAgentClasses() {
        return agentClasses;
    }

    /**
     * Get the set of acl:modes, empty set if none.
     *
     * @return set of acl:modes
     */
    public Set<URI> getModes() {
        return modes;
    }

    /**
     * Get the set of strings directly linked from this ACL, empty set if none.
     *
     * @return set of String
     */
    public Set<String> getAccessToURIs() {
        return accessTo;
    }

    /**
     * Get the set of strings describing the rdf:types for this ACL, empty set if none.
     *
     * @return set of Strings
     */
    public Set<String> getAccessToClassURIs() {
        return accessToClass;
    }

    /**
     * Get the set of strings describing the agent groups for this ACL, empty set if none.
     *
     * @return set of Strings
     */
    public Set<String> getAgentGroups() {
        return agentGroups;
    }

    /**
     * Get the set of strings describing the defaults for this ACL, empty set if none.
     *
     * @return set of Strings
     */
    public Set<String> getDefaults() {
        return defaults;
    }
}
