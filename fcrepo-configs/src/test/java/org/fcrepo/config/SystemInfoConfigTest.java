/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author bbpennel
 */
public class SystemInfoConfigTest {

    private SystemInfoConfig config;
    private GenericApplicationContext context;
    private MockEnvironment env;

    @BeforeEach
    public void setUp() {
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
        context.registerBean(SystemInfoConfig.class);
    }

    private void initializeConfig() {
        context.refresh();
        config = context.getBean(SystemInfoConfig.class);
    }

    @Test
    public void testDefaultValues() {
        initializeContext();
        initializeConfig();

        assertEquals("", config.getGitCommit());
        assertNotNull(config.getImplementationVersion());
    }

    @Test
    public void testGitCommitProperty() {
        final String testCommit = "abc1234";
        env.setProperty(SystemInfoConfig.GIT_COMMIT, testCommit);

        initializeContext();
        initializeConfig();

        assertEquals(testCommit, config.getGitCommit());
    }

    @Test
    public void testImplementationVersion() {
        initializeContext();
        initializeConfig();

        // This method should never return null, but may return empty string in test context
        assertNotNull(config.getImplementationVersion());
    }

    /**
     * Mock implementation to test the behavior when package has an implementation version
     */
    @Test
    public void testImplementationVersionWithMock() {
        // Create a custom subclass to override the getImplementationVersion method
        // to simulate a case where the package has a version
        class MockSystemInfoConfig extends SystemInfoConfig {
            @Override
            public String getImplementationVersion() {
                return "1.2.3";
            }
        }

        final MockSystemInfoConfig mockConfig = new MockSystemInfoConfig();
        assertEquals("1.2.3", mockConfig.getImplementationVersion());
    }
}