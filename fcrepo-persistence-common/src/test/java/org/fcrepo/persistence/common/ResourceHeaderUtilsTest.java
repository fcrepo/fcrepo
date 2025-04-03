/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceHeaders;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ResourceHeaderUtils}
 *
 * @author whikloj
 */
public class ResourceHeaderUtilsTest {

    private FedoraId fedoraId;

    private FedoraId parentId;

    private String interactionModel;

    private ResourceHeadersImpl headers;

    @BeforeEach
    public void setUp() {
        parentId = FedoraId.create(UUID.randomUUID().toString());
        fedoraId = parentId.resolve(UUID.randomUUID().toString());
        interactionModel = "interactionModel";
        headers = ResourceHeaderUtils.newResourceHeaders(parentId, fedoraId, interactionModel);
    }

    @Test
    public void testNewResourceHeaders() {
        final var headers = ResourceHeaderUtils.newResourceHeaders(parentId, fedoraId, interactionModel);
        assertEquals(fedoraId, headers.getId());
        assertEquals(parentId, headers.getParent());
        assertEquals(interactionModel, headers.getInteractionModel());
        assertEquals(ResourceHeaders.V1_0, headers.getHeadersVersion());
    }

    @Test
    public void testTouchCreationHeaders() {
        final var now = Instant.now();
        final var minusMillis = now.minusMillis(1000);
        final var plusMillis = now.plusMillis(1000);
        ResourceHeaderUtils.touchCreationHeaders(headers, "userPrincipal");
        assertEquals("userPrincipal", headers.getCreatedBy());
        assertTrue(minusMillis.isBefore(headers.getCreatedDate()) && plusMillis.isAfter(headers.getCreatedDate()));
    }

    @Test
    public void testTouchCreationHeadersWithDate() {
        final var date = Instant.now();
        ResourceHeaderUtils.touchCreationHeaders(headers, "userPrincipal", date);
        assertEquals("userPrincipal", headers.getCreatedBy());
        assertEquals(date, headers.getCreatedDate());
        assertEquals(date, headers.getMementoCreatedDate());
    }

    @Test
    public void testTouchModificationHeaders() {
        final var now = Instant.now();
        final var minusMillis = now.minusMillis(1000);
        final var plusMillis = now.plusMillis(1000);
        ResourceHeaderUtils.touchModificationHeaders(headers, "userPrincipal");
        assertEquals("userPrincipal", headers.getLastModifiedBy());
        assertTrue(minusMillis.isBefore(headers.getLastModifiedDate()) &&
                plusMillis.isAfter(headers.getLastModifiedDate()));
    }

    @Test
    public void testTouchModificationHeadersWithDate() {
        final var date = Instant.now();
        final var stateToken = DigestUtils.md5Hex(String.valueOf(date.toEpochMilli())).toUpperCase();
        ResourceHeaderUtils.touchModificationHeaders(headers, "userPrincipal", date);
        assertEquals("userPrincipal", headers.getLastModifiedBy());
        assertEquals(date, headers.getLastModifiedDate());
        assertEquals(stateToken, headers.getStateToken());
    }

    @Test
    public void testTouchMementoHeaders() {
        final var now = Instant.now();
        final var minusMillis = now.minusMillis(1000);
        final var plusMillis = now.plusMillis(1000);
        ResourceHeaderUtils.touchMementoCreateHeaders(headers);
        assertTrue(minusMillis.isBefore(headers.getMementoCreatedDate()) &&
                plusMillis.isAfter(headers.getMementoCreatedDate()));
    }

    @Test
    public void testTouchMementoHeadersWithDate() {
        final var date = Instant.now();
        ResourceHeaderUtils.touchMementoCreateHeaders(headers, date);
        assertEquals(date, headers.getMementoCreatedDate());
    }

    @Test
    public void testPopulateBinaryHeaders() {
        assertNull(headers.getMimeType());
        assertNull(headers.getFilename());
        assertEquals(-1L, headers.getContentSize());
        assertNull(headers.getDigests());
        ResourceHeaderUtils.populateBinaryHeaders(headers, "text/plain", "filename.txt", 123L,
                Collections.singleton(URI.create("urn:sha1:12345")));
        assertEquals("text/plain", headers.getMimeType());
        assertEquals("filename.txt", headers.getFilename());
        assertEquals(123L, headers.getContentSize());
        assertEquals(1, headers.getDigests().size());
        assertTrue(headers.getDigests().contains(URI.create("urn:sha1:12345")));
    }
}
