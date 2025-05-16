/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.jersey2.server.DefaultJerseyTagsProvider;
import io.micrometer.jersey2.server.MetricsApplicationEventListener;
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

    @Context
    private ServletContext servletContext;

    @Override
    public boolean configure(final FeatureContext context) {
        if (this.servletContext == null) {
            return false;
        }
        final var appCtx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        if (appCtx == null) {
            return false;
        }

        final var registry = appCtx.getBean(MeterRegistry.class);

        final var micrometerListener = new MetricsApplicationEventListener(
                registry,
                new DefaultJerseyTagsProvider(),
                "http.server.requests",
                false);

        context.register(micrometerListener);

        return true;
    }

}
