/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.models;

import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_BINARY;
import static org.fcrepo.kernel.api.models.ExternalContent.PROXY;
import static org.fcrepo.kernel.api.models.ExternalContent.REDIRECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.cache.UserTypesCache;
import org.fcrepo.kernel.api.exception.ItemNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BinaryImplTest {

    private FedoraId fedoraId;

    @Mock
    private Transaction mockTransaction;

    @Mock
    private PersistentStorageSessionManager mockPSessionManager;

    @Mock
    private PersistentStorageSession mockSession;

    @Mock
    private ResourceFactory mockResourceFactory;

    @Mock
    private UserTypesCache mockUserTypesCache;

    @Mock
    private FedoraResource mockDescription;

    @Mock
    private RdfStream mockRdfStream;

    private BinaryImpl binary;

    private final String TEST_CONTENT = "test content";
    private final String TEST_FILENAME = "test.txt";
    private final String TEST_MIME_TYPE = "text/plain";
    private final String TEST_EXTERNAL_URL = "http://example.org/test.txt";
    private final long TEST_SIZE = 12345L;
    private final Collection<URI> TEST_DIGESTS = Set.of(URI.create("urn:sha1:test"));

    @TempDir
    private Path tempDir;
    private Path testExternalFile;

    @BeforeEach
    void setUp() throws Exception {
        when(mockPSessionManager.getReadOnlySession()).thenReturn(mockSession);
        fedoraId = FedoraId.create("test-id");

        // Create the binary object
        binary = new BinaryImpl(fedoraId, mockTransaction, mockPSessionManager, mockResourceFactory,
                mockUserTypesCache);
        binary.setInteractionModel(RdfLexicon.NON_RDF_SOURCE.getURI());

        // Setup description behavior
        when(mockDescription.getTriples()).thenReturn(mockRdfStream);

        // Default properties
        binary.setContentSize(TEST_SIZE);
        binary.setFilename(TEST_FILENAME);
        binary.setMimeType(TEST_MIME_TYPE);
        binary.setDigests(TEST_DIGESTS);

        testExternalFile = tempDir.resolve("test.txt");
        Files.writeString(testExternalFile, TEST_CONTENT);
    }

    @Test
    void testGetContent() throws Exception {
        final InputStream contentStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());
        when(mockSession.getBinaryContent(eq(fedoraId), isNull())).thenReturn(contentStream);

        try (final InputStream result = binary.getContent()) {
            assertEquals(TEST_CONTENT, IOUtils.toString(result, StandardCharsets.UTF_8));
        }
    }

    @Test
    void testGetContentThrowsItemNotFound() throws Exception {
        when(mockSession.getBinaryContent(eq(fedoraId), isNull()))
                .thenThrow(new PersistentItemNotFoundException("Not found"));

        assertThrows(ItemNotFoundException.class, () -> binary.getContent());
    }

    @Test
    void testGetContentThrowsRepositoryRuntimeException() throws Exception {
        when(mockSession.getBinaryContent(eq(fedoraId), isNull()))
                .thenThrow(new PersistentStorageException("Storage error"));

        assertThrows(RepositoryRuntimeException.class, () -> binary.getContent());
    }

    @Test
    void testGetContentForProxy() throws Exception {
        binary.setExternalHandling(PROXY);
        binary.setExternalUrl(testExternalFile.toUri().toString());

        try (final var binaryStream = binary.getContent()) {
            assertEquals(TEST_CONTENT, IOUtils.toString(binaryStream, StandardCharsets.UTF_8));
        }
    }

    @Test
    void testGetContentForRedirect() throws Exception {
        binary.setExternalHandling(REDIRECT);
        binary.setExternalUrl(testExternalFile.toUri().toString());

        try (final var binaryStream = binary.getContent()) {
            assertEquals(TEST_CONTENT, IOUtils.toString(binaryStream, StandardCharsets.UTF_8));
        }
    }

    @Test
    void testGetRange() throws Exception {
        final InputStream contentStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());
        when(mockSession.getBinaryRange(eq(fedoraId), isNull(), eq(2L), eq(5L)))
                .thenReturn(contentStream);

        try (final InputStream result = binary.getRange(2L, 5L)) {
            assertEquals(TEST_CONTENT, IOUtils.toString(result, StandardCharsets.UTF_8));
        }
    }

    @Test
    void testGetRangeThrowsItemNotFound() throws Exception {
        when(mockSession.getBinaryRange(eq(fedoraId), isNull(), anyLong(), anyLong()))
                .thenThrow(new PersistentItemNotFoundException("Not found"));

        assertThrows(ItemNotFoundException.class, () -> binary.getRange(0L, 10L));
    }

    @Test
    void testGetRangeForProxy() throws Exception {
        binary.setExternalHandling(PROXY);
        binary.setExternalUrl(testExternalFile.toUri().toString());

        try (final var inputStream = binary.getRange(2L, 10L)) {
            assertEquals("st conten", IOUtils.toString(inputStream, StandardCharsets.UTF_8));
        }
    }

    @Test
    void testIsProxy() {
        binary.setExternalHandling(PROXY);
        assertTrue(binary.isProxy());

        binary.setExternalHandling(REDIRECT);
        assertFalse(binary.isProxy());

        binary.setExternalHandling(null);
        assertFalse(binary.isProxy());
    }

    @Test
    void testIsRedirect() {
        binary.setExternalHandling(REDIRECT);
        assertTrue(binary.isRedirect());

        binary.setExternalHandling(PROXY);
        assertFalse(binary.isRedirect());

        binary.setExternalHandling(null);
        assertFalse(binary.isRedirect());
    }

    @Test
    void testGetExternalURL() {
        binary.setExternalUrl(TEST_EXTERNAL_URL);
        assertEquals(TEST_EXTERNAL_URL, binary.getExternalURL());
    }

    @Test
    void testGetMimeType() {
        assertEquals(TEST_MIME_TYPE, binary.getMimeType());
    }

    @Test
    void testGetFilename() {
        assertEquals(TEST_FILENAME, binary.getFilename());
    }

    @Test
    void testGetContentSize() {
        assertEquals(TEST_SIZE, binary.getContentSize());
    }

    @Test
    void testGetContentDigests() {
        assertEquals(TEST_DIGESTS, binary.getContentDigests());

        binary.setDigests(null);
        assertNull(binary.getContentDigests());
    }

    @Test
    void testGetDescription() throws Exception {
        final FedoraId descId = fedoraId.asDescription();
        when(mockResourceFactory.getResource(mockTransaction, descId)).thenReturn(mockDescription);

        final FedoraResource result = binary.getDescription();

        assertSame(mockDescription, result);
    }

    @Test
    void testGetDescriptionForMemento() throws Exception {
        // Change the id to a memento
        final Instant mementoInstant = Instant.parse("2023-01-01T00:00:00Z");
        fedoraId = fedoraId.asMemento(mementoInstant);
        binary = new BinaryImpl(fedoraId, mockTransaction, mockPSessionManager, mockResourceFactory,
                mockUserTypesCache);
        binary.setIsMemento(true);
        binary.setMementoDatetime(mementoInstant);

        final FedoraId descId = fedoraId.asDescription();
        when(mockResourceFactory.getResource(mockTransaction, descId)).thenReturn(mockDescription);

        final FedoraResource result = binary.getDescription();

        assertSame(mockDescription, result);
    }

    @Test
    void testGetDescriptionThrowsPathNotFoundRuntimeException() throws Exception {
        when(mockResourceFactory.getResource(mockTransaction, fedoraId.asDescription()))
                .thenThrow(new PathNotFoundException("Path not found"));

        assertThrows(PathNotFoundRuntimeException.class, () -> {
            binary.getDescription();
        });
    }

    @Test
    void testGetSystemTypes() {
        final var typesList = binary.getSystemTypes(false).stream()
                .map(URI::toString)
                .collect(Collectors.toList());
        assertTrue(typesList.contains(FEDORA_BINARY.getURI()));
        assertTrue(typesList.contains(RdfLexicon.NON_RDF_SOURCE.getURI()));
        assertTrue(typesList.contains(RdfLexicon.VERSIONING_TIMEGATE_TYPE));
        assertTrue(typesList.contains(RdfLexicon.RESOURCE.getURI()));
        assertTrue(typesList.contains(RdfLexicon.FEDORA_RESOURCE.getURI()));
        assertTrue(typesList.contains(RdfLexicon.VERSIONED_RESOURCE.getURI()));
        assertEquals(6, typesList.size());
    }

    @Test
    void testGetTriples() throws Exception {
        // Setup the description to return a mock RDF stream
        when(mockResourceFactory.getResource(any(), any(FedoraId.class))).thenReturn(mockDescription);
        when(mockDescription.getTriples()).thenReturn(mockRdfStream);

        final RdfStream result = binary.getTriples();

        assertSame(mockRdfStream, result);
    }
}