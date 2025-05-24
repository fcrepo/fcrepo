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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.xbean.BrokerFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

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
     * @return JMS message factory
     */
    @Bean
    public JMSEventMessageFactory messageFactory() {
        return new DefaultMessageFactory();
    }

    /**
     * JMS Broker configuration
     *
     * @param propsConfig config properties
     * @return jms broker
     */
    @Bean
    public BrokerFactoryBean jmsBroker(final FedoraPropsConfig propsConfig) {
        final var factory = new BrokerFactoryBean();
        factory.setConfig(propsConfig.getActiveMQConfiguration());
        factory.setStart(true);
        return factory;
    }

    /**
     * ActiveMQ connection
     *
     * @param propsConfig config properties
     * @return ActiveMQ connection factory
     */
    @Bean
    @DependsOn("jmsBroker")
    public ActiveMQConnectionFactory connectionFactory(final FedoraPropsConfig propsConfig) {
        final var factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(String.format("vm://%s:%s?create=false",
                propsConfig.getJmsHost(), propsConfig.getJmsPort()));
        return factory;
    }

}
