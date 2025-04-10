/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for {@link TombstoneException}
 *
 * @author whikloj
 */
@ExtendWith(MockitoExtension.class)
public class TombstoneExceptionTest {

    @Mock
    private FedoraResource resource;

    private FedoraId fedoraId;

    private static final DateTimeFormatter FORMATTER = ISO_INSTANT.withZone(UTC);

    @BeforeEach
    public void setUp() {
        fedoraId = FedoraId.create("info:fedora/test-subject");
        when(resource.getFedoraId()).thenReturn(fedoraId);
    }

    @Test
    public void testTombstoneException() {
        final TombstoneException exception = new TombstoneException(resource);
        assertEquals("Discovered tombstone resource at " + fedoraId.getFullIdPath(), exception.getMessage());
        assertNull(exception.getTombstoneURI());
        assertNull(exception.getTimemapUri());
    }

    @Test
    public void testTombstoneExceptionWithUri() {
        final String tombstoneUri = "http://example.org/tombstone";
        final String timemapUri = "http://example.org/timemap";
        final TombstoneException exception = new TombstoneException(resource, tombstoneUri, timemapUri);
        assertEquals("Discovered tombstone resource at " + fedoraId.getFullIdPath(), exception.getMessage());
        assertEquals(tombstoneUri, exception.getTombstoneURI());
        assertEquals(timemapUri, exception.getTimemapUri());
    }

    @Test
    public void testTombstoneExceptionWithLastModified() {
        final var now = Instant.now();
        when(resource.getLastModifiedDate()).thenReturn(now);
        final TombstoneException exception = new TombstoneException(resource);
        assertEquals("Discovered tombstone resource at " + fedoraId.getFullIdPath() + ", departed at: " +
                FORMATTER.format(now), exception.getMessage());
    }
}
