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
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.config.FileDeploymentManager;
import org.apache.activemq.artemis.core.config.impl.FileConfiguration;
import org.apache.activemq.artemis.core.config.impl.LegacyJMSConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import jakarta.jms.ConnectionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;

import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Spring config for jms
 *
 * @author pwinckles
 */
@org.springframework.context.annotation.Configuration
@Conditional(JmsConfig.JmsEnabled.class)
public class JmsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmsConfig.class);

    static class JmsEnabled extends ConditionOnPropertyTrue {
        JmsEnabled() {
            super(FedoraPropsConfig.FCREPO_JMS_ENABLED, true);
        }
    }

    private static String configuredProvider(final ConditionContext context) {
        return FedoraPropsConfig.resolveJmsProvider(context.getEnvironment()
                .getProperty(FedoraPropsConfig.FCREPO_JMS_PROVIDER, FedoraPropsConfig.DEFAULT_JMS_PROVIDER));
    }

    static class ActiveMqConfigured implements Condition {
        @Override
        public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            final boolean matches = FedoraPropsConfig.JMS_PROVIDER_ACTIVEMQ.equals(configuredProvider(context));
            if (matches) {
                LOGGER.warn("ActiveMQ support is deprecated and will be removed in a later version");
            }
            return matches;
        }
    }

    static class ArtemisConfigured implements Condition {
        @Override
        public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
            return FedoraPropsConfig.JMS_PROVIDER_ARTEMIS.equals(configuredProvider(context));
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
        // Artemis resolves ${...} in broker.xml from Java system properties only, so values set in
        // fcrepo.properties would not reach it. Publish the resolved ports, data directory and destination name
        // before the broker reads its config.
        publishSystemProperty(FedoraPropsConfig.FCREPO_DYNAMIC_JMS_PORT, propsConfig.getJmsPort());
        publishSystemProperty(FedoraPropsConfig.FCREPO_DYNAMIC_STOMP_PORT, propsConfig.getStompPort());
        publishSystemProperty(FedoraPropsConfig.FCREPO_ARTEMIS_DIRECTORY, propsConfig.getArtemisDirectory());
        publishSystemProperty(FedoraPropsConfig.FCREPO_JMS_DESTINATION_NAME, propsConfig.getJmsDestinationName());
        final var configurationPath = materializeArtemisConfiguration(propsConfig.getArtemisConfiguration());
        LOGGER.info("Using Artemis broker configuration at {}", configurationPath);
        final var configuration = readArtemisConfiguration(configurationPath);
        final boolean declared = addDestinationAddress(configuration, propsConfig.getJmsDestinationName(),
                propsConfig.getJmsDestinationType());
        final var broker = new ArtemisBroker(propsConfig.getJmsDestinationName(),
                declared && propsConfig.getJmsDestinationType() == JmsDestination.TOPIC);
        broker.setConfiguration(configuration);
        return broker;
    }

    /**
     * Embedded Artemis broker that, after starting in topic mode, removes ANYCAST routing left on Fedora's
     * destination address by an earlier run in queue mode.
     */
    static class ArtemisBroker extends EmbeddedActiveMQ {

        private final String destinationName;

        private final boolean removeAnycastRouting;

        ArtemisBroker(final String destinationName, final boolean removeAnycastRouting) {
            this.destinationName = destinationName;
            this.removeAnycastRouting = removeAnycastRouting;
        }

        @Override
        public EmbeddedActiveMQ start() throws Exception {
            super.start();
            if (removeAnycastRouting) {
                removeAnycastRouting(getActiveMQServer(), destinationName);
            }
            return this;
        }
    }

    /**
     * Makes Fedora's topic address MULTICAST only. After a switch from queue to topic mode the journal still holds
     * the durable anycast queue declared in queue mode, which stops Artemis from removing ANYCAST routing from the
     * address at startup (AMQ229209). STOMP then treats /topic/ subscriptions as anycast and they receive no
     * events. Anycast queues on the address that are empty and have no consumers are removed; a queue that still
     * holds messages is kept and a warning is logged.
     *
     * @param server started broker
     * @param name destination name
     * @throws Exception if a queue or the address cannot be updated
     */
    static void removeAnycastRouting(final ActiveMQServer server, final String name) throws Exception {
        final var address = SimpleString.of(name);
        final var addressInfo = server.getAddressInfo(address);
        if (addressInfo == null || !addressInfo.getRoutingTypes().contains(RoutingType.ANYCAST)) {
            return;
        }

        boolean queueKept = false;
        for (final SimpleString queueName : server.bindingQuery(address).getQueueNames()) {
            final var queue = server.locateQueue(queueName);
            if (queue == null || queue.getRoutingType() != RoutingType.ANYCAST) {
                continue;
            }
            if (queue.getMessageCount() == 0 && queue.getConsumerCount() == 0) {
                server.destroyQueue(queueName);
                LOGGER.info("Removed empty anycast queue '{}' from topic address '{}'", queueName, name);
            } else {
                queueKept = true;
                LOGGER.warn("Topic address '{}' still has anycast queue '{}' with {} messages and {} consumers, "
                        + "probably from an earlier run with {}=queue. STOMP /topic/ subscriptions to '{}' will "
                        + "receive no events until the queue is drained or deleted.", name, queueName,
                        queue.getMessageCount(), queue.getConsumerCount(),
                        FedoraPropsConfig.FCREPO_JMS_DESTINATION_TYPE, name);
            }
        }

        if (!queueKept) {
            server.updateAddressInfo(address, EnumSet.of(RoutingType.MULTICAST));
            LOGGER.info("Removed ANYCAST routing from topic address '{}'", name);
        }
    }

    /**
     * Reads the broker configuration the same way EmbeddedActiveMQ does when given a resource path.
     */
    private static Configuration readArtemisConfiguration(final Path configurationPath) throws Exception {
        final var configuration = new FileConfiguration();
        final var deploymentManager = new FileDeploymentManager(configurationPath.toUri().toString());
        deploymentManager.addDeployable(configuration).addDeployable(new LegacyJMSConfiguration(configuration));
        deploymentManager.readConfiguration();
        return configuration;
    }

    /**
     * Declares Fedora's event destination with the single routing type that matches the destination type:
     * MULTICAST for a topic, ANYCAST with a durable queue of the same name for a queue. A static address in
     * broker.xml cannot follow fcrepo.jms.destination.type, and an address with both routing types makes the
     * Artemis STOMP protocol treat /topic/ subscriptions as anycast. An address that the broker configuration
     * already declares is left unchanged.
     *
     * @param configuration broker configuration
     * @param name destination name
     * @param type destination type
     * @return true if the address was added, false if the configuration already declares it
     */
    static boolean addDestinationAddress(final Configuration configuration, final String name,
                                      final JmsDestination type) {
        final boolean declared = configuration.getAddressConfigurations().stream()
                .anyMatch(address -> name.equals(address.getName()));
        if (declared) {
            LOGGER.info("Artemis configuration already declares address '{}'; leaving it unchanged", name);
            return false;
        }

        final var address = new CoreAddressConfiguration().setName(name);
        if (type == JmsDestination.QUEUE) {
            address.addRoutingType(RoutingType.ANYCAST);
            address.addQueueConfig(QueueConfiguration.of(name)
                    .setAddress(name)
                    .setRoutingType(RoutingType.ANYCAST)
                    .setDurable(true));
        } else {
            address.addRoutingType(RoutingType.MULTICAST);
        }
        configuration.addAddressConfiguration(address);
        LOGGER.info("Declared Artemis address '{}' with routing type {}", name, address.getRoutingTypes());
        return true;
    }

    private static void publishSystemProperty(final String key, final String value) {
        if (value != null) {
            System.setProperty(key, value);
        }
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
