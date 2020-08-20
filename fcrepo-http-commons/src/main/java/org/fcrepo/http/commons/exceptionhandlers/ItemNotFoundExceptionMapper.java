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

import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.slf4j.Logger;

/**
 * Catch ItemNotFoundException
 *
 * @author pwinckles
 */
@Provider
public class ItemNotFoundExceptionMapper implements
        ExceptionMapper<ItemNotFoundException>, ExceptionDebugLogging {

    private static final Logger LOGGER =
        getLogger(ItemNotFoundExceptionMapper.class);

    @Override
    public Response toResponse(final ItemNotFoundException e) {

        LOGGER.debug("Exception intercepted by ItemNotFoundExceptionMapper: {}\n", e.getMessage());
        debugException(this, e, LOGGER);
        String tmp = e.getMessage();
        LOGGER.debug("tmp is {}\n", tmp);
        final int idx = tmp.lastIndexOf("/");
        if (idx != -1) {
            tmp = tmp.substring(idx);
        }
        if (tmp.contains("fcr:tombstone")) {
            return Response.status(Response.Status.NOT_FOUND).
                            entity("Discovered tombstone resource at " + tmp).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).
                            entity("Resource at " + tmp + " not found").build();
        }
    }
}

