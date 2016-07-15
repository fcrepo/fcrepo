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
package org.fcrepo.metrics;

import javax.servlet.annotation.WebListener;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.AdminServletContextListener;

/**
 * A ServletContextListener to set the ServletContext attributes that the
 * Metrics servlets expect.
 * 
 * @author Edwin Shin
 * @see <a
 *      href="http://metrics.codahale.com/manual/servlets/">http://metrics.codahale.com/manual/servlets/</a>
 */
@WebListener
public class MetricsContextListener extends AdminServletContextListener {

    /**
     * Get the metrics registry for fcrepo
     * @return the metrics registry
     */
    @Override
    protected MetricRegistry getMetricRegistry() {
        return RegistryService.getInstance().getMetrics();
    }

    /**
     * Provide a health-check registry
     * TODO actually populate the health-check registry with checks
     * @return a new health check registry
     */
    @Override
    protected HealthCheckRegistry getHealthCheckRegistry() {
        return new HealthCheckRegistry();
    }
}
