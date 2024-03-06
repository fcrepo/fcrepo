/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.webac;

import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Test;

/**
 * @author peichman
 */
public class WebACPermissionTest {

    private static final URI resourceA = URI.create("http://localhost:8080/fcrepo/test");

    private static final URI resourceB = URI.create("http://localhost:8080/fcrepo/test2");

    @Test
    public void testEquality() {
        final WebACPermission p1 = new WebACPermission(WEBAC_MODE_READ, resourceA);
        final WebACPermission p2 = new WebACPermission(WEBAC_MODE_READ, resourceA);
        assertTrue(p1.implies(p2));
        assertTrue(p2.implies(p1));
    }

    @Test
    public void testInequalityOfResources() {
        final WebACPermission p1 = new WebACPermission(WEBAC_MODE_READ, resourceA);
        final WebACPermission p2 = new WebACPermission(WEBAC_MODE_READ, resourceB);
        assertFalse(p1.implies(p2));
        assertFalse(p2.implies(p1));
    }

    @Test
    public void testInequalityOfModes() {
        final WebACPermission p1 = new WebACPermission(WEBAC_MODE_READ, resourceA);
        final WebACPermission p2 = new WebACPermission(WEBAC_MODE_WRITE, resourceA);
        assertFalse(p1.implies(p2));
        assertFalse(p2.implies(p1));
    }

}
