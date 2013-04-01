package org.fcrepo.metrics;

import com.yammer.metrics.ScheduledReporter;
import com.yammer.metrics.Clock;
import com.yammer.metrics.MetricFilter;
import com.yammer.metrics.graphite.Graphite;
import com.yammer.metrics.graphite.GraphiteReporter;

import java.util.concurrent.TimeUnit;

public class ReporterFactory {

    public ScheduledReporter registerGraphiteReporter(Graphite g, String prefix) {
        GraphiteReporter r = GraphiteReporter.forRegistry(RegistryService.getMetrics()).build(g);

        r.start(1, TimeUnit.MINUTES);
        return r;
    }

    public static ReporterFactory buildDefaultReporterFactory() {
        return new ReporterFactory();
    }
}