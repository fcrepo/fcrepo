/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.metrics;

import static com.codahale.metrics.graphite.GraphiteReporter.forRegistry;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.fcrepo.metrics.RegistryService.getMetrics;

import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

/**
 * @todo Add Documentation.
 * @author cbeer
 * @date Mar 22, 2013
 */
public class ReporterFactory {

    /**
     * @todo Add Documentation.
     */
    public ScheduledReporter registerGraphiteReporter(final Graphite g,
            final String prefix) {
        final GraphiteReporter r = forRegistry(getMetrics()).build(g);
        r.start(1, MINUTES);
        return r;
    }

    /**
     * @todo Add Documentation.
     */
    public static ReporterFactory buildDefaultReporterFactory() {
        return new ReporterFactory();
    }
}
