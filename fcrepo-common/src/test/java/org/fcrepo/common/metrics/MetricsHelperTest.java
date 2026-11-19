/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.common.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

/**
 * @author Dan Field
 */
public class MetricsHelperTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    public void testTimesAndReturnsResult() {
        final var timer = registry.timer("test.success");

        assertEquals("result", MetricsHelper.time(timer, () -> "result"));
        assertEquals(1, timer.count());
    }

    @Test
    public void testTimesWhenClosureThrows() {
        final var timer = registry.timer("test.failure");
        final var failure = new IllegalStateException("boom");

        final var thrown = assertThrows(IllegalStateException.class, () -> MetricsHelper.time(timer, () -> {
            throw failure;
        }));

        assertSame(failure, thrown);
        assertEquals(1, timer.count());
    }
}
