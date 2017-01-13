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

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN;
import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;


import org.slf4j.Logger;

/**
 * @author dbernstein
 * @since Jan 13, 2017
 */
public abstract class FedoraExceptionMapper<T extends Throwable> implements
        ExceptionMapper<T>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(FedoraExceptionMapper.class);

    private static final String DEFAULT_CONTENT_TYPE = TEXT_PLAIN;

    private static final Response.Status DEFAULT_STATUS = BAD_REQUEST;

    private String contentType;

    private Response.Status status;

    /**
     * Default constructor
     */
    public FedoraExceptionMapper() {
        this(DEFAULT_STATUS, DEFAULT_CONTENT_TYPE);
    }

    /**
     * Alternate constructor to be used by subclasses that wish to override the default status only.
     * 
     * @param status http status
     */
    protected FedoraExceptionMapper(final Response.Status status) {
        this(status, DEFAULT_CONTENT_TYPE);
    }

    /**
     * Alternate constructor to be used by subclasses that wish to override the default status and contentType.
     * 
     * @param status http status
     * @param contentType the content type
     */
    protected FedoraExceptionMapper(final Response.Status status, final String contentType) {
        this.status = status;
        this.contentType = contentType;
    }

    @Override
    public Response toResponse(final T e) {
        logError(e);
        debugException(this, e, LOGGER);
        ResponseBuilder builder = status(e);
        builder = entity(builder, e);
        builder = contentType(builder, e);
        builder = links(builder,e);
        return builder.build();
    }

    /**
     * Sets the content on the builder. Subclasses can override this method in order to define custom logic for
     * resolving the appropriate contentType.
     * 
     * @param builder The response builder.
     * @param e The exception passed into the mapper.
     * @return response builder
     */
    protected ResponseBuilder contentType(final ResponseBuilder builder, final T e) {
        return builder.type(contentType);
    }

    /**
     * Sets the entity on the builder. Subclasses can override this method in order to define custom logic for
     * generating the appropriate entity.
     * 
     * @param builder The response builder.
     * @param e The exception passed into the mapper.
     * @return response builder
     */
    protected ResponseBuilder entity(final ResponseBuilder builder, final T e) {
        return builder.entity(e.getMessage());
    }

    /**
     * Set any links the entity on the builder. By default no links are set. Subclasses can override this method in
     * order to define custom logic for generating the appropriate links.
     * 
     * @param builder The response builder.
     * @param e The exception passed into the mapper.
     * @return response builder
     */
    protected ResponseBuilder links(final ResponseBuilder builder, final T e) {
        return builder;
    }

    /**
     * Logs an error with default formatting. Subclasses may override to provide custom error message formatting.
     * 
     * @param e The exception passed into the mapper.
     */
    protected void logError(final T e) {
        LOGGER.error("{} caught an exception: {}", getClass().getSimpleName(), e.getMessage());
    }

    /**
     * Creates a response builder with the status defined in the constructor (or the default value, ie BAD_REQUEST, if
     * the default constructor was used). Subclasses may override to specify a custom status.
     * 
     * @param e The exception passed into the mapper.
     * @return The response builder.
     */
    protected ResponseBuilder status(final T e) {
        return status(status);
    }

    /**
     * Creates a response builder with the specified status
     * 
     * @param status http status
     * @return The response builder.
     */
    protected ResponseBuilder status(final Response.Status status) {
        return status(status.getStatusCode());
    }

    /**
     * Creates a response builder with the specified http status code
     * 
     * @param statusCode http status code
     * @return response builder
     */
    protected ResponseBuilder status(final int statusCode) {
        return Response.status(statusCode);
    }

}
