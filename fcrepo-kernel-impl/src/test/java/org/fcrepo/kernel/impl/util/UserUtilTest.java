/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.fcrepo.kernel.impl.util.UserUtil.DEFAULT_USER_AGENT_BASE_URI;

import java.net.URI;
import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for UserUtil
 *
 * @author bbpennel
 */
public class UserUtilTest {

    private static final String CUSTOM_BASE_URI = "http://example.org/users/";

    @Test
    public void testGetUserURI_AbsoluteURI() {
        final String userURI = "http://example.org/users/testUser";
        final URI result = UserUtil.getUserURI(userURI, CUSTOM_BASE_URI);

        assertEquals(URI.create(userURI), result);
    }

    @Test
    public void testGetUserURI_OpaqueURI() {
        final String userURI = "mailto:user@example.org";
        final URI result = UserUtil.getUserURI(userURI, CUSTOM_BASE_URI);

        assertEquals(URI.create(userURI), result);
    }

    @Test
    public void testGetUserURI_RelativeURI() {
        final String userId = "testUser";
        final URI result = UserUtil.getUserURI(userId, CUSTOM_BASE_URI);

        assertEquals(URI.create(CUSTOM_BASE_URI + userId), result);
    }

    @Test
    public void testGetUserURI_WithAngledBrackets() {
        final String userId = "<testUser>";
        final URI result = UserUtil.getUserURI(userId, CUSTOM_BASE_URI);

        assertEquals(URI.create(CUSTOM_BASE_URI + "testUser"), result);
    }

    @Test
    public void testGetUserURI_WithSpaces() {
        final String userId = "test user with spaces";
        final String encodedUserId = URLEncoder.encode(userId, UTF_8);
        final URI result = UserUtil.getUserURI(userId, CUSTOM_BASE_URI);

        assertEquals(URI.create(CUSTOM_BASE_URI + encodedUserId), result);
    }

    @Test
    public void testGetUserURI_NullUserId() {
        final URI result = UserUtil.getUserURI(null, CUSTOM_BASE_URI);

        assertEquals(URI.create(CUSTOM_BASE_URI + "anonymous"), result);
    }

    @Test
    public void testGetUserURI_NullBaseURI() {
        final String userId = "testUser";
        final URI result = UserUtil.getUserURI(userId, null);

        assertEquals(URI.create(DEFAULT_USER_AGENT_BASE_URI + userId), result);
    }

    @Test
    public void testGetUserURI_EmptyBaseURI() {
        final String userId = "testUser";
        final URI result = UserUtil.getUserURI(userId, "");

        assertEquals(URI.create(DEFAULT_USER_AGENT_BASE_URI + userId), result);
    }

    @Test
    public void testGetUserURI_IdWithProtocolButNotURI() {
        final String userId = "http://example with space";
        final String encodedUserId = URLEncoder.encode(userId, UTF_8);
        final URI result = UserUtil.getUserURI(userId, CUSTOM_BASE_URI);

        assertEquals(URI.create(CUSTOM_BASE_URI + encodedUserId), result);
    }

    @Test
    public void testGetUserURI_ComplexAbsoluteURI() {
        final String userURI = "http://example.org/users?name=test&role=admin#section";
        final URI result = UserUtil.getUserURI(userURI, CUSTOM_BASE_URI);

        assertEquals(URI.create(userURI), result);
    }
}