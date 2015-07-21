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

import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The class translates {@link org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException}s to its proper
 * response code.
 *
 * @author awoods
 * @since July 14, 2015.
 */
@Provider
public class InvalidResourceIdentifierExceptionMapper implements ExceptionMapper<InvalidResourceIdentifierException> {

    private static final Logger LOGGER = getLogger(InvalidResourceIdentifierExceptionMapper.class);

    @Override
    public Response toResponse(final InvalidResourceIdentifierException e) {
        LOGGER.debug("InvalidResourceIdentifierExceptionMapper caught exception: {}", e.getMessage());
        return status(BAD_REQUEST).entity(e.getMessage()).build();
    }
}
