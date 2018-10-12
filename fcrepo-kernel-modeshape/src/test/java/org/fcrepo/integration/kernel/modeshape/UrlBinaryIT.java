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

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.utils.ContentDigest.asURI;
import static org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM.SHA1;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static java.util.Collections.singletonList;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * @author bbpennel
 */
@ContextConfiguration({ "/spring-test/fcrepo-config.xml" })
public class UrlBinaryIT extends AbstractIT {

    private static final String EXPECTED_CONTENT = "test content";

    private static final String CONTENT_SHA1 = "1eebdf4fdc9fc7bf283031b93f9aef3338de9052";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    private String fileUrl;

    @Inject
    private FedoraRepository repo;

    @Inject
    private BinaryService binaryService;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    private FedoraSession session;

    private String mimeType;

    private String dsId;

    @Before
    public void setup() {
        session = repo.login();

        fileUrl = "http://localhost:" + wireMockRule.port() + "/file.txt";

        mimeType = "application/octet-stream";

        dsId = makeDsId();

        stubFor(head(urlEqualTo("/file.txt"))
                .willReturn(aResponse()
                        .withHeader("Content-Length", Long.toString(EXPECTED_CONTENT.length()))
                        .withHeader("Content-Type", "text/plain")));
        stubFor(get(urlEqualTo("/file.txt"))
                .willReturn(aResponse()
                        .withHeader("Content-Length", Long.toString(EXPECTED_CONTENT.length()))
                        .withHeader("Content-Type", "text/plain")
                        .withBody(EXPECTED_CONTENT)));


        idTranslator = new DefaultIdentifierTranslator(getJcrSession(repo.login()));
    }

    @After
    public void after() {
        session.expire();
    }

    @Test
    public void testDatastream() throws Exception {
        final FedoraBinary binary = binaryService.findOrCreate(session, "/externalContent");
        binary.setExternalContent(mimeType, null, null, "proxy", fileUrl);

        assertTrue(binary.isProxy());
        assertFalse(binary.isRedirect());
        session.commit();

        final FedoraBinary bin = binaryService.findOrCreate(session, "/externalContent");

        assertTrue(bin.isProxy());
        assertFalse(bin.isRedirect());

        assertEquals(EXPECTED_CONTENT.length(), bin.getContentSize());
        assertEquals(EXPECTED_CONTENT, contentString(bin));

        assertEquals(mimeType, bin.getMimeType());
    }

    @Test
    public void testDatastreamWithMimeType() throws Exception {

        final String mt = "text/plain";
        final FedoraBinary binary = binaryService.findOrCreate(session, dsId);
        binary.setExternalContent(mt, null, null, "proxy", fileUrl);

        session.commit();

        final FedoraBinary ds = binaryService.findOrCreate(session, dsId);

        assertEquals(EXPECTED_CONTENT, contentString(ds));

        assertEquals(mt, ds.getMimeType());
    }

    @Test
    public void testWithValidChecksum() throws Exception {
        final FedoraBinary binary = binaryService.findOrCreate(session, dsId);
        binary.setExternalContent(mimeType, sha1Set(CONTENT_SHA1), null, "proxy", fileUrl);

        session.commit();

        final FedoraBinary ds = binaryService.findOrCreate(session, dsId);

        assertEquals(EXPECTED_CONTENT, contentString(ds));
    }

    @Test(expected = InvalidChecksumException.class)
    public void testWithInvalidChecksum() throws Exception {
        final FedoraBinary binary = binaryService.findOrCreate(session, dsId);
        binary.setExternalContent(mimeType, sha1Set("badsum"), null, "proxy", fileUrl);
    }

    @Test
    public void testCheckFixity() throws Exception {
        final FedoraBinary binary = binaryService.findOrCreate(session, dsId);
        binary.setExternalContent(mimeType, sha1Set(CONTENT_SHA1), null, "proxy", fileUrl);

        session.commit();

        final FedoraBinary ds = binaryService.findOrCreate(session, dsId);
        final Collection<URI> fixityResults = ds.checkFixity(idTranslator, singletonList("SHA"));

        assertNotEquals(0, fixityResults.size());

        final String checksum = fixityResults.iterator().next().toString();
        assertEquals("Fixity Checksum doesn't match",
                "urn:sha1:" + CONTENT_SHA1, checksum);
    }

    @Test
    public void testGetFixity() throws Exception {
        final FedoraBinary binary = binaryService.findOrCreate(session, dsId);
        binary.setExternalContent(mimeType, sha1Set(CONTENT_SHA1), null,"proxy", fileUrl);

        session.commit();

        final FedoraBinary ds = binaryService.findOrCreate(session, dsId);
        final Model fixityResults = ds.getFixity(idTranslator).collect(toModel());

        assertNotEquals(0, fixityResults.size());

        assertTrue("Expected to find checksum",
                fixityResults.contains(null,
                        HAS_MESSAGE_DIGEST,
                        createResource("urn:sha1:" + CONTENT_SHA1)));
    }

    private String makeDsId() {
        return "/ds_" + UUID.randomUUID().toString();
    }

    private String contentString(final FedoraBinary ds) throws Exception {
        return IOUtils.toString(ds.getContent(), "UTF-8");
    }

    private Set<URI> sha1Set(final String checksum) {
        return new HashSet<>(singletonList(asURI(SHA1.algorithm, checksum)));
    }
}
