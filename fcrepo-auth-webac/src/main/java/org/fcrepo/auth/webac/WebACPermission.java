/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.webac;

import java.net.URI;

import org.apache.shiro.authz.Permission;

/**
 * A WebAC permission represents a particular mode of access (e.g., acl:read) to a particular resource. Both the mode
 * and resource are URIs. One WebAC permission implies another if and only if their mode and resource URIs are both
 * equal to the other's.
 *
 * @author peichman
 */
public class WebACPermission implements Permission {

    private final URI resource;

    private final URI mode;

    /**
     * @param mode ACL access mode
     * @param resource resource to be accessed
     */
    public WebACPermission(final URI mode, final URI resource) {
        this.mode = mode;
        this.resource = resource;
    }

    /**
     * One WebACPermission implies another if they are equal (i.e., have the same mode and resource URIs).
     *
     * @param p permission to compare to
     */
    @Override
    public boolean implies(final Permission p) {
        return equals(p);
    }

    /**
     * One WebACPermission equals another if they have the same mode and resource URIs.
     *
     * @param o object to compare to
     */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof WebACPermission) {
            final WebACPermission perm = (WebACPermission) o;
            return perm.getResource().equals(resource) && perm.getMode().equals(mode);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mode == null) ? 0 : mode.hashCode());
        result = prime * result + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    /**
     * @return the mode
     */
    private URI getMode() {
        return mode;
    }

    /**
     * @return the resource
     */
    private URI getResource() {
        return resource;
    }

    @Override
    public String toString() {
        return "[" + mode.toString() + " " + resource.toString() + "]";
    }

}
