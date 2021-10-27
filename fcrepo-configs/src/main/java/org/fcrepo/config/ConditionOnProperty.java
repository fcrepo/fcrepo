/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * This condition enables a bean/configuration when the specified property matches the expected value
 *
 * Implementations must provide a no-arg constructor.
 *
 * @author pwinckles
 */
public abstract class ConditionOnProperty<T> implements ConfigurationCondition {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionOnProperty.class);

    private final String name;
    private final T expected;
    private final T defaultValue;
    private final Class<T> clazz;

    public ConditionOnProperty(final String name, final T expected, final T defaultValue, final Class<T> clazz) {
        this.name = name;
        this.expected = expected;
        this.defaultValue = defaultValue;
        this.clazz = clazz;
    }

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
        LOGGER.debug("Prop {}: {}", name, context.getEnvironment().getProperty(name));
        return Objects.equals(expected, context.getEnvironment().getProperty(name, clazz, defaultValue));
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        // This forces spring to not evaluate these conditions until after it has loaded other @Configuration classes,
        // ensuring that the properties have been loaded.
        return ConfigurationPhase.REGISTER_BEAN;
    }
}
