/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.common.metrics;

import io.micrometer.core.instrument.Timer;
import org.fcrepo.common.lang.UncheckedCallable;

/**
 * Helper for recording metrics
 *
 * @author pwinckles
 */
public final class MetricsHelper {

    private MetricsHelper() {
        // static class
    }

    /**
     * Records a timing metric around the code in the closure.
     *
     * @param timer the timer to record to
     * @param callable the closure to time
     * @param <T> the return type
     * @return the result of the closure
     */
    public static <T> T time(final Timer timer, final UncheckedCallable<T> callable) {
        final var stopwatch = Timer.start();
        try {
            return callable.call();
        } finally {
            stopwatch.stop(timer);
        }
    }

}
