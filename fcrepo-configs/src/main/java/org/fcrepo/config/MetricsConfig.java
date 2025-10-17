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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author pwinckles
 */
@Configuration
public class MetricsConfig extends BasePropsConfig {

    @Deprecated
    @Value("${fcrepo.metrics.enable:false}")
    private boolean metricsEnable;

    @Value("${fcrepo.metrics.enabled:false}")
    private boolean metricsEnabled;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsConfig.class);

    @Bean
    public MeterRegistry meterRegistry() {
        final MeterRegistry registry;
        if (metricsEnable) {
            LOGGER.warn("Property fcrepo.metrics.enable is deprecated and will be removed in Fedora 8. " +
                    "See https://wiki.lyrasis.org/display/FEDORA7x/Properties for details.");
            metricsEnabled = metricsEnable;
        }

        if (metricsEnabled || metricsEnable) {
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
        if (metricsEnabled || metricsEnable) {
            return true;
        } else {
            return false;
        }
    }
}
