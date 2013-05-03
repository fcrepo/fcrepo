package org.fcrepo.exceptionhandlers;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.junit.Assert.assertEquals;

import javax.jcr.security.AccessControlException;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

public class AccessControlExceptionMapperTest {

    private AccessControlExceptionMapper testObj;
    
    @Before
    public void setUp() {
        testObj = new AccessControlExceptionMapper();
    }
    
    @Test
    public void testToResponse() {
        AccessControlException input = new AccessControlException();
        Response actual = testObj.toResponse(input);
        assertEquals(FORBIDDEN.getStatusCode(), actual.getStatus());
    }
}
