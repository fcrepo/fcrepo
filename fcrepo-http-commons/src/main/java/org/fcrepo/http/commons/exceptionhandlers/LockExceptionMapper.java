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

import org.slf4j.Logger;

import javax.jcr.lock.LockException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.regex.Pattern;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Mike Durbin
 */
@Provider
public class LockExceptionMapper implements ExceptionMapper<LockException> {

    private static final Logger LOGGER = getLogger(LockExceptionMapper.class);

    @Override
    public Response toResponse(LockException exception) {
        LOGGER.debug("LockExceptionMapper intercepted exception: \n", exception);
        if (exception.getMessage() != null) {
            if (Pattern.matches("^\\QThe lock token '\\E.*'\\Q is not valid\\E$", exception.getMessage())) {
                return status(BAD_REQUEST).entity(exception.getMessage()).build();
            }
        }
        return status(CONFLICT).entity(exception.getMessage()).build();
    }
}
