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
package org.fcrepo.auth.common;

import static java.util.Collections.emptySet;

import org.modeshape.jcr.api.ServletCredentials;

import javax.jcr.Credentials;
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
public class HttpHeaderPrincipalProvider implements PrincipalProvider {

    protected static class HttpHeaderPrincipal implements Principal {

        private final String name;

        HttpHeaderPrincipal(final String name) {
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
     * org.fcrepo.auth.PrincipalProvider#getPrincipals(javax.jcr.Credentials)
     */
    @Override
    public Set<Principal> getPrincipals(final Credentials credentials) {

        if (headerName == null || separator == null) {
            return emptySet();
        }

        if (!(credentials instanceof ServletCredentials)) {
            return emptySet();
        }

        final ServletCredentials servletCredentials =
                (ServletCredentials) credentials;

        final HttpServletRequest request = servletCredentials.getRequest();

        if (request == null) {
            return emptySet();
        }

        final String value = request.getHeader(headerName);

        if (value == null) {
            return emptySet();
        }

        final String[] names = value.split(separator);

        final Set<Principal> principals = new HashSet<>();

        for (final String name : names) {
            principals.add(new HttpHeaderPrincipal(name.trim()));
        }

        return principals;

    }

}
