/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.servlet.jakarta.exporter.MetricsServlet;
import org.springframework.web.context.support.WebApplicationContextUtils;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;

/**
 * This class is an extension of Prometheus's MetricsServlet. It only exists because there isn't an easy way to
 * set the CollectorRegistry on with a Spring bean.
 *
 * @author pwinckles
 */
public class PrometheusMetricsServlet extends MetricsServlet {

    @Override
    public void init(final ServletConfig config) throws ServletException {
        final var context = WebApplicationContextUtils
                .getRequiredWebApplicationContext(config.getServletContext());
        final var collector = context.getBean(CollectorRegistry.class);

        try {
            final var field = MetricsServlet.class.getDeclaredField("registry");
            field.setAccessible(true);
            field.set(this, collector);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new ServletException(e);
        }

        super.init(config);
    }

}
