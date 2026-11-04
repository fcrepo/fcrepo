/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.webapp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import jakarta.jms.ConnectionFactory;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.BindingQueryResult;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.server.impl.AddressInfo;
import org.apache.activemq.xbean.BrokerFactoryBean;
import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.config.JmsDestination;
import org.fcrepo.jms.DefaultMessageFactory;
import org.fcrepo.jms.JMSQueuePublisher;
import org.fcrepo.jms.JMSTopicPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Unit tests for {@link JmsConfig}.
 *
 * @author Dan Field
 */
@ExtendWith(MockitoExtension.class)
public class JmsConfigTest {

    @Mock
    private FedoraPropsConfig propsConfig;

    @Mock
    private ConditionContext conditionContext;

    @Mock
    private Environment env;

    @Mock
    private AnnotatedTypeMetadata metadata;

    private final JmsConfig config = new JmsConfig();

    @Test
    public void testJmsEnabledConditionConstructs() {
        assertNotNull(new JmsConfig.JmsEnabled());
    }

    @Test
    public void testActiveMqConfiguredMatchesActiveMqProvider() {
        when(conditionContext.getEnvironment()).thenReturn(env);
        when(env.getProperty(FedoraPropsConfig.FCREPO_JMS_PROVIDER, "activemq")).thenReturn("activemq");

        assertTrue(new JmsConfig.ActiveMqConfigured().matches(conditionContext, metadata));
    }

    @Test
    public void testActiveMqConfiguredDoesNotMatchArtemisProvider() {
        when(conditionContext.getEnvironment()).thenReturn(env);
        when(env.getProperty(FedoraPropsConfig.FCREPO_JMS_PROVIDER, "activemq")).thenReturn("artemis");

        assertFalse(new JmsConfig.ActiveMqConfigured().matches(conditionContext, metadata));
    }

    @Test
    public void testArtemisConfiguredMatchesArtemisProvider() {
        when(conditionContext.getEnvironment()).thenReturn(env);
        when(env.getProperty(FedoraPropsConfig.FCREPO_JMS_PROVIDER, "activemq")).thenReturn("artemis");

        assertTrue(new JmsConfig.ArtemisConfigured().matches(conditionContext, metadata));
    }

    @Test
    public void testActiveMqConfiguredMatchesUnknownProvider() {
        when(conditionContext.getEnvironment()).thenReturn(env);
        when(env.getProperty(FedoraPropsConfig.FCREPO_JMS_PROVIDER, "activemq")).thenReturn("rabbitmq");

        assertTrue(new JmsConfig.ActiveMqConfigured().matches(conditionContext, metadata),
                "Unknown provider must fall back to the default provider");
    }

    @Test
    public void testArtemisConfiguredDoesNotMatchUnknownProvider() {
        when(conditionContext.getEnvironment()).thenReturn(env);
        when(env.getProperty(FedoraPropsConfig.FCREPO_JMS_PROVIDER, "activemq")).thenReturn("rabbitmq");

        assertFalse(new JmsConfig.ArtemisConfigured().matches(conditionContext, metadata));
    }

    @Test
    public void testArtemisConfiguredMatchesMixedCaseProvider() {
        when(conditionContext.getEnvironment()).thenReturn(env);
        when(env.getProperty(FedoraPropsConfig.FCREPO_JMS_PROVIDER, "activemq")).thenReturn("Artemis");

        assertTrue(new JmsConfig.ArtemisConfigured().matches(conditionContext, metadata));
    }

    @Test
    public void testArtemisConfiguredDoesNotMatchActiveMqProvider() {
        when(conditionContext.getEnvironment()).thenReturn(env);
        when(env.getProperty(FedoraPropsConfig.FCREPO_JMS_PROVIDER, "activemq")).thenReturn("activemq");

        assertFalse(new JmsConfig.ArtemisConfigured().matches(conditionContext, metadata));
    }

    @Test
    public void testPostConstruct() {
        config.postConstruct();
    }

    @Test
    public void testJmsPublisherReturnsTopicPublisherWhenDestinationIsTopic() {
        when(propsConfig.getJmsDestinationType()).thenReturn(JmsDestination.TOPIC);
        when(propsConfig.getJmsDestinationName()).thenReturn("/fedora");

        assertInstanceOf(JMSTopicPublisher.class, config.jmsPublisher(propsConfig));
    }

    @Test
    public void testJmsPublisherReturnsQueuePublisherWhenDestinationIsQueue() {
        when(propsConfig.getJmsDestinationType()).thenReturn(JmsDestination.QUEUE);
        when(propsConfig.getJmsDestinationName()).thenReturn("/fedora");

        assertInstanceOf(JMSQueuePublisher.class, config.jmsPublisher(propsConfig));
    }

    @Test
    public void testMessageFactoryAppliesArtemisProviderFromConfig() {
        when(propsConfig.getJmsProvider()).thenReturn("artemis");

        final var factory = (DefaultMessageFactory) config.messageFactory(propsConfig);

        assertTrue(factory.getTimestampHeaderName().startsWith("org_fcrepo_jms_"),
                "Artemis provider must yield underscore-namespaced headers");
    }

    @Test
    public void testMessageFactoryAppliesActiveMqProviderFromConfig() {
        when(propsConfig.getJmsProvider()).thenReturn("activemq");

        final var factory = (DefaultMessageFactory) config.messageFactory(propsConfig);

        assertTrue(factory.getTimestampHeaderName().startsWith("org.fcrepo.jms."),
                "ActiveMQ provider must yield dot-namespaced headers");
    }

    @Test
    public void testActiveMqBrokerConstructsFactoryBeanFromConfig() {
        final Resource activeMqXml = new ByteArrayResource("<beans/>".getBytes(UTF_8));
        when(propsConfig.getActiveMQConfiguration()).thenReturn(activeMqXml);

        final BrokerFactoryBean factoryBean = config.activeMqBroker(propsConfig);

        assertNotNull(factoryBean);
    }

    @Test
    public void testArtemisBrokerWithFileResource(@TempDir final Path tempDir) throws Exception {
        final Path brokerXml = tempDir.resolve("broker.xml");
        Files.writeString(brokerXml, "<configuration/>", UTF_8);
        when(propsConfig.getArtemisConfiguration()).thenReturn(new FileSystemResource(brokerXml));

        final EmbeddedActiveMQ broker = config.artemisBroker(propsConfig);

        assertNotNull(broker);
    }

    /**
     * Exercises the fallback path in {@code materializeArtemisConfiguration}: when the configured Resource exists
     * but cannot be opened directly as a {@code File} (e.g. a classpath resource inside a JAR), it must be copied
     * into a temp file before Artemis can read it.
     */
    @Test
    public void testArtemisBrokerWithNonFileResourceCopiesToTempFile() throws Exception {
        final Resource inJarResource = new ByteArrayResource("<configuration/>".getBytes(UTF_8));
        when(propsConfig.getArtemisConfiguration()).thenReturn(inJarResource);

        final EmbeddedActiveMQ broker = config.artemisBroker(propsConfig);

        assertNotNull(broker);
    }

    @Test
    public void testArtemisBrokerPublishesBrokerSettingsAsSystemProperties(@TempDir final Path tempDir)
            throws Exception {
        final Path brokerXml = tempDir.resolve("broker.xml");
        Files.writeString(brokerXml, "<configuration/>", UTF_8);
        when(propsConfig.getArtemisConfiguration()).thenReturn(new FileSystemResource(brokerXml));
        when(propsConfig.getJmsPort()).thenReturn("61626");
        when(propsConfig.getStompPort()).thenReturn("61623");
        when(propsConfig.getArtemisDirectory()).thenReturn("/data/fcrepo/Artemis/db");
        when(propsConfig.getJmsDestinationName()).thenReturn("repo-events");
        try {
            config.artemisBroker(propsConfig);

            assertEquals("61626", System.getProperty(FedoraPropsConfig.FCREPO_DYNAMIC_JMS_PORT));
            assertEquals("61623", System.getProperty(FedoraPropsConfig.FCREPO_DYNAMIC_STOMP_PORT));
            assertEquals("/data/fcrepo/Artemis/db", System.getProperty(FedoraPropsConfig.FCREPO_ARTEMIS_DIRECTORY));
            assertEquals("repo-events", System.getProperty(FedoraPropsConfig.FCREPO_JMS_DESTINATION_NAME));
        } finally {
            System.clearProperty(FedoraPropsConfig.FCREPO_DYNAMIC_JMS_PORT);
            System.clearProperty(FedoraPropsConfig.FCREPO_DYNAMIC_STOMP_PORT);
            System.clearProperty(FedoraPropsConfig.FCREPO_ARTEMIS_DIRECTORY);
            System.clearProperty(FedoraPropsConfig.FCREPO_JMS_DESTINATION_NAME);
        }
    }

    @Test
    public void testArtemisBrokerDeclaresDestinationAddress(@TempDir final Path tempDir) throws Exception {
        final Path brokerXml = tempDir.resolve("broker.xml");
        Files.writeString(brokerXml, "<configuration xmlns=\"urn:activemq\"><core xmlns=\"urn:activemq:core\">"
                + "<persistence-enabled>false</persistence-enabled></core></configuration>", UTF_8);
        when(propsConfig.getArtemisConfiguration()).thenReturn(new FileSystemResource(brokerXml));
        when(propsConfig.getJmsDestinationName()).thenReturn("repo-events");
        when(propsConfig.getJmsDestinationType()).thenReturn(JmsDestination.TOPIC);
        try {
            final Configuration configuration = config.artemisBroker(propsConfig).getConfiguration();

            assertFalse(configuration.isPersistenceEnabled(), "Settings from broker.xml must be kept");
            final CoreAddressConfiguration address = configuration.getAddressConfigurations().get(0);
            assertEquals("repo-events", address.getName());
            assertEquals(EnumSet.of(RoutingType.MULTICAST), address.getRoutingTypes());
        } finally {
            System.clearProperty(FedoraPropsConfig.FCREPO_JMS_DESTINATION_NAME);
        }
    }

    @Test
    public void testAddDestinationAddressForTopicIsMulticastOnly() {
        final Configuration configuration = new ConfigurationImpl();

        JmsConfig.addDestinationAddress(configuration, "fedora", JmsDestination.TOPIC);

        final CoreAddressConfiguration address = configuration.getAddressConfigurations().get(0);
        assertEquals("fedora", address.getName());
        assertEquals(EnumSet.of(RoutingType.MULTICAST), address.getRoutingTypes());
        assertTrue(address.getQueueConfigs().isEmpty(), "Topic address must not declare a queue");
    }

    @Test
    public void testAddDestinationAddressForQueueDeclaresAnycastQueue() {
        final Configuration configuration = new ConfigurationImpl();

        JmsConfig.addDestinationAddress(configuration, "fedora", JmsDestination.QUEUE);

        final CoreAddressConfiguration address = configuration.getAddressConfigurations().get(0);
        assertEquals(EnumSet.of(RoutingType.ANYCAST), address.getRoutingTypes());
        assertEquals(1, address.getQueueConfigs().size());
        assertEquals("fedora", address.getQueueConfigs().get(0).getName().toString());
        assertEquals(RoutingType.ANYCAST, address.getQueueConfigs().get(0).getRoutingType());
        assertTrue(address.getQueueConfigs().get(0).isDurable());
    }

    @Test
    public void testAddDestinationAddressKeepsAddressDeclaredInConfiguration() {
        final Configuration configuration = new ConfigurationImpl();
        configuration.addAddressConfiguration(new CoreAddressConfiguration().setName("fedora")
                .addRoutingType(RoutingType.ANYCAST).addRoutingType(RoutingType.MULTICAST));

        JmsConfig.addDestinationAddress(configuration, "fedora", JmsDestination.TOPIC);

        assertEquals(1, configuration.getAddressConfigurations().size());
        assertEquals(EnumSet.of(RoutingType.ANYCAST, RoutingType.MULTICAST),
                configuration.getAddressConfigurations().get(0).getRoutingTypes());
    }

    private static ActiveMQServer serverWithAnycastQueue(final long messageCount) throws Exception {
        final var name = SimpleString.of("fedora");
        final ActiveMQServer server = mock(ActiveMQServer.class);
        final BindingQueryResult bindings = mock(BindingQueryResult.class);
        final Queue queue = mock(Queue.class);
        when(server.getAddressInfo(name)).thenReturn(new AddressInfo(name)
                .addRoutingType(RoutingType.ANYCAST).addRoutingType(RoutingType.MULTICAST));
        when(server.bindingQuery(name)).thenReturn(bindings);
        when(bindings.getQueueNames()).thenReturn(List.of(name));
        when(server.locateQueue(name)).thenReturn(queue);
        when(queue.getRoutingType()).thenReturn(RoutingType.ANYCAST);
        when(queue.getMessageCount()).thenReturn(messageCount);
        return server;
    }

    @Test
    public void testRemoveAnycastRoutingRemovesEmptyQueue() throws Exception {
        final ActiveMQServer server = serverWithAnycastQueue(0);

        JmsConfig.removeAnycastRouting(server, "fedora");

        verify(server).destroyQueue(SimpleString.of("fedora"));
        verify(server).updateAddressInfo(SimpleString.of("fedora"), EnumSet.of(RoutingType.MULTICAST));
    }

    @Test
    public void testRemoveAnycastRoutingKeepsQueueWithMessages() throws Exception {
        final ActiveMQServer server = serverWithAnycastQueue(3);

        JmsConfig.removeAnycastRouting(server, "fedora");

        verify(server, never()).destroyQueue(any(SimpleString.class));
        verify(server, never()).updateAddressInfo(any(SimpleString.class), any(EnumSet.class));
    }

    @Test
    public void testRemoveAnycastRoutingIgnoresMulticastOnlyAddress() throws Exception {
        final var name = SimpleString.of("fedora");
        final ActiveMQServer server = mock(ActiveMQServer.class);
        when(server.getAddressInfo(name)).thenReturn(new AddressInfo(name).addRoutingType(RoutingType.MULTICAST));

        JmsConfig.removeAnycastRouting(server, "fedora");

        verify(server, never()).bindingQuery(any(SimpleString.class));
        verify(server, never()).updateAddressInfo(any(SimpleString.class), any(EnumSet.class));
    }

    @Test
    public void testArtemisBrokerThrowsWhenConfigurationDoesNotExist() {
        when(propsConfig.getArtemisConfiguration()).thenReturn(
                new FileSystemResource(Path.of("/this/path/does/not/exist/broker.xml")));

        assertThrows(IOException.class, () -> config.artemisBroker(propsConfig));
    }

    @Test
    public void testActiveMqConnectionFactoryUsesConfiguredHostAndPort() {
        when(propsConfig.getJmsHost()).thenReturn("localhost");
        when(propsConfig.getJmsPort()).thenReturn("61616");

        final ConnectionFactory cf = config.activeMqConnectionFactory(propsConfig);

        assertInstanceOf(org.apache.activemq.ActiveMQConnectionFactory.class, cf);
    }

    @Test
    public void testArtemisConnectionFactoryUsesConfiguredHostAndPort() {
        when(propsConfig.getJmsHost()).thenReturn("localhost");
        when(propsConfig.getJmsPort()).thenReturn("61616");

        final ConnectionFactory cf = config.artemisConnectionFactory(propsConfig);

        assertInstanceOf(org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory.class, cf);
    }
}
