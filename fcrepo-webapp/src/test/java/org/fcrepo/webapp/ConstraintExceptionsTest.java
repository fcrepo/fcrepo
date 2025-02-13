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
import org.reflections.Reflections;

import java.io.File;
import java.util.Set;

/**
 * @author awoods
 * @since 6/25/15
 */
public class ConstraintExceptionsTest {

    @Test
    public void testConstraintRdfExists() {
        final Reflections reflections = new Reflections("org.fcrepo");
        final Set<Class<? extends ConstraintViolationException>> subTypes =
                reflections.getSubTypesOf(ConstraintViolationException.class);
        // Multiple is a wrapper to hold other constraint violations, it has no static file.
        subTypes.remove(MultipleConstraintViolationException.class);
        // Relaxable is a sub-type of ServerManagedPropertyException to specially handle relaxable properties, it has
        // no static file.
        subTypes.remove(RelaxableServerManagedPropertyException.class);
        subTypes.add(ConstraintViolationException.class);

        for (final Class<? extends ConstraintViolationException> c : subTypes) {
            final File file = new File("src/main/webapp/static/constraints/" + c.getSimpleName() + ".rdf");
            assertTrue(file.exists(), "Expected to find: " + file.getPath());
        }
    }

}
