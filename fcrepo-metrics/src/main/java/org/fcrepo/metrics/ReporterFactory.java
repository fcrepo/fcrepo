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

import static org.fcrepo.metrics.RegistryService.getMetrics;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;

import org.slf4j.Logger;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

/**
 * Helpers for making the upstream metrics reporters play nice with Springg
 * @author cbeer
 * @date Mar 22, 2013
 */
public class ReporterFactory {

    private static final Logger logger = getLogger(ReporterFactory.class);

    public GraphiteReporter getGraphiteReporter(final String prefix,
            final Graphite g) {
        final GraphiteReporter reporter =
                GraphiteReporter.forRegistry(getMetrics()).prefixedWith(prefix)
                        .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(
                                TimeUnit.MILLISECONDS).filter(MetricFilter.ALL)
                        .build(g);
        reporter.start(1, TimeUnit.MINUTES);
        logger.debug("Started GraphiteReporter");
        return reporter;
    }

    /**
     * TODO
     * 
     * @param prefix
     * @return
     */
    public JmxReporter getJmxReporter(final String prefix) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final JmxReporter reporter =
                JmxReporter.forRegistry(getMetrics()).registerWith(mbs)
                        .inDomain("org.fcrepo").convertDurationsTo(
                                TimeUnit.MILLISECONDS).convertRatesTo(
                                TimeUnit.SECONDS).filter(MetricFilter.ALL)
                        .build();
        reporter.start();
        logger.debug("Started JmxReporter");
        return reporter;
    }

    public static ReporterFactory buildDefaultReporterFactory() {
        return new ReporterFactory();
    }
}