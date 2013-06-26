/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.metrics;

import static org.fcrepo.metrics.RegistryService.getMetrics;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;

import org.slf4j.Logger;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

/**
 * @todo Add Documentation.
 * @author cbeer
 * @date Mar 22, 2013
 */
public class ReporterFactory {

    private static final Logger logger = getLogger(ReporterFactory.class);

    /**
     * @todo Add Documentation.
     */
    public GraphiteReporter getGraphiteReporter(final String prefix,
            final Graphite g) {
        final GraphiteReporter reporter =
                GraphiteReporter.forRegistry(getMetrics()).prefixedWith(prefix)
                        .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(
                                TimeUnit.MILLISECONDS).filter(MetricFilter.ALL)
                        .build(g);
        reporter.start(1, TimeUnit.MINUTES);
        logger.debug("Started GraphiteReporter");
        return reporter;
    }

    /**
     * TODO
     * 
     * @param prefix
     * @return
     */
    public JmxReporter getJmxReporter(final String prefix) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final JmxReporter reporter =
                JmxReporter.forRegistry(getMetrics()).registerWith(mbs)
                        .inDomain("org.fcrepo").convertDurationsTo(
                                TimeUnit.MILLISECONDS).convertRatesTo(
                                TimeUnit.SECONDS).filter(MetricFilter.ALL)
                        .build();
        reporter.start();
        logger.debug("Started JmxReporter");
        return reporter;
    }

    /**
     * @todo Add Documentation.
     */
    public static ReporterFactory buildDefaultReporterFactory() {
        return new ReporterFactory();
    }
}