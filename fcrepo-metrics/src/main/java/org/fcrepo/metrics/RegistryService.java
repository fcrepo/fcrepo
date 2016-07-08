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

import static com.codahale.metrics.SharedMetricRegistries.getOrCreate;

import com.codahale.metrics.MetricRegistry;

/**
 * Provide helpers for working with the Metrics registry
 *
 * @author cbeer
 * @since Mar 22, 2013
 */
public final class RegistryService {

    private static final MetricRegistry METRICS = getOrCreate("fcrepo-metrics");

    private static volatile RegistryService instance = null;

    private RegistryService() {
      // New instances should come from the singleton
    }

    /**
     * Create the instance
     * @return the local object
     */
    public synchronized static RegistryService getInstance() {
        RegistryService local = instance;
        if (null == local) {
             instance = local = new RegistryService();
        }
        return local;
    }

    /**
     * Get the current registry service
     *
     * TODO the new upstream SharedMetricRegistries may make this obsolete
     * @return the current registry service
     */
    @SuppressWarnings("static-method")
    public MetricRegistry getMetrics() {
        return METRICS;
    }

}
