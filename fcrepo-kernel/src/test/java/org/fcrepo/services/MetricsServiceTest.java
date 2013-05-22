package org.fcrepo.services;

import com.codahale.metrics.MetricRegistry;
import org.junit.Test;

import java.io.PrintStream;

public class MetricsServiceTest {

    MetricRegistry mockMetricRegistry;

    PrintStream mockPrintStream;

    @Test
    public void testDumpMetrics() {
        MetricsService.dumpMetrics(mockPrintStream);
    }
}
