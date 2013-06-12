
package org.fcrepo.metrics;

import java.net.InetSocketAddress;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

/**
 * Configuration class for Metrics reporting to Graphite and JMX.
 * 
 * <p>To enable Metrics reporting to Graphite, activate the Spring profile 
 * "metrics.graphite". The system properties fcrepo.metrics.host and 
 * fcrepo.metrics.port can also be set (defaults to "localhost" and 2003, 
 * respectively.</p>
 * 
 * <p>To enable Metrics reporting to JMX, activate the Spring profile 
 * "metrics.jmx".</p>
 * 
 * <p>To enable both Graphite and JMX reporting, the Spring profile "metrics", 
 * can be used instead of specifying both metrics.graphite and metrics.jmx, 
 * e.g.:</p>
 * <blockquote><code>-Dspring.profiles.active="metrics"</code></blockquote>
 * 
 * @author Edwin Shin
 *
 */
@Configuration
public class MetricsConfig {

    @Bean
    public ReporterFactory reporterFactory() {
        return new ReporterFactory();
    }

    @Configuration
    @Profile({"metrics", "metrics.graphite"})
    public static class GraphiteConfig {

        @Bean
        public Graphite graphiteClient() {
            String hostname =
                    System.getProperty("fcrepo.metrics.host", "localhost");
            int port =
                    Integer.parseInt(System.getProperty("fcrepo.metrics.port",
                            "2003"));

            return new Graphite(new InetSocketAddress(hostname, port));
        }

        @Bean
        public GraphiteReporter graphiteReporter() {
            MetricsConfig cfg = new MetricsConfig();
            String prefix =
                    System.getProperty("fcrepo.metrics.prefix", "org.fcrepo");
            return cfg.reporterFactory().getGraphiteReporter(prefix,
                    graphiteClient());
        }
    }

    @Configuration
    @Profile({"metrics", "metrics.jmx"})
    public static class JmxConfig {

        String prefix = System.getProperty("fcrepo.metrics.prefix",
                "org.fcrepo");

        @Bean
        public JmxReporter jmxReporter() {
            MetricsConfig cfg = new MetricsConfig();
            return cfg.reporterFactory().getJmxReporter(prefix);
        }
    }
}
