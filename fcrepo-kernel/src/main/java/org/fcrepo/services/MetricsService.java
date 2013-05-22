package org.fcrepo.services;

import com.codahale.metrics.MetricRegistry;
import org.fcrepo.metrics.RegistryService;

import java.io.PrintStream;

public class MetricsService {

    public static final MetricRegistry metrics = RegistryService.getMetrics();

    public static MetricRegistry getMetrics() {
        return metrics;
    }

    public static void dumpMetrics(final PrintStream os) {
        RegistryService.dumpMetrics(os);
    }

}
