
package org.fcrepo.metrics;

import static org.fcrepo.metrics.RegistryService.getMetrics;

import javax.servlet.annotation.WebListener;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.AdminServletContextListener;

/**
 * A ServletContextListener to set the ServletContext attributes that the
 * Metrics servlets expect.
 * 
 * @author Edwin Shin
 * @see <a
 *      href="http://metrics.codahale.com/manual/servlets/">http://metrics.codahale.com/manual/servlets/</a>
 */
@WebListener
public class MetricsContextListener extends AdminServletContextListener {

    @Override
    protected MetricRegistry getMetricRegistry() {
        return getMetrics();
    }

    @Override
    protected HealthCheckRegistry getHealthCheckRegistry() {
        return new HealthCheckRegistry();
    }
}
