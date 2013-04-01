package org.fcrepo.metrics;

import com.yammer.metrics.Clock;
import com.yammer.metrics.ConsoleReporter;
import com.yammer.metrics.MetricFilter;
import com.yammer.metrics.MetricRegistry;

import java.io.PrintStream;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class RegistryService {
    private final static MetricRegistry metrics = new MetricRegistry("fcrepo");

    public static MetricRegistry getMetrics() {
        return metrics;
    }

    public static void dumpMetrics(PrintStream os) {

        final MetricRegistry registry = getMetrics();

        final MetricFilter filter = MetricFilter.ALL;
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(registry).build();

        reporter.report(registry.getGauges(filter),
                registry.getCounters(filter),
                registry.getHistograms(filter),
                registry.getMeters(filter),
                registry.getTimers(filter));

    }

}
