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

package org.fcrepo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * General Fedora properties
 *
 * @author pwinckles
 * @since 6.0.0
 */
@Configuration
public class FedoraPropsConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FedoraPropsConfig.class);

    public static final String FCREPO_JMS_HOST = "fcrepo.jms.host";
    public static final String FCREPO_DYNAMIC_JMS_PORT = "fcrepo.dynamic.jms.port";
    public static final String FCREPO_DYNAMIC_STOMP_PORT = "fcrepo.dynamic.stomp.port";
    public static final String FCREPO_ACTIVEMQ_CONFIGURATION = "fcrepo.activemq.configuration";
    public static final String FCREPO_NAMESPACE_REGISTRY = "fcrepo.namespace.registry";
    public static final String FCREPO_EXTERNAL_CONTENT_ALLOWED = "fcrepo.external.content.allowed";
    private static final String FCREPO_ACTIVEMQ_DIRECTORY = "fcrepo.activemq.directory";
    private static final String FCREPO_SESSION_TIMEOUT = "fcrepo.session.timeout";
    private static final String FCREPO_VELOCITY_RUNTIME_LOG = "fcrepo.velocity.runtime.log";

    private static final String DATA_DIR_DEFAULT_VALUE = "data";
    private static final String ACTIVE_MQ_DIR_DEFAULT_VALUE = "ActiveMQ/kahadb";

    @Value("${" + FCREPO_HOME_PROPERTY + ":" + DEFAULT_FCREPO_HOME_VALUE + "}")
    protected Path fedoraHome;

    @Value("#{fedoraPropsConfig.fedoraHome.resolve('" + DATA_DIR_DEFAULT_VALUE + "')}")
    private Path fedoraData;

    @Value("${" + FCREPO_JMS_HOST + ":localhost}")
    private String jmsHost;

    @Value("${" + FCREPO_DYNAMIC_JMS_PORT + ":61616}")
    private String jmsPort;

    @Value("${" + FCREPO_DYNAMIC_STOMP_PORT + ":61613}")
    private String stompPort;

    @Value("${" + FCREPO_ACTIVEMQ_CONFIGURATION + ":classpath:/config/activemq.xml}")
    private String activeMQConfiguration;

    @Value("${" + FCREPO_ACTIVEMQ_DIRECTORY + ":#{fedoraPropsConfig.fedoraData.resolve('" +
            ACTIVE_MQ_DIR_DEFAULT_VALUE + "').toAbsolutePath().toString()}}")
    private String activeMqDirectory;

    @Value("${" + FCREPO_NAMESPACE_REGISTRY + ":classpath:/namespaces.yml}")
    private String namespaceRegistry;

    @Value("${" + FCREPO_EXTERNAL_CONTENT_ALLOWED + ":#{null}}")
    private String externalContentAllowed;

    @Value("${" + FCREPO_SESSION_TIMEOUT + ":180000}")
    private String sessionTimeout;

    @Value("${" + FCREPO_VELOCITY_RUNTIME_LOG + ": " +
            "#{fedoraPropsConfig.fedoraHome.resolve('velocity.log').toAbsolutePath().toString()}}")
    private String velocityLog;

    @PostConstruct
    private void postConstruct() throws IOException {
        LOGGER.info("Fedora home: {}", fedoraHome);
        LOGGER.debug("Fedora home data: {}", fedoraData);
        try {
            Files.createDirectories(fedoraHome);
        } catch (final IOException e) {
            throw new IOException(String.format("Failed to create Fedora home directory at %s." +
                    " Fedora home can be configured by setting the %s property.", fedoraHome, FCREPO_HOME_PROPERTY), e);
        }
        Files.createDirectories(fedoraData);
    }

    /**
     * @return Path to Fedora home directory
     */
    public Path getFedoraHome() {
        return fedoraHome;
    }

    /**
     * Sets the path to the Fedora home directory -- should only be used for testing purposes.
     *
     * @param fedoraHome Path to Fedora home directory
     */
    public void setFedoraHome(final Path fedoraHome) {
        this.fedoraHome = fedoraHome;
    }

    /**
     * @return Path to Fedora home data directory
     */
    public Path getFedoraData() {
        return fedoraData;
    }

    /**
     * Sets the path to the Fedora home data directory -- should only be used for testing purposes.
     *
     * @param fedoraData Path to Fedora home data directory
     */
    public void setFedoraData(final Path fedoraData) {
        this.fedoraData = fedoraData;
    }

    /**
     * @return The JMS host
     */
    public String getJmsHost() {
        return jmsHost;
    }

    /**
     * @return The JMS/Open Wire port
     */
    public String getJmsPort() {
        return jmsPort;
    }

    /**
     * @return The STOMP protocol port
     */
    public String getStompPort() {
        return stompPort;
    }

    /**
     * @return The ActiveMQ data directory
     */
    public String getActiveMqDirectory() {
        return activeMqDirectory;
    }

    /**
     * @return The path to the ActiveMQ xml spring configuration.
     */
    public String getActiveMQConfiguration() {
        return activeMQConfiguration;
    }

    /**
     * @return The path to the allowed external content pattern definitions.
     */
    public String getExternalContentAllowed() {
        return externalContentAllowed;
    }

    /**
     * @return The path to the namespace registry file.
     */
    public String getNamespaceRegistry() {
        return namespaceRegistry;
    }

    /**
     * @return The timeout in milliseconds of the persistence session
     */
    public String getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * @return The path to the velocity log.
     */
    public String getVelocityLog() {
        return velocityLog;
    }
}
