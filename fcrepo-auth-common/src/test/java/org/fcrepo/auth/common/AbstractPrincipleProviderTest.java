/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcrepo.auth.common.utils.TestPrincipal;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for {@link AbstractPrincipalProvider}
 *
 * @author whikloj
 */
@ExtendWith(MockitoExtension.class)
public class AbstractPrincipleProviderTest {

    private TestProvider provider;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private FilterChain mockChain;

    @Mock
    private Subject mockSubject;

    @BeforeEach
    public void setUp() {
        provider = new TestProvider();

        // Set the mocked subject in Shiro's ThreadContext
        org.apache.shiro.util.ThreadContext.bind(mockSubject);
        when(mockSubject.getPrincipals()).thenReturn(new SimplePrincipalCollection());
    }

    /**
     * Principals are set, so the filter should add them to the Subject
     */
    @Test
    public void testWithPrincipal() throws Exception {

        provider.setPrincipals(Set.of(new TestPrincipal("a")));
        provider.doFilter(mockRequest, mockResponse, mockChain);

        final var principalCaptor = ArgumentCaptor.forClass(PrincipalCollection.class);
        verify(mockSubject).runAs(principalCaptor.capture());

        final var principals = principalCaptor.getValue();
        assertFalse(principals.isEmpty());
        assertEquals(1, principals.asList().size());

        final var principal = principals.asList().get(0);
        assertEquals(TestPrincipal.class, principal.getClass());
        assertEquals("a", ((TestPrincipal)principal).getName());
    }

    @Test
    public void testWithMorePrincipals() throws IOException, ServletException {
        provider.setPrincipals(Set.of(new TestPrincipal("a"), new TestPrincipal("b")));
        provider.doFilter(mockRequest, mockResponse, mockChain);

        final var principalCaptor = ArgumentCaptor.forClass(PrincipalCollection.class);
        verify(mockSubject).runAs(principalCaptor.capture());

        final var principals = principalCaptor.getValue();
        assertFalse(principals.isEmpty());
        assertEquals(2, principals.asList().size());

        for (final var principal : principals.asList()) {
            assertEquals(TestPrincipal.class, principal.getClass());
            assertTrue(((TestPrincipal)principal).getName().equals("a") ||
                    ((TestPrincipal)principal).getName().equals("b"));
        }
    }

    /**
     * No principals are set, so the filter should not add any principals to the Subject
     */
    @Test
    public void testWithoutPrincipal() throws Exception {
        provider.doFilter(mockRequest, mockResponse, mockChain);

        final var principalCaptor = ArgumentCaptor.forClass(PrincipalCollection.class);
        verify(mockSubject, never()).runAs(principalCaptor.capture());
    }

    /**
     * Test implementation of PrincipalProvider for testing.
     */
    static class TestProvider extends AbstractPrincipalProvider {

        private Set<Principal> principals = new HashSet<>();

        @Override
        public Set<Principal> getPrincipals(final HttpServletRequest request) {
            return principals;
        }

        public void setPrincipals(final Set<Principal> principals) {
            if (principals == null) {
                this.principals = Collections.emptySet();
            } else {
                this.principals = Collections.unmodifiableSet(principals);
            }
        }
    }


}
