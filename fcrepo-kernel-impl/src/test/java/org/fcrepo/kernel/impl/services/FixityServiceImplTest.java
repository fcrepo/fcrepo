/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.PREMIS_EVENT_OUTCOME_DETAIL;
import static org.fcrepo.kernel.api.RdfLexicon.PREMIS_FIXITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.RdfCollectors;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Unit test for FixityServiceImpl
 *
 * @author bbpennel
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class FixityServiceImplTest {

    @Mock
    private Binary binary;

    @InjectMocks
    private FixityServiceImpl fixityService;

    private static final String TEST_BINARY_CONTENT = "Test binary content";
    private static final String FEDORA_ID = "info:fedora/test-binary";
    private static final long CONTENT_SIZE = TEST_BINARY_CONTENT.length();
    private static final URI CORRECT_SHA1 = URI.create("urn:sha1:e070a846e478723070bd3c84cf83281acdb1cb09");
    private static final URI CORRECT_MD5 = URI.create("urn:md5:effd5cdb18646d898596e683a2b8d9ea");
    private static final URI OTHER_SHA1 = URI.create("urn:sha1:94e66df8cd09d410c62d9e0dc59d3a884e458e05");

    @BeforeEach
    public void setup() throws IOException {
        final InputStream contentStream = new ByteArrayInputStream(TEST_BINARY_CONTENT.getBytes());
        when(binary.getContent()).thenReturn(contentStream);
        when(binary.getId()).thenReturn(FEDORA_ID);
        when(binary.getFedoraId()).thenReturn(FedoraId.create(FEDORA_ID));
        when(binary.getContentSize()).thenReturn(CONTENT_SIZE);
        when(binary.getContentDigests()).thenReturn(List.of(CORRECT_SHA1));
    }

    @Test
    public void testGetFixity() throws UnsupportedAlgorithmException {
        final Collection<String> algorithms = Arrays.asList("SHA-1", "MD5");

        final Collection<URI> fixityResults = fixityService.getFixity(binary, algorithms);

        assertNotNull(fixityResults);
        assertEquals(2, fixityResults.size());

        // Verify the SHA-1 digest
        assertTrue(fixityResults.contains(CORRECT_SHA1));

        // Verify the MD5 digest
        assertTrue(fixityResults.contains(CORRECT_MD5));
    }

    @Test
    public void testGetFixity_UnsupportedAlgorithm() {
        final Collection<String> algorithms = List.of("INVALID-ALG");

        assertThrows(UnsupportedAlgorithmException.class, () -> {
            fixityService.getFixity(binary, algorithms);
        });
    }

    @Test
    public void testCheckFixity_Success() throws InvalidChecksumException {
        final RdfStream fixityStream = fixityService.checkFixity(binary);
        assertNotNull(fixityStream);
        final Model model = fixityStream.collect(RdfCollectors.toModel());

        final Resource subject = model.getResource(FEDORA_ID);
        final Resource outcomeSubject = subject.getProperty(HAS_FIXITY_RESULT).getObject().asResource();
        assertTrue(model.contains(outcomeSubject, HAS_FIXITY_STATE, "SUCCESS"));
        assertTrue(model.contains(outcomeSubject, HAS_SIZE, createTypedLiteral(CONTENT_SIZE)));
        assertTrue(model.contains(outcomeSubject, type, PREMIS_FIXITY));
        assertTrue(model.contains(outcomeSubject, type, PREMIS_EVENT_OUTCOME_DETAIL));
        assertTrue(model.contains(outcomeSubject, HAS_MESSAGE_DIGEST, createResource(CORRECT_SHA1.toString())));
    }

    @Test
    public void testCheckFixity_InvalidChecksum() throws Exception {
        when(binary.getContentDigests()).thenReturn(List.of(OTHER_SHA1));

        final RdfStream fixityStream = fixityService.checkFixity(binary);
        assertNotNull(fixityStream);
        final Model model = fixityStream.collect(RdfCollectors.toModel());

        final Resource subject = model.getResource(FEDORA_ID);
        final Resource outcomeSubject = subject.getProperty(HAS_FIXITY_RESULT).getObject().asResource();
        assertTrue(model.contains(outcomeSubject, HAS_FIXITY_STATE, "BAD_CHECKSUM"));
        assertTrue(model.contains(outcomeSubject, HAS_SIZE, createTypedLiteral(CONTENT_SIZE)));
        assertTrue(model.contains(outcomeSubject, type, PREMIS_FIXITY));
        assertTrue(model.contains(outcomeSubject, type, PREMIS_EVENT_OUTCOME_DETAIL));
        assertTrue(model.contains(outcomeSubject, HAS_MESSAGE_DIGEST, createResource(CORRECT_SHA1.toString())));
    }

    @Test
    public void testCheckFixity_NoDigests() throws InvalidChecksumException {
        // Setup binary with no digests
        when(binary.getContentDigests()).thenReturn(List.of());

        final RdfStream fixityStream = fixityService.checkFixity(binary);
        assertNotNull(fixityStream);
        final Model model = fixityStream.collect(RdfCollectors.toModel());

        final Resource subject = model.getResource(FEDORA_ID);
        final Resource outcomeSubject = subject.getProperty(HAS_FIXITY_RESULT).getObject().asResource();
        assertTrue(model.contains(outcomeSubject, HAS_FIXITY_STATE, "SUCCESS"));
        assertTrue(model.contains(outcomeSubject, HAS_SIZE, createTypedLiteral(CONTENT_SIZE)));
        assertTrue(model.contains(outcomeSubject, type, PREMIS_FIXITY));
        assertTrue(model.contains(outcomeSubject, type, PREMIS_EVENT_OUTCOME_DETAIL));
        assertFalse(model.contains(outcomeSubject, HAS_MESSAGE_DIGEST));
    }
}