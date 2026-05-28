/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.fcrepo.config.MetricsConfig;
import org.springframework.context.annotation.Scope;

/**
 * JAX-RS endpoint for exposing Prometheus metrics.
 * This endpoint provides a /prometheus path to scrape metrics.
 *
 * @author whikloj
 */
@Timed
@Scope("request")
@Path("/prometheus")
public class MetricsEndpoint {

    @Inject
    private MetricsConfig metricsConfig;

    @Inject
    private MeterRegistry meterRegistry;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response scrape() {
        if (!metricsConfig.isMetricsEnabled()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Metrics are disabled in the configuration.").build();
        }

        if (meterRegistry instanceof PrometheusMeterRegistry prometheusRegistry) {
            return Response.ok(prometheusRegistry.scrape()).build();
        }

        // Metrics enabled, but not using Prometheus
        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity("Metrics registry is not Prometheus-compatible.").build();
    }
}