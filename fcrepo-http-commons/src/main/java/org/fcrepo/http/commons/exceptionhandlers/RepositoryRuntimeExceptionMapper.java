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

import static com.google.common.base.Throwables.getStackTraceAsString;
import static javax.ws.rs.core.Response.serverError;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

/**
 * @author cabeer
 * @since 9/13/14
 */

@Provider
public class RepositoryRuntimeExceptionMapper implements
        ExceptionMapper<RepositoryRuntimeException>, ExceptionDebugLogging {

    private final Providers providers;

    /**
     * Get the context Providers so we can rethrow the cause to an appropriate handler
     * 
     * @param providers the providers
     */
    public RepositoryRuntimeExceptionMapper(@Context final Providers providers) {
        this.providers = providers;
    }

    @Override
    public Response toResponse(final RepositoryRuntimeException e) {
        final Throwable cause = e.getCause();
        @SuppressWarnings("unchecked")
        final ExceptionMapper<Throwable> exceptionMapper =
                (ExceptionMapper<Throwable>) providers.getExceptionMapper(cause.getClass());
        if (exceptionMapper != null) {
            return exceptionMapper.toResponse(cause);
        }
        return serverError().entity(getStackTraceAsString(e)).build();
    }
}
