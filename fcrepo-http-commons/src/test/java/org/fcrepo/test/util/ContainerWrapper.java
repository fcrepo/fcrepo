
package org.fcrepo.test.util;

import static java.util.Collections.emptyList;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.List;

import javax.servlet.Filter;

import org.slf4j.Logger;
import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.jersey.api.container.grizzly.GrizzlyServerFactory;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;

public class ContainerWrapper {

    private static final Logger logger = getLogger(ContainerWrapper.class);

    private String contextConfigLocation = null;

    private int port;

    private SelectorThread server;

    private String packagesToScan = null;

    private List<Filter> filters = emptyList();

    public void setPackagesToScan(final String packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    public void setContextConfigLocation(final String contextConfigLocation) {
        this.contextConfigLocation = contextConfigLocation;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void start() throws Exception {
        final URI uri = URI.create("http://localhost:" + port + "/");
        final ServletAdapter adapter = new ServletAdapter();
        if (packagesToScan != null) {
            adapter.addInitParameter("com.sun.jersey.config.property.packages",
                    packagesToScan);
        }
        adapter.addInitParameter("com.sun.jersey.api.json.POJOMappingFeature",
                "true");
        if (contextConfigLocation != null) {
            adapter.addContextParameter("contextConfigLocation",
                    contextConfigLocation);
        }
        for (final Filter filter : filters) {
            adapter.addFilter(filter, filter.getClass().getName() + "-" +
                    filter.hashCode(), null);
        }
        adapter.addServletListener("org.springframework.web.context.ContextLoaderListener");
        adapter.setServletInstance(new SpringServlet());
        adapter.setContextPath(uri.getPath());
        adapter.setProperty("load-on-startup", 1);
        server = GrizzlyServerFactory.create(uri, adapter);
        logger.debug("started grizzly webserver endpoint at " +
                server.getPort());
    }

    public void stop() throws Exception {
        server.stopEndpoint();
    }

    public void setFilters(final List<Filter> filters) {
        this.filters = filters;
    }

}
