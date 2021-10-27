/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.slf4j.Logger;

/**
 * @author barmintor
 * @since 6/28/16
 */
public interface ExceptionDebugLogging {
    /**
     * Log a Throwable at the DEBUG level, log the stacktrace at the TRACE level.
     *
     * @param context ExceptionDebugLogging the exception intercepting context.
     * @param error Throwable the intercepted error.
     * @param logger Logger the logger to use
     */
    default void debugException(final ExceptionDebugLogging context, final Throwable error, final Logger logger) {
        /*
         * Because the majority case is not debug logging, trade one additional
         * accessor call in a guard clause for multiple in message construction
         * and internal log level checks
         */
        if (!logger.isDebugEnabled()) {
            return;
        }
        logger.debug("{} intercepted exception:{} \n", context.getClass()
                .getSimpleName(), error);
        logger.trace(error.getMessage(), error);
    }
}
