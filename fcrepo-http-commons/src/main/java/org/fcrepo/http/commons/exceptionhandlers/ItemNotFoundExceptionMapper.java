/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static org.slf4j.LoggerFactory.getLogger;


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
        debugException(this, e, LOGGER);
        return Response.status(Response.Status.NOT_FOUND).
                entity("Error: " + e.getMessage()).build();
    }
}

