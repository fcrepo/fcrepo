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
package org.fcrepo.kernel.api.exception;

/**
 * @author cabeer
 * @since 9/15/14
 */
public class PathNotFoundRuntimeException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Wrap a PathNotFoundException in a runtime exception
     * @param rootCause the root cause
     */
    public PathNotFoundRuntimeException(final Throwable rootCause) {
        super(rootCause);
    }

    /**
     * Wrap a PathNotFoundException in a runtime exception
     * @param message the original message.
     * @param rootCause the root cause.
     */
    public PathNotFoundRuntimeException(final String message, final Throwable rootCause) {
        super(message, rootCause);
    }
}
