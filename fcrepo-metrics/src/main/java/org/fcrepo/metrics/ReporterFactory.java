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

import static com.codahale.metrics.MetricFilter.ALL;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import javax.management.MBeanServer;

import org.slf4j.Logger;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

/**
 * Helpers for making the upstream metrics reporters play nice with Springg
 * 
 * @author cbeer
 * @since Mar 22, 2013
 */
public class ReporterFactory {

    private static final Logger LOGGER = getLogger(ReporterFactory.class);

    private RegistryService registryService = RegistryService.getInstance();

    /**
     * Start a new GraphiteReporter, with reports every minute
     * 
     * @param prefix graphite metrics prefix
     * @param g a graphite client instance
     * @return a new GraphiteReporter
     */
    public GraphiteReporter getGraphiteReporter(final String prefix,
            final Graphite g) {
        final GraphiteReporter reporter =
                GraphiteReporter.forRegistry(registryService.getMetrics()).prefixedWith(prefix)
                        .convertRatesTo(SECONDS).convertDurationsTo(
                                MILLISECONDS).filter(ALL).build(g);
        reporter.start(1, MINUTES);
        LOGGER.debug("Started GraphiteReporter");
        return reporter;
    }

    /**
     * Publish metrics to JMX
     * 
     * @param prefix the prefix
     * @return a JMXReporter
     */
    public JmxReporter getJmxReporter(final String prefix) {
        final MBeanServer mbs = getPlatformMBeanServer();
        final JmxReporter reporter =
                JmxReporter.forRegistry(registryService.getMetrics()).registerWith(mbs)
                        .inDomain("org.fcrepo")
                        .convertDurationsTo(MILLISECONDS).convertRatesTo(
                                SECONDS).filter(ALL).build();
        reporter.start();
        LOGGER.debug("Started JmxReporter");
        return reporter;
    }

}
