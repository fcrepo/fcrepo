/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Test for {@link ConditionOnPropertyTrue}
 *
 * @author bbpennel
 */
public class ConditionOnPropertyTrueTest {

    private ConditionContext context;
    private Environment environment;
    private AnnotatedTypeMetadata metadata;

    @BeforeEach
    public void setUp() {
        context = mock(ConditionContext.class);
        environment = mock(Environment.class);
        when(context.getEnvironment()).thenReturn(environment);
        metadata = mock(AnnotatedTypeMetadata.class);
    }

    /**
     * Concrete implementation of ConditionOnPropertyTrue for testing
     */
    private static class TestConditionOnPropertyTrue extends ConditionOnPropertyTrue {
        public TestConditionOnPropertyTrue(final String name, final boolean defaultValue) {
            super(name, defaultValue);
        }

        // No-arg constructor required by the interface contract
        public TestConditionOnPropertyTrue() {
            this("test.property", false);
        }
    }

    @Test
    public void testMatchesWhenPropertyIsTrue() {
        final TestConditionOnPropertyTrue condition = new TestConditionOnPropertyTrue("test.property", false);
        when(environment.getProperty("test.property", Boolean.class, false)).thenReturn(true);

        assertTrue(condition.matches(context, metadata));
    }

    @Test
    public void testDoesNotMatchWhenPropertyIsFalse() {
        final TestConditionOnPropertyTrue condition = new TestConditionOnPropertyTrue("test.property", false);
        when(environment.getProperty("test.property", Boolean.class, false)).thenReturn(false);

        assertFalse(condition.matches(context, metadata));
    }

    @Test
    public void testMatchesWithDefaultValueTrue() {
        final TestConditionOnPropertyTrue condition = new TestConditionOnPropertyTrue("test.property", true);
        // Property not set, so default will be used
        when(environment.getProperty("test.property", Boolean.class, true)).thenReturn(true);

        assertTrue(condition.matches(context, metadata));
    }

    @Test
    public void testDoesNotMatchWithDefaultValueFalse() {
        final TestConditionOnPropertyTrue condition = new TestConditionOnPropertyTrue("test.property", false);
        // Property not set, so default will be used
        when(environment.getProperty("test.property", Boolean.class, false)).thenReturn(false);

        assertFalse(condition.matches(context, metadata));
    }

    @Test
    public void testConfigurationPhase() {
        final TestConditionOnPropertyTrue condition = new TestConditionOnPropertyTrue();
        assertEquals(ConfigurationCondition.ConfigurationPhase.REGISTER_BEAN, condition.getConfigurationPhase());
    }
}