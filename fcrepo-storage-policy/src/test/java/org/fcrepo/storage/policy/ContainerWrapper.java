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

package org.fcrepo.storage.policy;

import static org.slf4j.LoggerFactory.getLogger;

import org.fcrepo.storage.policy.bind.ContextParam;
import org.fcrepo.storage.policy.bind.InitParam;
import org.fcrepo.storage.policy.bind.Listener;
import org.fcrepo.storage.policy.bind.Servlet;
import org.fcrepo.storage.policy.bind.ServletMapping;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.grizzly2.servlet.GrizzlyWebContainerFactory;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ContainerWrapper implements ApplicationContextAware {

    private static final Logger logger = getLogger(ContainerWrapper.class);

    private int port;

    private HttpServer server;

    private String configLocation;

    public void setConfigLocation(final String configLocation) {
        this.configLocation = configLocation.replaceFirst("^classpath:", "/");
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void start() throws Exception {

        final JAXBContext context = JAXBContext.newInstance(WebAppConfig.class);
        final Unmarshaller u = context.createUnmarshaller();
        final WebAppConfig o =
            (WebAppConfig) u.unmarshal(getClass().getResource(configLocation));

        final URI uri = URI.create("http://localhost:" + port + "/");

        final Map<String, String> initParams = new HashMap<String, String>();

        server = GrizzlyWebContainerFactory.create(uri, initParams);

        // create a "root" web application
        final WebappContext wac = new WebappContext(o.displayName(), "");

        for (final ContextParam p : o.contextParams()) {
            wac.addContextInitParameter(p.name(), p.value());
        }

        for (final Listener l : o.listeners) {
            wac.addListener(l.className());
        }

        for (final Servlet s : o.servlets) {
            final ServletRegistration servlet =
                wac.addServlet(s.servletName(), s.servletClass());

            final Collection<ServletMapping> mappings =
                o.servletMappings(s.servletName());
            for (final ServletMapping sm : mappings) {
                servlet.addMapping(sm.urlPattern());
            }
            for (final InitParam p : s.initParams()) {
                servlet.setInitParameter(p.name(), p.value());
            }
        }

        wac.deploy(server);

        final URL webXml = this.getClass().getResource("/web.xml");
        logger.error(webXml.toString());

        logger.debug("started grizzly webserver endpoint at " +
            server.getHttpHandler().getName());
    }

    public void stop() throws Exception {
        server.stop();
    }

    @Override
    public void setApplicationContext(
        final ApplicationContext applicationContext) throws BeansException {
        // this.applicationContext = applicationContext;

    }

}
