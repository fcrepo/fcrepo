/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import java.security.AccessControlException;

/**
 * <p>AccessControlJavaSecurityExceptionMapperTest class.</p>
 *
 * @author lsitu
 * @author awoods
 * @deprecated AccessControlException is deprecated, so all use of it should be removed.
 */
public class AccessControlJavaSecurityExceptionMapperTest {

    private AccessControlJavaSecurityExceptionMapper testObj;

    @BeforeEach
    public void setUp() {
        testObj = new AccessControlJavaSecurityExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final AccessControlException input = new AccessControlException("");
        final Response actual = testObj.toResponse(input);
        assertEquals(FORBIDDEN.getStatusCode(), actual.getStatus());
    }
}
