/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.OcflVersionInfo;
import org.fcrepo.storage.ocfl.ResourceContent;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.fcrepo.storage.ocfl.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * Tests for {@link FcrepoOcflObjectSessionWrapper}
 *
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FcrepoOcflObjectSessionWrapperTest {

    @Mock
    private OcflObjectSession innerSession;

    @Mock
    private ResourceHeaders mockHeaders;

    @Mock
    private ResourceContent mockContent;

    private FcrepoOcflObjectSessionWrapper wrapper;

    private static final String RESOURCE_ID = "test-resource";
    private static final String VERSION_ID = "v1";
    private static final String OBJECT_ID = "test-object";
    private static final String SESSION_ID = "test-session";

    @BeforeEach
    public void setUp() {
        wrapper = new FcrepoOcflObjectSessionWrapper(innerSession);
        when(innerSession.ocflObjectId()).thenReturn(OBJECT_ID);
        when(innerSession.sessionId()).thenReturn(SESSION_ID);
    }

    @Test
    public void testSessionId() {
        assertEquals(SESSION_ID, wrapper.sessionId());
    }

    @Test
    public void testOcflObjectId() {
        assertEquals(OBJECT_ID, wrapper.ocflObjectId());
    }

    @Test
    public void testWriteResource() throws Exception {
        final InputStream content = new ByteArrayInputStream("test content".getBytes());

        when(innerSession.writeResource(mockHeaders, content)).thenReturn(mockHeaders);

        final ResourceHeaders result = wrapper.writeResource(mockHeaders, content);

        assertSame(mockHeaders, result);
        verify(innerSession).writeResource(mockHeaders, content);
    }

    @Test
    public void testWriteResourceThrowsNotFound() throws Exception {
        final InputStream content = new ByteArrayInputStream("test content".getBytes());

        when(innerSession.writeResource(mockHeaders, content)).thenThrow(new NotFoundException("Not found"));

        assertThrows(PersistentItemNotFoundException.class,
                () -> wrapper.writeResource(mockHeaders, content));
    }

    @Test
    public void testWriteResourceThrowsGenericError() throws Exception {
        final InputStream content = new ByteArrayInputStream("test content".getBytes());

        when(innerSession.writeResource(mockHeaders, content)).thenThrow(new PersistentStorageException("IO error"));

        assertThrows(PersistentStorageException.class,
                () -> wrapper.writeResource(mockHeaders, content));
    }

    @Test
    public void testWriteHeaders() throws Exception {
        wrapper.writeHeaders(mockHeaders);

        verify(innerSession).writeHeaders(mockHeaders);
    }

    @Test
    public void testWriteHeadersThrowsNotFound() throws Exception {
        doThrow(new NotFoundException("Not found")).when(innerSession).writeHeaders(mockHeaders);

        assertThrows(PersistentItemNotFoundException.class,
                () -> wrapper.writeHeaders(mockHeaders));
    }

    @Test
    public void testWriteHeadersThrowsGenericError() throws Exception {
        doThrow(new PersistentStorageException("IO error")).when(innerSession).writeHeaders(mockHeaders);

        assertThrows(PersistentStorageException.class,
                () -> wrapper.writeHeaders(mockHeaders));
    }

    @Test
    public void testDeleteContentFile() throws Exception {
        wrapper.deleteContentFile(mockHeaders);

        verify(innerSession).deleteContentFile(mockHeaders);
    }

    @Test
    public void testDeleteContentFileThrowsNotFound() throws Exception {
        doThrow(new NotFoundException("Not found")).when(innerSession).deleteContentFile(mockHeaders);

        assertThrows(PersistentItemNotFoundException.class,
                () -> wrapper.deleteContentFile(mockHeaders));
    }

    @Test
    public void testDeleteResource() throws Exception {
        wrapper.deleteResource(RESOURCE_ID);

        verify(innerSession).deleteResource(RESOURCE_ID);
    }

    @Test
    public void testDeleteResourceThrowsNotFound() throws Exception {
        doThrow(new NotFoundException("Not found")).when(innerSession).deleteResource(RESOURCE_ID);

        assertThrows(PersistentItemNotFoundException.class,
                () -> wrapper.deleteResource(RESOURCE_ID));
    }

    @Test
    public void testContainsResource() throws Exception {
        when(innerSession.containsResource(RESOURCE_ID)).thenReturn(true);

        assertTrue(wrapper.containsResource(RESOURCE_ID));
        verify(innerSession).containsResource(RESOURCE_ID);
    }

    @Test
    public void testReadHeaders() throws Exception {
        when(innerSession.readHeaders(RESOURCE_ID)).thenReturn(mockHeaders);

        final ResourceHeaders result = wrapper.readHeaders(RESOURCE_ID);

        assertSame(mockHeaders, result);
        verify(innerSession).readHeaders(RESOURCE_ID);
    }

    @Test
    public void testReadHeadersWithVersion() throws Exception {
        when(innerSession.readHeaders(RESOURCE_ID, VERSION_ID)).thenReturn(mockHeaders);

        final ResourceHeaders result = wrapper.readHeaders(RESOURCE_ID, VERSION_ID);

        assertSame(mockHeaders, result);
        verify(innerSession).readHeaders(RESOURCE_ID, VERSION_ID);
    }

    @Test
    public void testReadHeadersThrowsNotFound() throws Exception {
        when(innerSession.readHeaders(RESOURCE_ID)).thenThrow(new NotFoundException("Not found"));

        assertThrows(PersistentItemNotFoundException.class,
                () -> wrapper.readHeaders(RESOURCE_ID));
    }

    @Test
    public void testReadContent() throws Exception {
        when(innerSession.readContent(RESOURCE_ID)).thenReturn(mockContent);

        final ResourceContent result = wrapper.readContent(RESOURCE_ID);

        assertSame(mockContent, result);
        verify(innerSession).readContent(RESOURCE_ID);
    }

    @Test
    public void testReadContentWithVersion() throws Exception {
        when(innerSession.readContent(RESOURCE_ID, VERSION_ID)).thenReturn(mockContent);

        final ResourceContent result = wrapper.readContent(RESOURCE_ID, VERSION_ID);

        assertSame(mockContent, result);
        verify(innerSession).readContent(RESOURCE_ID, VERSION_ID);
    }

    @Test
    public void testReadRange() throws Exception {
        when(innerSession.readRange(RESOURCE_ID, 10L, 20L)).thenReturn(mockContent);

        final ResourceContent result = wrapper.readRange(RESOURCE_ID, 10L, 20L);

        assertSame(mockContent, result);
        verify(innerSession).readRange(RESOURCE_ID, 10L, 20L);
    }

    @Test
    public void testReadRangeWithVersion() throws Exception {
        when(innerSession.readRange(RESOURCE_ID, VERSION_ID, 10L, 20L)).thenReturn(mockContent);

        final ResourceContent result = wrapper.readRange(RESOURCE_ID, VERSION_ID, 10L, 20L);

        assertSame(mockContent, result);
        verify(innerSession).readRange(RESOURCE_ID, VERSION_ID, 10L, 20L);
    }

    @Test
    public void testReadRangeThrowsNotFound() throws Exception {
        when(innerSession.readRange(anyString(), anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Not found"));

        assertThrows(PersistentItemNotFoundException.class,
                () -> wrapper.readRange(RESOURCE_ID, 10L, 20L));
    }

    @Test
    public void testListVersions() throws Exception {
        final List<OcflVersionInfo> versionList = List.of(mock(OcflVersionInfo.class));
        when(innerSession.listVersions(RESOURCE_ID)).thenReturn(versionList);

        final List<OcflVersionInfo> result = wrapper.listVersions(RESOURCE_ID);

        assertSame(versionList, result);
        verify(innerSession).listVersions(RESOURCE_ID);
    }

    @Test
    public void testStreamResourceHeaders() throws Exception {
        final Stream<ResourceHeaders> stream = Stream.of(mockHeaders);
        when(innerSession.streamResourceHeaders()).thenReturn(stream);

        final Stream<ResourceHeaders> result = wrapper.streamResourceHeaders();

        assertNotNull(result);
        assertEquals(1, result.count());
        verify(innerSession).streamResourceHeaders();
    }

    @Test
    public void testVersionCreationTimestamp() {
        final OffsetDateTime timestamp = OffsetDateTime.now();

        wrapper.versionCreationTimestamp(timestamp);

        verify(innerSession).versionCreationTimestamp(timestamp);
    }

    @Test
    public void testVersionAuthor() {
        wrapper.versionAuthor("test-name", "test@example.org");

        verify(innerSession).versionAuthor("test-name", "test@example.org");
    }

    @Test
    public void testVersionMessage() {
        wrapper.versionMessage("test message");

        verify(innerSession).versionMessage("test message");
    }

    @Test
    public void testInvalidateCache() {
        wrapper.invalidateCache(RESOURCE_ID);

        verify(innerSession).invalidateCache(RESOURCE_ID);
    }

    @Test
    public void testCommitType() {
        wrapper.commitType(CommitType.NEW_VERSION);

        verify(innerSession).commitType(CommitType.NEW_VERSION);
    }

    @Test
    public void testCommit() throws Exception {
        wrapper.commit();

        verify(innerSession).commit();
    }

    @Test
    public void testCommitThrowsNotFound() throws Exception {
        doThrow(new NotFoundException("Not found")).when(innerSession).commit();

        assertThrows(PersistentItemNotFoundException.class, () -> wrapper.commit());
    }

    @Test
    public void testRollback() throws Exception {
        wrapper.rollback();

        verify(innerSession).rollback();
    }

    @Test
    public void testAbort() throws Exception {
        wrapper.abort();

        verify(innerSession).abort();
    }

    @Test
    public void testIsOpen() {
        when(innerSession.isOpen()).thenReturn(true);

        assertTrue(wrapper.isOpen());

        when(innerSession.isOpen()).thenReturn(false);

        assertFalse(wrapper.isOpen());
    }

    @Test
    public void testClose() throws Exception {
        wrapper.close();

        verify(innerSession).close();
    }
}