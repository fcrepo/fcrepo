/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.webapp;

import jakarta.annotation.PostConstruct;

import org.fcrepo.config.ConditionOnPropertyTrue;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.config.JmsDestination;
import org.fcrepo.jms.AbstractJMSPublisher;
import org.fcrepo.jms.DefaultMessageFactory;
import org.fcrepo.jms.JMSEventMessageFactory;
import org.fcrepo.jms.JMSQueuePublisher;
import org.fcrepo.jms.JMSTopicPublisher;

import org.apache.activemq.xbean.BrokerFactoryBean;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import jakarta.jms.ConnectionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Spring config for jms
 *
 * @author pwinckles
 */
@Configuration
@Conditional(JmsConfig.JmsEnabled.class)
public class JmsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmsConfig.class);

    static class JmsEnabled extends ConditionOnPropertyTrue {
        JmsEnabled() {
            super(FedoraPropsConfig.FCREPO_JMS_ENABLED, true);
        }
    }

    static class ActiveMqConfigured implements Condition {
        @Override
        public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            final var provider = context.getEnvironment().getProperty(FedoraPropsConfig.FCREPO_JMS_PROVIDER,
                    "activemq");
            LOGGER.info("ActiveMQ support is deprecated and will likely be removed in version 8");
            return "activemq".equalsIgnoreCase(provider);
        }

    }

    static class ArtemisConfigured implements Condition {
        @Override
        public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            final var provider = context.getEnvironment().getProperty(FedoraPropsConfig.FCREPO_JMS_PROVIDER,
                    "activemq");
            return "artemis".equalsIgnoreCase(provider);
        }
    }

    @PostConstruct
    public void postConstruct() {
        LOGGER.info("JMS messaging enabled");
    }

    /**
     * Creates a queue or topic publisher based on the property fcrepo.jms.destination.type. By default, this is a topic
     *
     * @param propsConfig config properties
     * @return jms publisher
     */
    @Bean
    public AbstractJMSPublisher jmsPublisher(final FedoraPropsConfig propsConfig) {
        if (propsConfig.getJmsDestinationType() == JmsDestination.QUEUE) {
            return new JMSQueuePublisher(propsConfig.getJmsDestinationName());
        } else {
            return new JMSTopicPublisher(propsConfig.getJmsDestinationName());
        }
    }

    /**
     * translates events into JMS header-only format
     *
     * @param propsConfig config properties
     * @return JMS message factory
     */
    @Bean
    public JMSEventMessageFactory messageFactory(final FedoraPropsConfig propsConfig) {
        final var factory = new DefaultMessageFactory();
        factory.setJmsProvider(propsConfig.getJmsProvider());
        return factory;
    }

    /**
     * ActiveMQ 5 broker configuration
     *
     * @param propsConfig config properties
     * @return ActiveMQ 5 broker factory
     */
    @Bean(name = "jmsBroker")
    @Conditional(ActiveMqConfigured.class)
    public BrokerFactoryBean activeMqBroker(final FedoraPropsConfig propsConfig) {
        final var factory = new BrokerFactoryBean();
        factory.setConfig(propsConfig.getActiveMQConfiguration());
        factory.setStart(true);
        return factory;
    }

    /**
     * Artemis broker configuration
     *
     * @param propsConfig config properties
     * @return embedded Artemis broker
     * @throws Exception if the broker configuration cannot be loaded
     */
    @Bean(name = "jmsBroker", initMethod = "start", destroyMethod = "stop")
    @Conditional(ArtemisConfigured.class)
    public EmbeddedActiveMQ artemisBroker(final FedoraPropsConfig propsConfig) throws Exception {
        final var broker = new EmbeddedActiveMQ();
        final var configuration = materializeArtemisConfiguration(propsConfig.getArtemisConfiguration());
        LOGGER.info("Using Artemis broker configuration at {}", configuration);
        broker.setConfigResourcePath(configuration.toUri().toString());
        return broker;
    }

    private Path materializeArtemisConfiguration(final Resource configuration) throws IOException {
        if (!configuration.exists()) {
            throw new IOException("Artemis configuration does not exist: " + configuration);
        }

        if (configuration.isFile()) {
            try {
                return configuration.getFile().toPath();
            } catch (final IOException e) {
                LOGGER.debug("Artemis configuration is not directly accessible as a file: {}",
                        configuration, e);
            }
        }

        final Path tempFile = Files.createTempFile("fcrepo-artemis-", "-broker.xml");
        tempFile.toFile().deleteOnExit();

        try (InputStream input = configuration.getInputStream()) {
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }

    /**
     * ActiveMQ 5 JMS connection
     *
     * @param propsConfig config properties
     * @return ActiveMQ 5 connection factory
     */
    @Bean
    @DependsOn("jmsBroker")
    @Conditional(ActiveMqConfigured.class)
    public ConnectionFactory activeMqConnectionFactory(final FedoraPropsConfig propsConfig) {
        return new org.apache.activemq.ActiveMQConnectionFactory(String.format("tcp://%s:%s",
                propsConfig.getJmsHost(), propsConfig.getJmsPort()));
    }

    /**
     * Artemis JMS connection
     *
     * @param propsConfig config properties
     * @return Artemis connection factory
     */
    @Bean
    @DependsOn("jmsBroker")
    @Conditional(ArtemisConfigured.class)
    public ConnectionFactory artemisConnectionFactory(final FedoraPropsConfig propsConfig) {
        return new org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory(String.format("tcp://%s:%s",
                propsConfig.getJmsHost(), propsConfig.getJmsPort()));
    }

}
