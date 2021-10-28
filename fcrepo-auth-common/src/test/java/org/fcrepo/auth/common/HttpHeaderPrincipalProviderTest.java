/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.fcrepo.auth.common.HttpHeaderPrincipalProvider.HttpHeaderPrincipal;

import org.junit.Before;
import org.junit.Test;
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

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        provider = new HttpHeaderPrincipalProvider();
    }

    @Test
    public void testPrincipalsExtractedFromHeaders() {

        when(request.getHeader("Groups")).thenReturn("a,b");

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(request);

        assertEquals(2, principals.size());
        assertTrue("The principals should contain 'a'", principals
                .contains(new HttpHeaderPrincipal("a")));
        assertTrue("The principals should contain 'b'", principals
                .contains(new HttpHeaderPrincipal("b")));

    }

    @Test
    public void testShouldTrimPrincipalNames() {

        when(request.getHeader("Groups")).thenReturn(" a ,b");

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(request);

        assertEquals(2, principals.size());
        assertTrue("The principals should contain 'a'", principals
                .contains(new HttpHeaderPrincipal("a")));
        assertTrue("The principals should contain 'b'", principals
                .contains(new HttpHeaderPrincipal("b")));

    }

    @Test
    public void testNoHeaderName() {

        final Set<Principal> principals = provider.getPrincipals(request);

        assertTrue("Empty set expected when no header name configured", principals.isEmpty());

    }

    @Test
    public void testNoSeparator() {

        provider.setHeaderName("Groups");

        final Set<Principal> principals = provider.getPrincipals(request);

        assertTrue("Empty set expected when no separator name configured", principals.isEmpty());

    }

    @Test
    public void testNoRequest() {

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(null);

        assertTrue("Empty set expected when no request supplied", principals.isEmpty());

    }

    @Test
    public void testPrincipalEqualsDifferentClass() {

        when(request.getHeader("Groups")).thenReturn("a");

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(request);

        final Principal principal = principals.iterator().next();

        assertNotEquals("Principals should not be equal if not the same class",
                principal, mock(Principal.class));

    }

}
