/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.persistence.api.exceptions.PersistentItemConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PathNotFoundRuntimeExceptionMapperTest
 *
 * @author dan.field@lyrasis.org
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PersistentItemConflictExceptionMapperTest {

    @Inject
    private FedoraPropsConfig config;

    private PersistentItemConflictExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new PersistentItemConflictExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final PersistentItemConflictException input = new PersistentItemConflictException("Exception");
        final Response actual = testObj.toResponse(input);
        assertEquals(CONFLICT.getStatusCode(), actual.getStatus());
    }

}
