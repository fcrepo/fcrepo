
package org.fcrepo.metrics;

import static com.yammer.metrics.graphite.GraphiteReporter.forRegistry;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.fcrepo.metrics.RegistryService.getMetrics;

import com.yammer.metrics.ScheduledReporter;
import com.yammer.metrics.graphite.Graphite;
import com.yammer.metrics.graphite.GraphiteReporter;

public class ReporterFactory {

    public ScheduledReporter registerGraphiteReporter(final Graphite g,
            final String prefix) {
        final GraphiteReporter r = forRegistry(getMetrics()).build(g);
        r.start(1, MINUTES);
        return r;
    }

    public static ReporterFactory buildDefaultReporterFactory() {
        return new ReporterFactory();
    }
}