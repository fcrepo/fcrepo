/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author pwinckles
 */
public class ResourceHeadersAdapterTest {

    @Test
    public void roundTrip() {
        final ResourceHeadersImpl kernelHeaders = new ResourceHeadersImpl();
        kernelHeaders.setDigests(List.of(URI.create("urn:sha-512:blah")));
        kernelHeaders.setStateToken("state");
        kernelHeaders.setParent(FedoraId.getRepositoryRootId());
        kernelHeaders.setId(FedoraId.create("info:fedora/blah"));
        kernelHeaders.setMimeType("text/plain");
        kernelHeaders.setMementoCreatedDate(Instant.now());
        kernelHeaders.setLastModifiedDate(Instant.now());
        kernelHeaders.setLastModifiedBy("modifiedBy");
        kernelHeaders.setInteractionModel(BASIC_CONTAINER.toString());
        kernelHeaders.setObjectRoot(true);
        kernelHeaders.setFilename("filename");
        kernelHeaders.setExternalUrl("externalUrl");
        kernelHeaders.setExternalHandling("externalHandling");
        kernelHeaders.setDeleted(true);
        kernelHeaders.setCreatedDate(Instant.now());
        kernelHeaders.setCreatedBy("createdBy");
        kernelHeaders.setContentSize(100L);
        kernelHeaders.setContentPath("contentPath");
        kernelHeaders.setArchivalGroup(true);
        kernelHeaders.setArchivalGroupId(FedoraId.create("info:fedora/ag"));
        kernelHeaders.setHeadersVersion("1.0");

        final ResourceHeaders storageHeaders = new ResourceHeadersAdapter(kernelHeaders).asStorageHeaders();

        final ResourceHeadersImpl roundTrip = new ResourceHeadersAdapter(storageHeaders).asKernelHeaders();

        assertEquals(kernelHeaders.getDigests(), roundTrip.getDigests());
        assertEquals(kernelHeaders.getStateToken(), roundTrip.getStateToken());
        assertEquals(kernelHeaders.getParent(), roundTrip.getParent());
        assertEquals(kernelHeaders.getId(), roundTrip.getId());
        assertEquals(kernelHeaders.getMimeType(), roundTrip.getMimeType());
        assertEquals(kernelHeaders.getLastModifiedBy(), roundTrip.getLastModifiedBy());
        assertEquals(kernelHeaders.getLastModifiedDate(), roundTrip.getLastModifiedDate());
        assertEquals(kernelHeaders.getMementoCreatedDate(), roundTrip.getMementoCreatedDate());
        assertEquals(kernelHeaders.getInteractionModel(), roundTrip.getInteractionModel());
        assertEquals(kernelHeaders.isObjectRoot(), roundTrip.isObjectRoot());
        assertEquals(kernelHeaders.getFilename(), roundTrip.getFilename());
        assertEquals(kernelHeaders.getExternalUrl(), roundTrip.getExternalUrl());
        assertEquals(kernelHeaders.getExternalHandling(), roundTrip.getExternalHandling());
        assertEquals(kernelHeaders.isDeleted(), roundTrip.isDeleted());
        assertEquals(kernelHeaders.getCreatedBy(), roundTrip.getCreatedBy());
        assertEquals(kernelHeaders.getCreatedDate(), roundTrip.getCreatedDate());
        assertEquals(kernelHeaders.getContentSize(), roundTrip.getContentSize());
        assertEquals(kernelHeaders.getContentPath(), roundTrip.getContentPath());
        assertEquals(kernelHeaders.isArchivalGroup(), roundTrip.isArchivalGroup());
        assertEquals(kernelHeaders.getArchivalGroupId(), roundTrip.getArchivalGroupId());
        assertEquals(kernelHeaders.getHeadersVersion(), roundTrip.getHeadersVersion());
    }

    @Test
    public void defaultConstructorCreatesEmptyHeaders() {
        final ResourceHeadersAdapter adapter = new ResourceHeadersAdapter();

        assertAll(
                () -> assertNull(adapter.getId()),
                () -> assertNull(adapter.getParent()),
                () -> assertNull(adapter.getArchivalGroupId()),
                () -> assertNull(adapter.getDigests()),
                () -> assertNull(adapter.getStateToken()),
                () -> assertNull(adapter.getInteractionModel()),
                () -> assertNull(adapter.getMimeType()),
                () -> assertNull(adapter.getFilename()),
                () -> assertEquals(-1L, adapter.getContentSize()),
                () -> assertNull(adapter.getExternalHandling()),
                () -> assertNull(adapter.getCreatedDate()),
                () -> assertNull(adapter.getCreatedBy()),
                () -> assertNull(adapter.getLastModifiedDate()),
                () -> assertNull(adapter.getLastModifiedBy()),
                () -> assertNull(adapter.getMementoCreatedDate()),
                () -> assertNull(adapter.getExternalUrl()),
                () -> assertFalse(adapter.isArchivalGroup()),
                () -> assertFalse(adapter.isObjectRoot()),
                () -> assertFalse(adapter.isDeleted()),
                () -> assertNull(adapter.getContentPath()),
                () -> assertNull(adapter.getHeadersVersion()),
                () -> assertNull(adapter.getStorageRelativePath())
        );

        // Test the storage headers are also initialized properly
        final ResourceHeaders storageHeaders = adapter.asStorageHeaders();
        assertNull(storageHeaders.getId());
    }

    @Test
    public void constructorThrowsNullPointerExceptionWhenStorageHeadersIsNull() {
        assertThrows(NullPointerException.class, () -> new ResourceHeadersAdapter((ResourceHeaders) null));
    }

    @Test
    public void constructorThrowsNullPointerExceptionWhenKernelHeadersIsNull() {
        assertThrows(NullPointerException.class, () -> new ResourceHeadersAdapter((ResourceHeadersImpl) null));
    }

    @Test
    public void settersUpdateBothHeaderTypes() {
        final ResourceHeadersAdapter adapter = new ResourceHeadersAdapter();

        final FedoraId id = FedoraId.create("info:fedora/test");
        final FedoraId parent = FedoraId.create("info:fedora/parent");
        final FedoraId agId = FedoraId.create("info:fedora/ag");
        final URI digest = URI.create("urn:sha-256:12345");
        final Instant now = Instant.now();

        adapter.setId(id);
        adapter.setParent(parent);
        adapter.setArchivalGroupId(agId);
        adapter.setDigests(List.of(digest));
        adapter.setStateToken("token123");
        adapter.setInteractionModel(BASIC_CONTAINER.toString());
        adapter.setMimeType("text/plain");
        adapter.setFilename("test.txt");
        adapter.setContentSize(500L);
        adapter.setExternalHandling("proxy");
        adapter.setExternalUrl("https://example.org/file");
        adapter.setCreatedDate(now);
        adapter.setCreatedBy("user1");
        adapter.setLastModifiedDate(now);
        adapter.setLastModifiedBy("user2");
        adapter.setMementoCreatedDate(now);
        adapter.setArchivalGroup(true);
        adapter.setObjectRoot(true);
        adapter.setDeleted(false);
        adapter.setContentPath("data/content.bin");
        adapter.setHeadersVersion("1.1");

        // Verify kernel headers have been updated
        final ResourceHeadersImpl kernelHeaders = adapter.asKernelHeaders();
        assertAll(
                () -> assertEquals(id, kernelHeaders.getId()),
                () -> assertEquals(parent, kernelHeaders.getParent()),
                () -> assertEquals(agId, kernelHeaders.getArchivalGroupId()),
                () -> assertEquals(List.of(digest), kernelHeaders.getDigests()),
                () -> assertEquals("token123", kernelHeaders.getStateToken()),
                () -> assertEquals(BASIC_CONTAINER.toString(), kernelHeaders.getInteractionModel()),
                () -> assertEquals("text/plain", kernelHeaders.getMimeType()),
                () -> assertEquals("test.txt", kernelHeaders.getFilename()),
                () -> assertEquals(500L, kernelHeaders.getContentSize()),
                () -> assertEquals("proxy", kernelHeaders.getExternalHandling()),
                () -> assertEquals("https://example.org/file", kernelHeaders.getExternalUrl()),
                () -> assertEquals(now, kernelHeaders.getCreatedDate()),
                () -> assertEquals("user1", kernelHeaders.getCreatedBy()),
                () -> assertEquals(now, kernelHeaders.getLastModifiedDate()),
                () -> assertEquals("user2", kernelHeaders.getLastModifiedBy()),
                () -> assertEquals(now, kernelHeaders.getMementoCreatedDate()),
                () -> assertTrue(kernelHeaders.isArchivalGroup()),
                () -> assertTrue(kernelHeaders.isObjectRoot()),
                () -> assertFalse(kernelHeaders.isDeleted()),
                () -> assertEquals("data/content.bin", kernelHeaders.getContentPath()),
                () -> assertEquals("1.1", kernelHeaders.getHeadersVersion())
        );

        // Verify storage headers have been updated
        final ResourceHeaders storageHeaders = adapter.asStorageHeaders();
        assertAll(
                () -> assertEquals(id.getFullId(), storageHeaders.getId()),
                () -> assertEquals(parent.getFullId(), storageHeaders.getParent()),
                () -> assertEquals(agId.getFullId(), storageHeaders.getArchivalGroupId()),
                () -> assertEquals(List.of(digest), storageHeaders.getDigests()),
                () -> assertEquals("token123", storageHeaders.getStateToken()),
                () -> assertEquals(BASIC_CONTAINER.toString(), storageHeaders.getInteractionModel()),
                () -> assertEquals("text/plain", storageHeaders.getMimeType()),
                () -> assertEquals("test.txt", storageHeaders.getFilename()),
                () -> assertEquals(500L, storageHeaders.getContentSize()),
                () -> assertEquals("proxy", storageHeaders.getExternalHandling()),
                () -> assertEquals("https://example.org/file", storageHeaders.getExternalUrl()),
                () -> assertEquals(now, storageHeaders.getCreatedDate()),
                () -> assertEquals("user1", storageHeaders.getCreatedBy()),
                () -> assertEquals(now, storageHeaders.getLastModifiedDate()),
                () -> assertEquals("user2", storageHeaders.getLastModifiedBy()),
                () -> assertEquals(now, storageHeaders.getMementoCreatedDate()),
                () -> assertTrue(storageHeaders.isArchivalGroup()),
                () -> assertTrue(storageHeaders.isObjectRoot()),
                () -> assertFalse(storageHeaders.isDeleted()),
                () -> assertEquals("data/content.bin", storageHeaders.getContentPath()),
                () -> assertEquals("1.1", storageHeaders.getHeadersVersion())
        );
    }

    @Test
    public void isObjectRootReturnsTrueWhenArchivalGroupIsTrue() {
        final ResourceHeadersAdapter adapter = new ResourceHeadersAdapter();

        // First check when both are false
        adapter.setArchivalGroup(false);
        adapter.setObjectRoot(false);
        assertFalse(adapter.isObjectRoot());

        // Set archival group to true, should make isObjectRoot true regardless of actual setting
        adapter.setArchivalGroup(true);
        assertTrue(adapter.isObjectRoot());

        // Explicitly setting objectRoot to false shouldn't matter
        adapter.setObjectRoot(false);
        assertTrue(adapter.isObjectRoot());
    }

    @Test
    public void idToStringReturnsNullForNullId() {
        final ResourceHeadersAdapter adapter = new ResourceHeadersAdapter();
        assertNull(adapter.getId());

        // Verify setting null ID works properly
        adapter.setId(null);
        assertNull(adapter.getId());
        assertNull(adapter.asStorageHeaders().getId());
    }

    @Test
    public void roundTripWithNullValues() {
        final ResourceHeadersImpl kernelHeaders = new ResourceHeadersImpl();
        // Leave all values at defaults/null

        final ResourceHeaders storageHeaders = new ResourceHeadersAdapter(kernelHeaders).asStorageHeaders();
        final ResourceHeadersImpl roundTrip = new ResourceHeadersAdapter(storageHeaders).asKernelHeaders();

        assertAll(
                () -> assertNull(roundTrip.getId()),
                () -> assertNull(roundTrip.getParent()),
                () -> assertNull(roundTrip.getArchivalGroupId()),
                () -> assertEquals(0, roundTrip.getDigests().size()),
                () -> assertNull(roundTrip.getStateToken()),
                () -> assertNull(roundTrip.getInteractionModel()),
                () -> assertNull(roundTrip.getMimeType()),
                () -> assertNull(roundTrip.getFilename()),
                () -> assertEquals(-1L, roundTrip.getContentSize()),
                () -> assertNull(roundTrip.getExternalHandling()),
                () -> assertNull(roundTrip.getCreatedDate()),
                () -> assertNull(roundTrip.getCreatedBy()),
                () -> assertNull(roundTrip.getLastModifiedDate()),
                () -> assertNull(roundTrip.getLastModifiedBy()),
                () -> assertNull(roundTrip.getMementoCreatedDate()),
                () -> assertNull(roundTrip.getExternalUrl()),
                () -> assertFalse(roundTrip.isArchivalGroup()),
                () -> assertFalse(roundTrip.isObjectRoot()),
                () -> assertFalse(roundTrip.isDeleted()),
                () -> assertNull(roundTrip.getContentPath()),
                () -> assertNull(roundTrip.getHeadersVersion()),
                () -> assertNull(roundTrip.getStorageRelativePath())
        );
    }
}
