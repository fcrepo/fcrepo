/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.integration;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.AbstractHttpMessage;
import org.junit.Test;

/**
 * @author peichman
 */
public class ServletContainerAuthenticatingRealmIT extends AbstractResourceIT {

    /**
     * Convenience method for applying HTTP Basic auth credentials to a request
     *
     * @param method the request to add the credentials to
     * @param username the username to add
     */
    private static void setAuth(final AbstractHttpMessage method, final String username) {
        final String creds = username + ":password";
        final String encCreds = new String(Base64.encodeBase64(creds.getBytes()));
        final String basic = "Basic " + encCreds;
        method.setHeader("Authorization", basic);
    }

   @Test
    public void testUserWithoutRoles() throws IOException {
        // make sure this doesn't cause Shiro to explode
        final HttpGet request = new HttpGet(serverAddress);
        setAuth(request, "noroles");
        assertEquals(200, getStatus(request));
    }

}
