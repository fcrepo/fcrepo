/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.api.RepositoryInitializationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
public class RepositoryInitializationFilterTest {

    @Mock
    private RepositoryInitializationStatus status;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RepositoryInitializationFilter filter;

    @Test
    public void testDoFilterWhenInitializationComplete() throws IOException, ServletException {
        when(status.isInitializationComplete()).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    public void testDoFilterWhenInitializationNotComplete() throws IOException, ServletException {
        when(status.isInitializationComplete()).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testDoFilterWithNonHttpRequest() {
        final var mockRequest = mock(ServletRequest.class);
        assertThrows(ServletException.class, () -> filter.doFilter(mockRequest, response, filterChain));
    }

    @Test
    public void testDoFilterWithNonHttpResponse() {
        final var mockResponse = mock(ServletResponse.class);
        assertThrows(ServletException.class, () -> filter.doFilter(request, mockResponse, filterChain));
    }
}