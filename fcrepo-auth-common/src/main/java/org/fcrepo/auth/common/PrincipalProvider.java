/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.Set;

/**
 * This interface provides a way for authentication code to communicate generic
 * credentials to authorization delegates. An implementation of this interface
 * could perform a query to determine group membership, for example.
 * <p>
 * The ServletContainerAuthenticationProvider's principalProviders set may be
 * configured with zero or more instances of implementations of this interface,
 * which it will consult during authentication. The union of the results will be
 * assigned to the FEDORA_ALL_PRINCIPALS session attribute.
 * </p>
 *
 * @author Gregory Jansen
 * @see HttpHeaderPrincipalProvider
 */
public interface PrincipalProvider extends Filter {

    /**
     * Extract principals from the provided HttpServletRequest.
     * <p>
     * If no principals can be extracted, implementations of this method
     * should return the empty set rather than null.
     * </p>
     *
     * @param request the request
     * @return a set of security principals
     */
    Set<Principal> getPrincipals(HttpServletRequest request);

}
