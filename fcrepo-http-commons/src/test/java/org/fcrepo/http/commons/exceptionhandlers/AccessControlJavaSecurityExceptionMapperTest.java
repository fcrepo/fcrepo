/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.AccessControlException;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>AccessControlJavaSecurityExceptionMapperTest class.</p>
 *
 * @author lsitu
 * @author awoods
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
