package org.fcrepo.metrics;

import com.yammer.metrics.Clock;
import com.yammer.metrics.MetricFilter;
import com.yammer.metrics.Reporter;
import com.yammer.metrics.graphite.Graphite;
import com.yammer.metrics.graphite.GraphiteReporter;

import java.util.concurrent.TimeUnit;

public class ReporterFactory {

    public Reporter registerGraphiteReporter(Graphite g, String prefix) {
        return new GraphiteReporter(RegistryService.getMetrics(), g, Clock.defaultClock(), prefix, TimeUnit.SECONDS, TimeUnit.MILLISECONDS, MetricFilter.ALL);
    }

    public static ReporterFactory buildDefaultReporterFactory() {
        return new ReporterFactory();
    }
}