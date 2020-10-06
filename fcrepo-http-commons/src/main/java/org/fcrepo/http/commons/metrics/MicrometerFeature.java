/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.commons.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.jersey2.server.DefaultJerseyTagsProvider;
import io.micrometer.jersey2.server.MetricsApplicationEventListener;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

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
