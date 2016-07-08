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

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;

import java.net.InetSocketAddress;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

/**
 * Configuration class for Metrics reporting to Graphite and JMX.
 * <p>
 * To enable Metrics reporting to Graphite, activate the Spring profile
 * "metrics.graphite". The system properties fcrepo.metrics.host and
 * fcrepo.metrics.port can also be set (defaults to "localhost" and 2003,
 * respectively.
 * </p>
 * <p>
 * To enable Metrics reporting to JMX, activate the Spring profile
 * "metrics.jmx".
 * </p>
 * <p>
 * To enable both Graphite and JMX reporting, the Spring profile "metrics", can
 * be used instead of specifying both metrics.graphite and metrics.jmx, e.g.:
 * </p>
 * <blockquote><code>-Dspring.profiles.active="metrics"</code></blockquote>
 * 
 * @author Edwin Shin
 */
@Configuration
public class MetricsConfig {

    public static final String METRIC_PREFIX = getProperty("fcrepo.metrics.prefix", "org.fcrepo");

    /**
     * Provide the reporter factory to Spring
     * 
     * @return the reporter factory
     */
    @Bean
    public ReporterFactory reporterFactory() {
        return new ReporterFactory();
    }

    /**
     * <p>
     * Metrics configuration for Graphite reporting.
     * </p>
     * <p>
     * Graphite reporting can be enabled by activating the "metrics.graphite"
     * Spring profile.
     * </p>
     */
    @Configuration
    @Profile({"metrics", "metrics.graphite"})
    public static class GraphiteConfig {

        /**
         * <p>
         * Host and port may be configured with system properties
         * "fcrepo.metrics.host" and "fcrepo.metrics.port", respectively.
         * </p>
         * <p>
         * Host and port default to "localhost" and "2003", respectively.
         * </p>
         * 
         * @return a Graphite client to a Carbon server
         */
        @Bean
        public Graphite graphiteClient() {
            final String hostname =
                    getProperty("fcrepo.metrics.host", "localhost");
            final int port =
                    parseInt(getProperty("fcrepo.metrics.port", "2003"));

            return new Graphite(new InetSocketAddress(hostname, port));
        }

        /**
         * @return a Reporter which publishes metrics to a Graphite server
         */
        @Bean
        public GraphiteReporter graphiteReporter() {
            final MetricsConfig cfg = new MetricsConfig();
            final String prefix = METRIC_PREFIX;
            return cfg.reporterFactory().getGraphiteReporter(prefix,
                    graphiteClient());
        }
    }

    /**
     * <p>
     * JMX configuration for metrics reporting.
     * </p>
     * <p>
     * JMX reporting can be enabled by activating the "metrics.jmx" Spring
     * profile.
     * </p>
     */
    @Configuration
    @Profile({"metrics", "metrics.jmx"})
    public static class JmxConfig {

        /**
         * @return a Reporter that exposes metrics under the "org.fcrepo" prefix
         */
        @Bean
        public JmxReporter jmxReporter() {
            final MetricsConfig cfg = new MetricsConfig();
            final String prefix = METRIC_PREFIX;
            return cfg.reporterFactory().getJmxReporter(prefix);
        }
    }
}
