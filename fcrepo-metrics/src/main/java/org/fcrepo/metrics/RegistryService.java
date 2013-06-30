/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.metrics;

import java.io.PrintStream;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * Provide helpers for working with the Metrics registry
 * @author cbeer
 * @date Mar 22, 2013
 */
public abstract class RegistryService {

    private static final MetricRegistry METRICS = SharedMetricRegistries
            .getOrCreate("fcrepo-metrics");

    /**
     * Get the current registry service
     * @todo the new upstream SharedMetricRegistries may make this obsolete
     * @return
     */
    public static MetricRegistry getMetrics() {
        return METRICS;
    }

    /**
     * Immediately dump the current metrics to the console
     * @param os
     */
    public static void dumpMetrics(final PrintStream os) {

        final MetricRegistry registry = getMetrics();

        final MetricFilter filter = MetricFilter.ALL;
        final ConsoleReporter reporter =
                ConsoleReporter.forRegistry(registry).build();

        reporter.report(registry.getGauges(filter), registry
                .getCounters(filter), registry.getHistograms(filter), registry
                .getMeters(filter), registry.getTimers(filter));

    }
}
