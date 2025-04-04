/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common;

import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_USER_ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcrepo.auth.common.utils.TestPrincipal;

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for {@link ServletContainerAuthFilter}
 *
 * @author whikloj
 */
@ExtendWith(MockitoExtension.class)
public class ServletContainerAuthFilterTest {

    private ServletContainerAuthFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private Subject subject;

    @BeforeEach
    public void setUp() {
        filter = new ServletContainerAuthFilter();
        final Principal principal = new TestPrincipal("testUser");
        // Set the mocked subject in Shiro's ThreadContext
        org.apache.shiro.util.ThreadContext.bind(subject);
        when(request.getUserPrincipal()).thenReturn(principal);
    }

    @Test
    public void testFilterWithAdminUser() throws IOException, ServletException {
        when(request.isUserInRole(FEDORA_ADMIN_ROLE)).thenReturn(true);
        when(request.isUserInRole(FEDORA_USER_ROLE)).thenReturn(false);

        // Call the filter method
        filter.doFilter(request, response, chain);

        // Verify that the roles were added to the Subject
        final var principalCaptor = ArgumentCaptor.forClass(ContainerAuthToken.class);
        verify(subject).login(principalCaptor.capture());

        final var token = principalCaptor.getValue();
        assertInstanceOf(BasicUserPrincipal.class, token.getPrincipal());
        final var principal = (BasicUserPrincipal) token.getPrincipal();
        assertEquals("testUser", principal.getName());

        // Verify that the roles were added to the Subject
        final var roles = token.getRoles();
        assertEquals(1, roles.size());
        assertEquals(FEDORA_ADMIN_ROLE, roles.iterator().next().getName());

        // Verify that the filter chain was called
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testFilterWithNormalUser() throws IOException, ServletException {
        when(request.isUserInRole(FEDORA_ADMIN_ROLE)).thenReturn(false);
        when(request.isUserInRole(FEDORA_USER_ROLE)).thenReturn(true);

        // Call the filter method
        filter.doFilter(request, response, chain);

        // Verify that the roles were added to the Subject
        final var principalCaptor = ArgumentCaptor.forClass(ContainerAuthToken.class);
        verify(subject).login(principalCaptor.capture());

        final var token = principalCaptor.getValue();
        assertInstanceOf(BasicUserPrincipal.class, token.getPrincipal());
        final var principal = (BasicUserPrincipal) token.getPrincipal();
        assertEquals("testUser", principal.getName());

        // Verify that the roles were added to the Subject
        final var roles = token.getRoles();
        assertEquals(1, roles.size());
        assertEquals(FEDORA_USER_ROLE, roles.iterator().next().getName());

        // Verify that the filter chain was called
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testFilterWithNoUser() throws IOException, ServletException {
        when(request.getUserPrincipal()).thenReturn(null);
        filter.doFilter(request, response, chain);
        verify(subject).logout();
        verify(chain).doFilter(request, response);
    }
}
