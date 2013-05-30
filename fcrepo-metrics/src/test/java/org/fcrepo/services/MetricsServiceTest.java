package org.fcrepo.services;

import static org.fcrepo.metrics.RegistryService.dumpMetrics;

import java.io.PrintStream;

import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

public class MetricsServiceTest {

    MetricRegistry mockMetricRegistry;

    PrintStream mockPrintStream;

    @Test
    public void testDumpMetrics() {
        dumpMetrics(mockPrintStream);
    }
}
