/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
