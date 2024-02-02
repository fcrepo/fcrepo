/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.exceptionhandlers;

import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static org.junit.Assert.assertEquals;

import java.security.AccessControlException;

import jakarta.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

/**
 * <p>AccessControlJavaSecurityExceptionMapperTest class.</p>
 *
 * @author lsitu
 * @author awoods
 */
public class AccessControlJavaSecurityExceptionMapperTest {

    private AccessControlJavaSecurityExceptionMapper testObj;

    @Before
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
