/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.persistence.ocfl.impl;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.DeleteResourceOperationFactory;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.fcrepo.persistence.ocfl.impl.OcflPersistentSessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/fcrepo-config.xml")
public class NonRdfSourcesPersistenceIT {
    private static final String BINARY_CONTENT = "The binary content";
    private static final URI CONTENT_SHA512 = URI.create(
            "urn:sha-512:abc25f06e3cde4157940d4298971722eab396ebb9ba939ef978aaba3b643f317c971f7b09b2e8667f7f41339"
            + "541da8a787e395f807cea57342f08d51a8da89a6");
    private static final URI CONTENT_SHA1 = URI.create("urn:sha1:42c5ca63d3c0ca79a1736fbfefb8fc29e71009fd");
    private static final URI CONTENT_MD5 = URI.create("urn:md5:519db02d3a09d960e12e6198c65e26db");

    private static final String UPDATED_CONTENT = "Some updated text";
    private static final URI UPDATED_SHA512 = URI.create(
            "urn:sha-512:aa45bac850667712561d37fd4f334f98f6e718951388b6f7d1e968a4738b11b35c568be28e15b408826da8f6"
            + "be89ce5246d1cb1d2f93a254cc15ff9a82809395");

    @Autowired
    private OcflPersistentSessionManager sessionManager;

    private PersistentStorageSession storageSession;

    @Autowired
    private NonRdfSourceOperationFactory nonRdfSourceOpFactory;
    @Autowired
    private RdfSourceOperationFactory rdfSourceOpFactory;
    @Autowired
    private DeleteResourceOperationFactory deleteResourceOpFactory;

    @Autowired
    private OcflPropsConfig ocflPropsConfig;

    private FedoraId rescId;

    @Before
    public void setup() {
        storageSession = startWriteSession();

        rescId = makeRescId();
    }

    @Test
    public void createInternalNonRdfResource() throws Exception {
        final var op = nonRdfSourceOpFactory.createInternalBinaryBuilder(
                    rescId, IOUtils.toInputStream(BINARY_CONTENT, UTF_8))
                .filename("test.txt")
                .mimeType("text/plain")
                .parentId(FedoraId.getRepositoryRootId())
                .build();

        storageSession.persist(op);
        storageSession.prepare();
        storageSession.commit();

        final var readSession = sessionManager.getReadOnlySession();
        assertContentPersisted(BINARY_CONTENT, readSession, rescId);

        final var headers = readSession.getHeaders(rescId, null);
        assertEquals(rescId, headers.getId());
        assertEquals(NON_RDF_SOURCE.getURI(), headers.getInteractionModel());
        assertEquals("test.txt", headers.getFilename());
        assertEquals("text/plain", headers.getMimeType());
        assertEquals(BINARY_CONTENT.length(), headers.getContentSize());
        assertTrue("Headers did not contain default digest",
                headers.getDigests().contains(CONTENT_SHA512));
    }

    @Test
    public void createInternalNonRdfResourceWithDigests() throws Exception {
        final var op = nonRdfSourceOpFactory.createInternalBinaryBuilder(
                    rescId, IOUtils.toInputStream(BINARY_CONTENT, UTF_8))
                .contentDigests(new ArrayList<>(List.of(CONTENT_SHA1, CONTENT_MD5)))
                .mimeType("text/plain")
                .parentId(FedoraId.getRepositoryRootId())
                .build();

        storageSession.persist(op);
        storageSession.prepare();
        storageSession.commit();

        final var readSession = sessionManager.getReadOnlySession();
        assertContentPersisted(BINARY_CONTENT, readSession, rescId);

        final var headers = readSession.getHeaders(rescId, null);
        assertEquals(rescId, headers.getId());
        assertEquals(NON_RDF_SOURCE.getURI(), headers.getInteractionModel());
        assertEquals("text/plain", headers.getMimeType());
        assertEquals(BINARY_CONTENT.length(), headers.getContentSize());
        assertTrue("Headers did not contain md5 digest",
                headers.getDigests().contains(CONTENT_MD5));
        assertTrue("Headers did not contain sha1 digest",
                headers.getDigests().contains(CONTENT_SHA1));
        assertTrue("Headers did not contain default sha512 digest",
                headers.getDigests().contains(CONTENT_SHA512));
    }

    @Test
    public void createInternalNonRdfResourceUncommittedSession() throws Exception {
        final var op = nonRdfSourceOpFactory.createInternalBinaryBuilder(
                    rescId, IOUtils.toInputStream(BINARY_CONTENT, UTF_8))
                .mimeType("text/plain")
                .parentId(FedoraId.getRepositoryRootId())
                .build();

        storageSession.persist(op);

        assertContentPersisted(BINARY_CONTENT, storageSession, rescId);

        final var headers = storageSession.getHeaders(rescId, null);
        assertEquals(rescId, headers.getId());
        assertEquals(NON_RDF_SOURCE.getURI(), headers.getInteractionModel());
        assertEquals("text/plain", headers.getMimeType());
        assertEquals(BINARY_CONTENT.length(), headers.getContentSize());
        assertTrue("Headers did not contain default digest",
                headers.getDigests().contains(CONTENT_SHA512));
    }

    @Test(expected = InvalidChecksumException.class)
    public void createInternalNonRdfResourceWithInvalidDigest() throws Exception {
        final var op = nonRdfSourceOpFactory.createInternalBinaryBuilder(
                    rescId, IOUtils.toInputStream(BINARY_CONTENT, UTF_8))
                .contentDigests(new ArrayList<>(List.of(URI.create("urn:sha1:ohnothisisbad"))))
                .mimeType("text/plain")
                .build();

        storageSession.persist(op);
    }

    @Test(expected = InvalidChecksumException.class)
    public void createInternalNonRdfResourceWithInvalidDefaultDigest() throws Exception {
        final var op = nonRdfSourceOpFactory.createInternalBinaryBuilder(
                    rescId, IOUtils.toInputStream(BINARY_CONTENT, UTF_8))
                .contentDigests(asList(URI.create("urn:sha-512:ohnothisisbad"), CONTENT_SHA1))
                .mimeType("text/plain")
                .build();

        storageSession.persist(op);
    }

    @Test
    public void updateInternalNonRdfResource() throws Exception {
        final var op = nonRdfSourceOpFactory.createInternalBinaryBuilder(
                    rescId, IOUtils.toInputStream(BINARY_CONTENT, UTF_8))
                .filename("test.txt")
                .mimeType("text/plain")
                .contentDigests(new ArrayList<>(List.of(CONTENT_SHA1)))
                .parentId(FedoraId.getRepositoryRootId())
                .build();

        storageSession.persist(op);

        final var updateOp = nonRdfSourceOpFactory.updateInternalBinaryBuilder(
                    rescId, IOUtils.toInputStream(UPDATED_CONTENT, UTF_8))
                .mimeType("text/plain")
                .build();

        storageSession.persist(updateOp);
        storageSession.prepare();
        storageSession.commit();

        final var readSession = sessionManager.getReadOnlySession();
        assertContentPersisted(UPDATED_CONTENT, readSession, rescId);

        final var headers = readSession.getHeaders(rescId, null);
        assertEquals(rescId, headers.getId());
        assertEquals(NON_RDF_SOURCE.getURI(), headers.getInteractionModel());
        assertNull(headers.getFilename());
        assertEquals("text/plain", headers.getMimeType());
        assertEquals(UPDATED_CONTENT.length(), headers.getContentSize());
        assertEquals("Only one digest expected", 1, headers.getDigests().size());
        assertTrue("Headers did not contain default digest",
                headers.getDigests().contains(UPDATED_SHA512));
    }

    @Test
    public void rollbackUpdateToInternalNonRdfResource() throws Exception {
        final var op = nonRdfSourceOpFactory.createInternalBinaryBuilder(
                rescId, IOUtils.toInputStream(BINARY_CONTENT, UTF_8))
            .filename("test.txt")
            .mimeType("text/plain")
            .parentId(FedoraId.getRepositoryRootId())
            .build();

        storageSession.persist(op);
        storageSession.prepare();
        storageSession.commit();

        final var updateOp = nonRdfSourceOpFactory.updateInternalBinaryBuilder(
                    rescId, IOUtils.toInputStream(UPDATED_CONTENT, UTF_8))
                .filename("test_updated.txt")
                .mimeType("text/plain")
                .build();

        final var updateSession = startWriteSession();
        updateSession.persist(updateOp);
        updateSession.rollback();

        final var readSession = sessionManager.getReadOnlySession();
        assertContentPersisted(BINARY_CONTENT, readSession, rescId);

        final var headers = readSession.getHeaders(rescId, null);
        assertEquals(rescId, headers.getId());
        assertEquals(NON_RDF_SOURCE.getURI(), headers.getInteractionModel());
        assertEquals("test.txt", headers.getFilename());
        assertEquals("text/plain", headers.getMimeType());
        assertEquals(BINARY_CONTENT.length(), headers.getContentSize());
        assertTrue("Headers did not contain default digest",
                headers.getDigests().contains(CONTENT_SHA512));
    }

    @Test
    public void createInternalNonRdfResourceInAG() throws Exception {
        final var agOp = rdfSourceOpFactory.createBuilder(rescId, BASIC_CONTAINER.getURI())
                .archivalGroup(true)
                .parentId(FedoraId.getRepositoryRootId())
                .build();
        storageSession.persist(agOp);

        final var binId = rescId.resolve(UUID.randomUUID().toString());
        final var op = nonRdfSourceOpFactory.createInternalBinaryBuilder(
                    binId, IOUtils.toInputStream(BINARY_CONTENT, UTF_8))
                .parentId(rescId)
                .mimeType("text/plain")
                .build();

        storageSession.persist(op);
        storageSession.prepare();
        storageSession.commit();

        final var readSession = sessionManager.getReadOnlySession();
        // Check the persisted AG details
        final var agHeaders = readSession.getHeaders(rescId, null);
        assertEquals(rescId, agHeaders.getId());
        assertEquals(FedoraId.getRepositoryRootId(), agHeaders.getParent());
        assertEquals(BASIC_CONTAINER.getURI(), agHeaders.getInteractionModel());

        // Check the binary details
        assertContentPersisted(BINARY_CONTENT, readSession, binId);

        final var headers = readSession.getHeaders(binId, null);
        assertEquals(binId, headers.getId());
        assertEquals(rescId, headers.getParent());
        assertEquals(NON_RDF_SOURCE.getURI(), headers.getInteractionModel());
        assertEquals("text/plain", headers.getMimeType());
        assertEquals(BINARY_CONTENT.length(), headers.getContentSize());
        assertTrue("Headers did not contain default digest",
                headers.getDigests().contains(CONTENT_SHA512));
    }

    @Test
    public void deleteInternalNonRdfResource() throws Exception {
        final var op = nonRdfSourceOpFactory.createInternalBinaryBuilder(
                    rescId, IOUtils.toInputStream(BINARY_CONTENT, UTF_8))
                .filename("test.txt")
                .mimeType("text/plain")
                .contentDigests(new ArrayList<>(List.of(CONTENT_SHA1)))
                .parentId(FedoraId.getRepositoryRootId())
                .build();

        storageSession.persist(op);
        storageSession.prepare();
        storageSession.commit();

        final var deleteOp = deleteResourceOpFactory.deleteBuilder(rescId).build();

        final var deleteSession = startWriteSession();
        deleteSession.persist(deleteOp);
        deleteSession.prepare();
        deleteSession.commit();

        final var readSession = sessionManager.getReadOnlySession();
        try {
            readSession.getBinaryContent(rescId, null);
            fail("Binary file must no longer exist");
        } catch (final PersistentItemNotFoundException e) {
            // expected
        }

        final var headers = readSession.getHeaders(rescId, null);
        assertEquals(rescId, headers.getId());
        assertTrue("Headers must indicate object deleted", headers.isDeleted());
        assertEquals(NON_RDF_SOURCE.getURI(), headers.getInteractionModel());
    }

    @Test
    public void createInternalNonRdfResourceTransmissionFixityToOcflFailure() throws Exception {
        final var op = nonRdfSourceOpFactory.createInternalBinaryBuilder(
                    rescId, IOUtils.toInputStream(BINARY_CONTENT, UTF_8))
                .filename("test.txt")
                .mimeType("text/plain")
                .parentId(FedoraId.getRepositoryRootId())
                .build();

        storageSession.persist(op);

        // Modify the file after staging to simulate a transmission error
        final Path ocflStagingDir = ocflPropsConfig.getFedoraOcflStaging();
        final String rawId = StringUtils.substringAfterLast(rescId.getFullId(), "/");
        final Path stagedFile = Files.find(ocflStagingDir, 10, (path, attrs) -> {
            return path.getFileName().toString().equals(rawId);
        }).findFirst().get();
        Files.write(stagedFile, "oops".getBytes(), StandardOpenOption.APPEND);

        try {
            storageSession.prepare();
            storageSession.commit();
            fail("Expected commit to fail to due fixity failure");
        } catch (final PersistentStorageException e) {
            assertThat(e.getMessage(), containsString("Failed to commit object"));
            assertThat(e.getCause().getMessage(), containsString("Expected sha-512 digest"));
        }
    }

    private void assertContentPersisted(final String expectedContent, final PersistentStorageSession session,
            final FedoraId rescId) throws Exception {
        final var resultContent = session.getBinaryContent(rescId, null);
        assertEquals("Binary content did not match expectations",
                expectedContent, IOUtils.toString(resultContent, UTF_8));
    }

    private PersistentStorageSession startWriteSession() {
        final String sessionId = UUID.randomUUID().toString();
        return sessionManager.getSession(sessionId);
    }

    private FedoraId makeRescId(final String... parentIds) {
        String parents = "";
        if (parentIds != null && parentIds.length > 0) {
            parents = Arrays.stream(parentIds).map(p -> p.replace("info:fedora/", ""))
                    .collect(Collectors.joining("/", "", "/"));
        }
        return FedoraId.create( "info:fedora/" + parents + UUID.randomUUID().toString());
    }
}
