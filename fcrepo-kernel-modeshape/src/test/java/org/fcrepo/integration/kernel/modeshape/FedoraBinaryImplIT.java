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
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.fcrepo.kernel.api.exception.UnsupportedAccessTypeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
            final Instant origMod = orig.getLastModifiedDate();

            final FedoraBinary ds = binaryService.findOrCreate(session,
                    "/testDatastreamObject/testDatastreamNode3");

            ds.setContent(new ByteArrayInputStream("0123456789".getBytes()), null, null, null, null);

            assertEquals("urn:sha1:87acec17cd9dcd20a716cc2cf67417b71c8a7016", ds
                    .getContentDigest().toString());
            assertEquals(10L, ds.getContentSize());

            final String contentString = IOUtils.toString(ds.getContent(), "ASCII");

            assertEquals("0123456789", contentString);
            assertTrue("Last-modified should be updated", ds.getLastModifiedDate().isAfter(origMod));
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
            final FedoraResource description = ds.getDescription();

            final Model fixityResults = ds.getFixity(idTranslator).collect(toModel());

            assertNotEquals(0, fixityResults.size());

            assertTrue("Expected to find checksum",
                    fixityResults.contains(null,
                            HAS_MESSAGE_DIGEST,
                            createResource("urn:sha1:9578f951955d37f20b601c26591e260c1e5389bf")));

            assertEquals("Expected to find mime type",
                    getJcrNode(description).getProperty("ebucore:hasMimeType").getString(), "text/plain");
            assertEquals("Expected to find file name",
                    getJcrNode(description).getProperty("ebucore:filename").getString(), "numbers.txt");
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
            containerService.findOrCreate(session, "/testLLObject");

            session.commit();

            final FedoraBinary ds = binaryService.findOrCreate(session, "/testLLObject/testRandomContent");

            final Node contentNode = getJcrNode(ds);
            contentNode.setProperty(JCR_DATA,
                    factory.createBinary(new ByteArrayInputStream("0123456789".getBytes())));

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
    public void testExceptionGetFixityWithWantDigest() throws RepositoryException, InvalidChecksumException,
            UnsupportedAlgorithmException, UnsupportedAccessTypeException {
        final Collection<String> digestAlgs = Collections.singletonList("sha256");
        final String pid = "testFixityWithWantDigest-" + randomUUID();
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

            // should thrown for unsupported digest algorithm sha256
            thrown.expect(UnsupportedAlgorithmException.class);

            ds.checkFixity(idTranslator, digestAlgs);
        } finally {
            session.expire();
        }
    }

    @Test
    public void testGetFixityWithWantDigest() throws RepositoryException, InvalidChecksumException,
            URISyntaxException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {
        final Collection<String> digestAlgs = Collections.singletonList("SHA");
        final String pid = "testFixityWithWantDigest-" + randomUUID();
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

            final Collection<URI> fixityResults1 = ds.checkFixity(idTranslator, digestAlgs);

            assertNotEquals(0, fixityResults1.size());

            final String checksum = fixityResults1.toArray()[0].toString();

            assertTrue("Fixity Checksum doesn't match",
                checksum.equals("urn:sha1:9578f951955d37f20b601c26591e260c1e5389bf"));
        } finally {
            session.expire();
        }
    }

    @Test
    public void testGetFixityWithWantDigestMultuple() throws RepositoryException, InvalidChecksumException,
            URISyntaxException, UnsupportedAlgorithmException, UnsupportedAccessTypeException {
        final String[] digestAlgValues = {"SHA", "md5"};
        final Collection<String> digestAlgs = Arrays.asList(digestAlgValues);
        final String pid = "testFixityWithWantDigestMultiple-" + randomUUID();
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

            final Collection<URI> fixityResults = ds.checkFixity(idTranslator, digestAlgs);

            assertEquals(2, fixityResults.size());

            assertTrue("SHA-1 fixity checksum doesn't match",
                    fixityResults.contains(new URI("urn:sha1:9578f951955d37f20b601c26591e260c1e5389bf")));
            assertTrue("MD5 fixity checksum doesn't match",
                    fixityResults.contains(new URI("urn:md5:baed005300234f3d1503c50a48ce8e6f")));
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

            final FedoraResource description = orig.getDescription();

            final Instant origMod = orig.getLastModifiedDate();
            final Instant origDescriptionMod = description.getLastModifiedDate();

            description.updateProperties(idTranslator, "INSERT { <> <info:fcrepo/foo> \"b\" } WHERE {}",
                    description.getTriples(idTranslator, PROPERTIES));
            session.commit();

            final FedoraBinary ds = binaryService.findOrCreate(session, "/testDatastreamObject/testDatastreamNode6");

            final Instant modMod = ds.getLastModifiedDate();
            final Instant modDescMod = ds.getDescription().getLastModifiedDate();

            assertEquals("Last-modified on the binary should be the same", modMod, origMod);
            assertNotEquals("Last-modified on the description should not be the same", modDescMod, origDescriptionMod);

            ds.setContent(new ByteArrayInputStream("0123456789".getBytes()), null, null, null, null);

            assertNotEquals("Last-modified on the binary should have changed", ds.getLastModifiedDate(), modMod);
            assertNotEquals("Last-modified on the description should have changed",
                    ds.getDescription().getLastModifiedDate(), modDescMod);

        } finally {
            session.expire();
        }
    }


}
