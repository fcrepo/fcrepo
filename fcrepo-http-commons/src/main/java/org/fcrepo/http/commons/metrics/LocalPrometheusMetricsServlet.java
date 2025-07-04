/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.metrics;
// TODO: Re-enable https://fedora-repository.atlassian.net/browse/FCREPO-4021
/*
import io.prometheus.client.CollectorRegistry;
import org.springframework.web.context.support.WebApplicationContextUtils;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
*/
/**
 * This class is an extension of Prometheus's MetricsServlet. It only exists because there isn't an easy way to
 * set the CollectorRegistry on with a Spring bean.
 *
 * @author pwinckles
 */
/*
public class LocalPrometheusMetricsServlet extends PrometheusMetricsServlet {

    @Override
    public void init(final ServletConfig config) throws ServletException {
        final var context = WebApplicationContextUtils
                .getRequiredWebApplicationContext(config.getServletContext());
        final var collector = context.getBean(CollectorRegistry.class);

        try {
            final var field = PrometheusMetricsServlet.class.getDeclaredField("registry");
            field.setAccessible(true);
            field.set(this, collector);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new ServletException(e);
        }

        super.init(config);
    }

}
*/