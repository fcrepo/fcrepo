/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter which blocks requests if the repository initialization is ongoing
 *
 * @author mikejritter
 */
public class RepositoryInitializationFilter implements Filter {

    final Logger logger = LoggerFactory.getLogger(RepositoryInitializationFilter.class);

    @Inject
    private RepositoryInitializer initializer;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        logger.info("Checking request for db init");
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("Unable to handle non-http request");
        }

        final var httpResponse = (HttpServletResponse) response;
        if (!initializer.isInitializationComplete()) {
            httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        chain.doFilter(request, response);
    }
}
