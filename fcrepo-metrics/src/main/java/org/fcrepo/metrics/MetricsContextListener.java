
package org.fcrepo.metrics;

import static org.fcrepo.metrics.RegistryService.getMetrics;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.AdminServletContextListener;

/**
 * A ServletContextListener to set the ServletContext attributes that the
 * Metrics servlets expect and initialize JMX reporting if the System property 
 * <code>fcrepo.metrics.jms</code> is set to "true".
 * 
 * @author Edwin Shin
 * @see <a href="http://metrics.codahale.com/manual/servlets/">http://metrics.codahale.com/manual/servlets/</a>
 */
@WebListener
public class MetricsContextListener extends AdminServletContextListener {

    private static final Logger LOGGER =
            getLogger(AdminServletContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        super.contextInitialized(event);

        if (reportMetricsViaJmx()) {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final JmxReporter reporter =
                    JmxReporter.forRegistry(getMetrics()).registerWith(mbs)
                            .inDomain("org.fcrepo").convertDurationsTo(
                                    TimeUnit.MILLISECONDS).convertRatesTo(
                                    TimeUnit.SECONDS).filter(MetricFilter.ALL)
                            .build();
            reporter.start();
        }
    }

    /**
     * 
     * @return true iff the System property <code>fcrepo.metrics.jmx</code> is
     * true.
     */
    public boolean reportMetricsViaJmx() {
        String useJmx = System.getProperty("fcrepo.metrics.jmx");
        if (useJmx != null && useJmx.equalsIgnoreCase(Boolean.toString(true))) {
            LOGGER.info("fcrepo.metrics.jmx == true");
            return true;
        } else {
            LOGGER.info("System property fcrepo.metrics.jmx != true");
            return false;
        }
    }

    @Override
    protected MetricRegistry getMetricRegistry() {
        return getMetrics();
    }

    @Override
    protected HealthCheckRegistry getHealthCheckRegistry() {
        return new HealthCheckRegistry();
    }
}
