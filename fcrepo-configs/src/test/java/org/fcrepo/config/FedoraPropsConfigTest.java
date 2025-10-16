/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * @author bbpennel
 */
public class FedoraPropsConfigTest {

    private FedoraPropsConfig config;
    private GenericApplicationContext context;
    private MockEnvironment env;

    @BeforeEach
    public void setUp() {
        config = new FedoraPropsConfig();
        env = new MockEnvironment();
    }

    @AfterEach
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    private void initializeContext() {
        context = new AnnotationConfigApplicationContext();
        context.setEnvironment(env);
        context.registerBean(FedoraPropsConfig.class);
    }

    private void initializeConfig() {
        context.refresh();
        config = context.getBean(FedoraPropsConfig.class);
    }

    @Test
    public void testDefaultValues() {
        initializeContext();
        initializeConfig();

        assertEquals(Paths.get("fcrepo-home"), config.getFedoraHome());
        assertEquals(Paths.get("fcrepo-home/data"), config.getFedoraData());
        assertEquals(Paths.get("fcrepo-home/logs"), config.getFedoraLogs());
        assertEquals("localhost", config.getJmsHost());
        assertEquals("61616", config.getJmsPort());
        assertEquals("61613", config.getStompPort());
        assertNotNull(config.getActiveMQConfiguration());
        assertEquals(Paths.get("fcrepo-home/data/ActiveMQ/kahadb").toAbsolutePath().toString(),
                config.getActiveMqDirectory());
        assertEquals("classpath:/namespaces.yml", config.getNamespaceRegistry());
        assertNull(config.getExternalContentAllowed());
        assertEquals(Duration.ofMillis(180000), config.getSessionTimeout());
        assertEquals(Paths.get("fcrepo-home/logs/velocity.log"), config.getVelocityLog());
        assertTrue(config.isRebuildValidation());
        assertFalse(config.isRebuildFixityCheck());
        assertFalse(config.isRebuildOnStart());
        assertFalse(config.isRebuildContinue());
        assertFalse(config.isRebuildEnabled());
        assertNull(config.getJmsBaseUrl());
        assertEquals(ServerManagedPropsMode.STRICT, config.getServerManagedPropsMode());
        assertEquals(JmsDestination.TOPIC, config.getJmsDestinationType());
        assertEquals("fedora", config.getJmsDestinationName());
        assertEquals(1, config.getEventBusThreads());
        assertEquals(1024L, config.getContainmentCacheSize());
        assertEquals(10L, config.getContainmentCacheTimeout());
        assertEquals(1024L, config.getUserTypesCacheSize());
        assertEquals(10L, config.getUserTypesCacheTimeout());
        assertEquals(1024L, config.getWebacCacheSize());
        assertEquals(10L, config.getWebacCacheTimeout());
        assertTrue(config.getBannerEnabled());
        assertEquals(0, config.getFcrepoPidMinterLength());
        assertEquals(0, config.getFcrepoPidMinterCount());
        assertFalse(config.includeTransactionOnConflict());
    }

    @Test
    public void testSetters() {
        // Test setFedoraHome
        final Path newHome = Paths.get("/new/home");
        config.setFedoraHome(newHome);
        assertEquals(newHome, config.getFedoraHome());

        // Test setFedoraData
        final Path newData = Paths.get("/new/data");
        config.setFedoraData(newData);
        assertEquals(newData, config.getFedoraData());

        // Test setSessionTimeout
        final Duration newTimeout = Duration.ofSeconds(30);
        config.setSessionTimeout(newTimeout);
        assertEquals(newTimeout, config.getSessionTimeout());

        // Test setRebuildOnStart
        config.setRebuildOnStart(true);
        assertTrue(config.isRebuildOnStart());

        // Test setRebuildContinue
        config.setRebuildContinue(true);
        assertTrue(config.isRebuildContinue());

        // Test setRebuildEnabled
        config.setRebuildEnabled(true);
        assertTrue(config.isRebuildEnabled());

        // Test setServerManagedPropsMode
        config.setServerManagedPropsMode(ServerManagedPropsMode.RELAXED);
        assertEquals(ServerManagedPropsMode.RELAXED, config.getServerManagedPropsMode());

        // Test setUserTypesCacheSize
        config.setUserTypesCacheSize(2048L);
        assertEquals(2048L, config.getUserTypesCacheSize());

        // Test setUserTypesCacheTimeout
        config.setUserTypesCacheTimeout(20L);
        assertEquals(20L, config.getUserTypesCacheTimeout());

        // Test setFcrepoPidMinterLength
        config.setFcrepoPidMinterLength(8);
        assertEquals(8, config.getFcrepoPidMinterLength());

        // Test setFcrepoPidMinterCount
        config.setFcrepoPidMinterCount(2);
        assertEquals(2, config.getFcrepoPidMinterCount());

        // Test setIncludeTransactionOnConflict
        config.setIncludeTransactionOnConflict(true);
        assertTrue(config.includeTransactionOnConflict());
    }

    @Test
    public void testCheckRebuildPropsValidationFalse() {
        // Valid case - both false
        env.setProperty("fcrepo.rebuild.validation", "false");
        env.setProperty("fcrepo.rebuild.validation.fixity", "false");
        initializeContext();
        initializeConfig();

        assertFalse(config.isRebuildValidation());
        assertFalse(config.isRebuildFixityCheck());
    }

    @Test
    public void testCheckRebuildPropsValidationTrue() {
        // Valid case - both false
        env.setProperty("fcrepo.rebuild.validation", "true");
        env.setProperty("fcrepo.rebuild.validation.fixity", "true");
        initializeContext();
        initializeConfig();

        assertTrue(config.isRebuildValidation());
        assertTrue(config.isRebuildFixityCheck());
    }

    @Test
    public void testCheckRebuildPropsValidationFixityTrueValidationFalse() {
        // Invalid case - validation must be true if fixity is true
        env.setProperty("fcrepo.rebuild.validation", "false");
        env.setProperty("fcrepo.rebuild.validation.fixity", "true");
        initializeContext();
        final var exception = assertThrows(BeanCreationException.class, this::initializeConfig);
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @Test
    public void testCheckRebuildPropsValidationFixityFalseValidationTrue() {
        // Valid case
        env.setProperty("fcrepo.rebuild.validation", "true");
        env.setProperty("fcrepo.rebuild.validation.fixity", "false");
        initializeContext();
        initializeConfig();
        assertTrue(config.isRebuildValidation());
        assertFalse(config.isRebuildFixityCheck());
    }

    @Test
    public void testEventBusThreadsWithZero() {
        env.setProperty("fcrepo.event.threads", "0");
        initializeContext();
        initializeConfig();

        assertEquals(1, config.getEventBusThreads());
    }

    @Test
    public void testEventBusThreadsWitNegative() {
        env.setProperty("fcrepo.event.threads", "-1");
        initializeContext();
        initializeConfig();

        assertEquals(1, config.getEventBusThreads());
    }

    @Test
    public void testEventBusThreadsWitPositive() {
        env.setProperty("fcrepo.event.threads", "5");
        initializeContext();
        initializeConfig();

        assertEquals(5, config.getEventBusThreads());
    }
}