/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.exception.ConcurrentUpdateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * ConcurrentUpdateExceptionMapperTest
 *
 * @author dan.field@lyrasis.org
 */
public class ConcurrentUpdateExceptionMapperTest {

    private FedoraPropsConfig config;

    private ConcurrentUpdateExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new ConcurrentUpdateExceptionMapper();
        config = new FedoraPropsConfig();
        setField(testObj, "config", config);
    }

    @Test
    public void testToResponse() {
        final ConcurrentUpdateException input = new ConcurrentUpdateException("Resource",
                                                                                "ConflictingTransaction",
                                                                                "Existing Transaction");
        final Response actual = testObj.toResponse(input);
        assertEquals(CONFLICT.getStatusCode(), actual.getStatus());
    }

}
