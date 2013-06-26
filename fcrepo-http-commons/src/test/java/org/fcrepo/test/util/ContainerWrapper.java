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

package org.fcrepo.test.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.fcrepo.webxml.WebAppConfig;
import org.fcrepo.webxml.bind.ContextParam;
import org.fcrepo.webxml.bind.Filter;
import org.fcrepo.webxml.bind.FilterMapping;
import org.fcrepo.webxml.bind.InitParam;
import org.fcrepo.webxml.bind.Listener;
import org.fcrepo.webxml.bind.Servlet;
import org.fcrepo.webxml.bind.ServletMapping;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;

public class ContainerWrapper implements ApplicationContextAware {

    private static final Logger logger = getLogger(ContainerWrapper.class);

    private int port;

    private HttpServer server;

    private WebappContext appContext;

    private String configLocation;

    public void setConfigLocation(final String configLocation) {
        this.configLocation = configLocation.replaceFirst("^classpath:", "/");
    }

    public void setPort(final int port) {
        this.port = port;
    }

    @PostConstruct
    public void start() throws Exception {

        JAXBContext context = JAXBContext.newInstance(WebAppConfig.class);
        Unmarshaller u = context.createUnmarshaller();
        WebAppConfig o =
                (WebAppConfig) u.unmarshal(getClass().getResource(
                        this.configLocation));

        final URI uri = URI.create("http://localhost:" + port);

        final Map<String, String> initParams = new HashMap<String, String>();

        server = GrizzlyServerFactory.createHttpServer(uri, new HttpHandler() {

            @Override
            public void service(Request req, Response res) throws Exception {
                res.setStatus(404, "Not found");
                res.getWriter().write("404: not found");
            }
        });

        // create a "root" web application
        appContext = new WebappContext(o.displayName(), "/");

        for (ContextParam p : o.contextParams()) {
            appContext.addContextInitParameter(p.name(), p.value());
        }

        for (Listener l : o.listeners()) {
            appContext.addListener(l.className());
        }

        for (Servlet s : o.servlets()) {
            ServletRegistration servlet =
                    appContext.addServlet(s.servletName(), s.servletClass());

            Collection<ServletMapping> mappings =
                    o.servletMappings(s.servletName());
            for (ServletMapping sm : mappings) {
                servlet.addMapping(sm.urlPattern());
            }
            for (InitParam p : s.initParams()) {
                servlet.setInitParameter(p.name(), p.value());
            }
        }

        for (Filter f : o.filters()) {
            FilterRegistration filter =
                    appContext.addFilter(f.filterName(), f.filterClass());

            Collection<FilterMapping> mappings =
                    o.filterMappings(f.filterName());
            for (FilterMapping sm : mappings) {
                String urlPattern = sm.urlPattern();
                String servletName = sm.servletName();
                if (urlPattern != null) {
                    filter.addMappingForUrlPatterns(null, urlPattern);
                } else {
                    filter.addMappingForServletNames(null, servletName);
                }

            }
            for (InitParam p : f.initParams()) {
                filter.setInitParameter(p.name(), p.value());
            }
        }

        appContext.deploy(server);

        logger.debug("started grizzly webserver endpoint at " +
                server.getHttpHandler().getName());
    }

    @PreDestroy
    public void stop() {
        try {
            appContext.undeploy();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        } finally {
            server.stop();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        // this.applicationContext = applicationContext;

    }

}
