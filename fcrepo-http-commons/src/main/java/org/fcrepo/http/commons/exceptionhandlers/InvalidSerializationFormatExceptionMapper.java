/**
 * Copyright 2014 DuraSpace, Inc.
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

import org.fcrepo.serialization.InvalidSerializationFormatException;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *  Translate InvalidSerializationFormatException errors into reasonable
 *  HTTP error codes
 *
 * @author md5wz
 * @since 11/17/14
 */
@Provider
public class InvalidSerializationFormatExceptionMapper implements
        ExceptionMapper<InvalidSerializationFormatException> {

    private static final Logger LOGGER =
        getLogger(InvalidSerializationFormatExceptionMapper.class);

    @Override
    public Response toResponse(final InvalidSerializationFormatException e) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("InvalidSerializationFormatException intercepted by InvalidSerializationFormatExceptionMapper"
                    + (e.getMessage() != null ? ": " + e.getMessage() : "."), e);
        } else {
            LOGGER.info("InvalidSerializationFormatException intercepted by InvalidSerializationFormatExceptionMapper"
                    + (e.getMessage() != null ? ": " + e.getMessage() : "."));
        }
        return status(BAD_REQUEST).entity(e.getMessage()).build();
    }
}
