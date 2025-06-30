/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author pwinckles
 */
@Configuration
public class MetricsConfig extends BasePropsConfig {

    @Value("${fcrepo.metrics.enable:false}")
    private boolean metricsEnabled;

    @Bean
    public MeterRegistry meterRegistry() {
        final MeterRegistry registry;
        if (metricsEnabled) {
            registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, PrometheusRegistry.defaultRegistry,
                    Clock.SYSTEM);
            registry.config().meterFilter(new MeterFilter() {
                @Override
                public DistributionStatisticConfig configure(final Meter.Id id,
                                                             final DistributionStatisticConfig config) {
                    if (id.getType() == Meter.Type.TIMER) {
                        return DistributionStatisticConfig.builder()
                                .percentilesHistogram(true)
                                .percentiles(0.5, 0.90, 0.99)
                                .build().merge(config);
                    }
                    return config;
                }
            });
            new JvmThreadMetrics().bindTo(registry);
            new JvmGcMetrics().bindTo(registry);
            new JvmMemoryMetrics().bindTo(registry);
            new ProcessorMetrics().bindTo(registry);
            new UptimeMetrics().bindTo(registry);
            Metrics.addRegistry(registry);
        } else {
            registry = new SimpleMeterRegistry();
        }
        return registry;
    }

    /**
     * @return whether metrics are enabled
     */
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

}
