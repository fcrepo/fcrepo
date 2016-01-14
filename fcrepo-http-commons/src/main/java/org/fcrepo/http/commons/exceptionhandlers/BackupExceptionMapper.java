/*
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

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.fcrepo.kernel.api.exception.BackupException;
import org.slf4j.Logger;

/**
 * For backup exception on backup action does not perform on the root of the repository.
 *
 * @author yinlinchen
 */
@Provider
public class BackupExceptionMapper implements ExceptionMapper<BackupException> {

    private static final Logger LOGGER = getLogger(BackupExceptionMapper.class);

    @Override
    public Response toResponse(final BackupException e) {
        LOGGER.error("BackupExceptionMapper caught an exception: {}", e.getMessage());
        return status(BAD_REQUEST).entity(e.getMessage()).build();
    }

}
