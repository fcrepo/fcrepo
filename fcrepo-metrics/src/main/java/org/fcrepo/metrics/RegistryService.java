/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.metrics;

import java.io.PrintStream;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * @todo Add Documentation.
 * @author cbeer
 * @date Mar 22, 2013
 */
public abstract class RegistryService {

    private static final MetricRegistry METRICS = SharedMetricRegistries
            .getOrCreate("fcrepo-metrics");

    /**
     * @todo Add Documentation.
     */
    public static MetricRegistry getMetrics() {
        return METRICS;
    }

    /**
     * @todo Add Documentation.
     */
    public static void dumpMetrics(final PrintStream os) {

        final MetricRegistry registry = getMetrics();

        final MetricFilter filter = MetricFilter.ALL;
        final ConsoleReporter reporter =
                ConsoleReporter.forRegistry(registry).build();

        reporter.report(registry.getGauges(filter), registry
                .getCounters(filter), registry.getHistograms(filter), registry
                .getMeters(filter), registry.getTimers(filter));

    }
}
