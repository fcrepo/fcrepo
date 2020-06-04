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
 * Thrown in circumstances where a client has used an unknown or unsupported hash algorithm
 * in a request, e.g. with `Digest` or `Want-Digest`.
 *
 * @author harring
 * @since 2017-09-12
 */
public class UnsupportedAlgorithmException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Exception with message
     * @param message the message
     */
    public UnsupportedAlgorithmException(final String message) {
        super(message);
    }

    /**
     * Ordinary constructor.
     *
     * @param message the message
     * @param rootCause the root cause
     */
    public UnsupportedAlgorithmException(final String message, final Throwable rootCause) {
        super(message, rootCause);
    }
}
