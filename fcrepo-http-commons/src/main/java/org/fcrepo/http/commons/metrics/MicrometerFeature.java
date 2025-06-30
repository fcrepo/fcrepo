/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.glassfish.jersey.micrometer.server.DefaultJerseyTagsProvider;
import org.glassfish.jersey.micrometer.server.MetricsApplicationEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

/**
 * Enables Micrometer metrics on Jersey APIs (still must be annotated with @Timed)
 *
 * @author pwinckles
 */
public class MicrometerFeature implements Feature {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerFeature.class);

    @Context
    private ServletContext servletContext;

    @Override
    public boolean configure(final FeatureContext context) {
        if (this.servletContext == null) {
            LOGGER.warn("ServletContext is not available. Micrometer metrics not enabled.");
            return false;
        }
        final var appCtx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        if (appCtx == null) {
            LOGGER.warn("Spring WebApplicationContext not found. Micrometer metrics not enabled.");
            return false;
        }

        final MeterRegistry registry;
        try {
            registry = appCtx.getBean(MeterRegistry.class);
        } catch (Exception e) {
            LOGGER.error("MeterRegistry bean not found. Micrometer metrics not enabled.", e);
            return false;
        }

        final var micrometerListener = new MetricsApplicationEventListener(
                registry,
                new DefaultJerseyTagsProvider(),
                "http.server.requests",
                false);

        context.register(micrometerListener);
        LOGGER.info("Micrometer metrics enabled for Jersey resources.");
        return true;
    }

}
