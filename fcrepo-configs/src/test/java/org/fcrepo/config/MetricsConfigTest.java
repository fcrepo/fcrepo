/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author bbpennel
 */
public class MetricsConfigTest {

    private MetricsConfig config;
    private GenericApplicationContext context;
    private MockEnvironment env;

    @BeforeEach
    public void setUp() {
        env = new MockEnvironment();
    }

    @AfterEach
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    private void initializeContext() {
        context = new AnnotationConfigApplicationContext();
        context.setEnvironment(env);
        context.registerBean(MetricsConfig.class);
    }

    private void initializeConfig() {
        context.refresh();
        config = context.getBean(MetricsConfig.class);
    }

    @Test
    public void testDefaultMetricsDisabled() {
        // Default state should have metrics disabled
        initializeContext();
        initializeConfig();

        assertFalse(config.isMetricsEnabled());

        // When metrics are disabled, a SimpleMeterRegistry should be returned
        final MeterRegistry registry = context.getBean(MeterRegistry.class);
        assertNotNull(registry);
        assertInstanceOf(SimpleMeterRegistry.class, registry);

    }

    @Test
    public void testMetricsEnabled() {
        env.setProperty("fcrepo.metrics.enabled", "true");
        initializeContext();
        initializeConfig();

        assertTrue(config.isMetricsEnabled());

        // When metrics are enabled, a PrometheusMeterRegistry should be returned
        final MeterRegistry registry = context.getBean(MeterRegistry.class);
        assertNotNull(registry);
        assertInstanceOf(PrometheusMeterRegistry.class, registry);

        // Verify the registry has the JVM metrics registered
        assertFalse(registry.getMeters().isEmpty());
    }

    @Test
    public void testMetricsEnabledOldProperty() {
        env.setProperty("fcrepo.metrics.enable", "true");
        initializeContext();
        initializeConfig();

        assertTrue(config.isMetricsEnabled());

        // When metrics are enabled, a PrometheusMeterRegistry should be returned
        final MeterRegistry registry = context.getBean(MeterRegistry.class);
        assertNotNull(registry);
        assertInstanceOf(PrometheusMeterRegistry.class, registry);

        // Verify the registry has the JVM metrics registered
        assertFalse(registry.getMeters().isEmpty());
    }

    @Test
    public void testTimerMetricsConfiguration() {
        env.setProperty("fcrepo.metrics.enabled", "true");
        initializeContext();
        initializeConfig();

        final MeterRegistry registry = context.getBean(MeterRegistry.class);
        assertInstanceOf(PrometheusMeterRegistry.class, registry);

        // Create a timer to test the configuration
        registry.timer("test.timer");

        // Timer should be created and present in the registry
        assertNotNull(registry.find("test.timer").timer());
    }
}