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
 * A constraint has been violated.
 *
 * @author whikloj
 * @since 2015-05-29
 */
public class ConstraintViolationException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     */
    public ConstraintViolationException(final String msg) {
        super(msg);
    }

    /**
     * Ordinary constructor.
     *
     * @param rootCause the root cause
     */
    public ConstraintViolationException(final Throwable rootCause) {
        super(rootCause);
    }

    /**
     * Ordinary constructor.
     *
     * @param msg the message
     * @param rootCause the root cause
     */
    public ConstraintViolationException(final String msg, final Throwable rootCause) {
        super(msg, rootCause);
    }

}
