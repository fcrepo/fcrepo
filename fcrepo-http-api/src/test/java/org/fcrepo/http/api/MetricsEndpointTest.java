/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.ws.rs.core.Response;
import org.fcrepo.config.MetricsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NOT_IMPLEMENTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * Test the MetricsEndpoint class.
 * @author whikloj
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MetricsEndpointTest {

    @Mock
    private MetricsConfig config;

    private MetricsEndpoint metricsEndpoint = null;

    @BeforeEach
    public void setUp() {
        config = mock(MetricsConfig.class);
        metricsEndpoint = spy(new MetricsEndpoint());
        setField(metricsEndpoint, "metricsConfig", config);
    }

    @Test
    public void testNotEnabled() {
        final var registry = new SimpleMeterRegistry();
        setField(metricsEndpoint, "meterRegistry", registry);
        when(config.isMetricsEnabled()).thenReturn(false);

        final Response actual = metricsEndpoint.scrape();
        assertEquals(NOT_FOUND.getStatusCode(), actual.getStatus());
        verify(config).isMetricsEnabled();
    }

    @Test
    public void testScrapeMetrics() {
        final var registry = new SimpleMeterRegistry();
        setField(metricsEndpoint, "meterRegistry", registry);
        when(config.isMetricsEnabled()).thenReturn(true);

        final Response actual = metricsEndpoint.scrape();
        assertEquals(NOT_IMPLEMENTED.getStatusCode(), actual.getStatus());
        assertEquals("Metrics registry is not Prometheus-compatible.", actual.getEntity());
    }

    @Test
    public void testScrapePrometheusMetrics() {
        when(config.isMetricsEnabled()).thenReturn(true);
        final var prometheusRegistry = mock(PrometheusMeterRegistry.class);
        setField(metricsEndpoint, "meterRegistry", prometheusRegistry);
        when(prometheusRegistry.scrape())
                .thenReturn("# HELP test_metric Test metric\n# TYPE test_metric gauge\ntest_metric 1.0");

        final Response actual = metricsEndpoint.scrape();
        assertEquals(Response.Status.OK.getStatusCode(), actual.getStatus());
        assertEquals("# HELP test_metric Test metric\n# TYPE test_metric gauge\ntest_metric 1.0",
                actual.getEntity());
    }
}
