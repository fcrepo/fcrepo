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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.fcrepo.auth.common.HttpHeaderPrincipalProvider.HttpHeaderPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Set;

/**
 * @author daines
 * @author bbpennel
 * @since Mar 6, 2014
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HttpHeaderPrincipalProviderTest {

    @Mock
    private HttpServletRequest request;

    private HttpHeaderPrincipalProvider provider;

    @BeforeEach
    public void setUp() {
        provider = new HttpHeaderPrincipalProvider();
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

        assertNotEquals(principal, mock(Principal.class), "Principals should not be equal if not the same class");

    }

}
