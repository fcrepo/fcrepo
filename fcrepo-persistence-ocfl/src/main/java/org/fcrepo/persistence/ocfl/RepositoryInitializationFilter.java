/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.fcrepo.kernel.api.RepositoryInitializationStatus;

/**
 * Filter which blocks requests if the repository initialization is ongoing
 *
 * @author mikejritter
 */
public class RepositoryInitializationFilter implements Filter {

    @Inject
    private RepositoryInitializationStatus initializationStatus;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("Unable to handle non http request");
        }

        final var httpResponse = (HttpServletResponse) response;
        if (!initializationStatus.isInitializationComplete()) {
            httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        chain.doFilter(request, response);
    }
}
