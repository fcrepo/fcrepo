package org.fcrepo.test.util;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import com.sun.grizzly.http.SelectorThread;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.jersey.api.container.grizzly.GrizzlyServerFactory;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;

public class ContainerWrapper {
	private static final Logger logger = LoggerFactory.getLogger(ContainerWrapper.class);

	private String contextConfigLocations;
	private int port;
	private ConfigurableApplicationContext context;
	private SelectorThread server;
	private String packagesToScan;
	
	public void setPackagesToScan(String packagesToScan) {
        this.packagesToScan = packagesToScan;
    }
	
	public void setContextConfigLocations(String contextConfigLocations) {
        this.contextConfigLocations = contextConfigLocations;
    }

	public void setPort(int port) {
		this.port = port;
	}

    public void start() throws Exception {
		URI uri = URI.create("http://localhost:" + port + "/");
		ServletAdapter adapter = new ServletAdapter();
		adapter.addInitParameter("com.sun.jersey.config.property.packages", packagesToScan);
		adapter.addInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
		adapter.addContextParameter("contextConfigLocation", contextConfigLocations);
		adapter.addServletListener("org.springframework.web.context.ContextLoaderListener");
		adapter.setServletInstance(new SpringServlet());
		adapter.setContextPath(uri.getPath());
		adapter.setProperty("load-on-startup", 1);
		this.server = GrizzlyServerFactory.create(uri, adapter);
		logger.debug("started grizzly webserver endpoint at " + this.server.getPort());
	}

	public void stop() throws Exception {
		server.stopEndpoint();
	}

}
