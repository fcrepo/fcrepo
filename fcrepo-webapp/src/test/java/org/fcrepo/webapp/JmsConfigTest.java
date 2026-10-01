/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.webapp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.jms.ConnectionFactory;

import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
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
