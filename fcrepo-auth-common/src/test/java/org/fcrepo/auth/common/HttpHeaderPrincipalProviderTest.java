/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.fcrepo.auth.common.HttpHeaderPrincipalProvider.HttpHeaderPrincipal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author daines
 * @author bbpennel
 * @since Mar 6, 2014
 */
public class HttpHeaderPrincipalProviderTest {

    @Mock
    private HttpServletRequest request;

    private HttpHeaderPrincipalProvider provider;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        provider = new HttpHeaderPrincipalProvider();
    }

    @AfterEach
    public void close() throws Exception {
        closeable.close();
    }

    @Test
    public void testPrincipalsExtractedFromHeaders() {

        when(request.getHeader("Groups")).thenReturn("a,b");

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(request);

        assertEquals(2, principals.size());
        assertTrue(principals.contains(new HttpHeaderPrincipal("a")),
                "The principals should contain 'a'");
        assertTrue(principals.contains(new HttpHeaderPrincipal("b")),
                "The principals should contain 'b'");

    }

    @Test
    public void testShouldTrimPrincipalNames() {

        when(request.getHeader("Groups")).thenReturn(" a ,b");

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(request);

        assertEquals(2, principals.size());
        assertTrue(principals.contains(new HttpHeaderPrincipal("a")),
                "The principals should contain 'a'");
        assertTrue(principals.contains(new HttpHeaderPrincipal("b")),
                "The principals should contain 'b'");

    }

    @Test
    public void testNoHeaderName() {

        final Set<Principal> principals = provider.getPrincipals(request);

        assertTrue(principals.isEmpty(), "Empty set expected when no header name configured");

    }

    @Test
    public void testNoSeparator() {

        provider.setHeaderName("Groups");

        final Set<Principal> principals = provider.getPrincipals(request);

        assertTrue(principals.isEmpty(), "Empty set expected when no separator name configured");

    }

    @Test
    public void testNoRequest() {

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(null);

        assertTrue(principals.isEmpty(), "Empty set expected when no request supplied");

    }

    @Test
    public void testPrincipalEqualsDifferentClass() {

        when(request.getHeader("Groups")).thenReturn("a");

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(request);

        final Principal principal = principals.iterator().next();

        assertNotEquals(principal, mock(Principal.class),
                "Principals should not be equal if not the same class");

    }

}
