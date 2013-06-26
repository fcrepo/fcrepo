
package org.fcrepo.exceptionhandlers;

import org.fcrepo.exception.TransactionMissingException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class TransactionMissingExceptionMapper implements
        ExceptionMapper<TransactionMissingException> {

    @Override
    public Response toResponse(TransactionMissingException exception) {
        return Response.status(Response.Status.GONE).build();
    }
}
