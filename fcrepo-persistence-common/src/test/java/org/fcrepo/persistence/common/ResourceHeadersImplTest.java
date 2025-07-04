/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ResourceHeadersImpl}
 *
 * @author whikloj
 */
public class ResourceHeadersImplTest {

    private ResourceHeadersImpl headers;

    @BeforeEach
    public void setUp() {
        headers = new ResourceHeadersImpl();
    }

    @Test
    public void testNewResourceHeaders() {
        assertNull(headers.getId());
        headers.setId(FedoraId.create("info:fedora/test-subject"));
        assertEquals(FedoraId.create("info:fedora/test-subject"), headers.getId());

        assertNull(headers.getParent());
        headers.setParent(FedoraId.create("info:fedora/test-parent"));
        assertEquals(FedoraId.create("info:fedora/test-parent"), headers.getParent());

        assertNull(headers.getHeadersVersion());
        headers.setHeadersVersion(ResourceHeaders.V1_0);
        assertEquals(ResourceHeaders.V1_0, headers.getHeadersVersion());

        assertNull(headers.getInteractionModel());
        headers.setInteractionModel("interactionModel");
        assertEquals("interactionModel", headers.getInteractionModel());

        assertNull(headers.getMimeType());
        headers.setMimeType("text/plain");
        assertEquals("text/plain", headers.getMimeType());

        assertNull(headers.getFilename());
        headers.setFilename("filename.txt");
        assertEquals("filename.txt", headers.getFilename());

        assertEquals(-1L, headers.getContentSize());
        headers.setContentSize(12345L);
        assertEquals(12345L, headers.getContentSize());

        assertNull(headers.getExternalUrl());
        headers.setExternalUrl("http://example.com");
        assertEquals("http://example.com", headers.getExternalUrl());

        assertNull(headers.getExternalHandling());
        headers.setExternalHandling("proxy");
        assertEquals("proxy", headers.getExternalHandling());

        final var now = Instant.now();
        assertNull(headers.getCreatedDate());
        headers.setCreatedDate(now);
        assertEquals(now, headers.getCreatedDate());

        assertNull(headers.getCreatedBy());
        headers.setCreatedBy("userPrincipal");
        assertEquals("userPrincipal", headers.getCreatedBy());

        assertNull(headers.getLastModifiedDate());
        headers.setLastModifiedDate(now);
        assertEquals(now, headers.getLastModifiedDate());

        assertNull(headers.getLastModifiedBy());
        headers.setLastModifiedBy("userPrincipal");
        assertEquals("userPrincipal", headers.getLastModifiedBy());

        assertNull(headers.getMementoCreatedDate());
        headers.setMementoCreatedDate(now);
        assertEquals(now, headers.getMementoCreatedDate());

        assertNull(headers.getArchivalGroupId());
        final var groupId = FedoraId.create("test-archival-group");
        headers.setArchivalGroupId(groupId);
        assertEquals(groupId, headers.getArchivalGroupId());


        assertNull(headers.getStateToken());
        headers.setStateToken("urn:sha1:12345");
        assertEquals("urn:sha1:12345", headers.getStateToken());

        assertFalse(headers.isArchivalGroup());
        headers.setArchivalGroup(true);
        assertTrue(headers.isArchivalGroup());

        assertTrue(headers.isObjectRoot());
        headers.setObjectRoot(false);
        // If isArchivalGroup is true, then isObjectRoot must be true
        assertTrue(headers.isObjectRoot());
        headers.setArchivalGroup(false);
        // Now isObjectRoot can be false
        assertFalse(headers.isObjectRoot());
        headers.setObjectRoot(true);
        assertTrue(headers.isObjectRoot());


        assertFalse(headers.isDeleted());
        headers.setDeleted(true);
        assertTrue(headers.isDeleted());

        assertNull(headers.getContentPath());
        headers.setContentPath("contentPath");
        assertEquals("contentPath", headers.getContentPath());

        assertNull(headers.getStorageRelativePath());
        headers.setStorageRelativePath("storageRelativePath");
        assertEquals("storageRelativePath", headers.getStorageRelativePath());
    }

}
