/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.webapp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.fcrepo.kernel.api.exception.MultipleConstraintViolationException;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.RelaxableServerManagedPropertyException;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.io.File;
import java.util.Set;

/**
 * @author awoods
 * @since 6/25/15
 */
public class ConstraintExceptionsTest {

    @Test
    public void testConstraintRdfExists() {
        final var subTypes = getConstraintViolationSubTypes();

        // Multiple is a wrapper to hold other constraint violations, it has no static file.
        subTypes.remove(MultipleConstraintViolationException.class);
        // Relaxable is a sub-type of ServerManagedPropertyException to specially handle relaxable properties, it has
        // no static file.
        subTypes.remove(RelaxableServerManagedPropertyException.class);
        subTypes.add(ConstraintViolationException.class);

        assertTrue(subTypes.size() > 1, "Must be more than one subtype of ConstraintViolationException");

        for (final Class<? extends ConstraintViolationException> c : subTypes) {
            final File file = new File("src/main/webapp/static/constraints/" + c.getSimpleName() + ".rdf");
            assertTrue(file.exists(), "Expected to find: " + file.getPath());
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Class<? extends ConstraintViolationException>> getConstraintViolationSubTypes() {
        final var provider =
                new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(ConstraintViolationException.class));

        final var components = provider.findCandidateComponents("org.fcrepo");
        final Set<Class<? extends ConstraintViolationException>> subTypes = new HashSet<>();
        for (final BeanDefinition component : components) {
            try {
                final var clazz = Class.forName(component.getBeanClassName());
                if (ConstraintViolationException.class.isAssignableFrom(clazz)) {
                    subTypes.add((Class<? extends ConstraintViolationException>) clazz);
                }
            } catch (ClassNotFoundException e) {
                // Skip if class can't be loaded
            }
        }
        return subTypes;
    }

}
