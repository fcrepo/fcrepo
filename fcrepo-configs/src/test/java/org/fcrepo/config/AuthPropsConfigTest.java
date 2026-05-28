/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @author bbpennel
 */
public class AuthPropsConfigTest {

    private AuthPropsConfig config;

    @BeforeEach
    public void setUp() {
        config = new AuthPropsConfig();
        // Set default values that would normally be set by Spring's @Value annotations
        ReflectionTestUtils.setField(config, "rootAuthAclPath", null);
        ReflectionTestUtils.setField(config, "userAgentBaseUri", null);
        ReflectionTestUtils.setField(config, "groupAgentBaseUri", null);
        ReflectionTestUtils.setField(config, "authPrincipalDelegateEnabled", true);
        ReflectionTestUtils.setField(config, "authPrincipalHeaderEnabled", false);
        ReflectionTestUtils.setField(config, "authPrincipalHeaderName", "some-header");
        ReflectionTestUtils.setField(config, "authPrincipalHeaderSeparator", ",");
        ReflectionTestUtils.setField(config, "authPrincipalRolesEnabled", false);
        ReflectionTestUtils.setField(config, "authPrincipalRolesList", Arrays.asList("role-1", "role-2"));
    }

    @Test
    public void testDefaultValues() {
        assertNull(config.getRootAuthAclPath());
        assertNull(config.getUserAgentBaseUri());
        assertNull(config.getGroupAgentBaseUri());
        assertTrue(config.isAuthPrincipalDelegateEnabled());
        assertFalse(config.isAuthPrincipalHeaderEnabled());
        assertEquals("some-header", config.getAuthPrincipalHeaderName());
        assertEquals(",", config.getAuthPrincipalHeaderSeparator());
        assertFalse(config.isAuthPrincipalRolesEnabled());
        assertEquals(Arrays.asList("role-1", "role-2"), config.getAuthPrincipalRolesList());
    }

    @Test
    public void testSetRootAuthAclPath() {
        final Path testPath = Paths.get("/test/path.acl");
        config.setRootAuthAclPath(testPath);
        assertEquals(testPath, config.getRootAuthAclPath());
    }

    @Test
    public void testStaticConstants() {
        // Verify static constant values
        assertEquals("fcrepo.auth.enabled", AuthPropsConfig.FCREPO_AUTH_ENABLED);
        assertEquals("fcrepo.auth.principal.header.enabled", AuthPropsConfig.FCREPO_AUTH_PRINCIPAL_HEADER_ENABLED);
        assertEquals("fcrepo.auth.principal.roles.enabled", AuthPropsConfig.FCREPO_AUTH_PRINCIPAL_ROLES_ENABLED);
        assertEquals("fcrepo.auth.principal.delegate.enabled", AuthPropsConfig.FCREPO_AUTH_PRINCIPAL_DELEGATE_ENABLED);
    }
}