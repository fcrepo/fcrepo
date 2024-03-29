/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.webac;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.fcrepo.kernel.api.auth.WebACAuthorization;

/**
 * @author whikloj
 * @author acoburn
 * @since 2015-08-25
 */
public class WebACAuthorizationImpl implements WebACAuthorization {

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
    public WebACAuthorizationImpl(final Collection<String> agents, final Collection<String> agentClasses,
                                  final Collection<URI> modes, final Collection<String> accessTo,
                                  final Collection<String> accessToClass, final Collection<String> agentGroups,
                                  final Collection<String> defaults) {
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
