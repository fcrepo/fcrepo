/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.http.commons.domain.RDFMediaType;
import org.fcrepo.http.commons.responses.ConcurrentExceptionResponse;
import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.status;
import static org.fcrepo.http.commons.session.TransactionConstants.TX_PREFIX;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author pwinckles
 */
@Provider
public class ConcurrentUpdateExceptionMapper implements
        ExceptionMapper<ConcurrentUpdateException>, ExceptionDebugLogging {

    private static final Logger LOGGER = getLogger(ConcurrentUpdateExceptionMapper.class);

    @Inject
    private FedoraPropsConfig config;

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(final ConcurrentUpdateException e) {
        debugException(this, e, LOGGER);
        final var response = new ConcurrentExceptionResponse(e.getResponseMessage());

        // create external links for the transaction ids
        if (config.includeTransactionOnConflict()) {
            final var identifierConverter = new HttpIdentifierConverter(uriInfo.getBaseUriBuilder()
                                                                               .clone().path("/{path: .*}"));
            final var existingId = FEDORA_ID_PREFIX + "/" + TX_PREFIX + e.getExistingTransactionId();
            final var conflictingId = FEDORA_ID_PREFIX + "/" + TX_PREFIX + e.getConflictingTransactionId();
            response.setExistingTransactionId(identifierConverter.toExternalId(existingId));
            response.setConflictingTransactionId(identifierConverter.toExternalId(conflictingId));
        }

        return status(Response.Status.CONFLICT)
            .entity(response)
            .type(RDFMediaType.APPLICATION_JSON_TYPE).build();
    }

}