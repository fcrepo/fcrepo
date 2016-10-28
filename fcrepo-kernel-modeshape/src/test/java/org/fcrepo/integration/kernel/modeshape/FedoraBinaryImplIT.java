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
package org.fcrepo.integration.kernel.modeshape;

import static java.util.Arrays.asList;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static java.util.UUID.randomUUID;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM.SHA1;
import static org.fcrepo.kernel.api.utils.ContentDigest.asURI;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import org.apache.commons.io.IOUtils;

import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * <p>{@link org.fcrepo.integration.kernel.modeshape.FedoraBinaryImplIT} class.</p>
 *
 * @author ksclarke
 * @author ajs6f
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraBinaryImplIT extends AbstractIT {

    @Inject
    FedoraRepository repo;

    @Inject
    BinaryService binaryService;

    @Inject
    ContainerService containerService;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    @Before
    public void setUp() throws RepositoryException {
        idTranslator = new DefaultIdentifierTranslator(getJcrSession(repo.login()));
    }

    @Test
    public void testCreatedDate() throws RepositoryException, InvalidChecksumException {
        FedoraSession session = repo.login();
        containerService.findOrCreate(session, "/testDatastreamObject");

        binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode1").setContent(
                new ByteArrayInputStream("asdf".getBytes()),
                "application/octet-stream",
                null,
                null,
                null
        );

        session.commit();
        session.expire();
        session = repo.login();
        final FedoraBinary ds = binaryService.findOrCreate(session,
                "/testDatastreamObject/testDatastreamNode1");
        assertNotNull("Couldn't find created date on datastream!", ds
                .getCreatedDate());
    }

    @Test
    public void testDatastreamContent() throws IOException,
            RepositoryException,
            InvalidChecksumException {
        final FedoraSession session = repo.login();
        containerService.findOrCreate(session, "/testDatastreamObject");

        binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode1").setContent(
                new ByteArrayInputStream("asdf".getBytes()),
                "application/octet-stream",
                null,
                null,
                null
        );

        session.commit();

        final FedoraBinary ds = binaryService.findOrCreate(session,
                "/testDatastreamObject/testDatastreamNode1");
        final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

        assertEquals("asdf", contentString);

    }

    @Test
    public void testDatastreamContentType() throws RepositoryException, InvalidChecksumException {
        final FedoraSession session = repo.login();
        try {
            containerService.findOrCreate(session, "/testDatastreamObject");

            binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode1").setContent(
                    new ByteArrayInputStream("asdf".getBytes()),
                    "some/mime-type; with=params",
                    null,
                    null,
                    null
                    );

            session.commit();

            final FedoraBinary ds = binaryService.findOrCreate(session,
                    "/testDatastreamObject/testDatastreamNode1");

            assertEquals("some/mime-type; with=params", ds.getMimeType());
        } finally {
            session.expire();
        }

    }

    @Test
    public void testDatastreamContentDigestAndLength() throws IOException, RepositoryException,
    InvalidChecksumException {
        final FedoraSession session = repo.login();
        try {
            containerService.findOrCreate(session, "/testDatastreamObject");

            binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode2").setContent(
                    new ByteArrayInputStream("asdf".getBytes()),
                    "application/octet-stream",
                    null,
                    null,
                    null
                    );

            session.commit();

            final FedoraBinary ds = binaryService.findOrCreate(session,
                    "/testDatastreamObject/testDatastreamNode2");
            assertEquals("urn:sha1:3da541559918a808c2402bba5012f6c60b27661c", ds
                    .getContentDigest().toString());
            assertEquals(4L, ds.getContentSize());

            final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

            assertEquals("asdf", contentString);
        } finally {
            session.expire();
        }
    }

    @Test
    public void
    testModifyDatastreamContentDigestAndLength() throws IOException, RepositoryException, InvalidChecksumException {
        final FedoraSession session = repo.login();
        try {
            containerService.findOrCreate(session, "/testDatastreamObject");

            final FedoraBinary orig = binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode3");
            orig.setContent(new ByteArrayInputStream("asdf".getBytes()), "application/octet-stream", null, null, null);
            session.commit();
            final long origMod = orig.getLastModifiedDate().toEpochMilli();

            final FedoraBinary ds = binaryService.findOrCreate(session,
                    "/testDatastreamObject/testDatastreamNode3");

            ds.setContent(new ByteArrayInputStream("0123456789".getBytes()), null, null, null, null);

            assertEquals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016", ds
                    .getContentDigest().toString());
            assertEquals(10L, ds.getContentSize());

            final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

            assertEquals("0123456789", contentString);
            assertTrue("Last-modified should be updated", ds.getLastModifiedDate().toEpochMilli() > origMod);
        } finally {
            session.expire();
        }
    }

    @Test
    public void testDatastreamContentWithChecksum() throws IOException, RepositoryException, InvalidChecksumException {
        final FedoraSession session = repo.login();
        try {
            containerService.findOrCreate(session, "/testDatastreamObject");

            binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode4").setContent(
                    new ByteArrayInputStream("asdf".getBytes()),
                    "application/octet-stream",
                    new HashSet<>(asList(asURI(SHA1.algorithm, "3da541559918a808c2402bba5012f6c60b27661c"))),
                    null,
                    null
                    );

            session.commit();

            final FedoraBinary ds = binaryService.findOrCreate(session,
                    "/testDatastreamObject/testDatastreamNode4");
            assertEquals("urn:sha1:3da541559918a808c2402bba5012f6c60b27661c", ds
                    .getContentDigest().toString());

            final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

            assertEquals("asdf", contentString);
        } finally {
            session.expire();
        }
    }

    @Test
    public void testDatastreamFileName() throws RepositoryException, InvalidChecksumException {
        final FedoraSession session = repo.login();
        try {
            containerService.findOrCreate(session, "/testDatastreamObject");

            binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode5").setContent(
                    new ByteArrayInputStream("asdf".getBytes()),
                    "application/octet-stream",
                    null,
                    "xyz.jpg",
                    null
                    );

            session.commit();

            final FedoraBinary ds = binaryService.findOrCreate(session,
                    "/testDatastreamObject/testDatastreamNode5");
            final String filename = ds.getFilename();

            assertEquals("xyz.jpg", filename);
        } finally {
            session.expire();
        }
    }

    @Test
    public void testChecksumBlobs() throws RepositoryException, InvalidChecksumException {
        final String pid = "testChecksumBlobs-" + randomUUID();
        final FedoraSession session = repo.login();
        try {

            containerService.findOrCreate(session, pid);

            binaryService.findOrCreate(session, pid + "/testRepositoryContent").setContent(
                    new ByteArrayInputStream("01234567890123456789012345678901234567890123456789".getBytes()),
                    "text/plain",
                    null,
                    "numbers.txt",
                    null
                    );

            session.commit();

            final FedoraBinary ds = binaryService.findOrCreate(session, pid + "/testRepositoryContent");

            final Model fixityResults = ds.getFixity(idTranslator).collect(toModel());

            assertNotEquals(0, fixityResults.size());

            assertTrue("Expected to find checksum",
                    fixityResults.contains(null,
                            HAS_MESSAGE_DIGEST,
                            createResource("urn:sha1:9578f951955d37f20b601c26591e260c1e5389bf")));

            assertEquals("Expected to find mime type",
                    getJcrNode(ds).getProperty("ebucore:hasMimeType").getString(), "text/plain");
            assertEquals("Expected to find file name",
                    getJcrNode(ds).getProperty("ebucore:filename").getString(), "numbers.txt");
        } finally {
            session.expire();
        }
    }

    @Test
    public void testChecksumBlobsForInMemoryValues() throws RepositoryException, InvalidChecksumException {

        final FedoraSession session = repo.login();
        try {
            containerService.findOrCreate(session, "/testLLObject");
            binaryService.findOrCreate(session, "/testLLObject/testMemoryContent").setContent(
                    new ByteArrayInputStream("0123456789".getBytes()),
                    "application/octet-stream",
                    null,
                    null,
                    null
                    );

            session.commit();

            final FedoraBinary ds = binaryService.findOrCreate(session, "/testLLObject/testMemoryContent");

            final Model fixityResults = ds.getFixity(idTranslator).collect(toModel());

            assertNotEquals(0, fixityResults.size());

            assertTrue("Expected to find checksum",
                    fixityResults.contains(null,
                            HAS_MESSAGE_DIGEST,
                            createResource("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016")));
        } finally {
            session.expire();
        }
    }

    @Test
    public void testChecksumBlobsForValuesWithoutChecksums() throws RepositoryException {

        final FedoraSession session = repo.login();
        final Session jcrSession = getJcrSession(session);
        try {
            final ValueFactory factory = jcrSession.getValueFactory();
            final Container object = containerService.findOrCreate(session, "/testLLObject");

            final Node testRandomContentNode = getJcrNode(object).addNode("testRandomContent", NT_FILE);
            testRandomContentNode.addMixin(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
            final Node testRandomContent = testRandomContentNode.addNode(JCR_CONTENT, NT_RESOURCE);
            testRandomContent.addMixin(FEDORA_BINARY);
            testRandomContent.setProperty(JCR_DATA,
                    factory.createBinary(new ByteArrayInputStream("0123456789".getBytes())));

            session.commit();

            final FedoraBinary ds = binaryService.findOrCreate(session, "/testLLObject/testRandomContent");

            final Model fixityResults = ds.getFixity(idTranslator).collect(toModel());

            assertNotEquals(0, fixityResults.size());

            assertTrue("Expected to find checksum",
                    fixityResults.contains(null,
                            HAS_MESSAGE_DIGEST,
                            createResource("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016")));
        } finally {
            session.expire();
        }
    }

    @Test
    public void testModifyDatastreamDescriptionLastMod() throws RepositoryException, InvalidChecksumException {
        final FedoraSession session = repo.login();
        try {
            containerService.findOrCreate(session, "/testDatastreamObject");

            final FedoraBinary orig = binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode6");
            orig.setContent(new ByteArrayInputStream("asdf".getBytes()), "application/octet-stream", null, null, null);
            session.commit();
            final long origMod = orig.getLastModifiedDate().toEpochMilli();

            final FedoraResource description = orig.getDescription();
            final long origDescriptionMod = description.getLastModifiedDate().toEpochMilli();

            description.updateProperties(idTranslator, "INSERT { <> <info:fcrepo/foo> \"b\" } WHERE {}",
                    description.getTriples(idTranslator, PROPERTIES));
            session.commit();

            final FedoraBinary ds = binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode6");

            final long modMod = ds.getLastModifiedDate().toEpochMilli();
            final long modDescMod = ds.getDescription().getLastModifiedDate().toEpochMilli();

            assertEquals("Last-modified on the binary should be the same", modMod,  origMod);
            assertNotEquals("Last-modified on the description should not be the same", modDescMod, origDescriptionMod);

            ds.setContent(new ByteArrayInputStream("0123456789".getBytes()), null, null, null, null);

            assertNotEquals("Last-modified on the binary should have changed",
                    ds.getLastModifiedDate().toEpochMilli(), modMod);
            assertNotEquals("Last-modified on the description should have changed",
                    ds.getDescription().getLastModifiedDate().toEpochMilli(), modDescMod);

        } finally {
            session.expire();
        }
    }


}
