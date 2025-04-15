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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


import javax.inject.Inject;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * ClientErrorExceptionMapperTest
 *
 * @author dan.field@lyrasis.org
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ConcurrentUpdateExceptionMapperTest {

    @Inject
    private FedoraPropsConfig config;

    private ConcurrentUpdateExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new ConcurrentUpdateExceptionMapper();
        config = new FedoraPropsConfig();
        setField(testObj, "config", config);
    }

    @Inject
    @Test
    public void testToResponse() {
        final ConcurrentUpdateException input = new ConcurrentUpdateException("Resource",
                                                                                "ConflictingTransaction",
                                                                                "Existing Transaction");
        final Response actual = testObj.toResponse(input);
        assertEquals(CONFLICT.getStatusCode(), actual.getStatus());
    }

}
