/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

import org.slf4j.Logger;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static javax.ws.rs.core.Response.serverError;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/13/14
 */

@Provider
public class RepositoryRuntimeExceptionMapper implements
        ExceptionMapper<RepositoryRuntimeException> {

    private final Providers providers;

    /**
     * Get the context Providers so we can rethrow the cause to an appropriate handler
     * @param providers the providers
     */
    public RepositoryRuntimeExceptionMapper(@Context final Providers providers) {
        this.providers = providers;
    }

    private static final Logger LOGGER = getLogger(RepositoryExceptionMapper.class);

    @Override
    public Response toResponse(final RepositoryRuntimeException e) {
        final Throwable cause = e.getCause();
        @SuppressWarnings("unchecked")
        final ExceptionMapper<Throwable> exceptionMapper =
                (ExceptionMapper<Throwable>) providers.getExceptionMapper(cause.getClass());
        if (exceptionMapper != null) {
            return exceptionMapper.toResponse(cause);
        }
        LOGGER.warn("Caught repository exception: {}", e.getMessage());
        return serverError().entity(getStackTraceAsString(e)).build();
    }
}
