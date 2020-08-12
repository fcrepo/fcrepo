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
package org.fcrepo.integration.http.api;

import static java.lang.Thread.sleep;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneId.of;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.PARTIAL_CONTENT;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static nu.validator.htmlparser.common.DoctypeExpectation.NO_DOCTYPE_ERRORS;
import static nu.validator.htmlparser.common.XmlViolationPolicy.ALLOW;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.entity.ContentType.parse;
import static org.apache.http.impl.client.cache.CacheConfig.DEFAULT;
import static org.apache.jena.datatypes.TypeMapper.getInstance;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDinteger;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ModelFactory.createModelForGraph;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.WebContent.contentTypeN3;
import static org.apache.jena.riot.WebContent.contentTypeN3Alt2;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.apache.jena.riot.WebContent.contentTypeTurtle;
import static org.apache.jena.vocabulary.DC_11.title;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_VARIANTS;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_TOMBSTONE;
import static org.fcrepo.kernel.api.RdfLexicon.ARCHIVAL_GROUP;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_ORIGINAL_NAME;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.IS_MEMBER_OF_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEGATE_TYPE;
import static org.fcrepo.kernel.api.models.ExternalContent.COPY;
import static org.fcrepo.kernel.api.models.ExternalContent.PROXY;
import static org.fcrepo.kernel.api.models.ExternalContent.REDIRECT;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.http.commons.domain.RDFMediaType;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.test.context.TestExecutionListeners;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;

import nu.validator.htmlparser.sax.HtmlParser;
import nu.validator.saxtree.TreeBuilder;

/**
 * @author cabeer
 * @author ajs6f
 */
@TestExecutionListeners(
        listeners = { LinuxTestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraLdpIT extends AbstractResourceIT {

    private static final Node DC_IDENTIFIER = DC_11.identifier.asNode();

    private static final String LDP_RESOURCE_LINK_HEADER = "<" + LDP_NAMESPACE + "Resource>; rel=\"type\"";

    private static final Node rdfType = type.asNode();

    private static final Node DCTITLE = title.asNode();

    private static final Resource PCDM_FILE_TYPE = createResource("http://pcdm.org/models#File");

    private static final String CONTAINER_LINK_HEADER = "<" + CONTAINER.getURI() + ">; rel=\"type\"";

    private static final String BASIC_CONTAINER_LINK_HEADER = "<" + BASIC_CONTAINER.getURI() + ">; rel=\"type\"";
    private static final String DIRECT_CONTAINER_LINK_HEADER = "<" + DIRECT_CONTAINER.getURI() + ">; rel=\"type\"";
    private static final String INDIRECT_CONTAINER_LINK_HEADER = "<" + INDIRECT_CONTAINER.getURI() + ">; rel=\"type\"";
    private static final String ARCHIVAL_GROUP_LINK_HEADER = "<" + ARCHIVAL_GROUP.getURI() + ">; rel=\"type\"";

    private static final String RESOURCE_LINK_HEADER = "<" + RESOURCE.getURI() + ">; rel=\"type\"";
    private static final String RDF_SOURCE_LINK_HEADER = "<" + RDF_SOURCE.getURI() + ">; rel=\"type\"";
    private static final String NON_RDF_SOURCE_LINK_HEADER = "<" + NON_RDF_SOURCE.getURI() + ">; rel=\"type\"";
    private static final String VERSIONED_RESOURCE_LINK_HEADER = "<" + VERSIONED_RESOURCE.getURI() + ">; rel=\"type\"";

    private static final String TEST_ACTIVATION_PROPERTY = "RUN_TEST_CREATE_MANY";

    private static final String WANT_DIGEST = "Want-Digest";

    private static final String DIGEST = "Digest";

    private static final String TEST_BINARY_CONTENT = "01234567890123456789012345678901234567890123456789";

    private static final String TEST_SHA_DIGEST_HEADER_VALUE = "sha=9578f951955d37f20b601c26591e260c1e5389bf";

    private static final String TEST_MD5_DIGEST_HEADER_VALUE = "md5=baed005300234f3d1503c50a48ce8e6f";

    private static final String TEST_SHA256_DIGEST_HEADER_VALUE =
            "sha-256=fb871ff8cce8fea83dfaeab41784305a1461e008dc02a371ed26d856c766c903";

    private static final Logger LOGGER = getLogger(FedoraLdpIT.class);

    private static final DateTimeFormatter headerFormat =
            RFC_1123_DATE_TIME.withLocale(Locale.US).withZone(ZoneId.of("GMT"));

    private static final DateTimeFormatter tripleFormat =
      DateTimeFormatter.ISO_INSTANT.withZone(of("GMT"));

    @Test
    public void testHeadRepositoryGraph() throws IOException {
        final HttpHead headObjMethod = new HttpHead(serverAddress);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
          assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
          checkForLinkHeader(response, serverAddress + FCR_ACL, "acl");
        }
    }

    @Test
    public void testHeadObject() throws IOException {
        final String id = getRandomUniqueId();
        createObject(id).close();
        final HttpHead headObjMethod = headObjMethod(id);
        assertEquals(OK.getStatusCode(), getStatus(headObjMethod));
    }

    @Test
    public void testHeadDefaultContainer() throws IOException {
        final String id = getRandomUniqueId();
        createObject(id).close();
        final HttpHead headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            checkForLinkHeader(response, RDF_SOURCE.getURI(), "type");
            assertTrue("Didn't find LDP container link header!", getLinkHeaders(response).contains(
                    BASIC_CONTAINER_LINK_HEADER));
        }
    }

    @Test
    public void testHeadBasicContainer() throws IOException {
        final String id = getRandomUniqueId();

        createObjectAndClose(id, BASIC_CONTAINER_LINK_HEADER);

        final HttpHead headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            final Collection<String> links = getLinkHeaders(response);
            checkForLinkHeader(response, RDF_SOURCE.getURI(), "type");
            assertTrue("Didn't find LDP container link header!", links.contains(BASIC_CONTAINER_LINK_HEADER));
        }
    }

    @Test
    public void testCreateArchivalGroup() throws Exception {
        final var id = getRandomUniqueId();
        final var childId = id + "/child";
        final var grandChildId = childId + "/grandchild";

        createObjectAndClose(id, BASIC_CONTAINER_LINK_HEADER, ARCHIVAL_GROUP_LINK_HEADER);

        final var headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            final Collection<String> links = getLinkHeaders(response);
            assertTrue("Didn't find ArchivalGroup link header!", links.contains(ARCHIVAL_GROUP_LINK_HEADER));
            assertTrue("Didn't find LDP container link header!", links.contains(BASIC_CONTAINER_LINK_HEADER));
        }


        executeAndClose(putObjMethod(childId, "text/turtle", "<> a <http://example.com/Foo> ."));

        final var childHeadObjMethod = headObjMethod(childId);
        try (final CloseableHttpResponse response = execute(childHeadObjMethod)) {
            final Collection<String> links = getLinkHeaders(response);
            assertTrue("Didn't find LDP container link header!", links.contains(BASIC_CONTAINER_LINK_HEADER));
            assertFalse("Unexpectedly found ArchivalGroup link header!",
                    links.contains(ARCHIVAL_GROUP_LINK_HEADER));
        }

        executeAndClose(putObjMethod(grandChildId, "text/turtle", "<> a <http://example.com/Foo> ."));

        final var grandChildHeadObjMethod = headObjMethod(grandChildId);
        try (final CloseableHttpResponse response = execute(grandChildHeadObjMethod)) {
            final Collection<String> links = getLinkHeaders(response);
            assertTrue("Didn't find LDP container link header!", links.contains(BASIC_CONTAINER_LINK_HEADER));
            assertFalse("Unexpectedly found ArchivalGroup link header!",
                    links.contains(ARCHIVAL_GROUP_LINK_HEADER));
        }
    }

    @Test
    public void testCreateArchivalGroupWithinAnArchivalGroupFails() throws Exception {
        final var id = getRandomUniqueId();
        final var childId = id + "/child";

        createObjectAndClose(id, BASIC_CONTAINER_LINK_HEADER, ARCHIVAL_GROUP_LINK_HEADER);

        final var headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            final Collection<String> links = getLinkHeaders(response);
            assertTrue("Didn't find ArchivalGroup link header!", links.contains(ARCHIVAL_GROUP_LINK_HEADER));
            assertTrue("Didn't find LDP container link header!", links.contains(BASIC_CONTAINER_LINK_HEADER));
        }

        final var putObjMethod = putObjMethod(childId, "text/turtle", "<> a <http://example.com/Foo> .");
        putObjMethod.setHeader("Link", ARCHIVAL_GROUP_LINK_HEADER);
        try (final CloseableHttpResponse response = execute(putObjMethod)) {
            assertEquals("Expected Conflict response", CONFLICT.getStatusCode(),
                    response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testCreateBinaryAsArchivalGroupWithPostFails() throws Exception {
        final var id = getRandomUniqueId();
        final var postMethod = postObjMethod(id);
        postMethod.setEntity(new StringEntity("test"));
        postMethod.addHeader("Link", ARCHIVAL_GROUP_LINK_HEADER);
        postMethod.addHeader("Link", NON_RDF_SOURCE_LINK_HEADER);
        postMethod.addHeader("Content-Type", "text/plain");

        try (final CloseableHttpResponse response = execute(postMethod)) {
            assertEquals("Expected Bad Request response", BAD_REQUEST.getStatusCode(),
                    response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testCreateBinaryAsArchivalGroupWithPutFails() throws Exception {
        final var id = getRandomUniqueId();
        final var putMethod = putObjMethod(id, "text/plain", "testcontent");
        putMethod.addHeader("Link", ARCHIVAL_GROUP_LINK_HEADER);
        putMethod.addHeader("Link", NON_RDF_SOURCE_LINK_HEADER);

        try (final CloseableHttpResponse response = execute(putMethod)) {
            assertEquals("Expected Bad Request response", BAD_REQUEST.getStatusCode(),
                    response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testHeadTurtleContentType() throws IOException {
        testHeadDefaultContentType(RDFMediaType.TURTLE_WITH_CHARSET);
    }

    @Test
    public void testHeadRDFContentType() throws IOException {
        testHeadDefaultContentType(RDFMediaType.RDF_XML);
    }

    @Test
    public void testHeadJSONLDContentType() throws IOException {
        testHeadDefaultContentType(RDFMediaType.JSON_LD);
    }

    @Test
    public void testHeadDefaultContentType() throws IOException {
        testHeadDefaultContentType(null);
    }

    private void testHeadDefaultContentType(final String mimeType) throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final HttpHead headObjMethod = headObjMethod(id);
        String mt = mimeType;
        if (mt != null) {
            headObjMethod.addHeader("Accept", mt);
        } else {
            mt = RDFMediaType.TURTLE_WITH_CHARSET;
        }
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            final Collection<String> contentTypes = getHeader(response, CONTENT_TYPE);
            final String contentType = contentTypes.iterator().next();
            assertTrue("Didn't find LDP valid content-type header: " + contentType +
                    "; expected result: " + mt, contentType.contains(mt));
            testHeadVaryAndPreferHeaders(response);
        }
    }

    private void testHeadVaryAndPreferHeaders(final CloseableHttpResponse response) {
        final Collection<String> preferenceApplied = getHeader(response, "Preference-Applied");
        final Collection<String> vary = getHeader(response, "Vary");
        assertTrue("Didn't find valid Preference-Applied header", preferenceApplied.contains("return=representation"));
        assertTrue("Didn't find valid Vary Prefer header", vary.contains("Prefer"));
        assertTrue("Didn't find valid Vary header",
                vary.contains("Accept"));
        assertTrue("Didn't find valid Vary header",
            vary.contains("Range"));
        assertTrue("Didn't find valid Vary header",
            vary.contains("Accept-Encoding"));
        assertTrue("Didn't find valid Vary header",
            vary.contains("Accept-Language"));

    }

    @Test
    public void testHeadDefaultRDF() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id, "text/turtle", "<> a <http://example.com/Foo> .");
        executeAndClose(put);

        final HttpHead headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            final Collection<String> links = getLinkHeaders(response);
            assertTrue("Didn't find LDP BasicContainer link header!", links.contains(BASIC_CONTAINER_LINK_HEADER));
            testHeadVaryAndPreferHeaders(response);
        }
    }

    @Test
    public void testHeadDefaultNonRDF() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id, "text/plain", "<> a <http://example.com/Foo> .");
        executeAndClose(put);

        final HttpHead headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            final Collection<String> links = getLinkHeaders(response);
            assertTrue("Didn't find LDP NonRDFSource link header!", links.contains(NON_RDF_SOURCE_LINK_HEADER));
        }
    }

    @Test
@Ignore
    public void testHeadDirectContainer() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final HttpHead headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            final Collection<String> links = getLinkHeaders(response);
            checkForLinkHeader(response, RDF_SOURCE.getURI(), "type");
            assertTrue("Didn't find LDP container link header!", links.contains(DIRECT_CONTAINER_LINK_HEADER));
            testHeadVaryAndPreferHeaders(response);
        }
    }

    @Test
@Ignore
    public void testHeadIndirectContainer() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final HttpHead headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            final Collection<String> links = getLinkHeaders(response);
            assertTrue("Didn't find LDP container link header!", links.contains(INDIRECT_CONTAINER_LINK_HEADER));
            testHeadVaryAndPreferHeaders(response);
        }
    }

    @Test
    public void testHeadDatastream() throws IOException, ParseException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "123");

        final HttpHead headObjMethod = headObjMethod(id + "/x");
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertEquals(TEXT_PLAIN, response.getFirstHeader(CONTENT_TYPE).getValue());
            assertEquals("3", response.getFirstHeader(CONTENT_LENGTH).getValue());
            assertEquals("bytes", response.getFirstHeader("Accept-Ranges").getValue());
            final ContentDisposition disposition =
                    new ContentDisposition(response.getFirstHeader(CONTENT_DISPOSITION).getValue());
            assertEquals("attachment", disposition.getType());
        }
    }

    @Test
    public void testHeadDatastreamWithWantDigest() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", TEST_BINARY_CONTENT);

        final HttpHead headObjMethod = headObjMethod(id + "/x");
        headObjMethod.addHeader(WANT_DIGEST, "SHA");
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertEquals(TEXT_PLAIN, response.getFirstHeader(CONTENT_TYPE).getValue());
            assertTrue(response.getHeaders(DIGEST).length > 0);
            final String digesterHeaderValue = response.getHeaders(DIGEST)[0].getValue();
            assertTrue("Fixity Checksum doesn't match",
                    digesterHeaderValue.equals(TEST_SHA_DIGEST_HEADER_VALUE));
        }
    }

    @Test
    public void testHeadDatastreamWithWantDigestMultiple() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", TEST_BINARY_CONTENT);

        final HttpHead headObjMethod = headObjMethod(id + "/x");
        headObjMethod.addHeader(WANT_DIGEST, "SHA, md5;q=0.3");
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertEquals(TEXT_PLAIN, response.getFirstHeader(CONTENT_TYPE).getValue());
            assertTrue(response.getHeaders(DIGEST).length > 0);

            final String digesterHeaderValue = response.getHeaders(DIGEST)[0].getValue();
            assertTrue("SHA-1 Fixity Checksum doesn't match",
                    digesterHeaderValue.contains(TEST_SHA_DIGEST_HEADER_VALUE));
            assertTrue("MD5 fixity checksum doesn't match",
                    digesterHeaderValue.contains(TEST_MD5_DIGEST_HEADER_VALUE));
        }
    }

    @Test
    public void testHeadDatastreamWithWantDigestMultipleOneUnsupported() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", TEST_BINARY_CONTENT);

        final HttpHead headObjMethod = headObjMethod(id + "/x");
        headObjMethod.addHeader(WANT_DIGEST, "md5, Indigestion");
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertEquals(1, response.getHeaders(DIGEST).length);
            final String digesterHeaderValue = response.getHeaders(DIGEST)[0].getValue();
            assertTrue("MD5 fixity checksum doesn't match",
                    digesterHeaderValue.contains(TEST_MD5_DIGEST_HEADER_VALUE));
        }
    }

    @Test
    public void testHeadDatastreamWithWantDigestSha256() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", TEST_BINARY_CONTENT);

        final HttpHead headObjMethod = headObjMethod(id + "/x");
        headObjMethod.addHeader(WANT_DIGEST, "sha-256");
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertEquals(TEXT_PLAIN, response.getFirstHeader(CONTENT_TYPE).getValue());
            assertTrue(response.getHeaders(DIGEST).length > 0);

            final String digesterHeaderValue = response.getHeaders(DIGEST)[0].getValue();
            assertTrue("SHA-256 Fixity Checksum doesn't match",
                    digesterHeaderValue.contains(TEST_SHA256_DIGEST_HEADER_VALUE));
        }
    }

    @Test
    public void testHeadRdfResourceHeaders() throws IOException {
        final String id = getRandomUniqueId();
        createObject(id).close();

        final String location = serverAddress + id;
        final HttpHead headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            checkForLinkHeader(response, location + "/" + FCR_ACL, "acl");
        }
    }

    @Test
    public void testHeadNonRdfHeaders() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id, "text/plain", "<> a <http://example.com/Foo> .");
        executeAndClose(put);

        final String location = serverAddress + id;
        final HttpHead headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            checkForLinkHeader(response, location + "/" + FCR_ACL, "acl");
        }
    }

    @Test
    public void testOptions() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final HttpOptions optionsRequest = new HttpOptions(serverAddress + id);
        try (final CloseableHttpResponse optionsResponse = execute(optionsRequest)) {
            assertEquals(OK.getStatusCode(), optionsResponse.getStatusLine().getStatusCode());
            assertContainerOptionsHeaders(optionsResponse);
        }
    }

    @Test
    public void testOptionsBinary() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", id);

        final HttpOptions optionsRequest = new HttpOptions(serverAddress + id + "/x");
        try (final CloseableHttpResponse optionsResponse = execute(optionsRequest)) {
            assertEquals(OK.getStatusCode(), optionsResponse.getStatusLine().getStatusCode());
            assertResourceOptionsHeaders(optionsResponse);
            assertEquals("0", optionsResponse.getFirstHeader(CONTENT_LENGTH).getValue());
        }
    }

    @Test
    public void testOptionsBinaryMetadata() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", null);

        final HttpOptions optionsRequest = new HttpOptions(serverAddress + id + "/x/fcr:metadata");
        try (final CloseableHttpResponse optionsResponse = execute(optionsRequest)) {
            assertEquals(OK.getStatusCode(), optionsResponse.getStatusLine().getStatusCode());
            assertNonRdfResourceDescriptionOptionsHeaders(optionsResponse);
        }
    }

    @Test
    public void testOptionsBinaryMetadataWithUriEncoding() throws Exception {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", null);
        final String location = serverAddress + id + "/x/fcr%3Ametadata";

        final HttpOptions optionsRequest = new HttpOptions(location);
        try (final CloseableHttpResponse optionsResponse = execute(optionsRequest)) {
            assertEquals(OK.getStatusCode(), optionsResponse.getStatusLine().getStatusCode());
            assertNonRdfResourceDescriptionOptionsHeaders(optionsResponse);
        }
    }

    private static void assertContainerOptionsHeaders(final HttpResponse httpResponse) {
        assertRdfOptionsHeaders(httpResponse);
        final List<String> methods = headerValues(httpResponse, "Allow");
        assertTrue("Should allow POST", methods.contains(HttpPost.METHOD_NAME));

        final List<String> postTypes = headerValues(httpResponse, "Accept-Post");
        assertTrue("POST should support text/turtle", postTypes.contains(contentTypeTurtle));
        assertTrue("POST should support text/rdf+n3", postTypes.contains(contentTypeN3));
        assertTrue("POST should support text/n3", postTypes.contains(contentTypeN3Alt2));
        assertTrue("POST should support application/rdf+xml", postTypes.contains(contentTypeRDFXML));
        assertTrue("POST should support application/n-triples", postTypes.contains(contentTypeNTriples));

        final List<String> externalTypes = headerValues(httpResponse, "Accept-External-Content-Handling");
        assertTrue("COPY should be advertised for accepted external content.", externalTypes.contains(COPY));
        assertTrue("PROXY should be advertised for accepted external content.", externalTypes.contains(PROXY));
        assertTrue("REDIRECT should be advertised for accepted external content.", externalTypes.contains(REDIRECT));
    }

    private static void assertRdfOptionsHeaders(final HttpResponse httpResponse) {
        final List<String> methods = headerValues(httpResponse, "Allow");
        assertTrue("Should allow PATCH", methods.contains(HttpPatch.METHOD_NAME));
        assertTrue("Should allow DELETE", methods.contains("DELETE"));
        assertTrue("Should allow HEAD", methods.contains("HEAD"));

        final List<String> patchTypes = headerValues(httpResponse, "Accept-Patch");
        assertTrue("PATCH should support application/sparql-update", patchTypes.contains(contentTypeSPARQLUpdate));
        assertResourceOptionsHeaders(httpResponse);
    }

    private static void assertNonRdfResourceDescriptionOptionsHeaders(final HttpResponse httpResponse) {
        final List<String> methods = headerValues(httpResponse, "Allow");
        assertTrue("Should allow PATCH", methods.contains(HttpPatch.METHOD_NAME));
        assertTrue("Should allow HEAD", methods.contains(HttpHead.METHOD_NAME));
        final List<String> patchTypes = headerValues(httpResponse, "Accept-Patch");
        assertTrue("PATCH should support application/sparql-update", patchTypes.contains(contentTypeSPARQLUpdate));
        assertResourceOptionsHeaders(httpResponse);
    }

    private static void assertResourceOptionsHeaders(final HttpResponse httpResponse) {
        final List<String> methods = headerValues(httpResponse, "Allow");
        assertTrue("Should allow GET", methods.contains(HttpGet.METHOD_NAME));
        assertTrue("Should allow PUT", methods.contains(HttpPut.METHOD_NAME));
        assertTrue("Should allow DELETE", methods.contains(HttpDelete.METHOD_NAME));
        assertTrue("Should allow OPTIONS", methods.contains(HttpOptions.METHOD_NAME));
    }

    @Test
    public void testGetRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final String location = serverAddress + id;
        try (final CloseableHttpResponse response = execute(new HttpGet(serverAddress + id))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            checkForLinkHeader(response, location + "/" + FCR_ACL, "acl");
            checkForLinkHeader(response, RDF_SOURCE.getURI(), "type");
            final HttpEntity entity = response.getEntity();
            final String contentType = parse(entity.getContentType().getValue()).getMimeType();
            assertNotNull("Entity is not an RDF serialization!", contentTypeToLang(contentType));
        }
    }

    @Test
    public void testCreateContainerWithCharset() throws IOException {
        final String id = getRandomUniqueId();

        final HttpPut put = putObjMethod(id, "text/turtle; charset=ISO-8859-1", "<> <http://test.org/title> 'hello'");
        put.setHeader(LINK, BASIC_CONTAINER_LINK_HEADER);
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());

        }
    }

    @Test
    public void testGetRDFSourceWithPreferMinimal() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final HttpGet getMethod = new HttpGet(serverAddress + id);
        final String preferHeader = "return=minimal";
        getMethod.addHeader("Prefer", preferHeader);

        try (final CloseableHttpResponse response = execute(getMethod)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final Collection<String> preferenceApplied = getHeader(response, "Preference-Applied");
            assertTrue("Preference-Applied header doesn't matched", preferenceApplied.contains(preferHeader));
        }
    }

    @Test
    public void testCheckGetAclResourceHeaders() throws IOException {
        final String aclUri = createAcl();

        try (final CloseableHttpResponse response = execute(new HttpGet(aclUri))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final Collection<String> links = getLinkHeaders(response);
            final String aclLink = "<" + aclUri + "/" + FCR_ACL + ">;rel=\"acl\"";
            assertFalse("ACL link header exists in ACL resource!", links.contains(aclLink));
        }
    }

    @Test
    public void testGetRDFSourceWithPreferRepresentation() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final HttpGet getMethod = new HttpGet(serverAddress + id);
        final String preferHeader = "return=representation;"
                + " include=\"http://fedora.info/definitions/fcrepo#PreferInboundReferences\";"
                + " omit=\"http://www.w3.org/ns/ldp#PreferMembership http://www.w3.org/ns/ldp#PreferContainment\"";
        getMethod.addHeader("Prefer", preferHeader);

        try (final CloseableHttpResponse response = execute(getMethod)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final Collection<String> preferenceApplied = getHeader(response, "Preference-Applied");
            assertTrue("Preference-Applied header doesn't matched", preferenceApplied.contains(preferHeader));
        }
    }

    @Test
    public void testGetNonRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "some content");

        final String location = serverAddress + id + "/x";
        try (final CloseableHttpResponse response = execute(getDSMethod(id, "x"))) {
            final HttpEntity entity = response.getEntity();
            final String content = EntityUtils.toString(entity);
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            checkForLinkHeader(response, location + "/" + FCR_ACL, "acl");
            assertEquals("some content", content);
        }
    }

    @Test
    public void testGetNonRDFSourceDescription() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "some content");

        final String location = serverAddress + id + "/x";
        try (final CloseableHttpResponse response = execute(getDSDescMethod(id, "x"));
                final CloseableDataset dataset = getDataset(response)) {
            checkForLinkHeader(response, location, "describes");
            checkForLinkHeader(response, location + "/" + FCR_ACL, "acl");
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node correctDSSubject = createURI(serverAddress + id + "/x");
            assertTrue("Binary should be a ldp:NonRDFSource", graph.contains(ANY,
                    correctDSSubject, rdfType, NON_RDF_SOURCE.asNode()));
            // every triple in the response should have a subject of the actual resource described
            LOGGER.info("Found graph:\n{}", graph);
            graph.find().forEachRemaining(quad -> {
                assertEquals("Found a triple with incorrect subject!", correctDSSubject, quad.getSubject());
            });
        }
    }

    @Test
    public void testGetNonRDFSourceWithWantDigest() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", TEST_BINARY_CONTENT);

        final HttpGet getMethod = getDSMethod(id, "x");
        getMethod.addHeader(WANT_DIGEST, "SHA");
        try (final CloseableHttpResponse response = execute(getMethod)) {
            final HttpEntity entity = response.getEntity();
            final String content = EntityUtils.toString(entity);
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertEquals(TEST_BINARY_CONTENT, content);

            final String digesterHeaderValue = response.getHeaders(DIGEST)[0].getValue();
            assertTrue("Fixity Checksum doesn't match",
                    digesterHeaderValue.equals(TEST_SHA_DIGEST_HEADER_VALUE));
        }
    }

    @Test
    public void testGetNonRDFSourceWithWantDigestMultiple() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", TEST_BINARY_CONTENT);

        final HttpGet getMethod = getDSMethod(id, "x");
        getMethod.addHeader(WANT_DIGEST, "SHA,md5;q=0.3,sha-256;q=0.2");
        try (final CloseableHttpResponse response = execute(getMethod)) {
            final HttpEntity entity = response.getEntity();
            final String content = EntityUtils.toString(entity);
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertEquals(TEST_BINARY_CONTENT, content);

            final String digesterHeaderValue = response.getHeaders(DIGEST)[0].getValue();
            assertTrue("SHA-1 Fixity Checksum doesn't match",
                    digesterHeaderValue.contains(TEST_SHA_DIGEST_HEADER_VALUE));
            assertTrue("MD5 fixity checksum doesn't match",
                    digesterHeaderValue.contains(TEST_MD5_DIGEST_HEADER_VALUE));
            assertTrue("SHA-256 fixity checksum doesn't match",digesterHeaderValue.contains(
                    "sha-256=fb871ff8cce8fea83dfaeab41784305a1461e008dc02a371ed26d856c766c903"));
        }
    }

    /**
     * Test that a 406 gets returned in the event of an invalid or unsupported
     * format being requested.
     */
    @Test
    public void testGetRDFSourceWrongAccept() {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final HttpGet get = new HttpGet(serverAddress + id);
        get.addHeader(ACCEPT, "application/turtle");

        assertEquals(NOT_ACCEPTABLE.getStatusCode(), getStatus(get));
    }

    @Test
    public void testGetRDFSourceWithUserTypes() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        verifyPresenceOfUserTypeHeader(id);
    }

    @Test
    public void testGetNonRDFSourceAndDescriptionWithUserTypes() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "ds", "sample-content");

        verifyPresenceOfUserTypeHeader(id + "/ds/" + FCR_METADATA);
    }

    private void verifyPresenceOfUserTypeHeader(final String id) throws IOException {
        final HttpHead headMethod = new HttpHead(serverAddress + id);

        final int numInitialHeaders;
        try (final CloseableHttpResponse response = execute(headMethod)) {
            assertEquals(headMethod.toString(), OK.getStatusCode(), response.getStatusLine().getStatusCode());
            numInitialHeaders = response.getAllHeaders().length;
        }

        // Add user type
        final URI userType = URI.create("http://example.org/ObjectType");
        setProperty(id, type.toString(), userType);

        try (final CloseableHttpResponse response = execute(headMethod)) {
            assertEquals(headMethod.toString(), OK.getStatusCode(), response.getStatusLine().getStatusCode());

            // Should be an additional header from the previous GET request
            assertEquals(numInitialHeaders + 1, response.getAllHeaders().length);

            // Verify presence of user type
            checkForLinkHeader(response, userType.toString(), "type");
        }

        // Verify presence of user type on NonRDFSource... if applicable
        if (id.endsWith(FCR_METADATA)) {
            // Strip the trailing /fcr:metadata
            final HttpHead headBinary = new HttpHead(serverAddress + id.replace("/" + FCR_METADATA, ""));

            try (final CloseableHttpResponse response = execute(headBinary)) {
                assertEquals(headBinary.toString(), OK.getStatusCode(), response.getStatusLine().getStatusCode());

                // Verify presence of user type
                checkForLinkHeader(response, userType.toString(), "type");
            }
        }
    }

    @Test
    public void testDeleteObject() {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteObjMethod(id)));
        assertDeleted(id);
    }

    @Test
    public void testDeleteContainerWithDepthHeaderSet() {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final HttpDelete httpDelete = deleteObjMethod(id);
        httpDelete.addHeader("Depth", "infinity");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(httpDelete));
        assertDeleted(id);
    }

    @Test
    public void testDeleteContainerWithIncorrectDepthHeaderSet() {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final HttpDelete httpDelete = deleteObjMethod(id);
        httpDelete.addHeader("Depth", "0");
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(httpDelete));
        assertNotDeleted(id);
    }

    @Test
    public void testDeleteHierarchy() {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createObjectAndClose(id + "/foo");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteObjMethod(id)));
        assertDeleted(id);
        assertDeleted(id + "/foo");
    }

    @Test
    public void testEmptyPatch() {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final HttpPatch patch = patchObjMethod(id);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patch));
    }

    @Test
    public void testUpdateObjectGraph() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final String location = serverAddress + id;
        final HttpPatch updateObjectGraphMethod = new HttpPatch(location);
        updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity("INSERT { <" + location + "> " +
                "<http://purl.org/dc/elements/1.1/identifier> \"this is an identifier\" } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));
    }

    @Test
    public void testDeleteMultipleMultiValuedProperties() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final String location = serverAddress + id;
        final HttpPatch addTriplesGraphMethod = new HttpPatch(location);
        addTriplesGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");

        final String insertStatement = "INSERT DATA { \n" +
                "  <> <http://example.org/test/x> \"x\" . \n" +
                "  <> <http://example.org/test/a> \"1\" . \n" +
                "  <> <http://example.org/test/a> \"2\" . \n" +
                "  <> <http://example.org/test/a> \"3\" . \n" +
                "  <> <http://example.org/test/a> \"4\" . \n" +
                "  <> <http://example.org/test/b> \"1\" . \n" +
                "  <> <http://example.org/test/b> \"2\" . \n" +
                "  <> <http://example.org/test/b> \"3\" . \n" +
                "  <> <http://example.org/test/b> \"4\" . \n" +
                "}";

        addTriplesGraphMethod.setEntity(new StringEntity(insertStatement));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(addTriplesGraphMethod));

        // ensure that the triples are there.
        try (final CloseableDataset dataset = getDataset(getObjMethod(id))) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Didn't find a triple we expected!", graph.contains(ANY,
                    createURI(location), createURI("http://example.org/test/x"), createLiteral("x")));
            assertTrue("Didn't find a triple we expected!", graph.contains(ANY,
                    createURI(location), createURI("http://example.org/test/a"), createLiteral("1")));

        }

        final HttpPatch deleteQuery = new HttpPatch(location);
        deleteQuery.addHeader(CONTENT_TYPE, "application/sparql-update");

        final String deleteQueryStatement = "" +
                "DELETE \n" +
                "WHERE \n" +
                "{ \n" +
                "  <> <http://example.org/test/a> ?a . \n" +
                "  <> <http://example.org/test/b> ?b . \n" +
                "} ";

        deleteQuery.setEntity(new StringEntity(deleteQueryStatement));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteQuery));

        // ensure that the expected triples removed.
        try (final CloseableDataset dataset = getDataset(getObjMethod(id))) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Didn't find a triple we expected!", graph.contains(ANY,
                    createURI(location), createURI("http://example.org/test/x"), createLiteral("x")));
            for (int i = 0; i < 4; ++i) {
                for (final String suffix : Arrays.asList("a", "b")) {
                    assertFalse("Found a triple we deleted!", graph.contains(ANY,
                            createURI(location), createURI("http://example.org/test/" + suffix), createLiteral(i +
                                    "")));
                }
            }
        }
    }

    @Test
    public void testPatchBinary() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "some content");

        final HttpPatch patch = patchObjMethod(id + "/x");
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patch));
    }

    /**
     * Descriptions of bitstreams contain triples about the described thing, so only triples with the described thing
     * as their subject are legal.
     *
     * @throws IOException in case of IOException
     */
    @Test
    public void testPatchBinaryDescription() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "some content");

        final String location = serverAddress + id + "/x/fcr:metadata";
        final HttpPatch patch = new HttpPatch(location);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity("INSERT { <" + serverAddress + id + "/x> "
                + "<http://purl.org/dc/elements/1.1/identifier> \"this is an identifier\" } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch));
    }

    /**
     * Descriptions of bitstreams contain only triples about the described thing, so only triples with the described
     * thing as their subject are legal.
     *
     * @throws IOException on error
     */
    @Test
    public void testPatchBinaryDescriptionWithBinaryProperties() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "some content");
        final String resource = serverAddress + id;
        final String location = resource + "/x/fcr:metadata";

        final HttpPatch patch = new HttpPatch(location);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity("INSERT { <" +
                resource + "/x>" + " <" + DC_IDENTIFIER + "> \"identifier\" } WHERE {}"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            try (final CloseableDataset dataset = getDataset(new HttpGet(location))) {
                final DatasetGraph graph = dataset.asDatasetGraph();
                assertTrue(graph.contains(ANY,
                        createURI(resource + "/x"), DC_IDENTIFIER, createLiteral("identifier")));
            }
        }
    }

    @Test
    public void testPatchBinaryNameAndType() throws IOException {
        final String pid = getRandomUniqueId();

        createDatastream(pid, "x", "some content");

        final String location = serverAddress + pid + "/x/fcr:metadata";
        final HttpPatch patch = new HttpPatch(location);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity("DELETE { " +
                "<" + serverAddress + pid + "/x> <" + HAS_MIME_TYPE + "> ?any . } " +
                "WHERE {" +
                "<" + serverAddress + pid + "/x> <" + HAS_MIME_TYPE + "> ?any . } ; " +
                "INSERT {" +
                "<" + serverAddress + pid + "/x> <" + HAS_MIME_TYPE + "> \"text/awesome\" ." +
                "<" + serverAddress + pid + "/x> <" + HAS_ORIGINAL_NAME + "> \"x.txt\" }" +
                "WHERE {}"));

        try (final CloseableHttpResponse response = client.execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            try (final CloseableDataset dataset = getDataset(new HttpGet(location))) {
                final DatasetGraph graphStore = dataset.asDatasetGraph();
                final Node subject = createURI(serverAddress + pid + "/x");
                assertTrue(graphStore.contains(ANY, subject, HAS_MIME_TYPE.asNode(), createLiteral("text/awesome")));
                assertTrue(graphStore.contains(ANY, subject, HAS_ORIGINAL_NAME.asNode(), createLiteral("x.txt")));
                assertFalse("Should not contain old mime type property", graphStore.contains(ANY,
                        subject, createURI(REPOSITORY_NAMESPACE + "mimeType"), ANY));
            }
        }

        // Ensure binary can be downloaded (test against regression of: https://jira.duraspace.org/browse/FCREPO-1720)
        assertEquals(OK.getStatusCode(), getStatus(getDSMethod(pid, "x")));
    }

    @Test
@Ignore
    public void testPatchWithBlankNode() throws Exception {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final String location = serverAddress + id;
        final HttpPatch updateObjectGraphMethod = patchObjMethod(id);
        updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity("INSERT { <" +
                location + "> <info:some-predicate> _:a .\n " +
                "_:a <http://purl.org/dc/elements/1.1/title> \"this is a title\"\n" + " } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));

        try (final CloseableDataset dataset = getDataset(new HttpGet(location))) {
            final DatasetGraph graphStore = dataset.asDatasetGraph();
            assertTrue(graphStore.contains(ANY, createURI(location), createURI("info:some-predicate"), ANY));
            final Node bnode = graphStore.find(ANY,
                    createURI(location), createURI("info:some-predicate"), ANY).next().getObject();
            try (final CloseableDataset dataset2 = getDataset(new HttpGet(bnode.getURI()))) {
                assertTrue(dataset2.asDatasetGraph().contains(ANY, bnode, DCTITLE, createLiteral("this is a title")));
            }
        }
    }

    @Test
@Ignore
    public void testReplaceGraph() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final String subjectURI = serverAddress + id;
        final String initialContent;
        try (final CloseableHttpResponse subjectResponse = execute(getObjMethod(id))) {
            initialContent = EntityUtils.toString(subjectResponse.getEntity());
        }
        final HttpPut replaceMethod = putObjMethod(id);
        replaceMethod.addHeader(CONTENT_TYPE, "text/n3");
        replaceMethod
                .setEntity(new StringEntity(initialContent + "\n<" + subjectURI + "> <info:test#label> \"foo\""));
        try (final CloseableHttpResponse response = execute(replaceMethod)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
            assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));
        }
        try (final CloseableDataset dataset = getDataset(getObjMethod(id))) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Didn't find a triple we tried to create!", graph.contains(ANY,
                    createURI(subjectURI), createURI("info:test#label"), createLiteral("foo")));
        }
    }

    @Test
    public void testCreateGraph() throws IOException {
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut createMethod = new HttpPut(subjectURI);
        createMethod.addHeader(CONTENT_TYPE, "text/n3");
        createMethod.setEntity(new StringEntity("<" + subjectURI + "> <info:test#label> \"foo\""));
        assertEquals(CREATED.getStatusCode(), getStatus(createMethod));

        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Didn't find a triple we tried to create!", graph.contains(ANY,
                    createURI(subjectURI), createURI("info:test#label"), createLiteral("foo")));
        }
    }

    @Test
    public void testCreateVersionedRDFResource() throws IOException {
        createVersionedRDFResource();
    }

    @Test
    public void testGetVersionedResourceHeaders() throws IOException {
        final String subjectURI = createVersionedRDFResource();
        try (final CloseableHttpResponse response = execute(new HttpGet(subjectURI))) {
            verifyVersionedResourceResponseHeaders(subjectURI, response);
            checkForLinkHeader(response, RDF_SOURCE.getURI(), "type");
        }
    }

    @Test
    public void testHeadVersionedResourceHeaders() throws IOException {
        final String subjectURI = createVersionedRDFResource();
        try (final CloseableHttpResponse response = execute(new HttpHead(subjectURI))) {
            verifyVersionedResourceResponseHeaders(subjectURI, response);
        }
    }

    private void verifyVersionedResourceResponseHeaders(final String subjectURI,
            final CloseableHttpResponse response) {
        assertEquals("Didn't get an OK (200) response!", OK.getStatusCode(), getStatus(response));
        checkForVersionedResourceLinkHeader(response);
        checkForMementoTimeGateLinkHeader(response);
        checkForLinkHeader(response, subjectURI, "original");
        checkForLinkHeader(response, subjectURI, "timegate");
        checkForLinkHeader(response, subjectURI + "/" + FCR_VERSIONS, "timemap");
        checkForLinkHeader(response, subjectURI + "/" + FCR_ACL, "acl");
        assertEquals(1, Arrays.stream(response.getHeaders("Vary")).filter(x -> x.getValue().contains(
                "Accept-Datetime")).count());
    }

    private String createVersionedRDFResource() throws IOException {
        final String id = getRandomUniqueId();
        final String subjectURI = serverAddress + id;
        final HttpPost createMethod = postObjMethod();
        createMethod.addHeader("Slug", id);
        createMethod.addHeader(CONTENT_TYPE, "text/n3");
        createMethod.setEntity(new StringEntity("<" + subjectURI + "> <info:test#label> \"foo\""));

        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            checkForVersionedResourceLinkHeader(response);
        }
        return subjectURI;
    }

    private void checkForVersionedResourceLinkHeader(final CloseableHttpResponse response) {
        checkForLinkHeader(response, VERSIONED_RESOURCE.getURI(), "type");
    }

    private void checkForMementoTimeGateLinkHeader(final CloseableHttpResponse response) {
        checkForLinkHeader(response, VERSIONING_TIMEGATE_TYPE, "type");
    }

    @Test
    public void testCreateVersionedBinaryResource() throws IOException {
        final HttpPost method = postObjMethod();
        final String id = getRandomUniqueId();
        method.addHeader("Slug", id);
        method.addHeader(CONTENT_TYPE, "text/plain");
        method.setEntity(new StringEntity("test content"));

        method.addHeader(LINK, VERSIONED_RESOURCE_LINK_HEADER);
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            checkForVersionedResourceLinkHeader(response);
        }
    }

    private String createAcl() throws UnsupportedEncodingException {
        final String aclPid = "acl" + getRandomUniqueId();
        final String aclURI = serverAddress + aclPid;
        createObjectAndClose(aclPid);
        final HttpPatch patch = patchObjMethod(aclPid);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        // add webac:Acl type to aclURI
        patch.setEntity(new StringEntity(
                "INSERT { <> a <http://fedora.info/definitions/v4/webac#Acl> } WHERE {}"));
        assertEquals("Couldn't add webac:Acl type", NO_CONTENT.getStatusCode(), getStatus(patch));
        return aclURI;
    }

    @Test
@Ignore
    public void testCreateGraphWithBlanknodes() throws IOException {
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut createMethod = new HttpPut(subjectURI);
        createMethod.addHeader(CONTENT_TYPE, "text/n3");
        createMethod.setEntity(new StringEntity("<" + subjectURI + "> <info:some-predicate> _:a ." +
                "_:a <info:test#label> \"asdfg\""));
        assertEquals(CREATED.getStatusCode(), getStatus(createMethod));

        final HttpGet getObjMethod = new HttpGet(subjectURI);
        getObjMethod.addHeader(ACCEPT, "application/rdf+xml");
        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Iterator<Quad> quads =
                    graph.find(ANY, createURI(subjectURI), createURI("info:some-predicate"), ANY);
            assertTrue("Didn't find skolemized blank node assertion", quads.hasNext());
            final Node skolemizedNode = quads.next().getObject();
            assertTrue("Didn't find a triple we tried to create!", graph.contains(ANY,
                    skolemizedNode, createURI("info:test#label"), createLiteral("asdfg")));
        }
    }

    @Test
@Ignore
    public void testRoundTripReplaceGraph() throws IOException {

        final String subjectURI = getLocation(postObjMethod());
        final HttpGet getObjMethod = new HttpGet(subjectURI);
        getObjMethod.addHeader(ACCEPT, "text/turtle");

        final Model model = createDefaultModel();
        try (final CloseableHttpResponse getResponse = execute(getObjMethod)) {
            model.read(getResponse.getEntity().getContent(), subjectURI, "TURTLE");
        }
        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader(CONTENT_TYPE, "application/n-triples");
        try (final StringWriter w = new StringWriter()) {
            model.write(w, "N-TRIPLE");
            replaceMethod.setEntity(new StringEntity(w.toString()));
            logger.trace("Retrieved object graph for testRoundTripReplaceGraph():\n {}", w);
        }
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(replaceMethod));
    }

    @Test
    public void testPutBinary() throws IOException {

        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final String location = serverAddress + id + "/x";
        final HttpPut method = new HttpPut(location);
        method.setEntity(new StringEntity("foo"));
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        try (final CloseableHttpResponse response = execute(method)) {
            assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
            assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));

            final String receivedLocation = response.getFirstHeader("Location").getValue();
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            assertEquals("Got wrong URI in Location header for datastream creation!", location, receivedLocation);

            assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
            final String lastmod = response.getFirstHeader("Last-Modified").getValue();
            assertNotNull("Should set Last-Modified for new nodes", lastmod);
            assertNotEquals("Last-Modified should not be blank for new nodes", lastmod.trim(), "");
            final Link link = Link.valueOf(response.getFirstHeader(LINK).getValue());
            assertEquals("describedby", link.getRel());
        }
    }

    @Test
    public void testPutBinaryChildViolation() throws IOException {
        final String id = getRandomUniqueId();
        createObject(id);
        createDatastream(id, "binary", "some-content");

        final String location = serverAddress + id + "/binary/xx";
        assertEquals("Should be a 409 Conflict!", CONFLICT.getStatusCode(), getStatus(new HttpPut(location)));
    }

    @Test
@Ignore
    public void testBinaryEtags() throws IOException, InterruptedException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final String location = serverAddress + id + "/binary";
        final HttpPut method = putDSMethod(id, "binary", "foo");

        final String binaryEtag1, binaryEtag2, binaryEtag3, descEtag1, descEtag2, descEtag3;
        final String binaryLastModed1, binaryLastModed2, binaryLastModed3;
        final String descLastModed1, descLastModed2, descLastModed3;
        final String descLocation;

        try (final CloseableHttpResponse response = execute(method)) {
            binaryEtag1 = response.getFirstHeader("ETag").getValue();
            binaryLastModed1 = response.getFirstHeader("Last-Modified").getValue();
            descLocation = Link.valueOf(response.getFirstHeader(LINK).getValue()).getUri().toString();
        }

        // First check ETags and Last-Modified headers for the binary
        final HttpGet get1 = new HttpGet(location);
        get1.addHeader("If-None-Match", binaryEtag1);
        assertEquals("Expected 304 Not Modified", NOT_MODIFIED.getStatusCode(), getStatus(get1));

        final HttpGet get2 = new HttpGet(location);
        get2.addHeader("If-Modified-Since", binaryLastModed1);
        assertEquals("Expected 304 Not Modified", NOT_MODIFIED.getStatusCode(), getStatus(get2));

        // Next, check ETags and Last-Modified headers on the description
        final HttpGet get3 = new HttpGet(descLocation);
        try (final CloseableHttpResponse response = execute(get3)) {
            descEtag1 = response.getFirstHeader("ETag").getValue();
            descLastModed1 = response.getFirstHeader("Last-Modified").getValue();
        }
        assertNotEquals("Binary, description ETags should be different", binaryEtag1, descEtag1);

        final HttpGet get4 = new HttpGet(descLocation);
        get4.addHeader("If-None-Match", descEtag1);
        assertEquals("Expected 304 Not Modified", NOT_MODIFIED.getStatusCode(), getStatus(get4));

        final HttpGet get5 = new HttpGet(descLocation);
        get5.addHeader("If-Modified-Since", descLastModed1);
        assertEquals("Expected 304 Not Modified", NOT_MODIFIED.getStatusCode(), getStatus(get5));

        // Pause two seconds before updating the description
        sleep(2000);

        // Next, update the description
        final HttpPatch httpPatch = patchObjMethod(id + "/binary/fcr:metadata");
        assertTrue("Expected weak ETag", descEtag1.startsWith("W/"));
        httpPatch.addHeader(CONTENT_TYPE, "application/sparql-update");
        httpPatch.addHeader("If-Match", descEtag1.substring(2));
        httpPatch.setEntity(new StringEntity(
                "INSERT { <> <http://purl.org/dc/elements/1.1/title> 'this is a title' } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(httpPatch));

        // Next, check headers for the binary; they should not have changed
        final HttpHead head1 = new HttpHead(location);
        try (final CloseableHttpResponse response = execute(head1)) {
            binaryEtag2 = response.getFirstHeader("ETag").getValue();
            binaryLastModed2 = response.getFirstHeader("Last-Modified").getValue();
        }

        assertEquals("ETags should be the same", binaryEtag1, binaryEtag2);
        assertEquals("Last-Modified should be the same", binaryLastModed1, binaryLastModed2);

        final HttpGet get6 = new HttpGet(location);
        get6.addHeader("If-None-Match", binaryEtag1);
        assertEquals("Expected 304 Not Modified", NOT_MODIFIED.getStatusCode(), getStatus(get6));

        final HttpGet get7 = new HttpGet(location);
        get7.addHeader("If-Modified-Since", binaryLastModed1);
        assertEquals("Expected 304 Not Modified", NOT_MODIFIED.getStatusCode(), getStatus(get7));

        // Next, check headers for the description; they should have changed
        final HttpHead head2 = new HttpHead(descLocation);
        try (final CloseableHttpResponse response = execute(head2)) {
            descEtag2 = response.getFirstHeader("ETag").getValue();
            descLastModed2 = response.getFirstHeader("Last-Modified").getValue();
        }

        assertNotEquals("ETags should not be the same", descEtag1, descEtag2);
        assertNotEquals("Last-Modified should not be the same", descLastModed1, descLastModed2);

        final HttpGet get8 = new HttpGet(descLocation);
        get8.addHeader("If-None-Match", descEtag2);
        assertEquals("Expected 304 Not Modified", NOT_MODIFIED.getStatusCode(), getStatus(get8));

        final HttpGet get9 = new HttpGet(descLocation);
        get9.addHeader("If-Modified-Since", descLastModed2);
        assertEquals("Expected 304 Not Modified", NOT_MODIFIED.getStatusCode(), getStatus(get9));

        sleep(1000);

        // Next, update the binary itself
        final HttpPut method2 = new HttpPut(location);
        assertFalse("Expected strong ETag", binaryEtag1.startsWith("W/"));
        method2.addHeader("If-Match", binaryEtag1);
        method2.setEntity(new StringEntity("foobar"));
        try (final CloseableHttpResponse response = execute(method2)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            binaryEtag3 = response.getFirstHeader("ETag").getValue();
            binaryLastModed3 = response.getFirstHeader("Last-Modified").getValue();
        }

        final HttpGet get10 = new HttpGet(location);
        get10.addHeader("If-None-Match", binaryEtag1);
        assertEquals("Expected 200 OK", OK.getStatusCode(), getStatus(get10));

        final HttpGet get11 = new HttpGet(location);
        get11.addHeader("If-Modified-Since", binaryLastModed1);
        assertEquals("Expected 200 OK", OK.getStatusCode(), getStatus(get11));

        assertNotEquals("ETags should have changed", binaryEtag1, binaryEtag3);
        assertNotEquals("Last-Modified should have changed", binaryLastModed1, binaryLastModed3);

        // Next, check headers for the description; they should have changed
        final HttpHead head3 = new HttpHead(descLocation);
        try (final CloseableHttpResponse response = execute(head3)) {
            descEtag3 = response.getFirstHeader("ETag").getValue();
            descLastModed3 = response.getFirstHeader("Last-Modified").getValue();
        }

        assertNotEquals("ETags should have changed", descEtag2, descEtag3);
        assertNotEquals("Last-Modified should have changed", descLastModed2, descLastModed3);
    }

    @Test
    public void testPutBinaryRdfChanges() throws Exception {
        final String id = getRandomUniqueId();
        final Model model = createDefaultModel();
        final Resource subject = model.createResource(serverAddress + id);
        final Property property = model.createProperty("http://purl.org/dcterms/", "title");

        // Create the binary
        createObjectAndClose(id, "<" + NON_RDF_SOURCE + ">; rel=\"type\"");

        // Get the current description
        final HttpGet get = getObjMethod(id + "/" + FCR_METADATA);
        get.addHeader("Prefer",
            "return=representation; omit=\"http://fedora.info/definitions/v4/repository#ServerManaged\"");
        get.addHeader("Accept", "text/turtle");
        try (final CloseableHttpResponse response = execute(get)) {
            assertEquals("Expected 200 OK", OK.getStatusCode(), getStatus(response));
            model.read(response.getEntity().getContent(), null, "TURTLE");
        }

        // Put triples to the description.
        model.add(subject, property, model.createLiteral("ABC"));
        final ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        final Writer writer1 = new OutputStreamWriter(out1, "UTF-8");
        model.write(writer1, "TURTLE");

        final HttpPut put = putObjMethod(id + "/" + FCR_METADATA);
        final String outputModel = out1.toString("UTF-8");
        put.setEntity(new StringEntity(outputModel, "UTF-8"));
        put.setHeader("Content-type", "text/turtle");
        put.setHeader("Prefer", "handling=lenient; received=\"minimal\"");
        assertEquals("Did put the binary description", NO_CONTENT.getStatusCode(), getStatus(put));

        // Get the description and verify the content.
        final Model model2 = createDefaultModel();
        try (final CloseableHttpResponse response = execute(get)) {
            assertEquals("Expected 200 OK", OK.getStatusCode(), getStatus(response));
            model2.read(response.getEntity().getContent(), serverAddress, "TURTLE");
            final StmtIterator st = model.listStatements(subject, property, "ABC");
            assertTrue(st.hasNext());
        }

        // Alter the triples.
        model2.remove(subject, property, model.createLiteral("ABC"));
        model2.add(subject, property, model.createLiteral("XYZ"));

        // Put it again.
        final ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        final Writer writer2 = new OutputStreamWriter(out2, "UTF-8");
        model2.write(writer2, "TURTLE");
        final HttpPut put2 = putObjMethod(id + "/" + FCR_METADATA);
        put2.setHeader("Content-type", "text/turtle");
        put2.setHeader("Prefer", "handling=lenient; received=\"minimal\"");
        put2.setEntity(new ByteArrayEntity(out2.toByteArray()));
        assertEquals("Did not update binary description", NO_CONTENT.getStatusCode(), getStatus(put2));

        // Get the description and verify the content again.
        final Model model3 = createDefaultModel();
        try (final CloseableHttpResponse response = execute(get)) {
            assertEquals("Expected 200 OK", OK.getStatusCode(), getStatus(response));
            model3.read(response.getEntity().getContent(), serverAddress, "TURTLE");
            final StmtIterator st1 = model3.listStatements(subject, property, "XYZ");
            assertTrue(st1.hasNext());
            final StmtIterator st2 = model3.listStatements(subject, property, "ABC");
            assertFalse(st2.hasNext());
        }
    }

    @Test
@Ignore
    public void testETagOnDeletedChild() throws Exception {
        final String id = getRandomUniqueId();
        final String child = id + "/child";
        createObjectAndClose(id);
        createObjectAndClose(child);

        final HttpGet get = new HttpGet(serverAddress + id);
        final String etag1;
        try (final CloseableHttpResponse response = execute(get)) {
            etag1 = response.getFirstHeader("ETag").getValue();
        }

        assertEquals("Child resource not deleted!", NO_CONTENT.getStatusCode(),
                getStatus(new HttpDelete(serverAddress + child)));
        final String etag2;
        try (final CloseableHttpResponse response = execute(get)) {
            etag2 = response.getFirstHeader("ETag").getValue();
        }

        assertNotEquals("ETag didn't change!", etag1, etag2);
    }

    @Test
@Ignore
    public void testETagOnDeletedLdpDirectContainerChild() throws Exception {
        final String id = getRandomUniqueId();
        final String members = id + "/members";
        final String child = members + "/child";

        createObjectAndClose(id);

        // Create the DirectContainer
        final HttpPut createContainer = new HttpPut(serverAddress + members);
        createContainer.addHeader(CONTENT_TYPE, "text/turtle");
        createContainer.addHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        final String membersRDF = "<> <http://www.w3.org/ns/ldp#hasMemberRelation> <http://pcdm.org/models#hasMember>; "
            + "<http://www.w3.org/ns/ldp#membershipResource> <" + serverAddress + id + "> . ";
        createContainer.setEntity(new StringEntity(membersRDF));
        assertEquals("Membership container not created!", CREATED.getStatusCode(), getStatus(createContainer));

        // Create the child resource
        createObjectAndClose(child);

        final HttpGet get = new HttpGet(serverAddress + id);
        final String etag1;
        try (final CloseableHttpResponse response = execute(get)) {
            etag1 = response.getFirstHeader("ETag").getValue();
        }

        assertEquals("Child resource not deleted!", NO_CONTENT.getStatusCode(),
                getStatus(new HttpDelete(serverAddress + child)));
        final String etag2;
        try (final CloseableHttpResponse response = execute(get)) {
            etag2 = response.getFirstHeader("ETag").getValue();
        }

        assertNotEquals("ETag didn't change!", etag1, etag2);
    }

    @Test
@Ignore
    public void testETagOnDeletedLdpIndirectContainerChild() throws Exception {
        final String id = getRandomUniqueId();
        final String members = id + "/members";
        final String child = members + "/child";

        createObjectAndClose(id);

        // Create the IndirectContainer
        final HttpPut createContainer = new HttpPut(serverAddress + members);
        createContainer.addHeader(CONTENT_TYPE, "text/turtle");
        createContainer.addHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        final String membersRDF = "<> <http://www.w3.org/ns/ldp#hasMemberRelation> <info:fedora/test/hasTitle> ; "
            + "<http://www.w3.org/ns/ldp#insertedContentRelation> <http://www.w3.org/2004/02/skos/core#prefLabel>; "
            + "<http://www.w3.org/ns/ldp#membershipResource> <" + serverAddress + id + "> . ";
        createContainer.setEntity(new StringEntity(membersRDF));
        assertEquals("Membership container not created!", CREATED.getStatusCode(), getStatus(createContainer));

        // Create a child with the appropriate property
        final HttpPut createChild = new HttpPut(serverAddress + child);
        createChild.addHeader(CONTENT_TYPE, "text/turtle");
        final String childRDF = "<> <http://www.w3.org/2004/02/skos/core#prefLabel> \"A title\".";
        createChild.setEntity(new StringEntity(childRDF));
        assertEquals("Child container not created!", CREATED.getStatusCode(), getStatus(createChild));

        final HttpGet get = new HttpGet(serverAddress + id);
        final String etag1;
        try (final CloseableHttpResponse response = execute(get)) {
            etag1 = response.getFirstHeader("ETag").getValue();
            IOUtils.toString(response.getEntity().getContent(), UTF_8);
        }

        assertEquals("Child resource not deleted!", NO_CONTENT.getStatusCode(),
                getStatus(new HttpDelete(serverAddress + child)));

        final String etag2;
        try (final CloseableHttpResponse response = execute(get)) {
            etag2 = response.getFirstHeader("ETag").getValue();
            IOUtils.toString(response.getEntity().getContent(), UTF_8);
        }

        assertNotEquals("ETag didn't change!", etag1, etag2);
    }

    @Test
@Ignore
    public void testPutDatastreamContentOnObject() throws IOException {
        final String content = "foo";
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final HttpPut put = putObjMethod(id);
        put.setEntity(new StringEntity(content));
        put.setHeader(CONTENT_TYPE, "application/octet-stream");
        assertEquals(
                "Expected UNSUPPORTED MEDIA TYPE response when PUTing content to an object (as opposed to datastream)",
                UNSUPPORTED_MEDIA_TYPE.getStatusCode(), getStatus(put));
    }

    @Test
@Ignore
    public void testEmptyPutToExistingObject() {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        assertEquals("Expected CONFLICT response code when doing empty PUT on an existing object.",
                CONFLICT.getStatusCode(), getStatus(putObjMethod(id)));
    }

    @Test
    public void testPutMalformedRDFOnObject() throws IOException {
        final String content = "this is not legitimate RDF";
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final HttpPut put = putObjMethod(id);
        put.setEntity(new StringEntity(content));
        put.setHeader(CONTENT_TYPE, "text/plain");
        assertEquals("Expected BAD REQUEST response code when PUTing malformed RDF on an object",
                BAD_REQUEST.getStatusCode(), getStatus(put));
    }

    @Test
    public void testIngest() throws IOException {
        final String id = getRandomUniqueId();
        try (final CloseableHttpResponse response = createObject(id)) {
            final String content = EntityUtils.toString(response.getEntity());
            assertIdentifierness(content);
            assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
            assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));
            assertTrue("Didn't find Location header!", response.containsHeader("Location"));
        }
    }

    @Test
@Ignore
    public void testIngestWithNewAndSparqlQuery() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "application/sparql-update");
        method.setEntity(new StringEntity(
                "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"title\" } WHERE {}"));
        try (final CloseableHttpResponse response = execute(method)) {
            final String content = EntityUtils.toString(response.getEntity());
            final int status = getStatus(response);
            assertEquals("Didn't get a CREATED response! Got content:\n" + content, CREATED.getStatusCode(), status);
            final String lastmod = response.getFirstHeader("Last-Modified").getValue();
            assertNotNull("Should set Last-Modified for new nodes", lastmod);
            assertNotEquals("Last-Modified should not be blank for new nodes", lastmod.trim(), "");
            assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
            final String location = getLocation(response);
            try (final CloseableDataset dataset = getDataset(new HttpGet(location))) {
                final DatasetGraph graphStore = dataset.asDatasetGraph();
                assertTrue(graphStore.contains(ANY, createURI(location), DCTITLE, createLiteral("title")));
            }
        }
    }

    @Test
    public void testIngestWithSparqlQueryBadNS() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "application/sparql-update");
        method.setEntity(new StringEntity("PREFIX fcr: <http://xmlns.com/my-fcr/> "
                + "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"this is a title\" } WHERE {}"));
        assertNotEquals("Should not get a CREATED response with bad namspace prefix!",
                CREATED.getStatusCode(), getStatus(method));
    }

    @Test
    public void testIngestWithNewAndGraph() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "text/n3");
        method.setEntity(new StringEntity("<> <http://purl.org/dc/elements/1.1/title> \"title\"."));

        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
            final String lastmod = response.getFirstHeader("Last-Modified").getValue();
            assertNotNull("Should set Last-Modified for new nodes", lastmod);
            assertNotEquals("Last-Modified should not be blank for new nodes", lastmod.trim(), "");
            final String location = getLocation(response);
            try (final CloseableDataset dataset = getDataset(new HttpGet(location))) {
                final DatasetGraph graphStore = dataset.asDatasetGraph();
                assertTrue(graphStore.contains(ANY, createURI(location), DCTITLE, createLiteral("title")));
            }
        }
    }

    @Test
    public void testIngestWithSlug() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader("Slug", getRandomUniqueId());
        try (final CloseableHttpResponse response = execute(method)) {
            final String content = EntityUtils.toString(response.getEntity());
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            assertIdentifierness(content);
            final String location = getLocation(response);
            assertNotEquals(serverAddress + "/objects", location);
            assertEquals("Object wasn't created!", OK.getStatusCode(), getStatus(new HttpGet(location)));
        }
    }

    @Test
    public void testIngestWithRepeatedSlug() {
        final String id = getRandomUniqueId();
        assertEquals(CREATED.getStatusCode(), getStatus(putObjMethod(id)));
        final HttpPost method = postObjMethod();
        method.addHeader("Slug", id);
        assertEquals(CREATED.getStatusCode(), getStatus(method));
    }

    // TODO this was copied from extant tests as a refactoring, but is this a good test for identifier-ness?
    public static void assertIdentifierness(final String content) {
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content).find());
    }

    @Test
    public void testIngestWithBinary() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "application/octet-stream");
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        final String binaryContent = "xyz";
        method.setEntity(new StringEntity(binaryContent));

        try (final CloseableHttpResponse response = execute(method)) {
            final String content = EntityUtils.toString(response.getEntity());
            final int status = getStatus(response);
            assertEquals("Didn't get a CREATED response! Got content:\n" + content, CREATED.getStatusCode(), status);
            assertIdentifierness(content);
            final String location = getLocation(response);

            try (final CloseableHttpResponse getResponse = execute(new HttpGet(location))) {
                assertEquals("Object wasn't created!", OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
                final String resp = IOUtils.toString(getResponse.getEntity().getContent(), UTF_8);
                assertEquals("application/octet-stream", getResponse.getFirstHeader(CONTENT_TYPE).getValue());
                assertEquals(binaryContent, resp);
            }

            final Link link = Link.valueOf(response.getFirstHeader(LINK).getValue());

            assertEquals("describedby", link.getRel());
            assertTrue("Expected an anchor to the newly created resource", link.getParams().containsKey("anchor"));
            assertEquals("Expected anchor at the newly created resource", location,
                    link.getParams().get("anchor"));
            assertEquals("Expected describedBy link", location + "/" + FCR_METADATA, link.getUri().toString());
        }
    }

    /* Verifies RDF persisted as binary is retrieved with byte-for-byte fidelity */
    @Test
    public void testIngestOpaqueRdfAsBinary() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "application/n-triples");
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);

        final String rdf = "<test:/subject> <test:/predicate> <test:/object> .";
        method.setEntity(new StringEntity(rdf));

        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));

            final String location = getLocation(response);
            final HttpGet get = new HttpGet(location);

            try (final CloseableHttpResponse getResponse = execute(get)) {
                final String resp = IOUtils.toString(getResponse.getEntity().getContent(), UTF_8);
                assertEquals("application/n-triples", getResponse.getFirstHeader(CONTENT_TYPE).getValue());
                assertEquals(rdf, resp);
            }
        }
    }

    /**
     * Ensure that the objects can be created with a Digest header
     * with a SHA1 sum of the binary content
     */
    @Test
    public void testIngestWithBinaryAndChecksum() {
        final HttpPost method = postObjMethod();
        final File img = new File("src/test/resources/test-objects/img.png");
        method.addHeader(CONTENT_TYPE, "application/octet-stream");
        method.addHeader("Digest", "SHA=f0b632679fab4f22e031010bd81a3b0544294730");
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        method.setEntity(new FileEntity(img));

        assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(method));
    }

    /**
     * Ensure that the objects cannot be created when a Digest header
     * contains a SHA1 sum that does not match the uploaded binary
     * content
     */
    @Test
    public void testIngestWithBinaryAndChecksumMismatch() {
        final HttpPost method = postObjMethod();
        final File img = new File("src/test/resources/test-objects/img.png");
        method.addHeader(CONTENT_TYPE, "application/octet-stream");
        method.addHeader("Digest", "SHA=fedoraicon");
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        method.setEntity(new FileEntity(img));

        assertEquals("Should be a 409 Conflict!", CONFLICT.getStatusCode(), getStatus(method));
    }

    /**
     * Ensure that the a malformed Digest header returns a 400 Bad Request
     */
    @Test
    public void testIngestWithBinaryAndMalformedDigestHeader() {
        final HttpPost method = postObjMethod();
        final File img = new File("src/test/resources/test-objects/img.png");
        method.addHeader(CONTENT_TYPE, "application/octet-stream");
        method.addHeader("Digest", "md5=not a valid hash,SHA:thisisbadtoo");
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        method.setEntity(new FileEntity(img));
        assertEquals("Should be a 400 BAD REQUEST!", BAD_REQUEST.getStatusCode(), getStatus(method));
    }

    /**
     * Ensure that a non-SHA1 Digest header returns a 409 Conflict
     */
    @Test
    public void testIngestWithBinaryAndNonSha1DigestHeader() {
        final HttpPost method = postObjMethod();
        final File img = new File("src/test/resources/test-objects/img.png");
        method.addHeader(CONTENT_TYPE, "application/octet-stream");
        method.addHeader("Digest", "md5=anything");
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        method.setEntity(new FileEntity(img));

        assertEquals("Should be a 409 Conflict!", CONFLICT.getStatusCode(), getStatus(method));
    }

    @Test
    public void testIngestWithBinaryAndMD5DigestHeader() {
        final HttpPost method = postObjMethod();
        final File img = new File("src/test/resources/test-objects/img.png");
        method.addHeader(CONTENT_TYPE, "application/octet-stream");
        method.addHeader("Digest", "md5=6668675a91f39ca1afe46c084e8406ba");
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        method.setEntity(new FileEntity(img));

        assertEquals("Should be a 201 Created!", CREATED.getStatusCode(), getStatus(method));
    }

    @Test
    public void testIngestWithBinaryAndTwoValidHeadersDigestHeaders() {
        final HttpPost method = postObjMethod();
        final File img = new File("src/test/resources/test-objects/img.png");
        method.addHeader(CONTENT_TYPE, "application/octet-stream");
        method.addHeader("Digest", "md5=6668675a91f39ca1afe46c084e8406ba," +
                " sha-256=7b115a72978fe138287c1a6dfe6cc1afce4720fb3610a81d32e4ad518700c923");
        method.setEntity(new FileEntity(img));
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);

        assertEquals("Should be a 201 Created!", CREATED.getStatusCode(), getStatus(method));
    }

    @Test
    public void testIngestWithBinaryAndValidAndInvalidDigestHeaders() {
        final HttpPost method = postObjMethod();
        final File img = new File("src/test/resources/test-objects/img.png");
        method.addHeader(CONTENT_TYPE, "application/octet-stream");
        method.addHeader("Digest", "md5=6668675a91f39ca1afe46c084e8406ba," +
                " sha99=7b115a72978fe138287c1a6dfe6cc1afce4720fb3610a81d32e4ad518700c923");
        method.setEntity(new FileEntity(img));
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);

        assertEquals("Should be a 400 Bad Request!", BAD_REQUEST.getStatusCode(), getStatus(method));
    }

    @Test
    public void testContentDispositionHeader() throws ParseException, IOException {
        final HttpPost method = postObjMethod();
        final File img = new File("src/test/resources/test-objects/img.png");
        final String filename = "some-file.png";
        method.addHeader(CONTENT_TYPE, "application/png");
        method.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        method.setEntity(new FileEntity(img));
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);

        // Create a binary resource with content-disposition
        final String location;
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Should be a 201 Created!", CREATED.getStatusCode(), getStatus(response));
            location = getLocation(response);
        }

        // Retrieve the new resource and verify the content-disposition
        verifyContentDispositionFilename(location, filename);

        // TODO enble once PATCH is working
        // Update the filename
        // final String filename1 = "new-file.png";
        // final HttpPatch patch = new HttpPatch(location + "/" + FCR_METADATA);
        // patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        // final String updateString = "PREFIX ebucore: <http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#>\n" +
        // "DELETE { <> ebucore:filename ?x}\n" +
        // "INSERT { <> ebucore:filename \"" + filename1 + "\"}\n" +
        // "WHERE { <> ebucore:filename ?x}";
        //
        // patch.setEntity(new StringEntity(updateString));
        // assertEquals(location, NO_CONTENT.getStatusCode(), getStatus(patch));
        //
        // // Retrieve the new resource and verify the content-disposition
        // verifyContentDispositionFilename(location, filename1);
    }

    private void verifyContentDispositionFilename(final String location, final String filename)
            throws IOException, ParseException {
        final HttpHead head = new HttpHead(location);
        try (final CloseableHttpResponse headResponse = execute(head)) {
            final Header header = headResponse.getFirstHeader(CONTENT_DISPOSITION);
            assertNotNull(header);

            final ContentDisposition contentDisposition = new ContentDisposition(header.getValue());
            assertEquals(filename, contentDisposition.getFileName());
        }
    }

    @Test
    public void testIngestOnSubtree() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final HttpPost method = postObjMethod(id);
        method.addHeader("Slug", "x");
        assertEquals(serverAddress + id + "/x", getLocation(method));
    }

    /**
     * Ensure that the objects cannot be pairtree child resources
     *
     * @throws IOException in case of IOException
     */
    @Test
@Ignore
    public void testIngestOnPairtree() throws IOException {
        final String parent = "123456/789/10";
        executeAndClose(putObjMethod(parent));
        final String objName = serverAddress + parent;
        final String pairtreeName = objName.substring(serverAddress.length(), objName.lastIndexOf('/'));

        try (final CloseableDataset dataset = getDataset(getObjMethod(pairtreeName))) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Resource \"" + objName + " " + pairtreeName + "\" must be pairtree.", graph.contains(ANY,
                createURI(serverAddress + pairtreeName), type.asNode(), createURI(REPOSITORY_NAMESPACE + "Pairtree")));
        }
        // Attempting to POST to the child of the pairtree node...
        final int status = getStatus(postObjMethod(pairtreeName));
        assertEquals("Created an Object under a pairtree node!", FORBIDDEN.getStatusCode(), status);
    }

    @Test
    public void testIngestWithRDFLang() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "text/n3");
        method.setEntity(new StringEntity("<> <http://purl.org/dc/elements/1.1/title> \"french title\"@fr ."
                + "<> <http://purl.org/dc/elements/1.1/title> \"english title\"@en ."));

        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            final String location = getLocation(response);
            try (final CloseableDataset dataset = getDataset(new HttpGet(location))) {
                final DatasetGraph graphStore = dataset.asDatasetGraph();
                final Node subj = createURI(location);
                assertTrue(graphStore.contains(ANY, subj, DCTITLE, createLiteral("english title", "en", false)));
                assertTrue(graphStore.contains(ANY, subj, DCTITLE, createLiteral("french title", "fr", false)));
            }
        }
    }

    @Test
    // TODO It's not clear what this test is actually testing, or why it sleeps while running
            public
            void testCreateManyObjects() throws IOException, InterruptedException {
        if (System.getProperty(TEST_ACTIVATION_PROPERTY) == null) {
            LOGGER.info("Not running testCreateManyObjects because system property TEST_ACTIVATION_PROPERTY not set.");
            return;
        }
        final int manyObjects = 2000;
        for (int i = 0; i < manyObjects; i++) {
            sleep(10); // needed to prevent overloading TODO why?
            createObject().close();
        }
    }

    @Test
    public void testDeleteWithBadEtag() throws IOException {
        try (final CloseableHttpResponse response = execute(postObjMethod())) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            final HttpDelete request = new HttpDelete(getLocation(response));
            request.addHeader("If-Match", "\"doesnt-match\"");
            assertEquals(PRECONDITION_FAILED.getStatusCode(), getStatus(request));
        }
    }

    @Test
    public void testGetDatastream() throws IOException, ParseException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createDatastream(id, "ds1", "foo");

        try (final CloseableHttpResponse response = execute(getDSMethod(id, "ds1"))) {
            assertEquals("Wasn't able to retrieve a datastream!", OK.getStatusCode(), getStatus(response));
            assertEquals(TEXT_PLAIN, response.getFirstHeader(CONTENT_TYPE).getValue());
            assertEquals("3", response.getFirstHeader(CONTENT_LENGTH).getValue());
            assertEquals("bytes", response.getFirstHeader("Accept-Ranges").getValue());
            final ContentDisposition disposition =
                    new ContentDisposition(response.getFirstHeader(CONTENT_DISPOSITION).getValue());
            assertEquals("attachment", disposition.getType());

            final Collection<String> links = getLinkHeaders(response);
            final String describedByHeader =
                    "<" + serverAddress + id + "/ds1/" + FCR_METADATA + ">; rel=\"describedby\"";
            assertTrue("Didn't find 'describedby' link header!", links.contains(describedByHeader));
        }
    }

    @Test
    public void testGetLongRange() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final StringBuilder buf = new StringBuilder();
        while ( buf.length() < 9000 ) {
            buf.append("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        }
        createDatastream(id, "ds1", buf.toString());

        final HttpGet get = getDSMethod(id, "ds1");
        get.setHeader("Range", "bytes=0-8199");
        try (final CloseableHttpResponse response = execute(get)) {
            assertEquals("Expected 206 Partial Content!", PARTIAL_CONTENT.getStatusCode(), getStatus(response));
            assertEquals("Expected range length (8200)!", "8200", response.getFirstHeader(CONTENT_LENGTH).getValue());
        }
    }

    @Test
    public void testDeleteDatastream() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createDatastream(id, "ds1", "foo");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteObjMethod(id + "/ds1")));
        assertDeleted(id + "/ds1");
    }

    @Test
    public void testDeleteBinaryDescription() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id, NON_RDF_SOURCE_LINK_HEADER);

        // Check that the binary and description exist.
        binaryExists(id);

        // Try to delete the description.
        final HttpDelete deleteBinaryDesc = deleteObjMethod(id + "/" + FCR_METADATA);
        try (final CloseableHttpResponse response = execute(deleteBinaryDesc)) {
            assertEquals(METHOD_NOT_ALLOWED.getStatusCode(), getStatus(response));
        }

        // ensure the binary and description still exist.
        binaryExists(id);

        // Delete the binary
        final HttpDelete deleteBinary = deleteObjMethod(id);
        try (final CloseableHttpResponse response = execute(deleteBinary)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        // Ensure the binary and description are gone.
        binaryDoesntExist(id);

    }

    /**
     * Check if a binary and its description exist.
     *
     * @param id id of the binary.
     * @throws IOException on error with http communication.
     */
    private void binaryExists(final String id) throws IOException {
        binaryStatus(id, OK);
    }

    /**
     * Check if a binary and its description don't exist.
     *
     * @param id id of the binary.
     * @throws IOException on error with http communication.
     */
    private void binaryDoesntExist(final String id) throws IOException {
        binaryStatus(id, GONE);
    }

    /**
     * Utility function to confirm status of both a binary and its description
     *
     * @param id id of the binary.
     * @param status the expected status.
     * @throws IOException on error with http communication.
     */
    private void binaryStatus(final String id, final Status status) throws IOException {
        final HttpHead headBinary = headObjMethod(id);
        final var tombstoneUri = serverAddress + id + "/" + FCR_TOMBSTONE;
        try (final CloseableHttpResponse response = execute(headBinary)) {
            assertEquals(status.getStatusCode(), getStatus(response));
            if (status.equals(GONE)) {
                checkForLinkHeader(response, tombstoneUri, "hasTombstone");
            }
        }

        final HttpHead headBinaryDesc = headObjMethod(id + "/" + FCR_METADATA);
        try (final CloseableHttpResponse response = execute(headBinaryDesc)) {
            assertEquals(status.getStatusCode(), getStatus(response));
            if (status.equals(GONE)) {
                checkForLinkHeader(response, tombstoneUri, "hasTombstone");
            }
        }
    }

    @Test
    public void testGetObjectGraphHtml() throws IOException {
        final HttpGet getObjMethod = new HttpGet(getLocation(postObjMethod()));
        getObjMethod.addHeader(ACCEPT, "text/html");
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod));
    }

    @Test
    public void testGetObjectGraphVariants() throws IOException {
        final String location = getLocation(postObjMethod());
        for (final Variant variant : POSSIBLE_RDF_VARIANTS) {
            final HttpGet getObjMethod = new HttpGet(location);
            final String type = variant.getMediaType().getType();
            getObjMethod.addHeader(ACCEPT, type);
            assertEquals("Got bad response for type " + type + " !", OK.getStatusCode(), getStatus(getObjMethod));
        }
    }

    @Test
    public void testGetObjectGraph() throws IOException {
        logger.trace("Entering testGetObjectGraph()...");
        final String location = getLocation(postObjMethod());
        final HttpGet getObjMethod = new HttpGet(location);

        try (final CloseableHttpResponse response = execute(getObjMethod)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertResourceOptionsHeaders(response);
            checkForLinkHeader(response, RDF_SOURCE.getURI(), "type");
            assertTrue("Didn't find LDP link header!", getLinkHeaders(response).contains(LDP_RESOURCE_LINK_HEADER));
            checkForLinkHeader(response, location + "/" + FCR_ACL, "acl");
            try (final CloseableDataset dataset = getDataset(response)) {
                assertTrue("Didn't find any type triples!", dataset.asDatasetGraph().contains(ANY,
                        createURI(location), rdfType, ANY));
            }
            logger.trace("Leaving testGetObjectGraph()...");
        }
    }

    @Test
    public void verifyFullSetOfRdfTypes() throws IOException {
        logger.trace("Entering verifyFullSetOfRdfTypes()...");
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        try (final CloseableDataset dataset = getDataset(getObjMethod(id))) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node resource = createURI(serverAddress + id);
            verifyResource(graph, resource, REPOSITORY_NAMESPACE, "Container");
            verifyResource(graph, resource, REPOSITORY_NAMESPACE, "Resource");
        }
        logger.trace("Leaving verifyFullSetOfRdfTypes()...");
    }

    private static void verifyResource(final DatasetGraph g, final Node subject, final String ns, final String type) {
        assertTrue("Should find type: " + ns + type, g.contains(ANY, subject, rdfType, createURI(ns + type)));
    }

    @Test
    public void testGetObjectGraphWithChild() throws IOException {
        final String id = getRandomUniqueId();
        final String location = getLocation(createObject(id));
        createObjectAndClose(id + "/c");

        try (final CloseableHttpResponse response = execute(getObjMethod(id))) {
            try (final CloseableDataset dataset = getDataset(response)) {
                assertTrue("Didn't find child node!", dataset.asDatasetGraph().contains(ANY,
                        createURI(location), createURI(LDP_NAMESPACE + "contains"), createURI(location + "/c")));

                final Collection<String> links = getLinkHeaders(response);
                assertTrue("Didn't find LDP resource link header!", links.contains(LDP_RESOURCE_LINK_HEADER));
            }
        }
    }

    @Test
    public void testGetObjectGraphWithChildAndRemove() throws IOException {
        final String id = getRandomUniqueId();
        final String location = getLocation(createObject(id));
        createObjectAndClose(id + "/c");
        try (final CloseableHttpResponse response = execute(getObjMethod(id))) {
            try (final CloseableDataset dataset = getDataset(response)) {
                assertTrue("Didn't find child node!", dataset.asDatasetGraph().contains(ANY,
                        createURI(location), createURI(LDP_NAMESPACE + "contains"), createURI(location + "/c")));
            }
        }

        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteObjMethod(id + "/c")));

        try (final CloseableHttpResponse response1 = execute(getObjMethod(id))) {
            try (final CloseableDataset dataset = getDataset(response1)) {
                assertFalse("Found child node!", dataset.asDatasetGraph().contains(ANY,
                        createURI(location), createURI(LDP_NAMESPACE + "contains"), createURI(location + "/c")));
            }
        }
    }

    @Test
    public void testGetObjectGraphWithChildren() throws IOException {
        final String id = getRandomUniqueId();
        final String location = getLocation(createObject(id));

        // Create some children
        final int CHILDREN_TOTAL = 20;
        for (int x = 0; x < CHILDREN_TOTAL; ++x) {
            createObjectAndClose(id + "/child-" + x);
        }

        final int CHILDREN_LIMIT = 12;
        final HttpGet httpGet = getObjMethod(id);
        httpGet.setHeader("Limit", Integer.toString(CHILDREN_LIMIT));
        try (final CloseableHttpResponse response = execute(httpGet)) {
            try (final CloseableDataset dataset = getDataset(response)) {
                final DatasetGraph graph = dataset.asDatasetGraph();
                final Iterator<Quad> contains = graph.find(ANY, createURI(location), CONTAINS.asNode(), ANY);
                assertTrue("Should find contained child!", contains.hasNext());
                assertEquals(CHILDREN_LIMIT, Iterators.size(contains));
            }
        }
    }

    @Test
    public void testGetObjectGraphWithBadLimit() throws IOException {
        final String id = getRandomUniqueId();
        getLocation(createObject(id));

        final HttpGet httpGet = getObjMethod(id);
        httpGet.setHeader("Limit", "not-an-integer");
        try (final CloseableHttpResponse response = execute(httpGet)) {
            assertEquals(SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testGetObjectGraphMinimal() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id, BASIC_CONTAINER_LINK_HEADER);
        createObjectAndClose(id + "/a");
        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader("Prefer", "return=minimal");
        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node resource = createURI(serverAddress + id);
            assertFalse("Didn't expect members", graph.find(ANY, resource, CONTAINS.asNode(), ANY).hasNext());
        }
    }

    @Test
@Ignore
    public void testGetObjectOmitMembership() throws IOException {
        final String id = getRandomUniqueId();
        final Node resource = createURI(serverAddress + id);
        createObjectAndClose(id, BASIC_CONTAINER_LINK_HEADER);
        createObjectAndClose(id + "/a");
        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader("Prefer", "return=representation; "
                + "omit=\"http://www.w3.org/ns/ldp#PreferContainment http://www.w3.org/ns/ldp#PreferMembership\"");
        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Expected server managed", graph.find(ANY, resource, ANY, CONTAINER.asNode()).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, ANY, BASIC_CONTAINER.asNode()).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, ANY, RDF_SOURCE.asNode()).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, ANY, FEDORA_CONTAINER.asNode()).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, ANY, FEDORA_RESOURCE.asNode()).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, CREATED_DATE.asNode(), ANY).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, CREATED_BY.asNode(), ANY).hasNext());
            assertTrue("Expected server managed",
                    graph.find(ANY, resource, LAST_MODIFIED_DATE.asNode(), ANY).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, LAST_MODIFIED_BY.asNode(), ANY).hasNext());
        }
    }

    @Test
@Ignore
    public void testGetObjectOmitContainment() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final String location = serverAddress + id;
        final String updateString = "<> <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + location + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_NAMESPACE + "member> .";
        final HttpPut put = putObjMethod(id + "/a", "text/turtle", updateString);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals(CREATED.getStatusCode(), getStatus(put));
        assertTrue(getLinkHeaders(getObjMethod(id + "/a")).contains(DIRECT_CONTAINER_LINK_HEADER));
        createObject(id + "/a/1");

        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod
                .addHeader("Prefer", "return=representation; omit=\"http://www.w3.org/ns/ldp#PreferContainment\"");
        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node resource = createURI(serverAddress + id);
            assertTrue("Didn't find member resources", graph.find(ANY, resource, LDP_MEMBER.asNode(), ANY).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, ANY, CONTAINER.asNode()).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, ANY, BASIC_CONTAINER.asNode()).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, ANY, RDF_SOURCE.asNode()).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, ANY, FEDORA_CONTAINER.asNode()).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, ANY, FEDORA_RESOURCE.asNode()).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, CREATED_DATE.asNode(), ANY).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, CREATED_BY.asNode(), ANY).hasNext());
            assertTrue("Expected server managed",
                    graph.find(ANY, resource, LAST_MODIFIED_DATE.asNode(), ANY).hasNext());
            assertTrue("Expected server managed", graph.find(ANY, resource, LAST_MODIFIED_BY.asNode(), ANY).hasNext());
            assertFalse("Expected nothing contained", graph.find(ANY, resource, CONTAINS.asNode(), ANY).hasNext());
        }
    }

    @Test
@Ignore
    public void testGetObjectOmitServerManaged() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final String location = serverAddress + id;
        final String updateString = "<> <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + location + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_NAMESPACE + "member> .";
        final HttpPut put = putObjMethod(id + "/a", "text/turtle", updateString);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals(CREATED.getStatusCode(), getStatus(put));
        assertTrue(getLinkHeaders(getObjMethod(id + "/a")).contains(DIRECT_CONTAINER_LINK_HEADER));
        createObject(id + "/a/1");

        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader("Prefer",
                "return=representation; omit=\"http://fedora.info/definitions/v4/repository#ServerManaged\"");
        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node resource = createURI(serverAddress + id);
            assertTrue("Didn't find member resources", graph.find(ANY, resource, LDP_MEMBER.asNode(), ANY).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, CONTAINS.asNode(), ANY).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, ANY, CONTAINER.asNode()).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, ANY, BASIC_CONTAINER.asNode()).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, ANY, RDF_SOURCE.asNode()).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, ANY, FEDORA_CONTAINER.asNode()).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, ANY, FEDORA_RESOURCE.asNode()).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, CREATED_DATE.asNode(), ANY).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, CREATED_BY.asNode(), ANY).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, LAST_MODIFIED_DATE.asNode(), ANY).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, LAST_MODIFIED_BY.asNode(), ANY).hasNext());
        }
    }

    @Test
@Ignore
    public void testGetObjectIncludeContainmentAndOmitServerManaged() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final String location = serverAddress + id;
        final String updateString = "<> <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + location + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_NAMESPACE + "member> .";
        final HttpPut put = putObjMethod(id + "/a", "text/turtle", updateString);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals(CREATED.getStatusCode(), getStatus(put));
        assertTrue(getLinkHeaders(getObjMethod(id + "/a")).contains(DIRECT_CONTAINER_LINK_HEADER));
        createObject(id + "/a/1");

        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader("Prefer",
                "return=representation; omit=\"http://fedora.info/definitions/v4/repository#ServerManaged\"; " +
                "include=\"http://www.w3.org/ns/ldp#PreferContainment\"");
        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node resource = createURI(serverAddress + id);
            assertTrue("Didn't find ldp containment", graph.find(ANY, resource, CONTAINS.asNode(), ANY).hasNext());
        }
    }

    @Test
@Ignore
    public void testGetLDPRmOmitServerManaged() throws IOException {
        final String versionedResourceURI = createVersionedRDFResource();
        final String mementoResourceURI = getLocation(new HttpPost(versionedResourceURI + "/fcr:versions"));

        final HttpGet mementoGetMethod = new HttpGet(mementoResourceURI);
        mementoGetMethod.addHeader("Prefer",
                "return=representation; omit=\"http://fedora.info/definitions/v4/repository#ServerManaged\"");
        try (final CloseableDataset dataset = getDataset(mementoGetMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node resource = createURI(versionedResourceURI);
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, ANY, CONTAINER.asNode()).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, ANY, RDF_SOURCE.asNode()).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, ANY, FEDORA_CONTAINER.asNode()).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, ANY, FEDORA_RESOURCE.asNode()).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, CREATED_DATE.asNode(), ANY).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, CREATED_BY.asNode(), ANY).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, LAST_MODIFIED_DATE.asNode(), ANY).hasNext());
            assertFalse("Expected nothing server managed",
                    graph.find(ANY, resource, LAST_MODIFIED_BY.asNode(), ANY).hasNext());
        }
    }

    @Test
@Ignore
    public void testGetLDPRmWithoutOmitServerManager() throws IOException {
        final String versionedResourceURI = createVersionedRDFResource();
        final String mementoResourceURI = getLocation(new HttpPost(versionedResourceURI + "/fcr:versions"));

        final HttpGet mementoGetMethod = new HttpGet(mementoResourceURI);
        try (final CloseableDataset dataset = getDataset(mementoGetMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            final Node resource = createURI(versionedResourceURI);
            assertTrue("Expected server managed",
                    graph.find(ANY, resource, ANY, CONTAINER.asNode()).hasNext());
            assertTrue("Expected server managed",
                    graph.find(ANY, resource, ANY, RDF_SOURCE.asNode()).hasNext());
            assertTrue("Expected server managed",
                    graph.find(ANY, resource, ANY, FEDORA_CONTAINER.asNode()).hasNext());
            assertTrue("Expected server managed",
                    graph.find(ANY, resource, ANY, FEDORA_RESOURCE.asNode()).hasNext());
            assertTrue("Expected server managed",
                    graph.find(ANY, resource, CREATED_DATE.asNode(), ANY).hasNext());
            assertTrue("Expected server managed",
                    graph.find(ANY, resource, CREATED_BY.asNode(), ANY).hasNext());
            assertTrue("Expected server managed",
                    graph.find(ANY, resource, LAST_MODIFIED_DATE.asNode(), ANY).hasNext());
            assertTrue("Expected server managed",
                    graph.find(ANY, resource, LAST_MODIFIED_BY.asNode(), ANY).hasNext());
        }
    }

    @Test
@Ignore
    public void testPatchToCreateDirectContainerInSparqlUpdate() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        final String updateString =
                "INSERT DATA { <> a <" + DIRECT_CONTAINER.getURI() + "> ; <" + MEMBERSHIP_RESOURCE.getURI() +
                        "> <> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_NAMESPACE + "member> .}";
        patch.setEntity(new StringEntity(updateString));
        assertEquals("Patch with sparql update created direct container from basic container!",
                CONFLICT.getStatusCode(), getStatus(patch));
    }

    @Test
@Ignore
    public void testPatchWithLdpContainsIndirectContainer() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        final String updateString =
                "INSERT DATA { <> <" + MEMBERSHIP_RESOURCE.getURI() +
                        "> <> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_NAMESPACE + "contains> .}";
        patch.setEntity(new StringEntity(updateString));
        assertEquals("Patch with sparql update allowed ldp:contains in indirect container!",
                CONFLICT.getStatusCode(), getStatus(patch));
    }

    @Test
@Ignore
    public void testPatchWithoutLdpContainsIndirectContainer() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        final String updateString =
                "INSERT DATA { <> <" + MEMBERSHIP_RESOURCE.getURI() +
                        "> <> ; <" + HAS_MEMBER_RELATION + "> <info:some/relation> .}";
        patch.setEntity(new StringEntity(updateString));
        assertEquals("Patch with sparql update did not allow non SMT relation in indirect container!",
                NO_CONTENT.getStatusCode(), getStatus(patch));
    }

    @Test
@Ignore
    public void testPatchWithLdpContainsDirectContainer() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        final String updateString =
                "INSERT DATA { <> <" + MEMBERSHIP_RESOURCE.getURI() +
                        "> <> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_NAMESPACE + "contains> .}";
        patch.setEntity(new StringEntity(updateString));
        assertEquals("Patch with sparql update allowed ldp:contains in direct container!",
                CONFLICT.getStatusCode(), getStatus(patch));
    }

    @Test
@Ignore
    public void testPatchWithoutDirectContainer() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put);

        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        final String updateString =
                "INSERT DATA { <> <" + MEMBERSHIP_RESOURCE.getURI() +
                        "> <> ; <" + HAS_MEMBER_RELATION + "> <info:some/relation> .}";
        patch.setEntity(new StringEntity(updateString));
        assertEquals("Patch with sparql update did not allow non SMT relation in direct container!",
                NO_CONTENT.getStatusCode(), getStatus(patch));
    }

    @Test
@Ignore
    public void testPatchToDeleteNonRdfSourceInteractionModel() throws IOException {
        final String pid = getRandomUniqueId();

        createDatastream(pid, "x", "some content");

        final String location = serverAddress + pid + "/x/fcr:metadata";
        final HttpPatch patchDeleteMethod = new HttpPatch(location);
        patchDeleteMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        patchDeleteMethod.setEntity(new StringEntity("PREFIX ldp: <http://www.w3.org/ns/ldp#> " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> DELETE { " +
                "<> rdf:type  ldp:NonRDFSource .} WHERE {}"));
        assertEquals("Delete interaction model got status 409!\n",
                CONFLICT.getStatusCode(), getStatus(patchDeleteMethod));
    }

    @Test
@Ignore
    public void testPutToChangeNonRdfSourceToRdfSource() throws IOException {
        final String pid = getRandomUniqueId();

        createDatastream(pid, "x", "some content");

        final String ttl = "<> <http://purl.org/dc/elements/1.1/title> \"this is a title\" .";
        final HttpPut put = putObjMethod(pid + "/x/fcr:metadata", "text/turtle", ttl);
        put.setHeader(LINK, BASIC_CONTAINER_LINK_HEADER);
        assertEquals("Changed the NonRdfSource ixn to basic container",
                CONFLICT.getStatusCode(), getStatus(put));
    }

    @Test
@Ignore
    public void testPutChangeBinaryTypeNotAllowed() throws IOException {
        final HttpPost postMethod = new HttpPost(serverAddress);
        postMethod.setEntity(new StringEntity("TestString."));
        postMethod.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"postCreate.txt\"");

        final String location;
        try (final CloseableHttpResponse response = execute(postMethod)) {
            location = getLocation(response);
        }

        // Change to RDFSource
        final HttpPut put1Method = new HttpPut(location);
        put1Method.setEntity(new StringEntity("TestString2."));
        put1Method.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"putUpdate.txt\"");
        put1Method.setHeader(LINK, RDF_SOURCE_LINK_HEADER);
        assertEquals("Changed the NonRdfSource interaction model to RdfSource",
                BAD_REQUEST.getStatusCode(), getStatus(put1Method));

        // Change to Basic Container
        final HttpPut put2Method = new HttpPut(location);
        put2Method.setEntity(new StringEntity("TestString2."));
        put2Method.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"putUpdate.txt\"");
        put2Method.setHeader(LINK, BASIC_CONTAINER_LINK_HEADER);
        assertEquals("Changed the NonRdfSource interaction model to RdfSource",
                CONFLICT.getStatusCode(), getStatus(put2Method));
    }

    @Test
@Ignore
    public void testPutChangeBasicContainerToParent() throws Exception {
        final String pid = getRandomUniqueId();
        final String location = serverAddress + pid;
        createObjectAndClose(pid);

        // Change to RDFSource
        final HttpPut put1Method = new HttpPut(location);
        put1Method.setHeader(LINK, CONTAINER_LINK_HEADER);
        assertEquals("Changed the BasicContainer interaction model to Container",
                BAD_REQUEST.getStatusCode(), getStatus(put1Method));

        final HttpPut put2Method = new HttpPut(location);
        put2Method.setHeader(LINK, RESOURCE_LINK_HEADER);
        assertEquals("Changed the BasicContainer interaction model to Resource",
                BAD_REQUEST.getStatusCode(), getStatus(put2Method));

        final HttpPut put3Method = new HttpPut(location);
        put3Method.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals("Changed the BasicContainer interaction model to DirectContainer",
                CONFLICT.getStatusCode(), getStatus(put3Method));
    }

    @Test
@Ignore
    public void testPutToChangeInteractionModelWithRdf() throws IOException {
        final String pid = getRandomUniqueId();
        final String resource = serverAddress + pid;
        final String container = serverAddress + pid + "/c";

        createObjectAndClose(pid);
        createObjectAndClose(pid + "/a");
        createObjectAndClose(pid + "/c");

        // attempt to change basic container to NonRdfSource
        final String ttl1 = "<> a <" + NON_RDF_SOURCE.getURI() + "> .";
        final HttpPut put1 = putObjMethod(pid + "/a", "text/turtle", ttl1);
        put1.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
        assertEquals("Changed the basic container ixn to NonRdfSource through PUT with RDF content!",
                CONFLICT.getStatusCode(), getStatus(put1));

        // attempt to change basic container to direct container
        final String ttl2 = "<> a <" + DIRECT_CONTAINER.getURI() + "> ; <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + resource + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_NAMESPACE + "member> .";
        final HttpPut put2 = putObjMethod(pid + "/a", "text/turtle", ttl2);
        put2.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
        assertEquals("Changed the basic container ixn to Direct Container through PUT with RDF content!",
                CONFLICT.getStatusCode(), getStatus(put2));

        // create direct container
        final String ttl = "<> <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + resource + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_NAMESPACE + "member> .";
        final HttpPut put = putObjMethod(pid + "/b", "text/turtle", ttl);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals(CREATED.getStatusCode(), getStatus(put));
        assertTrue(getLinkHeaders(getObjMethod(pid + "/b")).contains(DIRECT_CONTAINER_LINK_HEADER));

        // successful update the properties with the interaction mode
        final String ttla = "<> <" + MEMBERSHIP_RESOURCE + "> <" + container + ">;\n"
                + "<" + HAS_MEMBER_RELATION + "> <info:some/relation> .\n";
        final HttpPut puta = putObjMethod(pid + "/b", "text/turtle", ttla);
        puta.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
        puta.addHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(puta));

        // attempt to change direct container to basic container
        final String ttl3 = "<> a <" + BASIC_CONTAINER.getURI() +
                "> ; <http://purl.org/dc/elements/1.1/title> \"this is a title\".";
        final HttpPut put3 = putObjMethod(pid + "/b", "text/turtle", ttl3);
        put3.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
        assertEquals("Changed the direct container ixn to basic container through PUT with RDF content!",
                CONFLICT.getStatusCode(), getStatus(put3));

        // attempt to change direct container to indirect container
        final String ttl4 = "<> a <" + INDIRECT_CONTAINER.getURI()
                + "> ; <" + MEMBERSHIP_RESOURCE + "> <" + container + ">;\n"
                + "<" + HAS_MEMBER_RELATION + "> <info:some/relation>;\n"
                + "<" + LDP_NAMESPACE + "insertedContentRelation> <info:proxy/for> .\n";
        final HttpPut put4 = putObjMethod(pid + "/b", "text/turtle", ttl4);
        put4.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
        assertEquals("Changed the direct container ixn to indirect container through PUT with RDF content!",
                CONFLICT.getStatusCode(), getStatus(put4));
    }

    @Test
@Ignore
    public void testChangeInteractionModelWithPut() throws IOException {
        final String pid = getRandomUniqueId();
        final String resource = serverAddress + pid;
        final String container = serverAddress + pid + "/c";

        createObjectAndClose(pid);
        createObjectAndClose(pid + "/a");
        createObjectAndClose(pid + "/c");

        final String ttl1 = "<> <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + resource + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_NAMESPACE + "member> .";
        final HttpPut put1 = putObjMethod(pid + "/a", "text/turtle", ttl1);
        put1.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        put1.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
        assertEquals("Changed the basic container ixn to direct container!",
                CONFLICT.getStatusCode(), getStatus(put1));

        // create direct container
        final String ttl = "<> <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + resource + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_NAMESPACE + "member> .";
        final HttpPut put = putObjMethod(pid + "/b", "text/turtle", ttl);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals(CREATED.getStatusCode(), getStatus(put));
        assertTrue(getLinkHeaders(getObjMethod(pid + "/b")).contains(DIRECT_CONTAINER_LINK_HEADER));

        final String ttl2 = "<> <http://purl.org/dc/elements/1.1/title> \"this is a title\"";
        final HttpPut put2 = putObjMethod(pid + "/b", "text/turtle", ttl2);
        put2.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        put2.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
        assertEquals("Changed the direct container ixn to basic container",
                CONFLICT.getStatusCode(), getStatus(put2));

        final String ttl3 = "<> <" + MEMBERSHIP_RESOURCE + "> <" + container + ">;\n"
                + "<" + HAS_MEMBER_RELATION + "> <info:some/relation>;\n"
                + "<" + LDP_NAMESPACE + "insertedContentRelation> <info:proxy/for> .\n";
        final HttpPut put3 = putObjMethod(pid + "/b", "text/turtle", ttl3);
        put3.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        put3.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
        assertEquals("Changed the direct container ixn to indirect container!",
                CONFLICT.getStatusCode(), getStatus(put3));
    }

    @Test
@Ignore
    public void testGetObjectReferences() throws IOException {
        final String id = getRandomUniqueId();
        final String resource = serverAddress + id;
        final String resourcea = resource + "/a";
        final String resourceb = resource + "/b";

        createObjectAndClose(id);
        createObjectAndClose(id + "/a");
        createObjectAndClose(id + "/b");
        final HttpPatch updateObjectGraphMethod = patchObjMethod(id + "/a");
        updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity("INSERT { <" +
                resourcea + "> <http://purl.org/dc/terms/isPartOf> <" + resourceb + "> . \n <" +
                resourcea + "> <info:xyz#some-other-property> <" + resourceb + "> } WHERE {}"));
        executeAndClose(updateObjectGraphMethod);

        final HttpGet getObjMethod = new HttpGet(resourceb);

        getObjMethod.addHeader("Prefer", "return=representation; include=\"" + INBOUND_REFERENCES + "\"");
        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(ANY,
                    createURI(resourcea), createURI("http://purl.org/dc/terms/isPartOf"), createURI(resourceb)));

            assertTrue(graph.contains(ANY,
                    createURI(resourcea), createURI("info:xyz#some-other-property"), createURI(resourceb)));
        }
    }

    @Test
@Ignore
    public void testGetObjectReferencesIndirect() throws Exception {
        final String uuid = getRandomUniqueId();
        final String pid1 = uuid + "/parent";
        final String pid2 = uuid + "/child1";
        final String pid3 = uuid + "/child2";
        createObjectAndClose(pid1);
        createObjectAndClose(pid2);
        createObjectAndClose(pid3);

        final String memberRelation = "http://pcdm.org/models#hasMember";

        // create an indirect container
        final HttpPut createContainer = new HttpPut(serverAddress + pid1 + "/members");
        createContainer.addHeader(CONTENT_TYPE, "text/turtle");
        createContainer.addHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        final String membersRDF = "<> <http://www.w3.org/ns/ldp#hasMemberRelation> <" + memberRelation + ">; "
            + "<http://www.w3.org/ns/ldp#insertedContentRelation> <http://www.openarchives.org/ore/terms/proxyFor>; "
            + "<http://www.w3.org/ns/ldp#membershipResource> <" + serverAddress + pid1 + "> . ";
        createContainer.setEntity(new StringEntity(membersRDF));
        assertEquals(CREATED.getStatusCode(), getStatus(createContainer));

        // create proxies for the children in the indirect container
        createProxy(pid1, pid2);
        createProxy(pid1, pid3);

        // retrieve the parent and verify the outbound triples exist
        final HttpGet getParent =  new HttpGet(serverAddress + pid1);
        getParent.addHeader(ACCEPT, "application/n-triples");
        try (final CloseableDataset dataset = getDataset(getParent)) {
            final DatasetGraph parentGraph = dataset.asDatasetGraph();
            assertTrue(parentGraph.contains(Node.ANY,
                    createURI(serverAddress + pid1),
                    createURI(memberRelation),
                    createURI(serverAddress + pid2)));
            assertTrue(parentGraph.contains(Node.ANY,
                    createURI(serverAddress + pid1),
                    createURI(memberRelation),
                    createURI(serverAddress + pid3)));
        }

        // retrieve the members container and verify the LDP triples exist
        final HttpGet getContainer =  new HttpGet(serverAddress + pid1 + "/members");
        getContainer.addHeader("Prefer", "return=representation;include=\"http://www.w3.org/ns/ldp#PreferMembership\"");
        getContainer.addHeader(ACCEPT, "application/n-triples");
        try (final CloseableDataset dataset = getDataset(getContainer)) {
            final DatasetGraph containerGraph = dataset.asDatasetGraph();
            assertTrue(containerGraph.contains(Node.ANY,
                    createURI(serverAddress + pid1 + "/members"),
                    createURI("http://www.w3.org/ns/ldp#hasMemberRelation"),
                    createURI(memberRelation)));

            assertTrue(containerGraph.contains(Node.ANY,
                    createURI(serverAddress + pid1 + "/members"),
                    createURI("http://www.w3.org/ns/ldp#insertedContentRelation"),
                    createURI("http://www.openarchives.org/ore/terms/proxyFor")));

            assertTrue(containerGraph.contains(Node.ANY,
                    createURI(serverAddress + pid1 + "/members"),
                    createURI("http://www.w3.org/ns/ldp#membershipResource"),
                    createURI(serverAddress + pid1)));
        }


        // retrieve the member and verify inbound triples exist
        final HttpGet getMember =  new HttpGet(serverAddress + pid2);
        getMember.addHeader("Prefer", "return=representation; include=\"" + INBOUND_REFERENCES.toString() + "\"");
        getMember.addHeader(ACCEPT, "application/n-triples");
        try (final CloseableDataset dataset = getDataset(getMember)) {
            final DatasetGraph memberGraph = dataset.asDatasetGraph();
            assertTrue(memberGraph.contains(Node.ANY,
                    Node.ANY,
                    createURI("http://www.openarchives.org/ore/terms/proxyFor"),
                    createURI(serverAddress + pid2)));

            assertTrue(memberGraph.contains(Node.ANY,
                    createURI(serverAddress + pid1),
                    createURI(memberRelation),
                    createURI(serverAddress + pid2)));

            assertFalse("Should not contain inbound references to the other child", memberGraph.contains(Node.ANY,
                    createURI(serverAddress + pid1),
                    createURI(memberRelation),
                    createURI(serverAddress + pid3)));
        }
    }
    private static void createProxy(final String parent, final String child) {
        final HttpPost createProxy = new HttpPost(serverAddress + parent + "/members");
        createProxy.addHeader(CONTENT_TYPE, "text/turtle");
        final String proxyRDF = "<> <http://www.openarchives.org/ore/terms/proxyFor> <" + serverAddress + child + ">;"
            + " <http://www.openarchives.org/ore/terms/proxyIn> <" + serverAddress + parent + "> .";
        createProxy.setEntity(new StringEntity(proxyRDF, UTF_8));
        assertEquals(CREATED.getStatusCode(), getStatus(createProxy));
    }

    @Test
@Ignore
    public void testLinkToNonExistent() throws IOException {
        final HttpPatch patch = new HttpPatch(getLocation(postObjMethod()));
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity("INSERT { " +
                "<> <http://some-vocabulary#isMemberOfCollection> <" + serverAddress + "non-existant> } WHERE {}"));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patch));
    }

    @Test
@Ignore
    public void testUpdateAndReplaceObjectGraph() throws IOException {
        final String subjectURI = getLocation(postObjMethod());
        final HttpPatch updateObjectGraphMethod = new HttpPatch(subjectURI);
        updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity("INSERT {<" + subjectURI + "> <info:test#label> \"foo\" ; " +
                " <info:test#number> 42 ; " +
                " <info:test#date> \"1953?\"^^<http://id.loc.gov/datatypes/edtf/EDTF> }" +
                " WHERE {}"));
        executeAndClose(updateObjectGraphMethod);
        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue("Didn't find a triple we thought we added.", graph.contains(ANY,
                    createURI(subjectURI), createURI("info:test#label"), createLiteral("foo")));
            assertTrue("Didn't find a triple we thought we added.", graph.contains(ANY,
                    createURI(subjectURI), createURI("info:test#number"), createLiteral("42", XSDinteger)));
            assertTrue("Didn't find a triple we thought we added.", graph.contains(ANY,
                    createURI(subjectURI), createURI("info:test#date"), createLiteral("1953?",
                        getInstance().getSafeTypeByName("http://id.loc.gov/datatypes/edtf/EDTF"))));
        }
        updateObjectGraphMethod.setEntity(new StringEntity("DELETE WHERE { " +
                    "<" + subjectURI + "> <info:test#label> \"foo\"." +
                    "<" + subjectURI + "> <info:test#number> 42 . " +
                    "<" + subjectURI + "> <info:test#date> \"1953?\"^^<http://id.loc.gov/datatypes/edtf/EDTF> . }; \n" +
                "INSERT {<" + subjectURI + "> <info:test#label> \"qwerty\" ; " +
                "<info:test#number> 43 ; " +
                "<info:test#date> \"1953\"^^<http://id.loc.gov/datatypes/edtf/EDTF> . } \n" +
                "WHERE { }"));

        try (final CloseableHttpResponse response = execute(updateObjectGraphMethod)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
            assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));
        }
        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertFalse("Found a triple we thought we deleted.", graph.contains(ANY,
                    createURI(subjectURI), createURI("info:test#label"), createLiteral("foo")));
            assertFalse("Found a triple we thought we deleted.", graph.contains(ANY,
                    createURI(subjectURI), createURI("info:test#number"), createLiteral("42", XSDinteger)));
            assertFalse("Found a triple we thought we deleted.", graph.contains(ANY,
                    createURI(subjectURI), createURI("info:test#date"), createLiteral("1953?",
                        getInstance().getSafeTypeByName("http://id.loc.gov/datatypes/edtf/EDTF"))));
        }
    }

    @Test
@Ignore
    public void testUpdateObjectGraphWithProblems() throws IOException {
        final String subjectURI = getLocation(postObjMethod());
        final Link ex = fromUri(URI.create(serverAddress + "static/constraints/ServerManagedPropertyException.rdf"))
                .rel(CONSTRAINED_BY.getURI()).build();

        final HttpPatch patchObjMethod = new HttpPatch(subjectURI);
        patchObjMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        patchObjMethod.setEntity(new StringEntity("INSERT { <" +
                subjectURI + "> <" + REPOSITORY_NAMESPACE + "uuid> \"value-doesn't-matter\" } WHERE {}\n"));
        try (final CloseableHttpResponse response = execute(patchObjMethod)) {
            assertEquals(CONFLICT.getStatusCode(), getStatus(response));
            assertEquals(ex.toString(), response.getFirstHeader(LINK).getValue());
        }
    }

    @Test
@Ignore
    public void testPutResourceBadRdf() throws IOException {
        final HttpPut httpPut = new HttpPut(serverAddress + getRandomUniqueId());
        httpPut.setHeader(CONTENT_TYPE, "text/turtle");
        httpPut.setEntity(new StringEntity("<> a \"still image\"."));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(httpPut));
    }

    @Test
@Ignore
    public void testRepeatedPut() {
        final String id = getRandomUniqueId();
        assertEquals(CREATED.getStatusCode(), getStatus(new HttpPut(serverAddress + id)));

        final HttpPut secondPut = new HttpPut(serverAddress + id);
        secondPut.setHeader(CONTENT_TYPE, "text/turtle");
        assertEquals(CONFLICT.getStatusCode(), getStatus(secondPut));
    }

    @Test
    public void testCreateResourceWithoutContentType() {
        assertEquals(CREATED.getStatusCode(), getStatus(new HttpPut(serverAddress + getRandomUniqueId())));
    }

    @Test
@Ignore
    public void testUpdateObjectWithoutContentType() throws IOException {
        final HttpPut httpPut = new HttpPut(getLocation(postObjMethod()));
        // use a bytestream-based entity to avoid settin a content type
        httpPut.setEntity(new ByteArrayEntity("bogus content".getBytes(UTF_8)));
        assertEquals(UNSUPPORTED_MEDIA_TYPE.getStatusCode(), getStatus(httpPut));
    }

    @Test
    public void testUpdateBinaryWithoutContentType() throws IOException {
        final String id = getRandomUniqueId();
        assertEquals(CREATED.getStatusCode(), getStatus(putObjMethod(id)));
        createDatastream(id, "x", "xyz");
        final HttpPut httpPut = new HttpPut(serverAddress + id + "/x");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(httpPut));
    }

    @Test
    public void testCreateBinaryUpperCaseMimeType() throws IOException {
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut createMethod = new HttpPut(subjectURI);
        createMethod.addHeader(CONTENT_TYPE, "TEXT/PlAiN");
        createMethod.setEntity(new StringEntity("Some text here."));
        createMethod.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);

        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));

            final String receivedLocation = response.getFirstHeader("Location").getValue();
            assertEquals("Got wrong URI in Location header for datastream creation!", subjectURI, receivedLocation);

            final Link link = Link.valueOf(response.getFirstHeader(LINK).getValue());
            // ensure it's a binary; if it doesn't have describedby, then it's not
            assertEquals("No described by header!", "describedby", link.getRel());
        }
    }

    /* https://jira.duraspace.org/browse/FCREPO-2520: create binary with fine mime type then
     * attempt to change the mime type to something syntactically invalid. Ensure one can still retrieve it.
     */
    @Test
@Ignore
    public void testBinarySetBadMimeType() throws IOException {
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut createMethod = new HttpPut(subjectURI);
        createMethod.addHeader(CONTENT_TYPE, "text/plain");
        createMethod.setEntity(new StringEntity("Some text here."));
        createMethod.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);

        assertEquals(CREATED.getStatusCode(), getStatus(createMethod));

        final HttpPatch patch = new HttpPatch(subjectURI + "/fcr:metadata");
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ebucore: <http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#> " +
                "INSERT { <" + subjectURI + "> ebucore:hasMimeType> \"-- invalid syntax! --\" } WHERE {}")
        );

        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patch));

        // make sure it's still retrievable
        final HttpGet getMethod = new HttpGet(subjectURI);
        try (final CloseableHttpResponse response = execute(getMethod)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final Collection<String> contentTypes = getHeader(response, CONTENT_TYPE);
            final String contentType = contentTypes.iterator().next();
            assertTrue("GET: Expected 'text/plain' instead got: '" + contentType + "'",
                    contentType.contains("text/plain"));
        }

        final HttpHead httpHead = new HttpHead(subjectURI);
        try (final CloseableHttpResponse response = execute(httpHead)) {
            final Collection<String> contentTypes = getHeader(response, CONTENT_TYPE);
            final String contentType = contentTypes.iterator().next();
            assertTrue("HEAD: Expected 'text/plain' instead got: '" + contentType + "'",
                    contentType.contains("text/plain"));
        }
    }

    @Test
    public void testCreateBinaryCSV() throws IOException {
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut createMethod = new HttpPut(subjectURI);
        createMethod.addHeader(CONTENT_TYPE, "text/csv");
        createMethod.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        createMethod.setEntity(new StringEntity("Header 1, Header 2, Header 3\r1,2,3\r1,2,3"));

        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));

            final String receivedLocation = response.getFirstHeader("Location").getValue();

            assertEquals("Got wrong URI in Location header for datastream creation!", subjectURI, receivedLocation);

            final Link link = Link.valueOf(response.getFirstHeader(LINK).getValue());
            // ensure it's a binary; if it doesn't have describedby, then it's not
            assertEquals("No described by header!", "describedby", link.getRel());
        }
    }

    @Test
@Ignore
    public void testRoundTripReplaceGraphForDatastreamDescription() throws IOException {
        final String id = getRandomUniqueId();
        final String subjectURI = serverAddress + id + "/ds1";
        createDatastream(id, "ds1", "some-content");

        final HttpGet getObjMethod = new HttpGet(subjectURI + "/" + FCR_METADATA);
        getObjMethod.addHeader(ACCEPT, "text/turtle");
        final Model model = createDefaultModel();
        try (final CloseableHttpResponse getResponse = execute(getObjMethod)) {
            final String graph = EntityUtils.toString(getResponse.getEntity());
            logger.trace("Got serialized object graph for testRoundTripReplaceGraphForDatastream():\n {}", graph);
            try (final StringReader r = new StringReader(graph)) {
                model.read(r, subjectURI, "TURTLE");
            }
        }
        final HttpPut replaceMethod = new HttpPut(subjectURI + "/" + FCR_METADATA);
        try (final StringWriter w = new StringWriter()) {
            model.write(w, "N-TRIPLE");
            final String updatedMetadata = w.toString() +
                "<" + subjectURI + "> <http://www.w3.org/2000/01/rdf-schema#label> 'foo' .";
            replaceMethod.setEntity(new StringEntity(updatedMetadata));
            logger.trace("Transmitting object graph for testRoundTripReplaceGraphForDatastream():\n {}", w);
        }
        replaceMethod.addHeader(CONTENT_TYPE, "application/n-triples");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(replaceMethod));
    }

    @Test
    public void testResponseContentTypes() throws Exception {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        for (final String type : POSSIBLE_RDF_RESPONSE_VARIANTS_STRING) {
            final HttpGet method = new HttpGet(serverAddress + id);
            method.addHeader(ACCEPT, type);
            assertEquals(type, getContentType(method));
        }
    }

    @Test
    public void testDescribeRdfCached() throws IOException {
        try (final CloseableHttpClient cachClient = CachingHttpClientBuilder.create().setCacheConfig(DEFAULT).build()) {
            final String location = getLocation(postObjMethod());
            try (final CloseableHttpResponse response = cachClient.execute(new HttpGet(location))) {
                assertEquals("Client didn't return a OK!", OK.getStatusCode(), getStatus(response));
                logger.debug("Found HTTP headers:\n{}", asList(response.getAllHeaders()));
                assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
                final String lastModed = response.getFirstHeader("Last-Modified").getValue();
                final String etag = response.getFirstHeader("ETag").getValue();
                final HttpGet getObjMethod2 = new HttpGet(location);
                getObjMethod2.setHeader("If-Modified-Since", lastModed);
                getObjMethod2.setHeader("If-None-Match", etag);
                assertEquals("Client didn't get a NOT_MODIFIED!", NOT_MODIFIED.getStatusCode(),
                        getStatus(getObjMethod2));
            }
        }
    }

    @Test
    public void testValidHTMLForRepo() throws IOException, SAXException {
        validateHTML("");
    }

    @Test
    public void testValidHTMLForObject() throws IOException, SAXException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        validateHTML(id);
    }

    @Test
    public void testValidHTMLForDS() throws IOException, SAXException {
        final String id = getRandomUniqueId();
        createDatastream(id, "ds", "content");
        validateHTML(id + "/ds/" + FCR_METADATA);
    }

    private static void validateHTML(final String path) throws IOException, SAXException {
        final HttpGet getMethod = getObjMethod(path);
        getMethod.addHeader(ACCEPT, "text/html");
        try (final CloseableHttpResponse response = execute(getMethod)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final String content = EntityUtils.toString(response.getEntity());
            logger.trace("Retrieved HTML view:\n" + content);
            final HtmlParser htmlParser = new HtmlParser(ALLOW);
            htmlParser.setDoctypeExpectation(NO_DOCTYPE_ERRORS);
            htmlParser.setErrorHandler(new HTMLErrorHandler());
            htmlParser.setContentHandler(new TreeBuilder());
            try (final InputStream htmlStream = new ByteArrayInputStream(content.getBytes(UTF_8))) {
                htmlParser.parse(new InputSource(htmlStream));
            }
            logger.debug("HTML found to be valid.");
        }
    }

    private static class HTMLErrorHandler implements ErrorHandler {

        @Override
        public void warning(final SAXParseException e) {
            fail(e.toString());
        }

        @Override
        public void error(final SAXParseException e) {
            fail(e.toString());
        }

        @Override
        public void fatalError(final SAXParseException e) {
            fail(e.toString());
        }
    }

    @Test
@Ignore
    public void testLinkedDeletion() {
        final String linkedFrom = getRandomUniqueId();
        final String linkedTo = getRandomUniqueId();
        createObjectAndClose(linkedFrom);
        createObjectAndClose(linkedTo);

        final String sparql =
                "INSERT DATA { <" + serverAddress + linkedFrom + "> " +
                        "<http://some-vocabulary#isMemberOfCollection> <" + serverAddress + linkedTo + "> . }";
        final HttpPatch patch = patchObjMethod(linkedFrom);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new ByteArrayEntity(sparql.getBytes(UTF_8)));
        assertEquals("Couldn't link resources!", NO_CONTENT.getStatusCode(), getStatus(patch));
        assertEquals("Error deleting linked-to!", NO_CONTENT.getStatusCode(), getStatus(deleteObjMethod(linkedTo)));
        assertEquals("Linked to should still exist!", OK.getStatusCode(), getStatus(getObjMethod(linkedFrom)));
    }

    @Test
@Ignore
    public void testUpdateObjectWithSpaces() throws IOException {
        final String id = getRandomUniqueId() + " 2";
        try (final CloseableHttpResponse createResponse = createObject(id)) {
            final String subjectURI = getLocation(createResponse);
            final HttpPatch updateObjectGraphMethod = new HttpPatch(subjectURI);
            updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
            updateObjectGraphMethod.setEntity(new StringEntity(
                    "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"test\" } WHERE {}"));
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));
        }
    }

    @Test
    public void testCreatedAndModifiedDates() throws IOException {
        final String location = getLocation(postObjMethod());
        final HttpGet getObjMethod = new HttpGet(location);
        try (final CloseableHttpResponse response = execute(getObjMethod)) {
            try (final CloseableDataset dataset = getDataset(response)) {
                final DatasetGraph graph = dataset.asDatasetGraph();
                final Model model = createModelForGraph(graph.getDefaultGraph());
                final Resource nodeUri = createResource(location);
                final String lastmodString = response.getFirstHeader("Last-Modified").getValue();
                final Optional<Instant> createdDateTriples =
                        getDateFromModel(model, nodeUri, createProperty(REPOSITORY_NAMESPACE + "created"));
                final Optional<Instant> lastmodDateTriples =
                        getDateFromModel(model, nodeUri, createProperty(REPOSITORY_NAMESPACE + "lastModified"));
                assertTrue(createdDateTriples.isPresent());
                assertTrue(lastmodDateTriples.isPresent());
                // Reformatting lastModified header to ensure consistent formatting between it and fedora timestamps
                final Instant lastMod = headerFormat.parse(lastmodString, Instant::from);
                final String formattedLastModifiedHeader = headerFormat.format(lastMod);
                assertEquals(formattedLastModifiedHeader, headerFormat.format(createdDateTriples.get()));
                assertEquals(formattedLastModifiedHeader, headerFormat.format(lastmodDateTriples.get()));
            }
        }
    }

    @Test
@Ignore
    public void testPostCreateDate() throws IOException {
        final HttpPost httpPost = postObjMethod("/");
        httpPost.addHeader("Slug", getRandomUniqueId());
        httpPost.addHeader(CONTENT_TYPE, "text/turtle");
        httpPost.setEntity(new StringEntity(getTTLThatUpdatesServerManagedTriples("fakeuser", null, null, null)));
        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Must not be able to update createdBy!", CONFLICT.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testNonRDFSourceInteraction() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id, "text/turtle", "<> a <http://example.com/Foo> .");
        put.setHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        executeAndClose(put);
        assertTrue(getLinkHeaders(getObjMethod(id)).contains(NON_RDF_SOURCE_LINK_HEADER));
    }

    @Test
@Ignore
    public void testContainerInteraction() throws IOException {
        final String id = getRandomUniqueId();
        final String location;
        final HttpPut put0 = putObjMethod(id);
        put0.setHeader(LINK, BASIC_CONTAINER_LINK_HEADER);
        try (final CloseableHttpResponse response = execute(put0)) {
            location = getLocation(response);
        }
        assertTrue(getLinkHeaders(getObjMethod(id)).contains(BASIC_CONTAINER_LINK_HEADER));

        final String ttl = "<> <" + MEMBERSHIP_RESOURCE + "> <" + location + ">; "
                + "<" + HAS_MEMBER_RELATION + "> <info:some/relation> .\n";
        final HttpPut put1 = putObjMethod(id + "/a", "text/turtle", ttl);
        put1.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put1);
        assertTrue(getLinkHeaders(getObjMethod(id + "/a")).contains(DIRECT_CONTAINER_LINK_HEADER));
        createObject(id + "/a/1");

        final String ttl2 = "<> <" + MEMBERSHIP_RESOURCE + "> <" + location + ">; "
                + "<" + HAS_MEMBER_RELATION + "> <info:some/another-relation> .\n";
        final HttpPut put2 = putObjMethod(id + "/b", "text/turtle", ttl2);
        put2.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        executeAndClose(put2);
        assertTrue(getLinkHeaders(getObjMethod(id + "/b")).contains(DIRECT_CONTAINER_LINK_HEADER));
        createObject(id + "/b/1");

        try (final CloseableDataset dataset = getDataset(new HttpGet(location))) {
            final DatasetGraph graphStore = dataset.asDatasetGraph();
            final Node resource = createURI(location);
            assertTrue("Expected to have container t", graphStore.contains(ANY,
                    resource, createURI(LDP_NAMESPACE + "contains"), createURI(location + "/a")));
            assertTrue("Expected to have container b", graphStore.contains(ANY,
                    resource, createURI(LDP_NAMESPACE + "contains"), createURI(location + "/b")));
            assertTrue("Expected member relation", graphStore.contains(ANY,
                    resource, createURI("info:some/relation"), createURI(location + "/a/1")));
            assertTrue("Expected other member relation", graphStore.contains(ANY,
                    resource, createURI("info:some/another-relation"), createURI(location + "/b/1")));
        }
    }

    @Test
@Ignore
    public void testIndirectContainerInteraction() throws IOException {

        // Create resource (object)
        final String resourceId = getRandomUniqueId();
        final String resource;
        try (final CloseableHttpResponse createResponse = createObject(resourceId)) {
            resource = getLocation(createResponse);
        }
        // Create container (c0)
        final String containerId = getRandomUniqueId();
        final String container;
        try (final CloseableHttpResponse createResponse = createObject(containerId)) {
            container = getLocation(createResponse);
        }
        // Create indirect container (c0/members)
        final String indirectContainerId = containerId + "/t";
        final String indirectContainer;
        final String ttl = "<> <" + MEMBERSHIP_RESOURCE + "> <" + container + ">;\n"
                + "<" + HAS_MEMBER_RELATION + "> <info:some/relation>;\n"
                + "<" + LDP_NAMESPACE + "insertedContentRelation> <info:proxy/for> .\n";
        final HttpPut put = putObjMethod(containerId + "/t", "text/turtle", ttl);
        put.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        try (final CloseableHttpResponse response = execute(put)) {
            indirectContainer = getLocation(response);
        }
        assertTrue(getLinkHeaders(getObjMethod(containerId + "/t")).contains(INDIRECT_CONTAINER_LINK_HEADER));

        // Add indirect resource to indirect container
        final HttpPost postIndirectResource = postObjMethod(indirectContainerId);
        final String irRdf =
                "<> <info:proxy/in>  <" + container + "> ;\n" +
                        "   <info:proxy/for> <" + resource + "> .";
        postIndirectResource.setEntity(new StringEntity(irRdf));
        postIndirectResource.setHeader(CONTENT_TYPE, "text/turtle");

        final String indirectResource;
        try (final CloseableHttpResponse postResponse = execute(postIndirectResource)) {
            indirectResource = getLocation(postResponse);
            assertEquals("Expected post to succeed", CREATED.getStatusCode(), getStatus(postResponse));
        }
        // Ensure container has been updated with relationship... indirectly
        try (final CloseableHttpResponse getResponse = execute(new HttpGet(container));
                final CloseableDataset dataset = getDataset(getResponse)) {
            final DatasetGraph graphStore = dataset.asDatasetGraph();
            assertTrue("Expected to have indirect container", graphStore.contains(ANY,
                    createURI(container), createURI(LDP_NAMESPACE + "contains"), createURI(indirectContainer)));

            assertTrue("Expected to have resource: " + graphStore.toString(), graphStore.contains(ANY,
                    createURI(container), createURI("info:some/relation"), createURI(resource)));
        }
        // Remove indirect resource
        assertEquals("Expected delete to succeed",
                NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(indirectResource)));

        // Ensure container has been updated with relationship... indirectly
        try (final CloseableHttpResponse getResponse1 = execute(new HttpGet(container));
                final CloseableDataset dataset = getDataset(getResponse1)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertFalse("Expected NOT to have resource: " + graph, graph.contains(ANY,
                    createURI(container), createURI("info:some/relation"), createURI(resource)));
        }
    }

    @Test
@Ignore
    public void testIndirectContainerInteractionMemberOf() throws IOException {
        // Create resource (object)
        final String resourceId = getRandomUniqueId();
        final String resource;
        try (final CloseableHttpResponse createResponse = createObject(resourceId)) {
            resource = getLocation(createResponse);
        }
        // Create container (c0)
        final String containerId = getRandomUniqueId();
        final String container;
        try (final CloseableHttpResponse createResponse = createObject(containerId)) {
            container = getLocation(createResponse);
        }
        // Create indirect container (c0/members)
        final String indirectContainerId = containerId + "/t";
        final HttpPut putIndirect = putObjMethod(indirectContainerId);
        putIndirect.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        final String indirectContainer;
        try (final CloseableHttpResponse createResponse = execute(putIndirect)) {
            indirectContainer = getLocation(createResponse);
        }

        // Add LDP properties to indirect container
        final HttpPatch patch = patchObjMethod(indirectContainerId);
        patch.addHeader("Content-Type", "application/sparql-update");
        final String sparql = "INSERT DATA { "
                + "<> <" + MEMBERSHIP_RESOURCE + "> <" + container + "> .\n"
                + "<> <" + IS_MEMBER_OF_RELATION + "> <info:some/relation> .\n"
                + "<> <" + LDP_NAMESPACE + "insertedContentRelation> <info:proxy/for> .\n"
                + " }";
        patch.setEntity(new StringEntity(sparql));
        assertEquals("Expected patch to succeed", NO_CONTENT.getStatusCode(), getStatus(patch));

        // Add indirect resource to indirect container
        final HttpPost postIndirectResource = postObjMethod(indirectContainerId);
        final String irRdf =
                "<> <info:proxy/in>  <" + container + "> ;\n" +
                        "   <info:proxy/for> <" + resource + "> .";
        postIndirectResource.setEntity(new StringEntity(irRdf));
        postIndirectResource.setHeader("Content-Type", "text/turtle");

        final String indirectResource;
        try (final CloseableHttpResponse postResponse = execute(postIndirectResource)) {
            indirectResource = getLocation(postResponse);
            assertEquals("Expected post to succeed", CREATED.getStatusCode(), getStatus(postResponse));
        }
        // Ensure resource has been updated with relationship... indirectly
        try (final CloseableHttpResponse getResponse = execute(new HttpGet(resource));
             final CloseableDataset dataset = getDataset(getResponse)) {
                final DatasetGraph graph = dataset.asDatasetGraph();
                assertTrue("Expected to have triple on resource: " + createURI("info:some/relation") + " <"
                                + createURI(container) + ">, graph: " + graph.toString(),
                    graph.contains(ANY, createURI(resource), createURI("info:some/relation"), createURI(container)));
        }
        // Remove indirect resource
        assertEquals("Expected delete to succeed",
                NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(indirectResource)));

        // Ensure resource has been updated with relationship... indirectly
        try (final CloseableHttpResponse getResponse1 = execute(new HttpGet(resource));
             final CloseableDataset dataset = getDataset(getResponse1)) {
            final DatasetGraph graph1 = dataset.asDatasetGraph();
            assertFalse("Expected NOT to have resource: " + graph1, graph1.contains(ANY,
                    createURI(resource), createURI("info:some/relation"), createURI(container)));
        }
    }

    @Test
@Ignore
    public void testWithHashUris() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "text/turtle");
        method.setEntity(new StringEntity("<> <info:some-predicate> <#abc> .\n"
                + "<#abc> <info:test#label> \"asdfg\" ."));
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            final String location = getLocation(response);
            try (final CloseableDataset dataset = getDataset(new HttpGet(location))) {
                final DatasetGraph graphStore = dataset.asDatasetGraph();
                assertTrue(graphStore.contains(ANY,
                        createURI(location), createURI("info:some-predicate"), createURI(location + "#abc")));
                assertTrue(graphStore.contains(ANY,
                        createURI(location + "#abc"), createURI("info:test#label"), createLiteral("asdfg")));
                assertFalse(graphStore.contains(ANY,
                        createURI(location + "#abc"), createURI(REPOSITORY_NAMESPACE + "lastModified"), ANY));
                assertFalse(graphStore.contains(ANY,
                        createURI(location + "#abc"), rdfType, createURI(REPOSITORY_NAMESPACE + "Resource")));
            }
        }
    }

    @Test
    public void testCreateAndReplaceGraphMinimal() throws IOException {
        LOGGER.trace("Entering testCreateAndReplaceGraphMinimal()...");
        final HttpPost httpPost = postObjMethod("/");
        httpPost.addHeader("Slug", getRandomUniqueId());
        httpPost.addHeader(CONTENT_TYPE, "text/turtle");
        httpPost.setEntity(new StringEntity("<> <" + DCTITLE.getURI() + "> \"abc\""));
        final String subjectURI;
        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }
        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader(CONTENT_TYPE, "text/turtle");
        replaceMethod.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
        replaceMethod.setEntity(new StringEntity("<> <" + DCTITLE.getURI() + "> \"xyz\""));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(replaceMethod));

        final HttpGet get = new HttpGet(subjectURI);
        get.addHeader("Prefer", "return=minimal");
        try (final CloseableDataset dataset = getDataset(get)) {
            assertTrue(dataset.asDatasetGraph().contains(ANY, ANY, DCTITLE, createLiteral("xyz")));
        }
        LOGGER.trace("Done with testCreateAndReplaceGraphMinimal().");
    }

    @Test
    public void testAclHeaderWithPost() throws IOException {
        final String pid = getRandomUniqueId();
        final HttpPost httpPost = postObjMethod("/");
        httpPost.addHeader("Slug", pid);
        final String location = serverAddress + pid;
        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            checkForLinkHeader(response, location + "/" + FCR_ACL, "acl");
        }
    }

    @Test
    public void testAclHeaderWithPut() throws IOException {
        final String pid = getRandomUniqueId();
        final String location = serverAddress + pid;

        final HttpPut httpPut = new HttpPut(location);
        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            checkForLinkHeader(response, location + "/" + FCR_ACL, "acl");
        }
    }

    @Test
    @Ignore("This test needs manual intervention to decide how \"good\" the graph looks")
    // TODO Do we have any way to proceed with this kind of aesthetic goal?
    public void testGraphShouldNotBeTooLumpy() throws IOException {

        final HttpPut httpPut = putObjMethod(getRandomUniqueId());
        httpPut.addHeader(CONTENT_TYPE, "text/turtle");
        httpPut.setEntity(new StringEntity("<> a <" + DIRECT_CONTAINER.getURI() + ">;" +
                "    <" + MEMBERSHIP_RESOURCE.getURI() + "> <> ;" +
                "    <" + HAS_MEMBER_RELATION.getURI() + "> <" + LDP_NAMESPACE + "member> ;" +
                "    <info:x> <#hash-uri> ;" +
                "    <info:x> [ <" + DCTITLE.getURI() + "> \"xyz\" ] . " +
                "<#hash-uri>  <" + DCTITLE.getURI() + "> \"some-hash-uri\" ."));

        /*
         * final HttpResponse response = execute(httpPut); final int status =
         * response.getStatusLine().getStatusCode(); assertEquals("Didn't get a CREATED response!",
         * CREATED.getStatusCode(), status); final String subjectURI = response.getFirstHeader("Location").getValue();
         * final HttpGet get = new HttpGet(subjectURI); final HttpResponse getResponse = execute(get); final String s
         * = EntityUtils.toString(getResponse.getEntity());
         */

    }

    @Test
@Ignore
    public void testEmbeddedContainedResources() throws IOException {
        final String id = getRandomUniqueId();
        final String binaryId = "binary0";
        final String preferEmbed =
                "return=representation; include=\"http://www.w3.org/ns/oa#PreferContainedDescriptions\"";

        assertEquals(CREATED.getStatusCode(), getStatus(putObjMethod(id)));
        assertEquals(CREATED.getStatusCode(), getStatus(putDSMethod(id, binaryId, "some test content")));

        final HttpPatch httpPatch = patchObjMethod(id + "/" + binaryId + "/fcr:metadata");
        httpPatch.addHeader(CONTENT_TYPE, "application/sparql-update");
        httpPatch.setEntity(new StringEntity(
                "INSERT { <> <http://purl.org/dc/elements/1.1/title> 'this is a title' } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(httpPatch));

        final HttpGet httpGet = getObjMethod(id);
        httpGet.setHeader("Prefer", preferEmbed);
        try (final CloseableHttpResponse response = execute(httpGet)) {
            final Collection<String> preferenceApplied = getHeader(response, "Preference-Applied");
            assertTrue("Preference-Applied header doesn't match", preferenceApplied.contains(preferEmbed));

            final DatasetGraph graphStore = getDataset(response).asDatasetGraph();
            assertTrue("Property on child binary should be found!" + graphStore, graphStore.contains(ANY,
                    createURI(serverAddress + id + "/" + binaryId),
                    createURI("http://purl.org/dc/elements/1.1/title"), createLiteral("this is a title")));
        }
    }



    @Test
    public void testJsonLdProfileCompacted() throws IOException {
        // Create a resource
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "text/n3");
        final BasicHttpEntity entity = new BasicHttpEntity();
        final String rdf = "<> <http://purl.org/dc/elements/1.1/title> \"ceci n'est pas un titre français\"@fr ." +
                "<> <http://purl.org/dc/elements/1.1/title> \"this is an english title\"@en .";
        entity.setContent(new ByteArrayInputStream(rdf.getBytes(UTF_8)));
        method.setEntity(entity);

        final String location;
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            location = response.getFirstHeader("Location").getValue();
        }
        // GET the resource with a JSON profile
        final HttpGet httpGet = new HttpGet(location);
        httpGet.setHeader(ACCEPT, "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"");
        final JsonNode json;
        try (final CloseableHttpResponse responseGET = execute(httpGet)) {
            // Inspect the response
            final ObjectMapper mapper = new ObjectMapper();
            json = mapper.readTree(responseGET.getEntity().getContent());
        }

        final JsonNode titles = json.get("title");
        assertNotNull(titles);
        assertTrue("Should be a list", titles.isArray());

        assertEquals("Should be two langs!", 2, titles.findValues("@language").size());
        assertEquals("Should be two values!", 2, titles.findValues("@value").size());
    }

    @Test
    public void testJsonLdProfileExpanded() throws IOException {
        // Create a resource
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "text/n3");
        final BasicHttpEntity entity = new BasicHttpEntity();
        final String rdf = "<> <http://purl.org/dc/elements/1.1/title> \"ceci n'est pas un titre français\"@fr ." +
                "<> <http://purl.org/dc/elements/1.1/title> \"this is an english title\"@en .";
        entity.setContent(new ByteArrayInputStream(rdf.getBytes(UTF_8)));
        method.setEntity(entity);

        final String location;
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            location = response.getFirstHeader("Location").getValue();
        }
        // GET the resource with a JSON profile
        final HttpGet httpGet = new HttpGet(location);
        httpGet.setHeader(ACCEPT, "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#expanded\"");
        final JsonNode json;
        try (final CloseableHttpResponse responseGET = execute(httpGet)) {
            // Inspect the response
            final ObjectMapper mapper = new ObjectMapper();
            json = mapper.readTree(responseGET.getEntity().getContent());
        }

        final List<JsonNode> titlesList = json.findValues("http://purl.org/dc/elements/1.1/title");
        assertNotNull(titlesList);
        assertEquals("Should be list of lists", 1, titlesList.size());

        final JsonNode titles = titlesList.get(0);
        assertEquals("Should be two langs!", 2, titles.findValues("@language").size());
        assertEquals("Should be two values!", 2, titles.findValues("@value").size());
    }

    @Test
    public void testJsonLdProfileFlattened() throws IOException {
        // Create a resource
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "text/n3");
        final BasicHttpEntity entity = new BasicHttpEntity();
        final String rdf = "<> <http://purl.org/dc/elements/1.1/title> \"ceci n'est pas un titre français\"@fr ." +
                "<> <http://purl.org/dc/elements/1.1/title> \"this is an english title\"@en .";
        entity.setContent(new ByteArrayInputStream(rdf.getBytes(UTF_8)));
        method.setEntity(entity);

        final String location;
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            location = response.getFirstHeader("Location").getValue();
        }
        // GET the resource with a JSON profile
        final HttpGet httpGet = new HttpGet(location);
        httpGet.setHeader(ACCEPT, "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#flattened\"");
        final JsonNode json;
        try (final CloseableHttpResponse responseGET = execute(httpGet)) {
            // Inspect the response
            final ObjectMapper mapper = new ObjectMapper();
            json = mapper.readTree(responseGET.getEntity().getContent());
        }

        final List<JsonNode> titlesList = json.get("@graph").findValues("title");
        assertNotNull(titlesList);
        assertEquals("Should be list of lists", 1, titlesList.size());

        final JsonNode titles = titlesList.get(0);
        assertEquals("Should be two langs!", 2, titles.findValues("@language").size());
        assertEquals("Should be two values!", 2, titles.findValues("@value").size());
    }

    @Test
@Ignore
    public void testPathWithEmptySegment() {
        final String badLocation = "test/me/mb/er/s//members/9528a300-22da-40f2-bf3c-5b345d71affb";
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(headObjMethod(badLocation)));
    }

    private static Optional<Instant> getDateFromModel(final Model model, final Resource subj, final Property pred)
            throws NoSuchElementException {
        final StmtIterator stmts = model.listStatements(subj, pred, (String) null);
        return Optional.ofNullable(
           stmts.hasNext() ? Instant.from(tripleFormat.parse(stmts.nextStatement().getString())) : null);
    }

    @Test
@Ignore
    public void testUpdateObjectGraphWithNonLocalTriples() throws IOException {
        final String pid = getRandomUniqueId();
        createObject(pid);
        final String otherPid = getRandomUniqueId();
        createObject(otherPid);
        final String location = serverAddress + pid;
        final String otherLocation = serverAddress + otherPid;
        final HttpPatch updateObjectGraphMethod = new HttpPatch(location);
        updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity("INSERT { <" + location +
                "> <http://purl.org/dc/elements/1.1/identifier> \"this is an identifier\". " + "<" + otherLocation +
                "> <http://purl.org/dc/elements/1.1/identifier> \"this is an identifier\"" + " } WHERE {}"));
        assertEquals("It ought not be possible to use PATCH to create non-local triples!",
                FORBIDDEN.getStatusCode(),getStatus(updateObjectGraphMethod));
    }

    @Test
@Ignore
    public void testPutMalformedHeader() throws IOException {
        // Create a resource
        final String id = getRandomUniqueId();
        executeAndClose(putObjMethod(id));

        // Get the resource's etag
        final String etag;
        final HttpHead httpHead = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(httpHead)) {
            etag = response.getFirstHeader("ETag").getValue();
            assertNotNull("ETag was missing!?", etag);
        }

        // PUT properly formatted etag
        final HttpPut httpPut = putObjMethod(id);
        httpPut.addHeader("If-Match", etag);

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Should be a 412 Precondition Failed!", PRECONDITION_FAILED.getStatusCode(),
                    getStatus(response));
        }

        // PUT improperly formatted etag ... not quoted.
        final HttpPut httpPut2 = putObjMethod(id);
        httpPut2.addHeader("If-Match", etag.replace("\"", ""));

        try (final CloseableHttpResponse response = execute(httpPut2)) {
            assertEquals("Should be a 400 BAD REQUEST!", BAD_REQUEST.getStatusCode(), getStatus(response));
        }
    }

    @Test
@Ignore
    public void testPutServerManagedPredicateInIndirectContainer() throws IOException {
        LOGGER.info("running testPutServerManagedPredicateInIndirectContainer");
        // Create a resource
        final String id = getRandomUniqueId();
        executeAndClose(putObjMethod(id));

        final String child1 = id + "/1";
        executeAndClose(putObjMethod(child1));

        // create indirect container using a server managed triple.
        final String body =
                "@prefix ore: <http://www.openarchives.org/ore/terms/> .\n" +
                        "@prefix ldp: <http://www.w3.org/ns/ldp#> .\n" +
                        "@prefix pcdm: <http://pcdm.org/models#> .\n" +
                        "<> a ldp:IndirectContainer ;\n" +
                        "   ldp:hasMemberRelation <http://fedora.info/definitions/v4/repository#lastModified> ; \n" +
                        "   ldp:insertedContentRelation ore:proxyFor ; \n" +
                        "   ldp:membershipResource <" + serverAddress + child1 + "> . ";
        final String child1MembersId = child1 + "/members";
        final HttpPut httpPut = putObjMethod(child1MembersId);
        httpPut.addHeader("Content-Type", "text/turtle");
        httpPut.setEntity(new StringEntity(body));

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Should be a 409 CONFLICT!", CONFLICT.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testPutEmptyBody() throws IOException {
        final HttpPut httpPut = putObjMethod(getRandomUniqueId());
        httpPut.addHeader(CONTENT_TYPE, "application/ld+json");

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Should be a client error", BAD_REQUEST.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testPutOgg() throws IOException {
        final String id = getRandomUniqueId();
        execute(putObjMethod(id));
        createDatastream(id, "x", "OggS");
    }

    @Test
    public void testPutReferenceRoot() throws Exception {
        final HttpPut httpPut = putObjMethod(getRandomUniqueId());
        httpPut.addHeader(CONTENT_TYPE, "text/turtle");
        httpPut.setEntity(new StringEntity("@prefix acl: <http://www.w3.org/ns/auth/acl#> . " +
                "<> a acl:Authorization ; " +
                "acl:agent \"smith123\" ; " +
                "acl:mode acl:Read ;" +
                "acl:accessTo <" + serverAddress + "> ."));

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }
    }

    @Test
@Ignore
    public void testDeleteLargeLiteralStoredAsBinary() throws IOException {

        final String pid = getRandomUniqueId();
        createObject(pid);

        final Node DC_TITLE = title.asNode();
        final String location = serverAddress + pid;
        final HttpPatch patch = new HttpPatch(location);
        final HttpPatch delPatch = new HttpPatch(location);

        final String longLiteral =   // minimumBinaryInByteSize is currently 40 bytes
                TEST_BINARY_CONTENT +
                        TEST_BINARY_CONTENT +
                        TEST_BINARY_CONTENT +
                        TEST_BINARY_CONTENT;

        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "INSERT { <> <" + DC_TITLE + "> \"\"\"" + longLiteral + "\"\"\" } WHERE {}"));

        assertEquals("Unable to add property value", NO_CONTENT.getStatusCode(), getStatus(patch));

        delPatch.addHeader(CONTENT_TYPE, "application/sparql-update");
        delPatch.setEntity(new StringEntity(
                "DELETE WHERE { <> <" + DC_TITLE + "> \"\"\"" + longLiteral + "\"\"\"}"));

        // delete that triple, or at least try to.
        assertEquals("Unable to complete delete property HTTP request", NO_CONTENT.getStatusCode(),
                getStatus(delPatch));

        // now test if property exists anymore (it shouldn't).
        try (final CloseableDataset dataset = getDataset(getObjMethod(pid))) {
            assertFalse("Found the literal we tried to delete!", dataset.asDatasetGraph().contains(ANY,
                        createURI(location), DC_TITLE, createLiteral(longLiteral)));
        }
    }

    @Test
@Ignore
    public void testInboundLinksDoNotUpdateEtag() throws IOException {
        final String id1 = getRandomUniqueId();
        final HttpPut httpPut = putObjMethod(id1);
        final String oldETag;
        final String oldMod;
        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            oldETag = response.getFirstHeader("ETag").getValue();
            oldMod = response.getFirstHeader("Last-Modified").getValue();
        }

        final String id2 = getRandomUniqueId();
        createObject(id2).close();

        final HttpPatch patch = patchObjMethod(id2);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "INSERT { <> <http://purl.org/dc/elements/1.1/relation> <" + serverAddress + id1 + "> } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(execute(patch)));

        try (final CloseableHttpResponse response = execute(getObjMethod(id1))) {
            final String etag = response.getFirstHeader("ETag").getValue();
            final String lastmod = response.getFirstHeader("Last-Modified").getValue();
            assertEquals(oldMod, lastmod);
            assertEquals(oldETag, etag);
        }
    }

    @Test
    public void testCreationResponseDefault() {
        testCreationResponse(null, null, CREATED, "text/plain");
        testCreationResponse(null, "application/ld+json", NOT_ACCEPTABLE, "text/html");
    }

    @Test
    public void testCreationResponseMinimal() {
        testCreationResponse("minimal", null, CREATED, null);
        testCreationResponse("minimal", "application/ld+json", CREATED, null);
    }

    @Test
    public void testCreationResponseRepresentation() {
        testCreationResponse("representation", null, CREATED, "text/turtle");
        testCreationResponse("representation", "application/ld+json", CREATED, "application/ld+json");
    }

    private static void testCreationResponse(final String prefer,
                                      final String accept,
                                      final Status expectedStatus,
                                      final String expectedType) {

        final HttpPost createMethod = new HttpPost(serverAddress);
        if (prefer != null) {
            createMethod.addHeader("Prefer", "return=" + prefer);
        }
        if (accept != null) {
            createMethod.addHeader(ACCEPT, accept);
        }

        try (final CloseableHttpResponse createResponse = execute(createMethod)) {
            assertEquals(expectedStatus.getStatusCode(), createResponse.getStatusLine().getStatusCode());
            if (expectedType == null) {
                assertNull(createResponse.getFirstHeader(CONTENT_TYPE));
            } else {
                assertTrue(createResponse.getFirstHeader(CONTENT_TYPE).getValue().startsWith(expectedType));
            }
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
@Ignore
    public void testPathsWithBrackets() {
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(deleteObjMethod("%5bfoo")));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(getObjMethod("%5bfoo")));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patchObjMethod("%5bfoo")));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(postObjMethod("%5bfoo")));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(putObjMethod("%5bfoo")));
    }

    @Test
    public void testConcurrentPuts() throws InterruptedException, IOException {
        final String parent = getRandomUniqueId();
        executeAndClose(putObjMethod(parent));
        final String newResource = parent + "/test";
        final Runnable updateRunnable = () -> {
            executeAndClose(putObjMethod(newResource));
        };
        final Thread t1 = new Thread(updateRunnable);
        final Thread t2 = new Thread(updateRunnable);
        final Thread t3 = new Thread(updateRunnable);
        final Thread t4 = new Thread(updateRunnable);
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t1.join();
        t2.join();
        t3.join();
        t4.join();

        try (final CloseableDataset dataset = getDataset(getObjMethod(parent))) {
            final DatasetGraph graphStore = dataset.asDatasetGraph();
            final Iterator<Quad> children = graphStore.find(ANY, ANY, CONTAINS.asNode(), ANY);
            assertTrue("One of the PUTs should have resulted in a child.", children.hasNext());
            children.next();
            if (children.hasNext()) {
                fail("Only one of the PUTs should have resulted in a child (unexpected child "
                       + children.next().getObject().getURI() + ")!");
            }
        }
    }

    @Test
    public void testConcurrentPutsWithPairtrees() throws InterruptedException, IOException {
        final String parent = getRandomUniqueId();
        executeAndClose(putObjMethod(parent));
        final String first = parent + "/00/1";
        final String second = parent + "/00/2";
        final String third = parent + "/00/3";
        final String fourth = parent + "/00/4";
        final Thread t1 = new Thread(() -> {
            executeAndClose(putObjMethod(first));
        });
        final Thread t2 = new Thread(() -> {
            executeAndClose(putObjMethod(second));
        });
        final Thread t3 = new Thread(() -> {
            executeAndClose(putObjMethod(third));
        });
        final Thread t4 = new Thread(() -> {
            executeAndClose(putObjMethod(fourth));
        });
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t1.join();
        t2.join();
        t3.join();
        t4.join();

        try (final CloseableDataset dataset = getDataset(getObjMethod(parent))) {
            final DatasetGraph graphStore = dataset.asDatasetGraph();
            final List<String> childPaths = new ArrayList<>();
            final Iterator<Quad> children = graphStore.find(ANY, ANY, CONTAINS.asNode(), ANY);
            assertTrue("Four children should have been created (none found).", children.hasNext());
            childPaths.add(children.next().getObject().getURI());
            LOGGER.info("Found child: {}", childPaths.get(0));
            assertTrue("Four children should have been created (only one found).", children.hasNext());
            childPaths.add(children.next().getObject().getURI());
            LOGGER.info("Found child: {}", childPaths.get(1));
            assertTrue("Four children should have been created (only two found).", children.hasNext());
            childPaths.add(children.next().getObject().getURI());
            LOGGER.info("Found child: {}", childPaths.get(2));
            assertTrue("Four children should have been created. (only three found)", children.hasNext());
            childPaths.add(children.next().getObject().getURI());
            LOGGER.info("Found child: {}", childPaths.get(3));
            assertFalse("Only four children should have been created.", children.hasNext());
            assertTrue(childPaths.contains(serverAddress + first));
            assertTrue(childPaths.contains(serverAddress + second));
            assertTrue(childPaths.contains(serverAddress + third));
            assertTrue(childPaths.contains(serverAddress + fourth));
        }

    }

    @Test
    // TODO Test flaps in travis, causing more PUTs to succeed some of the time, possibly
    // because the etag is timestamp based. Should also make sure the correct failure status returned
    @Ignore
    public void testConcurrentUpdatesToBinary() throws IOException, InterruptedException {
        // create a binary
        final String path = getRandomUniqueId();

        final HttpPut method = new HttpPut(serverAddress + path);
        method.setHeader(CONTENT_TYPE, "text/plain");
        method.setHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        method.setEntity(new StringEntity("initial value"));

        final String binaryEtag;

        try (final CloseableHttpResponse response = execute(method)) {
            binaryEtag = response.getFirstHeader("ETag").getValue();
        }

        final PutThread[] threads = new PutThread[] {
                new PutThread(putBinaryObjMethodIfMatch(path, binaryEtag, "thread 1")),
                new PutThread(putBinaryObjMethodIfMatch(path, binaryEtag, "thread 2")),
                new PutThread(putBinaryObjMethodIfMatch(path, binaryEtag, "thread 3")),
                new PutThread(putBinaryObjMethodIfMatch(path, binaryEtag, "thread 4"))  };

        for (final PutThread t : threads) {
            t.start();
        }

        final List<PutThread> successfulThreads = new ArrayList<>();
        for (final PutThread t : threads) {
            t.join(1000);
            assertFalse("Thread " + t.getId() + " could not perform its operation in time!", t.isAlive());
            final int status = t.response.getStatusLine().getStatusCode();
            LOGGER.info("{} received a {} status code.", t.getId(), status);
            if (status == 204) {
                successfulThreads.add(t);
            }
        }

        assertEquals("Only one PUT request should have been successful!", 1, successfulThreads.size());
    }

    private class PutThread extends Thread {

        private final HttpPut put;

        private HttpResponse response;

        public PutThread(final HttpPut put) {
            this.put = put;
        }

        @Override
        public void run() {
            try {
                response = execute(put);
            } catch (final IOException e) {
                LOGGER.error("Thread " + Thread.currentThread().getId() + ", failed to PUT resource!", e);
            }

        }
    }

    private HttpPut putBinaryObjMethodIfMatch(final String location, final String etag, final String content)
            throws IOException {
        final HttpPut put = putObjMethod(location, "text/plain", content);
        put.setHeader("If-Match", etag);
        return put;
    }

    @Test
@Ignore
    public void testConcurrentPatches() throws IOException, InterruptedException {
        // create a resource
        final String path = getRandomUniqueId();
        executeAndClose(putObjMethod(path));
        final int[] responseCodes = new int[4];
        final Thread t1 = new Thread(() -> {
            responseCodes[0] = patchWithSparql(path,
                "PREFIX dc: <http://purl.org/dc/elements/1.1/>\nINSERT DATA { <> dc:identifier 'one' . }");
        });
        final Thread t2 = new Thread(() -> {
            responseCodes[1] = patchWithSparql(path,
                "PREFIX dc: <http://purl.org/dc/elements/1.1/>\nINSERT DATA { <> dc:identifier 'two' . }");
        });
        final Thread t3 = new Thread(() -> {
            responseCodes[2] = patchWithSparql(path,
                "PREFIX dc: <http://purl.org/dc/elements/1.1/>\nINSERT DATA { <> dc:identifier 'three' . }");
        });
        final Thread t4 = new Thread(() -> {
            responseCodes[3] = patchWithSparql(path,
                "PREFIX dc: <http://purl.org/dc/elements/1.1/>\nINSERT DATA { <> dc:identifier 'four' . }");
        });
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t1.join();
        t2.join();
        t3.join();
        t4.join();
        assertEquals("Patch should have succeeded!", 204, responseCodes[0]);
        assertEquals("Patch should have succeeded!", 204, responseCodes[1]);
        assertEquals("Patch should have succeeded!", 204, responseCodes[2]);
        assertEquals("Patch should have succeeded!", 204, responseCodes[3]);
        try (final CloseableDataset dataset = getDataset(getObjMethod(path))) {
            final DatasetGraph graphStore = dataset.asDatasetGraph();
            final List<String> missingDcIdentifiers
                    = new ArrayList<>(Arrays.asList("one", "two", "three", "four"));
            final Iterator<Quad> dcIdentifierIt = graphStore.find(ANY, createURI(serverAddress + path),
                    createProperty("http://purl.org/dc/elements/1.1/identifier").asNode(), ANY);
            while (dcIdentifierIt.hasNext()) {
                final String value = dcIdentifierIt.next().getObject().getLiteralValue().toString();
                assertTrue("Unexpected dc:identifier found: " + value, missingDcIdentifiers.remove(value));
            }
            assertTrue("All of the dc:identifiers should have been applied! (missing "
            + Arrays.toString(missingDcIdentifiers.toArray()) + ")", missingDcIdentifiers.isEmpty());

            assertTrue("Added property must exist!", graphStore.contains(ANY, createURI(serverAddress + path),
                    createProperty("http://purl.org/dc/elements/1.1/identifier").asNode(), createLiteral("one")));
            assertTrue("Added property must exist!", graphStore.contains(ANY, createURI(serverAddress + path),
                    createProperty("http://purl.org/dc/elements/1.1/identifier").asNode(), createLiteral("two")));
            assertTrue("Added property must exist!", graphStore.contains(ANY, createURI(serverAddress + path),
                    createProperty("http://purl.org/dc/elements/1.1/identifier").asNode(), createLiteral("three")));
            assertTrue("Added property must exist!", graphStore.contains(ANY, createURI(serverAddress + path),
                    createProperty("http://purl.org/dc/elements/1.1/identifier").asNode(), createLiteral("four")));
        }
    }

    private int patchWithSparql(final String path, final String sparqlUpdate) {
        try {
            final HttpPatch patch = new HttpPatch(serverAddress + path);
            patch.addHeader(CONTENT_TYPE, "application/sparql-update");
            patch.setEntity(new StringEntity(sparqlUpdate));
            final CloseableHttpResponse r = client.execute(patch);
            final int code = r.getStatusLine().getStatusCode();
            r.close();
            return code;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    @Ignore
    public void testBinaryLastModified() throws Exception {
        final String objid = getRandomUniqueId();
        final String objURI = serverAddress + objid;
        final String binURI = objURI + "/binary1";

        final Instant lastmod1;
        try (final CloseableHttpResponse response = execute(putDSMethod(objid, "binary1", "some test content"))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            lastmod1 = Instant.from(headerFormat.parse(response.getFirstHeader("Last-Modified").getValue()));
        }

        sleep(1000); // wait a second to make sure last-modified value will be different

        try (final CloseableDataset dataset = getDataset(new HttpGet(binURI + "/fcr:metadata"))) {
            verifyModifiedMatchesCreated(dataset);
        }

        final HttpPatch patchBinary = new HttpPatch(binURI + "/fcr:metadata");
        patchBinary.addHeader(CONTENT_TYPE, "application/sparql-update");
        patchBinary.setEntity(new StringEntity("INSERT { <" + binURI + "> " +
                "<http://www.w3.org/TR/rdf-schema/label> \"this is a label\" } WHERE {}"));

        final Instant lastmod2;
        try (final CloseableHttpResponse response = execute(patchBinary)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            lastmod2 = Instant.from(headerFormat.parse(response.getFirstHeader("Last-Modified").getValue()));
            assertTrue(lastmod2.isAfter(lastmod1));
        }

        sleep(1000); // wait a second to make sure last-modified value will be different

        final Instant lastmod3;
        try (final CloseableHttpResponse response = execute(putDSMethod(objid, "binary1", "new test content"))) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            lastmod3 = Instant.from(headerFormat.parse(response.getFirstHeader("Last-Modified").getValue()));
            assertTrue(lastmod3.isAfter(lastmod2));
        }
    }

    @Test
@Ignore
    public void testContainerLastModified() throws Exception {
        final String objid = getRandomUniqueId();
        final String objURI = serverAddress + objid;

        // create an object
        final long lastmod1;
        try (final CloseableHttpResponse response = execute(putObjMethod(objid))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            lastmod1 = Instant.from(
                headerFormat.parse(response.getFirstHeader("Last-Modified").getValue())).toEpochMilli();
        }

        sleep(1000); // wait a second to make sure last-modified value will be different

        // initial created and last-modified properties should match
        try (final CloseableDataset dataset = getDataset(getObjMethod(objid))) {
            verifyModifiedMatchesCreated(dataset);
        }

        // update the object properties (last-modified should be updated)
        final HttpPatch patchObject = new HttpPatch(objURI);
        patchObject.addHeader(CONTENT_TYPE, "application/sparql-update");
        patchObject.setEntity(new StringEntity("INSERT { <> " +
                "<http://www.w3.org/TR/rdf-schema/label> \"this is a label\" } WHERE {}"));
        final long lastmod2;
        try (final CloseableHttpResponse response = execute(patchObject)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            lastmod2 = Instant.from(
                headerFormat.parse(response.getFirstHeader("Last-Modified").getValue())).toEpochMilli();
            assertTrue(lastmod2 > lastmod1);
        }

        sleep(1000); // wait a second to make sure last-modified value will be different

        // create a direct container (last-modified should be updated)
        final long lastmod3;
        final HttpPut createContainer = new HttpPut(objURI + "/members");
        createContainer.addHeader(CONTENT_TYPE, "text/turtle");
        createContainer.addHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        final String membersRDF = "<> <http://www.w3.org/ns/ldp#hasMemberRelation> <http://pcdm.org/models#hasMember>; "
            + "<http://www.w3.org/ns/ldp#membershipResource> <" + objURI + "> . ";
        createContainer.setEntity(new StringEntity(membersRDF));
        try (final CloseableHttpResponse response = execute(createContainer)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            lastmod3 = Instant.from(
                headerFormat.parse(response.getFirstHeader("Last-Modified").getValue())).toEpochMilli();
            assertTrue(lastmod3 > lastmod2);
        }

        sleep(1000); // wait a second to make sure last-modified value will be different

        // create child in the container
        final long lastmod4;
        assertEquals(CREATED.getStatusCode(), getStatus(new HttpPut(objURI + "/members/member1")));

        // last-modified should be updated
        try (final CloseableHttpResponse response = execute(headObjMethod(objid))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            lastmod4 = Instant.from(
                headerFormat.parse(response.getFirstHeader("Last-Modified").getValue())).toEpochMilli();
            assertTrue(lastmod4 > lastmod3);
        }
    }

    private static void verifyModifiedMatchesCreated(final Dataset dataset) {
        final DatasetGraph graph = dataset.asDatasetGraph();
        final Node cre = graph.find(ANY, ANY, CREATED_DATE.asNode(), ANY).next().getObject();
        final Node mod = graph.find(ANY, ANY, LAST_MODIFIED_DATE.asNode(), ANY).next().getObject();
        assertEquals(cre.getLiteralValue(), mod.getLiteralValue());
    }

    @Test
    public void testDigestConsistency() throws IOException {
        final String id = getRandomUniqueId();
        executeAndClose(putDSMethod(id, "binary1", "some test content"));

        final String headDigestValue;
        final HttpHead headObjMethod = headObjMethod(id + "/binary1");
        headObjMethod.addHeader(WANT_DIGEST, "sha, md5");
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertTrue(response.getHeaders(DIGEST).length > 0);
            headDigestValue = response.getHeaders(DIGEST)[0].getValue();
        }

        final HttpGet getObjMethod = getObjMethod(id + "/binary1");
        getObjMethod.addHeader(WANT_DIGEST, "sha, md5");
        try (final CloseableHttpResponse response = execute(getObjMethod)) {
            assertTrue(response.getHeaders(DIGEST).length > 0);
            assertEquals(headDigestValue, response.getHeaders(DIGEST)[0].getValue());
        }
    }

    @Test
    public void testDigestAbsence() throws IOException {
        final String id = getRandomUniqueId();
        executeAndClose(putDSMethod(id, "binary1", "some test content"));

        final HttpHead headObjMethod = headObjMethod(id + "/binary1");
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertTrue(response.getHeaders(DIGEST).length == 0);
        }

        final HttpGet getObjMethod = getObjMethod(id + "/binary1");
        try (final CloseableHttpResponse response = execute(getObjMethod)) {
            assertTrue(response.getHeaders(DIGEST).length == 0);
        }
    }

    @Test
@Ignore
    public void testPostFedoraSlug() throws IOException {
        final HttpPost httpPost = postObjMethod("/");
        httpPost.addHeader("Slug", "fedora:path");
        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Must not be able to POST with fedora namespaced Slug!", CONFLICT.getStatusCode(),
                getStatus(response));
        }
    }

    @Test
    public void testPutFedoraPath() throws IOException {
        final HttpPut httpPut = putObjMethod("/fedora:path");
        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Must not be able to PUT with fedora namespaced path!", CONFLICT.getStatusCode(),
                getStatus(response));
        }
    }

    @Test
    public void testDeleteWithFedoraPath() throws IOException {
        final String id = getRandomUniqueId() + "/fedora:delete";
        final HttpDelete httpDelete = deleteObjMethod(id);
        httpDelete.addHeader("Depth", "infinity");
        try (final CloseableHttpResponse response = execute(httpDelete)) {
            assertEquals("Must not be able to DELETE with fedora namespaced path!", CONFLICT.getStatusCode(),
                getStatus(response));
        }
    }

    @Test
    public void testPostCreateNonRDFSourceWithAcl() throws IOException {
        final String aclURI = createAcl();
        final String subjectURI = getRandomUniqueId();
        final HttpPost createMethod = new HttpPost(serverAddress);
        createMethod.addHeader(CONTENT_TYPE, "text/plain");
        createMethod.addHeader("Link", "<" + aclURI + ">; rel=\"acl\"");
        createMethod.addHeader("Slug", subjectURI);
        createMethod.setEntity(new StringEntity("test body"));

        checkResponseForMethodWithAcl(createMethod);
    }

    @Test
    public void testPostCreateRDFSourceWithAcl() throws IOException {
        final String aclURI = createAcl();
        final String subjectURI = getRandomUniqueId();
        final HttpPost createMethod = new HttpPost(serverAddress);
        createMethod.addHeader(CONTENT_TYPE, "text/n3");
        createMethod.addHeader("Link", "<" + aclURI + ">; rel=\"acl\"");
        createMethod.addHeader("Slug", subjectURI);
        createMethod.setEntity(new StringEntity("<> <info:test#label> \"foo\""));

        checkResponseForMethodWithAcl(createMethod);
}

    @Test
    public void testPutCreateNonRDFSourceWithAcl() throws IOException {
        final String aclURI = createAcl();
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut putMethod = new HttpPut(subjectURI);
        putMethod.addHeader(CONTENT_TYPE, "text/plain");
        putMethod.addHeader("Link", "<" + aclURI + ">; rel=\"acl\"");
        putMethod.setEntity(new StringEntity("test body"));

        checkResponseForMethodWithAcl(putMethod);
    }

    @Test
    public void testPutCreateRDFSourceWithAcl() throws IOException {
        final String aclURI = createAcl();
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut putMethod = new HttpPut(subjectURI);
        putMethod.addHeader(CONTENT_TYPE, "text/n3");
        putMethod.addHeader("Link", "<" + aclURI + ">; rel=\"acl\"");
        putMethod.setEntity(new StringEntity("<" + subjectURI + "> <info:test#label> \"foo\""));

        checkResponseForMethodWithAcl(putMethod);
    }

    private void checkResponseForMethodWithAcl(final HttpUriRequest req) throws IOException {
        try (final CloseableHttpResponse response = execute(req)) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
            assertConstrainedByPresent(response);
        }
    }

    @Test
@Ignore
    public void testPostCreateRDFSourceWithMementoNamespaceType() throws IOException {
        final String subjectURI = getRandomUniqueId();
        final HttpPost createMethod = new HttpPost(serverAddress);
        createMethod.addHeader(CONTENT_TYPE, "text/turtle");
        createMethod.addHeader("Slug", subjectURI);
        createMethod.setEntity(new StringEntity("<> a <" + MEMENTO_TYPE + "> ."));
        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Must not be able to POST RDF that contains an \"<> a  <" + MEMENTO_TYPE + ">\"",
                         CONFLICT.getStatusCode(),
                         getStatus(response));
        }
    }

    @Test
@Ignore
    public void testPutCreateRDFSourceWithMementoNamespaceType() throws IOException {
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut putMethod = new HttpPut(subjectURI);
        putMethod.addHeader(CONTENT_TYPE, "text/turtle");
        putMethod.setEntity(new StringEntity("<> a <" + MEMENTO_TYPE + "> ."));
        try (final CloseableHttpResponse response = execute(putMethod)) {
            assertEquals("Must not be able to PUT RDF that contains an \"<> a  <" + MEMENTO_TYPE + ">\"",
                         CONFLICT.getStatusCode(),
                         getStatus(response));
        }
    }

    @Test
@Ignore
    public void testPatchInsertMementoNamespaceType() throws IOException {
        final String pid =  getRandomUniqueId();
        final String subjectURI = serverAddress + pid;
        createObjectAndClose(pid);
        final HttpPatch patch = new HttpPatch(subjectURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity("INSERT { <> a <" + MEMENTO_TYPE + "> . } WHERE {} "));

        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals("Must not be able to INSERT  \"<>  a <" + MEMENTO_TYPE + ">\"",
                         CONFLICT.getStatusCode(), getStatus(patch));
        }
    }

    @Test
@Ignore
    public void testPostCreateRDFSourceWithMementoNamespacePredicate() throws IOException {
        final String subjectURI = getRandomUniqueId();
        final HttpPost createMethod = new HttpPost(serverAddress);
        createMethod.addHeader(CONTENT_TYPE, "text/turtle");
        createMethod.addHeader("Slug", subjectURI);
        createMethod.setEntity(
            new StringEntity("<>  <" + MEMENTO_NAMESPACE + "mementoDatetime" + "> \"Thu, 21 Jan 2010 00:09:40 GMT\""));
        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Must not be able to POST RDF that contains a memento namespace predicate",
                         CONFLICT.getStatusCode(),
                         getStatus(response));
        }
    }

    @Test
@Ignore
    public void testPutCreateRDFSourceWithMementoNamespacePredicate() throws IOException {
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut putMethod = new HttpPut(subjectURI);
        putMethod.addHeader(CONTENT_TYPE, "text/turtle");
        putMethod.setEntity(
            new StringEntity("<>  <" + MEMENTO_NAMESPACE + "mementoDatetime" + "> \"Thu, 21 Jan 2010 00:09:40 GMT\""));
        try (final CloseableHttpResponse response = execute(putMethod)) {
            assertEquals("Must not be able to PUT RDF that contains a memento namespace predicate",
                         CONFLICT.getStatusCode(),
                         getStatus(response));
        }
    }

    @Test
@Ignore
    public void testPatchInsertMementoNamespacePredicate() throws IOException {
        final String pid = getRandomUniqueId();
        final String subjectURI = serverAddress + pid;
        createObjectAndClose(pid);
        final HttpPatch patch = new HttpPatch(subjectURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
            "INSERT { <>  <" + MEMENTO_NAMESPACE + "mementoDatetime" +
            "> \"Thu, 21 Jan 2010 00:09:40 GMT\". } WHERE {}"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals("Must not be able to PATCH RDF that contains a memento namespace predicate",
                         CONFLICT.getStatusCode(), getStatus(response));
        }
    }

    @Test
@Ignore
    public void testPatchUpdateIndirectContainerServerManaged() throws IOException {
        final String pid = getRandomUniqueId();
        final String subjectURI = serverAddress + pid;
        createObjectAndClose(pid);
        final String indirectContainerPid = getRandomUniqueId();
        final String indirectContainerURI = serverAddress + indirectContainerPid;
        createObjectAndClose(indirectContainerPid, INDIRECT_CONTAINER_LINK_HEADER);
        final HttpPatch patch = new HttpPatch(indirectContainerURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "PREFIX fedora: <http://fedora.info/definitions/v4/repository#>\n" +
                "PREFIX ore: <http://www.openarchives.org/ore/terms/>\n" +
                "INSERT { <> ldp:membershipResource <" + subjectURI + "> ;\n" +
                    "ldp:hasMemberRelation fedora:createdBy ;\n" +
                    "ldp:insertedContentRelation ore:proxyFor. } WHERE {}"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals("Must not be able to PATCH IndirectContainer updating Server Managed triples",
                         CONFLICT.getStatusCode(), getStatus(response));
        }
    }

    @Test
@Ignore
    public void testPutUpdateBinaryWithType() throws Exception {
        final String objid = getRandomUniqueId();
        final String objURI = serverAddress + objid;
        final String binURI = objURI + "/binary1";
        final String descURI = objURI + "/binary1/fcr:metadata";

        try (final CloseableHttpResponse response = execute(putDSMethod(objid, "binary1", "some test content"))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        // Add type using PUT
        final Model model1 = getModel(objid + "/binary1/fcr:metadata");
        final Resource resc = model1.getResource(binURI);
        resc.addProperty(RDF.type, PCDM_FILE_TYPE);

        final HttpPut addMethod = new HttpPut(descURI);
        addMethod.addHeader(CONTENT_TYPE, "text/turtle");
        addMethod.setEntity(new InputStreamEntity(streamModel(model1, RDFFormat.TURTLE)));
        try (final CloseableHttpResponse response = execute(addMethod)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        final Model model2 = getModel(objid + "/binary1/fcr:metadata");
        final Resource resc2 = model2.getResource(binURI);
        assertTrue(resc2.hasProperty(RDF.type, PCDM_FILE_TYPE));

        // Remove type using PUT
        model2.remove(resc2, RDF.type, PCDM_FILE_TYPE);

        final HttpPut removeMethod = new HttpPut(descURI);
        removeMethod.addHeader(CONTENT_TYPE, "text/turtle");
        removeMethod.setEntity(new InputStreamEntity(streamModel(model2, RDFFormat.TURTLE)));
        try (final CloseableHttpResponse response = execute(removeMethod)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        final Model model3 = getModel(objid + "/binary1/fcr:metadata");
        final Resource resc3 = model3.getResource(binURI);
        assertFalse(resc3.hasProperty(RDF.type, PCDM_FILE_TYPE));
    }

    @Test
@Ignore
    public void testPatchUpdateBinaryWithType() throws Exception {
        final String objid = getRandomUniqueId();
        final String objURI = serverAddress + objid;
        final String binURI = objURI + "/binary1";
        final String descURI = objURI + "/binary1/fcr:metadata";

        try (final CloseableHttpResponse response = execute(putDSMethod(objid, "binary1", "some test content"))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        // Test adding an rdf:type
        final HttpPatch patchBinary = new HttpPatch(descURI);
        patchBinary.addHeader(CONTENT_TYPE, "application/sparql-update");
        patchBinary.setEntity(new StringEntity("INSERT { <" + binURI + "> " +
                "<" + RDF.type.getURI() + "> <" + PCDM_FILE_TYPE.getURI() + "> } WHERE {}"));

        try (final CloseableHttpResponse response = execute(patchBinary)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        final Model model = getModel(objid + "/binary1/fcr:metadata");
        final Resource resc = model.getResource(binURI);
        assertTrue(resc.hasProperty(RDF.type, PCDM_FILE_TYPE));

        // Test removing an rdf:type
        final HttpPatch patchBinary2 = new HttpPatch(descURI);
        patchBinary2.addHeader(CONTENT_TYPE, "application/sparql-update");
        patchBinary2.setEntity(new StringEntity("DELETE { <" + binURI + "> " +
                "<" + RDF.type.getURI() + "> <" + PCDM_FILE_TYPE.getURI() + "> } WHERE {}"));

        try (final CloseableHttpResponse response = execute(patchBinary2)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        final Model model2 = getModel(objid + "/binary1/fcr:metadata");
        final Resource resc2 = model2.getResource(binURI);
        assertFalse(resc2.hasProperty(RDF.type, PCDM_FILE_TYPE));
    }

    /**
     * Utility to assert a GET of id and id/fcr:versions
     *
     * @param id the path
     * @throws Exception
     */
    private void doGetIdAndVersions(final String id) throws Exception {
        final HttpGet getMethod = getObjMethod(id);
        try (final CloseableHttpResponse response = execute(getMethod)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
        }

        final HttpGet getVersion = getObjMethod(id + "/" + FCR_VERSIONS);
        try (final CloseableHttpResponse response = execute(getVersion)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testPutGetRdfSourceWithUndefinedPrefix() throws Exception {
        final String id = "some_prefix:" + getRandomUniqueId();
        final HttpPut putMethod = putObjMethod(id);
        try (final CloseableHttpResponse response = execute(putMethod)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        doGetIdAndVersions(id);
    }

    @Test
    public void testPutGetNonRdfSourceWithUndefinedPrefix() throws Exception {
        final String id = "some_prefix:" + getRandomUniqueId();
        final String dsid = "anotherPrefix:" + getRandomUniqueId();

        try (final CloseableHttpResponse response = execute(putDSMethod(id, dsid, "some test content"))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        // Check id/dsid
        doGetIdAndVersions(id + "/" + dsid);

        // Check id/dsid/fcr:metadata
        doGetIdAndVersions(id + "/" + dsid + "/" + FCR_METADATA);

    }

    @Test
    public void testContentTypeWithCharset() throws Exception {
        final String uuid = getRandomUniqueId();
        final HttpPost postMethod = postObjMethod();
        postMethod.setHeader("Slug", uuid);
        postMethod.setHeader(CONTENT_TYPE, "text/turtle;charset=utf-8");
        final HttpEntity body = new StringEntity("@prefix acl: <http://www.w3.org/ns/auth/acl#>.\n" +
                "@prefix vcard: <http://www.w3.org/2006/vcard/ns#>.\n" +
                "<> a vcard:Group;\n" +
                "  vcard:hasMember  \"someUser\".");
        postMethod.setEntity(body);
        assertEquals(CREATED.getStatusCode(), getStatus(postMethod));

        final HttpGet getMethod = getObjMethod(uuid);
        try (final CloseableHttpResponse response = execute(getMethod)) {
            final Dataset dataset = getDataset(response);
            final DatasetGraph graph = dataset.asDatasetGraph();
            checkForLinkHeader(response, BASIC_CONTAINER.toString(), "type");
            assertTrue(graph.contains(
                    ANY,
                    ANY,
                    NodeFactory.createURI("http://www.w3.org/2006/vcard/ns#hasMember"),
                    NodeFactory.createLiteral("someUser")));
        }
    }

}
