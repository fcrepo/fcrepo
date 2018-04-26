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

import static org.apache.http.HttpStatus.SC_PRECONDITION_FAILED;
import static org.apache.http.HttpStatus.SC_NOT_MODIFIED;

/**
 * @author dbernstein
 * @since Jun 22, 2017
 */
public class PreconditionException extends RepositoryRuntimeException {

    private static final long serialVersionUID = 1L;

    private int httpStatus;

    /**
     * Ordinary constructor
     *
     * @param msg error message
     * @param httpStatus the http status code
     */
    public PreconditionException(final String msg, final int httpStatus) {
        super(msg);
        if (httpStatus != SC_PRECONDITION_FAILED && httpStatus != SC_NOT_MODIFIED) {
            throw new IllegalArgumentException("Invalid httpStatus (" + httpStatus +
                    "). The http status for PreconditionExceptions must be " +
                    SC_PRECONDITION_FAILED + " or " + SC_NOT_MODIFIED);
        }
        this.httpStatus = httpStatus;
    }

    /**
     * @return the httpStatus
     */
    public int getHttpStatus() {
        return httpStatus;
    }
}
