/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.exception.TransactionRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TransactionRuntimeExceptionMapperTest
 *
 * @author dan.field@lyrasis.org
 */
public class TransactionRuntimeExceptionMapperTest {

    private FedoraPropsConfig config;

    private TransactionRuntimeExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new TransactionRuntimeExceptionMapper();
        config = new FedoraPropsConfig();
    }

    @Test
    public void testToResponse() {
        final TransactionRuntimeException input = new TransactionRuntimeException("Exception Message");
        final Response actual = testObj.toResponse(input);
        assertEquals(CONFLICT.getStatusCode(), actual.getStatus());
    }

}
