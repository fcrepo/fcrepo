/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test for {@link DigestAlgorithm}
 *
 * @author bbpennel
 */
public class DigestAlgorithmTest {

    @Test
    public void testGetScheme() {
        // Test standard algorithm names
        assertEquals("urn:sha1", DigestAlgorithm.getScheme("SHA"));
        assertEquals("urn:sha-256", DigestAlgorithm.getScheme("SHA-256"));
        assertEquals("urn:sha-512", DigestAlgorithm.getScheme("SHA-512"));
        assertEquals("urn:sha-512/256", DigestAlgorithm.getScheme("SHA-512/256"));
        assertEquals("urn:md5", DigestAlgorithm.getScheme("MD5"));

        // Test case insensitivity
        assertEquals("urn:sha1", DigestAlgorithm.getScheme("sha"));
        assertEquals("urn:sha-256", DigestAlgorithm.getScheme("sha-256"));

        // Test without dashes
        assertEquals("urn:sha-256", DigestAlgorithm.getScheme("SHA256"));
        assertEquals("urn:sha-512", DigestAlgorithm.getScheme("SHA512"));

        // Test unsupported algorithm
        assertEquals("missing", DigestAlgorithm.getScheme("UNKNOWN"));
    }

    @Test
    public void testFromScheme() {
        // Test standard schemes
        assertEquals(DigestAlgorithm.SHA1, DigestAlgorithm.fromScheme("urn:sha1"));
        assertEquals(DigestAlgorithm.SHA256, DigestAlgorithm.fromScheme("urn:sha-256"));
        assertEquals(DigestAlgorithm.SHA512, DigestAlgorithm.fromScheme("urn:sha-512"));
        assertEquals(DigestAlgorithm.SHA512256, DigestAlgorithm.fromScheme("urn:sha-512/256"));
        assertEquals(DigestAlgorithm.MD5, DigestAlgorithm.fromScheme("urn:md5"));

        // Test case insensitivity
        assertEquals(DigestAlgorithm.SHA1, DigestAlgorithm.fromScheme("URN:SHA1"));
        assertEquals(DigestAlgorithm.SHA256, DigestAlgorithm.fromScheme("URN:SHA-256"));

        // Test unsupported scheme
        assertEquals(DigestAlgorithm.MISSING, DigestAlgorithm.fromScheme("urn:unknown"));
    }

    @Test
    public void testFromAlgorithm() {
        // Test standard algorithm names
        assertEquals(DigestAlgorithm.SHA1, DigestAlgorithm.fromAlgorithm("SHA"));
        assertEquals(DigestAlgorithm.SHA256, DigestAlgorithm.fromAlgorithm("SHA-256"));
        assertEquals(DigestAlgorithm.SHA512, DigestAlgorithm.fromAlgorithm("SHA-512"));
        assertEquals(DigestAlgorithm.SHA512256, DigestAlgorithm.fromAlgorithm("SHA-512/256"));
        assertEquals(DigestAlgorithm.MD5, DigestAlgorithm.fromAlgorithm("MD5"));

        // Test aliases
        assertEquals(DigestAlgorithm.SHA1, DigestAlgorithm.fromAlgorithm("sha1"));
        assertEquals(DigestAlgorithm.SHA1, DigestAlgorithm.fromAlgorithm("sha-1"));
        assertEquals(DigestAlgorithm.SHA256, DigestAlgorithm.fromAlgorithm("sha256"));
        assertEquals(DigestAlgorithm.SHA512, DigestAlgorithm.fromAlgorithm("sha512"));
        assertEquals(DigestAlgorithm.SHA512256, DigestAlgorithm.fromAlgorithm("sha512/256"));

        // Test case insensitivity
        assertEquals(DigestAlgorithm.SHA1, DigestAlgorithm.fromAlgorithm("sha"));
        assertEquals(DigestAlgorithm.SHA256, DigestAlgorithm.fromAlgorithm("Sha-256"));

        // Test unsupported algorithm
        assertEquals(DigestAlgorithm.MISSING, DigestAlgorithm.fromAlgorithm("UNKNOWN"));
    }

    @Test
    public void testIsSupportedAlgorithm() {
        // Test supported algorithms
        assertTrue(DigestAlgorithm.isSupportedAlgorithm("SHA"));
        assertTrue(DigestAlgorithm.isSupportedAlgorithm("SHA-256"));
        assertTrue(DigestAlgorithm.isSupportedAlgorithm("SHA-512"));
        assertTrue(DigestAlgorithm.isSupportedAlgorithm("SHA-512/256"));
        assertTrue(DigestAlgorithm.isSupportedAlgorithm("MD5"));

        // Test case insensitivity
        assertTrue(DigestAlgorithm.isSupportedAlgorithm("sha"));
        assertTrue(DigestAlgorithm.isSupportedAlgorithm("sha-256"));

        // Test without dashes
        assertTrue(DigestAlgorithm.isSupportedAlgorithm("SHA256"));
        assertTrue(DigestAlgorithm.isSupportedAlgorithm("SHA512"));

        // Test unsupported algorithm
        assertFalse(DigestAlgorithm.isSupportedAlgorithm("UNKNOWN"));
    }

    @Test
    public void testGetAliases() {
        // SHA1 should have aliases: sha, sha1, sha-1
        final var sha1Aliases = DigestAlgorithm.SHA1.getAliases();
        assertTrue(sha1Aliases.contains("sha"));
        assertTrue(sha1Aliases.contains("sha1"));
        assertTrue(sha1Aliases.contains("sha-1"));

        // SHA256 should have aliases: sha-256, sha256
        final var sha256Aliases = DigestAlgorithm.SHA256.getAliases();
        assertTrue(sha256Aliases.contains("sha-256"));
        assertTrue(sha256Aliases.contains("sha256"));

        // SHA512 should have aliases: sha-512, sha512
        final var sha512Aliases = DigestAlgorithm.SHA512.getAliases();
        assertTrue(sha512Aliases.contains("sha-512"));
        assertTrue(sha512Aliases.contains("sha512"));

        // SHA512256 should have aliases: sha-512/256, sha512/256
        final var sha512256Aliases = DigestAlgorithm.SHA512256.getAliases();
        assertTrue(sha512256Aliases.contains("sha-512/256"));
        assertTrue(sha512256Aliases.contains("sha512/256"));

        // MD5 should have alias: md5
        final var md5Aliases = DigestAlgorithm.MD5.getAliases();
        assertTrue(md5Aliases.contains("md5"));
    }

    @Test
    public void testGetAlgorithm() {
        assertEquals("SHA", DigestAlgorithm.SHA1.getAlgorithm());
        assertEquals("SHA-256", DigestAlgorithm.SHA256.getAlgorithm());
        assertEquals("SHA-512", DigestAlgorithm.SHA512.getAlgorithm());
        assertEquals("SHA-512/256", DigestAlgorithm.SHA512256.getAlgorithm());
        assertEquals("MD5", DigestAlgorithm.MD5.getAlgorithm());
        assertEquals("NONE", DigestAlgorithm.MISSING.getAlgorithm());
    }
}