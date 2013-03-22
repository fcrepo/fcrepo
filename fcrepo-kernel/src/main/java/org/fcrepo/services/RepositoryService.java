package org.fcrepo.services;

import java.io.PrintStream;

import org.fcrepo.metrics.RegistryService;

import com.yammer.metrics.MetricRegistry;

public class RepositoryService {
    public final static MetricRegistry metrics = RegistryService.getMetrics();

    public static MetricRegistry getMetrics() {
        return metrics;
    }

    public static void dumpMetrics(PrintStream os) {
        RegistryService.dumpMetrics(os);
    }

}
