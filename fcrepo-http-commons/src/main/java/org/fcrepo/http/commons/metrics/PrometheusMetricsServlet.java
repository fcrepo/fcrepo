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

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * This class is an extension of Prometheus's MetricsServlet. It only exists because there isn't an easy way to
 * set the CollectorRegistry on with a Spring bean.
 *
 * @author pwinckles
 */
public class PrometheusMetricsServlet extends MetricsServlet {

    @Override
    public void init(final ServletConfig config) throws ServletException {
        final var context = WebApplicationContextUtils
                .getRequiredWebApplicationContext(config.getServletContext());
        final var collector = context.getBean(CollectorRegistry.class);

        try {
            final var field = MetricsServlet.class.getDeclaredField("registry");
            field.setAccessible(true);
            field.set(this, collector);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new ServletException(e);
        }

        super.init(config);
    }

}
