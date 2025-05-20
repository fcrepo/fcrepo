package org.fcrepo.utils;

import com.google.common.base.Strings;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;

import java.io.FileInputStream;
import java.util.Objects;
import java.util.Properties;

import static java.lang.Integer.parseInt;
import static org.eclipse.jetty.util.resource.Resource.newResource;

/**
 * A simple Jetty server launcher for development purposes.
 * This class sets up a Jetty server with a web application context
 * and loads configuration from XML files.
 * @author whikloj
 */
public class JettyDevLauncher {
    private Server server;

    /**
     * Main method to start the Jetty server.
     * @param port the port to start the server on
     * @throws Exception if an error occurs during server startup
     */
    public void start(final int port) throws Exception {
        // Load jetty.xml configuration if present
        try (final var xml = newResource("src/test/resources/jetty-test.xml")) {
            XmlConfiguration configuration = new XmlConfiguration(xml);
            server = (Server) configuration.configure();
        }


        // Configure the web application context
        final WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/rest");
        webapp.setWar(determineWarFilePath());

        try (final var envXml = newResource("src/test/resources/jetty-env.xml")) {
            // applies env config to explicitly configure the LoginService. (new in Jetty 10+)
            XmlConfiguration envConfig = new XmlConfiguration(envXml);
            envConfig.configure(webapp);
        }

        // Add the webapp to the server
        server.setHandler(webapp);

        // Start Jetty
        server.start();
        server.join();
    }

    /**
     * Stops the Jetty server.
     * @throws Exception if an error occurs during server shutdown
     */
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Determines the path to the WAR file based on the version specified in pom.properties.
     * @return the path to the WAR file
     */
    private String determineWarFilePath() {
        final String version = System.getProperty("project.version", "6.5.2-SNAPSHOT");
        return "target/fcrepo-webapp-" + version + ".war";
    }

    /**
     * Main method to launch the Jetty server.
     * @param args command line arguments
     * @throws Exception if an error occurs during server startup
     */
    public static void main(final String[] args) throws Exception {
        final int port = parseInt(Objects.requireNonNullElse(
                Strings.emptyToNull(System.getProperty("fcrepo.dynamic.test.port")), "8080"));
        final JettyDevLauncher launcher = new JettyDevLauncher();
        launcher.start(port);
    }
}
