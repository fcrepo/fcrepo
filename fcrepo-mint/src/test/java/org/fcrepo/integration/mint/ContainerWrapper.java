/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.integration.mint;


import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>ContainerWrapper class.</p>
 *
 * @author osmandin
 * @author ajs6f
 */
public class ContainerWrapper implements ApplicationContextAware {

    private static final Logger logger = getLogger(ContainerWrapper.class);

    private int port;

    public void setPort(final int port) {
        this.port = port;
    }

    private HttpServer server;

    @PostConstruct
    public void start() {
        server = new HttpServer();
        final NetworkListener listener = new NetworkListener("grizzly", "localhost", new PortRange(port));
        server.addListener(listener);

        try {
            server.start();
            logger.info("Test server running on {}", port);
        } catch (final Throwable e) {
            logger.error("Error with test server", e);
        }
    }

    public void addHandler(final String data, final String path) {
        final HttpHandler httpHandler = new HttpHandler() {

            @Override
            public void service(final Request request, final Response response)
                    throws Exception {
                response.getWriter().write(data);
            }
        };

        server.getServerConfiguration().addHttpHandler(httpHandler, "/" + path);
    }

    @PreDestroy
    public void stop() {
        server.shutdownNow();
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
    }

}
