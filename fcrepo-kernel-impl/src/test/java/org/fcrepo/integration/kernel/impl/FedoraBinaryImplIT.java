/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.kernel.impl;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.UUID.randomUUID;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.RdfLexicon.HAS_MESSAGE_DIGEST;
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
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import org.apache.commons.io.IOUtils;

import org.fcrepo.kernel.models.FedoraBinary;
import org.fcrepo.kernel.models.Container;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.services.BinaryService;
import org.fcrepo.kernel.services.ContainerService;
import org.fcrepo.kernel.utils.ContentDigest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * <p>{@link org.fcrepo.integration.kernel.impl.FedoraBinaryImplIT} class.</p>
 *
 * @author ksclarke
 * @author ajs6f
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraBinaryImplIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    BinaryService binaryService;

    @Inject
    ContainerService containerService;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    @Before
    public void setUp() throws RepositoryException {
        idTranslator = new DefaultIdentifierTranslator(repo.login());
    }

    @Test
    public void testCreatedDate() throws RepositoryException, InvalidChecksumException {
        Session session = repo.login();
        containerService.findOrCreate(session, "/testDatastreamObject");

        binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode1").setContent(
                new ByteArrayInputStream("asdf".getBytes()),
                "application/octet-stream",
                null,
                null,
                null
        );

        session.save();
        session.logout();
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
        final Session session = repo.login();
        containerService.findOrCreate(session, "/testDatastreamObject");

        binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode1").setContent(
                new ByteArrayInputStream("asdf".getBytes()),
                "application/octet-stream",
                null,
                null,
                null
        );

        session.save();

        final FedoraBinary ds = binaryService.findOrCreate(session,
                "/testDatastreamObject/testDatastreamNode1");
        final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

        assertEquals("asdf", contentString);

    }

    @Test
    public void testDatastreamContentType() throws RepositoryException, InvalidChecksumException {
        final Session session = repo.login();
        try {
            containerService.findOrCreate(session, "/testDatastreamObject");

            binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode1").setContent(
                    new ByteArrayInputStream("asdf".getBytes()),
                    "some/mime-type; with=params",
                    null,
                    null,
                    null
                    );

            session.save();

            final FedoraBinary ds = binaryService.findOrCreate(session,
                    "/testDatastreamObject/testDatastreamNode1");

            assertEquals("some/mime-type; with=params", ds.getMimeType());
        } finally {
            session.logout();
        }

    }

    @Test
    public void testDatastreamContentDigestAndLength() throws IOException, RepositoryException,
    InvalidChecksumException {
        final Session session = repo.login();
        try {
            containerService.findOrCreate(session, "/testDatastreamObject");

            binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode2").setContent(
                    new ByteArrayInputStream("asdf".getBytes()),
                    "application/octet-stream",
                    null,
                    null,
                    null
                    );

            session.save();

            final FedoraBinary ds = binaryService.findOrCreate(session,
                    "/testDatastreamObject/testDatastreamNode2");
            assertEquals("urn:sha1:3da541559918a808c2402bba5012f6c60b27661c", ds
                    .getContentDigest().toString());
            assertEquals(4L, ds.getContentSize());

            final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

            assertEquals("asdf", contentString);
        } finally {
            session.logout();
        }
    }

    @Test
    public void
    testModifyDatastreamContentDigestAndLength() throws IOException, RepositoryException, InvalidChecksumException {
        final Session session = repo.login();
        try {
            containerService.findOrCreate(session, "/testDatastreamObject");

            binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode3").setContent(
                    new ByteArrayInputStream("asdf".getBytes()),
                    "application/octet-stream",
                    null,
                    null,
                    null
                    );

            session.save();

            final FedoraBinary ds = binaryService.findOrCreate(session,
                    "/testDatastreamObject/testDatastreamNode3");

            ds.setContent(new ByteArrayInputStream("0123456789".getBytes()), null, null, null, null);

            assertEquals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016", ds
                    .getContentDigest().toString());
            assertEquals(10L, ds.getContentSize());

            final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

            assertEquals("0123456789", contentString);
        } finally {
            session.logout();
        }
    }

    @Test
    public void testDatastreamContentWithChecksum() throws IOException, RepositoryException, InvalidChecksumException {
        final Session session = repo.login();
        try {
            containerService.findOrCreate(session, "/testDatastreamObject");

            binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode4").setContent(
                    new ByteArrayInputStream("asdf".getBytes()),
                    "application/octet-stream",
                    ContentDigest.asURI("SHA-1", "3da541559918a808c2402bba5012f6c60b27661c"),
                    null,
                    null
                    );

            session.save();

            final FedoraBinary ds = binaryService.findOrCreate(session,
                    "/testDatastreamObject/testDatastreamNode4");
            assertEquals("urn:sha1:3da541559918a808c2402bba5012f6c60b27661c", ds
                    .getContentDigest().toString());

            final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

            assertEquals("asdf", contentString);
        } finally {
            session.logout();
        }
    }

    @Test
    public void testDatastreamFileName() throws RepositoryException, InvalidChecksumException {
        final Session session = repo.login();
        try {
            containerService.findOrCreate(session, "/testDatastreamObject");

            binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode5").setContent(
                    new ByteArrayInputStream("asdf".getBytes()),
                    "application/octet-stream",
                    null,
                    "xyz.jpg",
                    null
                    );

            session.save();

            final FedoraBinary ds = binaryService.findOrCreate(session,
                    "/testDatastreamObject/testDatastreamNode5");
            final String filename = ds.getFilename();

            assertEquals("xyz.jpg", filename);
        } finally {
            session.logout();
        }
    }

    @Test
    public void testChecksumBlobs() throws RepositoryException, InvalidChecksumException {
        final String pid = "testChecksumBlobs-" + randomUUID();
        final Session session = repo.login();
        try {

            containerService.findOrCreate(session, pid);

            binaryService.findOrCreate(session, pid + "/testRepositoryContent").setContent(
                    new ByteArrayInputStream("01234567890123456789012345678901234567890123456789".getBytes()),
                    "application/octet-stream",
                    null,
                    null,
                    null
                    );

            session.save();

            final FedoraBinary ds = binaryService.findOrCreate(session, pid + "/testRepositoryContent");

            final Model fixityResults = ds.getFixity(idTranslator).asModel();

            assertNotEquals(0, fixityResults.size());

            assertTrue("Expected to find checksum",
                    fixityResults.contains(null,
                            HAS_MESSAGE_DIGEST,
                            createResource("urn:sha1:9578f951955d37f20b601c26591e260c1e5389bf")));
        } finally {
            session.logout();
        }
    }

    @Test
    public void testChecksumBlobsForInMemoryValues() throws RepositoryException, InvalidChecksumException {

        final Session session = repo.login();
        try {
            containerService.findOrCreate(session, "/testLLObject");
            binaryService.findOrCreate(session, "/testLLObject/testMemoryContent").setContent(
                    new ByteArrayInputStream("0123456789".getBytes()),
                    "application/octet-stream",
                    null,
                    null,
                    null
                    );

            session.save();

            final FedoraBinary ds = binaryService.findOrCreate(session, "/testLLObject/testMemoryContent");

            final Model fixityResults = ds.getFixity(idTranslator).asModel();

            assertNotEquals(0, fixityResults.size());

            assertTrue("Expected to find checksum",
                    fixityResults.contains(null,
                            HAS_MESSAGE_DIGEST,
                            createResource("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016")));
        } finally {
            session.logout();
        }
    }

    @Test
    public void testChecksumBlobsForValuesWithoutChecksums() throws RepositoryException {

        final Session session = repo.login();
        try {
            final ValueFactory factory = session.getValueFactory();
            final Container object = containerService.findOrCreate(session, "/testLLObject");

            final Node testRandomContentNode = object.getNode().addNode("testRandomContent", NT_FILE);
            testRandomContentNode.addMixin(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
            final Node testRandomContent = testRandomContentNode.addNode(JCR_CONTENT, NT_RESOURCE);
            testRandomContent.addMixin(FEDORA_BINARY);
            testRandomContent.setProperty(JCR_DATA,
                    factory.createBinary(new ByteArrayInputStream("0123456789".getBytes())));

            session.save();

            final FedoraBinary ds = binaryService.findOrCreate(session, "/testLLObject/testRandomContent");

            final Model fixityResults = ds.getFixity(idTranslator).asModel();

            assertNotEquals(0, fixityResults.size());

            assertTrue("Expected to find checksum",
                    fixityResults.contains(null,
                            HAS_MESSAGE_DIGEST,
                            createResource("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016")));
        } finally {
            session.logout();
        }
    }
}
