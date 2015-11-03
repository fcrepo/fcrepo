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
package org.fcrepo.http.commons.api;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.api.models.FedoraResource;

import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.google.common.collect.ListMultimap;

/**
 * Inject optional headers from external processes
 *
 * @author whikloj
 * @since 2015-10-30
 */
@Component
public class HttpHeaderInjection implements ApplicationContextAware {

    private static final Logger LOGGER = getLogger(HttpHeaderInjection.class);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Add additional Http Headers
     *
     * @param servletResponse the response
     * @param uriInfo the URI context
     * @param resource the resource
     */
    public void addHttpHeaderToResponseStream(final HttpServletResponse servletResponse, final UriInfo uriInfo,
            final FedoraResource resource) {
        LOGGER.debug("Adding additional HTTP headers to stream");

        for (final String h : applicationContext.getBeanDefinitionNames()) {
            LOGGER.debug("Found bean name: {}", h);
        }
        getUriAwareHttpHeaderFactories().forEach((bean, factory) -> {
            LOGGER.debug("Adding HTTP headers using: {}", bean);
            final ListMultimap<String, String> h =
                    factory.createHttpHeadersForResource(uriInfo, resource);

            h.entries().forEach((final Map.Entry<String, String> entry) -> {
                servletResponse.addHeader(entry.getKey(), entry.getValue());
            });
        });
    }

    private Map<String, UriAwareHttpHeaderFactory> getUriAwareHttpHeaderFactories() {
        return applicationContext
                .getBeansOfType(UriAwareHttpHeaderFactory.class);
    }
}
