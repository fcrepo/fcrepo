
package org.fcrepo.metrics;

import java.io.PrintStream;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public abstract class RegistryService {

    private static final MetricRegistry METRICS = SharedMetricRegistries
            .getOrCreate("fcrepo-metrics");

    public static MetricRegistry getMetrics() {
        return METRICS;
    }

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
