package org.fcrepo.services;

import com.yammer.metrics.Clock;
import com.yammer.metrics.ConsoleReporter;
import com.yammer.metrics.MetricFilter;
import com.yammer.metrics.MetricRegistry;
import org.fcrepo.metrics.RegistryService;

import java.io.PrintStream;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class RepositoryService {
    public final static MetricRegistry metrics = RegistryService.getMetrics();

    public static MetricRegistry getMetrics() {
        return metrics;
    }

    public static void dumpMetrics(PrintStream os) {
        RegistryService.dumpMetrics(os);
    }

}
