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
 * Test for {@link ConditionOnPropertyFalse}
 *
 * @author bbpennel
 */
public class ConditionOnPropertyFalseTest {

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
     * Concrete implementation of ConditionOnPropertyFalse for testing
     */
    private static class TestConditionOnPropertyFalse extends ConditionOnPropertyFalse {
        public TestConditionOnPropertyFalse(final String name, final boolean defaultValue) {
            super(name, defaultValue);
        }

        // No-arg constructor required by the interface contract
        public TestConditionOnPropertyFalse() {
            this("test.property", true);
        }
    }

    @Test
    public void testMatchesWhenPropertyIsFalse() {
        final TestConditionOnPropertyFalse condition = new TestConditionOnPropertyFalse("test.property", true);
        when(environment.getProperty("test.property", Boolean.class, true)).thenReturn(false);

        assertTrue(condition.matches(context, metadata));
    }

    @Test
    public void testDoesNotMatchWhenPropertyIsTrue() {
        final TestConditionOnPropertyFalse condition = new TestConditionOnPropertyFalse("test.property", true);
        when(environment.getProperty("test.property", Boolean.class, true)).thenReturn(true);

        assertFalse(condition.matches(context, metadata));
    }

    @Test
    public void testMatchesWithDefaultValueFalse() {
        final TestConditionOnPropertyFalse condition = new TestConditionOnPropertyFalse("test.property", false);
        // Property not set, so default will be used
        when(environment.getProperty("test.property", Boolean.class, false)).thenReturn(false);

        assertTrue(condition.matches(context, metadata));
    }

    @Test
    public void testDoesNotMatchWithDefaultValueTrue() {
        final TestConditionOnPropertyFalse condition = new TestConditionOnPropertyFalse("test.property", true);
        // Property not set, so default will be used
        when(environment.getProperty("test.property", Boolean.class, true)).thenReturn(true);

        assertFalse(condition.matches(context, metadata));
    }

    @Test
    public void testConfigurationPhase() {
        final TestConditionOnPropertyFalse condition = new TestConditionOnPropertyFalse();
        assertEquals(ConfigurationCondition.ConfigurationPhase.REGISTER_BEAN, condition.getConfigurationPhase());
    }
}