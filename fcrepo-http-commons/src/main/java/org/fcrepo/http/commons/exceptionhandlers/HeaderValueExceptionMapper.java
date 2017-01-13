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

import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.message.internal.HeaderValueException;

/**
 * If a client-provided header value fails to parse, return an HTTP 400 Bad Request.
 *
 * @author awoods
 * @since 2015-08-06
 */
@Provider
public class HeaderValueExceptionMapper extends FedoraExceptionMapper<HeaderValueException> {

    @Override
    protected ResponseBuilder entity(final ResponseBuilder builder, final HeaderValueException e) {
        return builder.entity(e.getMessage() + " ...should value be quoted?");
    }
}