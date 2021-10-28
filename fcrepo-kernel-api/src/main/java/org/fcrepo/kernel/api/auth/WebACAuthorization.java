/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
