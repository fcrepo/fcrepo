/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import javax.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.security.Principal;
import java.util.Set;

import org.fcrepo.kernel.api.exception.RepositoryConfigurationException;

/**
 * An example principal provider that extracts principals from request headers.
 *
 * @author awoods
 * @since 2015-10-31
 */
public class DelegateHeaderPrincipalProvider extends HttpHeaderPrincipalProvider {

    private static final String SEP = "no-separator";
    protected static final String DELEGATE_HEADER = "On-Behalf-Of";

    public static class DelegatedHeaderPrincipal implements Principal, Serializable {

        private final String name;

        protected DelegatedHeaderPrincipal(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof DelegatedHeaderPrincipal) {
                return ((DelegatedHeaderPrincipal) o).getName().equals(
                        this.getName());
            }
            return false;
        }

        @Override
        public int hashCode() {
            if (name == null) {
                return 0;
            }
            return name.hashCode();
        }

    }

    /**
     * Default Constructor
     */
    public DelegateHeaderPrincipalProvider() {
        super();
        setHeaderName(DELEGATE_HEADER);
        setSeparator(SEP);
    }

    /**
     * @param request from which the principal header is extracted
     * @return null if no delegate found, and the delegate if one found
     * @throws RepositoryConfigurationException if more than one delegate found
     */
    public Principal getDelegate(final HttpServletRequest request) {
        final Set<Principal> principals = getPrincipals(request);
        // No delegate
        if (principals.isEmpty()) {
            return null;
        }

        // One delegate
        if (principals.size() == 1) {
            return principals.iterator().next();
        }

        throw new RepositoryConfigurationException("Too many delegates! " + principals);
    }

    @Override
    public Principal createPrincipal(final String name) {
        return new DelegatedHeaderPrincipal(name.trim());
    }

}
