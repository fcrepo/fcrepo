package org.fcrepo.metrics;

import com.yammer.metrics.AbstractPollingReporter;
import com.yammer.metrics.Clock;
import com.yammer.metrics.MetricFilter;
import com.yammer.metrics.graphite.Graphite;
import com.yammer.metrics.graphite.GraphiteReporter;

import java.util.concurrent.TimeUnit;

public class ReporterFactory {

    public AbstractPollingReporter registerGraphiteReporter(Graphite g, String prefix) {
        GraphiteReporter r = new GraphiteReporter(RegistryService.getMetrics(), g, Clock.defaultClock(), prefix, TimeUnit.SECONDS, TimeUnit.MILLISECONDS, MetricFilter.ALL);

        r.start(1, TimeUnit.MINUTES);
        return r;
    }

    public static ReporterFactory buildDefaultReporterFactory() {
        return new ReporterFactory();
    }
}