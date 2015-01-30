/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.commons;

import org.fcrepo.http.commons.session.SessionProvider;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.process.internal.RequestScoped;

import com.codahale.metrics.jersey2.InstrumentedResourceMethodApplicationListener;
import com.codahale.metrics.MetricRegistry;
import javax.jcr.Session;

import java.util.logging.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/22/14
 */
public class FedoraApplication extends ResourceConfig {

    private static final org.slf4j.Logger LOGGER = getLogger(FedoraApplication.class);

    /**
     * THIS IS OUR RESOURCE CONFIG!
     */
    public FedoraApplication() {
        super();
        packages("org.fcrepo");
        register(new FactoryBinder());
        register(MultiPartFeature.class);
        register(JacksonFeature.class);

        if (LOGGER.isDebugEnabled()) {
            register(new LoggingFilter(Logger.getLogger(LoggingFilter.class.getName()), LOGGER.isTraceEnabled()));
        }

        register(new InstrumentedResourceMethodApplicationListener(new MetricRegistry()));
    }

    static class FactoryBinder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(SessionProvider.class)
                    .to(Session.class)
                    .in(RequestScoped.class);
        }
    }
}
