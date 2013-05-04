
package org.fcrepo.metrics;

import static com.codahale.metrics.graphite.GraphiteReporter.forRegistry;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.fcrepo.metrics.RegistryService.getMetrics;

import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

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