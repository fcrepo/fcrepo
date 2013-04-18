
package org.fcrepo.metrics;

import java.util.concurrent.TimeUnit;

import com.yammer.metrics.ScheduledReporter;
import com.yammer.metrics.graphite.Graphite;
import com.yammer.metrics.graphite.GraphiteReporter;

public class ReporterFactory {

    public ScheduledReporter registerGraphiteReporter(final Graphite g,
            final String prefix) {
        final GraphiteReporter r =
                GraphiteReporter.forRegistry(RegistryService.getMetrics())
                        .build(g);

        r.start(1, TimeUnit.MINUTES);
        return r;
    }

    public static ReporterFactory buildDefaultReporterFactory() {
        return new ReporterFactory();
    }
}