/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import static java.util.Collections.emptySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

/**
 * An example principal provider that extracts principals from request headers.
 *
 * @author Gregory Jansen
 * @author Mike Daines
 * @see PrincipalProvider
 */
public class HttpHeaderPrincipalProvider extends AbstractPrincipalProvider {

    public static class HttpHeaderPrincipal implements Principal {

        private final String name;

        protected HttpHeaderPrincipal(final String name) {
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
            if (o instanceof HttpHeaderPrincipal) {
                return ((HttpHeaderPrincipal) o).getName().equals(
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

    private String headerName;

    private String separator = "";

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpHeaderPrincipalProvider.class);

    /**
     * @param headerName The name of the header from which to extract principals
     */
    public void setHeaderName(final String headerName) {
        this.headerName = headerName;
    }

    /**
     * @param separator The string by which to split header values
     */
    public void setSeparator(final String separator) {
        this.separator = separator;
    }

    /*
    * (non-Javadoc)
    * @see
    * org.fcrepo.auth.PrincipalProvider#getPrincipals(javax.servlet.http.HttpServletRequest)
    */
    @Override
    public Set<Principal> getPrincipals(final HttpServletRequest request) {
        LOGGER.debug("Checking for principals using {}", HttpHeaderPrincipalProvider.class.getSimpleName());

        if (headerName == null || separator == null) {
            LOGGER.debug("headerName or separator not initialized");
            return emptySet();
        }

        LOGGER.debug("Trying to get principals from header: {}; separator: {}", headerName, separator);

        if (request == null) {
            LOGGER.debug("Servlet request from servletCredentials was null");
            return emptySet();
        }

        final String value = request.getHeader(headerName);

        if (value == null) {
            LOGGER.debug("Value for header {} is null", headerName);
            return emptySet();
        }

        final String[] names = value.split(separator);

        final Set<Principal> principals = new HashSet<>();

        for (final String name : names) {
            LOGGER.debug("Adding HTTP header-provided principal: {}", name.trim());
            principals.add(createPrincipal(name));
        }

        return principals;

    }

    protected Principal createPrincipal(final String name) {
        return new HttpHeaderPrincipal(name.trim());
    }

}
