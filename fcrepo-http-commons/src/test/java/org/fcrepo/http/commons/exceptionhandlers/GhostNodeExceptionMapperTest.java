/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.kernel.api.exception.GhostNodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GhostNodeExceptionMapperTest
 *
 * @author dan.field@lyrasis.org
 */
public class GhostNodeExceptionMapperTest {

    @Inject
    private FedoraPropsConfig config;

    private GhostNodeExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new GhostNodeExceptionMapper();
        config = new FedoraPropsConfig();
    }

    @Test
    public void testToResponse() {
        final GhostNodeException input = new GhostNodeException("Exception Message");
        final Response actual = testObj.toResponse(input);
        assertEquals(CONFLICT.getStatusCode(), actual.getStatus());
    }

}
