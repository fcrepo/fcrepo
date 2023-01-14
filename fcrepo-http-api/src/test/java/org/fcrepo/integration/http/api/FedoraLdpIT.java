/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.PARTIAL_CONTENT;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static nu.validator.htmlparser.common.DoctypeExpectation.NO_DOCTYPE_ERRORS;
import static nu.validator.htmlparser.common.XmlViolationPolicy.ALLOW;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.parse;
import static org.apache.http.impl.client.cache.CacheConfig.DEFAULT;
import static org.apache.jena.datatypes.TypeMapper.getInstance;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDinteger;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDlong;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ModelFactory.createModelForGraph;
import static org.apache.jena.rdf.model.ResourceFactory.createPlainLiteral;
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
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_FIXITY;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_TOMBSTONE;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.RdfLexicon.ARCHIVAL_GROUP;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_BINARY;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_ORIGINAL_NAME;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_MINIMAL_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_SERVER_MANAGED;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_ROOT;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONED_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.VERSIONING_TIMEGATE_TYPE;
import static org.fcrepo.kernel.api.models.ExternalContent.COPY;
import static org.fcrepo.kernel.api.models.ExternalContent.PROXY;
import static org.fcrepo.kernel.api.models.ExternalContent.REDIRECT;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import java.util.Random;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import nu.validator.htmlparser.sax.HtmlParser;
import nu.validator.saxtree.TreeBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
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
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.http.commons.domain.RDFMediaType;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.fcrepo.kernel.api.RdfLexicon;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.test.context.TestExecutionListeners;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author cabeer
 * @author ajs6f
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraLdpIT extends AbstractResourceIT {

    private static final Node DC_IDENTIFIER = DC_11.identifier.asNode();

    private static final String LDP_RESOURCE_LINK_HEADER = "<" + RESOURCE.getURI() + ">; rel=\"type\"";

    private static final Node rdfType = type.asNode();

    private static final Resource PCDM_FILE_TYPE = createResource("http://pcdm.org/models#File");

    private static final String CONTAINER_LINK_HEADER = "<" + CONTAINER.getURI() + ">; rel=\"type\"";

    private static final String BASIC_CONTAINER_LINK_HEADER = "<" + BASIC_CONTAINER.getURI() + ">; rel=\"type\"";
    private static final String INDIRECT_CONTAINER_LINK_HEADER = "<" + INDIRECT_CONTAINER.getURI() + ">; rel=\"type\"";
    private static final String ARCHIVAL_GROUP_LINK_HEADER = "<" + ARCHIVAL_GROUP.getURI() + ">; rel=\"type\"";

    private static final String RESOURCE_LINK_HEADER = "<" + RESOURCE.getURI() + ">; rel=\"type\"";
    private static final String RDF_SOURCE_LINK_HEADER = "<" + RDF_SOURCE.getURI() + ">; rel=\"type\"";
    private static final String NON_RDF_SOURCE_LINK_HEADER = "<" + NON_RDF_SOURCE.getURI() + ">; rel=\"type\"";
    private static final String VERSIONED_RESOURCE_LINK_HEADER = "<" + VERSIONED_RESOURCE.getURI() + ">; rel=\"type\"";

    private static final String SERVER_MANAGED_TYPE_CONSTRAINT_URI = serverAddress +
            "static/constraints/ServerManagedTypeException.rdf";

    private static final String SERVER_MANAGED_PROPERTY_CONSTRAINT_URI = serverAddress +
            "static/constraints/ServerManagedPropertyException.rdf";

    private static final String INBOUND_REFERENCE_PREFER_HEADER = "return=representation; include=\"" +
            INBOUND_REFERENCES + "\"";

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
        final var postMethod = postObjMethod();
        postMethod.setHeader("Slug", id);
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
        testDefaultContentType(RDFMediaType.TURTLE_WITH_CHARSET, false);
    }

    @Test
    public void testHeadRDFContentType() throws IOException {
        testDefaultContentType(RDFMediaType.RDF_XML, false);
    }

    @Test
    public void testHeadJSONLDContentType() throws IOException {
        testDefaultContentType(RDFMediaType.JSON_LD, false);
    }

    @Test
    public void testHeadDefaultContentType() throws IOException {
        testDefaultContentType(null, false);
    }

    @Test
    public void testHeadAnyContentType() throws IOException {
        testDefaultContentType(RDFMediaType.WILDCARD, false);
    }

    @Test
    public void testGetTurtleContentType() throws IOException {
        testDefaultContentType(RDFMediaType.TURTLE_WITH_CHARSET, true);
    }

    @Test
    public void testGetRDFContentType() throws IOException {
        testDefaultContentType(RDFMediaType.RDF_XML, true);
    }

    @Test
    public void testGetJSONLDContentType() throws IOException {
        testDefaultContentType(RDFMediaType.JSON_LD, true);
    }

    @Test
    public void testGetDefaultContentType() throws IOException {
        testDefaultContentType(null, true);
    }

    @Test
    public void testGetAnyContentType() throws IOException {
        testDefaultContentType(RDFMediaType.WILDCARD, true);
    }

    private void testDefaultContentType(final String mimeType, final boolean isGet) throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final HttpRequestBase req;
        if (isGet) {
            req = getObjMethod(id);
        } else {
            req = headObjMethod(id);
        }
        String mt = mimeType;
        final String returnType;
        if (mt != null) {
            req.addHeader("Accept", mt);
            if (mt.equals(RDFMediaType.WILDCARD)) {
                returnType = RDFMediaType.TURTLE_WITH_CHARSET;
            } else {
                returnType = mt;
            }
        } else {
            mt = RDFMediaType.TURTLE_WITH_CHARSET;
            returnType = mt;
        }
        try (final CloseableHttpResponse response = execute(req)) {
            checkContentTypeMatches(response, returnType);
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
    public void testHeadDatastreamAttachment() throws IOException, ParseException {
        testHeadDatastream(false);
    }

    @Test
    public void testHeadDatastreamInline() throws IOException, ParseException {
        testHeadDatastream(true);
    }

    private void testHeadDatastream(final boolean inline) throws IOException, ParseException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "123");

        final HttpHead headObjMethod = headObjMethod(id + "/x" + (inline ? "?inline=true" : ""));
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertEquals(TEXT_PLAIN, response.getFirstHeader(CONTENT_TYPE).getValue());
            assertEquals("3", response.getFirstHeader(CONTENT_LENGTH).getValue());
            assertEquals("bytes", response.getFirstHeader("Accept-Ranges").getValue());
            final ContentDisposition disposition =
                    new ContentDisposition(response.getFirstHeader(CONTENT_DISPOSITION).getValue());
            if (inline) {
                assertEquals("inline", disposition.getType());
            } else {
                assertEquals("attachment", disposition.getType());
            }
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
    public void testPatchBinaryNameAndType() throws Exception {
        final String pid = getRandomUniqueId();

        final String dsLocation = createDatastream(pid, "x", "some content");
        verifyContentType(dsLocation, "text/plain");
        verifyContentDispositionFilename(dsLocation, null);

        final String location = dsLocation + "/fcr:metadata";
        final HttpPatch patch = new HttpPatch(location);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity("DELETE { " +
                "<" + dsLocation + "> <" + HAS_MIME_TYPE + "> ?any . } " +
                "WHERE {" +
                "<" + dsLocation + "> <" + HAS_MIME_TYPE + "> ?any . } ; " +
                "INSERT {" +
                "<" + dsLocation + "> <" + HAS_MIME_TYPE + "> \"text/awesome\" ." +
                "<" + dsLocation + "> <" + HAS_ORIGINAL_NAME + "> \"x.txt\" }" +
                "WHERE {}"));

        try (final CloseableHttpResponse response = client.execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            try (final CloseableDataset dataset = getDataset(new HttpGet(location))) {
                final DatasetGraph graphStore = dataset.asDatasetGraph();
                final Node subject = createURI(dsLocation);
                assertTrue(graphStore.contains(ANY, subject, HAS_MIME_TYPE.asNode(), createLiteral("text/awesome")));
                assertTrue(graphStore.contains(ANY, subject, HAS_ORIGINAL_NAME.asNode(), createLiteral("x.txt")));
                assertFalse("Should not contain old mime type property", graphStore.contains(ANY,
                        subject, createURI(REPOSITORY_NAMESPACE + "mimeType"), ANY));
            }
        }

        // Ensure binary can be downloaded (test against regression of: https://jira.duraspace.org/browse/FCREPO-1720)
        assertEquals(OK.getStatusCode(), getStatus(getDSMethod(pid, "x")));

        // Ensure headers for binary have updated, FCREPO-3739
        verifyContentType(dsLocation, "text/awesome");
        verifyContentDispositionFilename(dsLocation, "x.txt");
    }

    @Test
    public void testPatchBinaryMultipleTypes() throws Exception {
        final String pid = getRandomUniqueId();

        final String dsLocation = createDatastream(pid, "x", "some content");
        verifyContentType(dsLocation, "text/plain");
        verifyContentDispositionFilename(dsLocation, null);

        final String location = dsLocation + "/fcr:metadata";
        final HttpPatch patch = new HttpPatch(location);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "INSERT {" +
                "<" + dsLocation + "> <" + HAS_MIME_TYPE + "> \"text/awesome\" ." +
                "<" + dsLocation + "> <" + HAS_MIME_TYPE + "> \"text/moreawesome\" }" +
                "WHERE {}"));

        try (final CloseableHttpResponse response = client.execute(patch)) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
            final String body = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            assertTrue(body.contains("Invalid RDF, cannot provided multiple values for property " + HAS_MIME_TYPE));
        }

        verifyContentType(dsLocation, "text/plain");
    }

    @Test
    public void testPatchBinaryMultipleFilenames() throws Exception {
        final String pid = getRandomUniqueId();

        final String dsLocation = createDatastream(pid, "x", "some content");
        verifyContentType(dsLocation, "text/plain");
        verifyContentDispositionFilename(dsLocation, null);

        final String location = dsLocation + "/fcr:metadata";
        final HttpPatch patch = new HttpPatch(location);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "INSERT {" +
                        "<" + dsLocation + "> <" + HAS_ORIGINAL_NAME + "> \"file1.txt\" ." +
                        "<" + dsLocation + "> <" + HAS_ORIGINAL_NAME + "> \"file2.txt\" }" +
                        "WHERE {}"));

        try (final CloseableHttpResponse response = client.execute(patch)) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
            final String body = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            assertTrue(body.contains("Invalid RDF, cannot provided multiple values for property " + HAS_ORIGINAL_NAME));
        }
    }

    @Test
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
            assertTrue(graphStore.contains(ANY, bnode, DCTITLE, createLiteral("this is a title")));
        }
    }

    @Test
    public void testReplaceGraph() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final String subjectURI = serverAddress + id;
        final String initialContent;
        final HttpGet getMethod = getObjMethod(id);
        getMethod.addHeader("Prefer", "return=representation; omit=\"" + PREFER_SERVER_MANAGED + "\"");
        try (final CloseableHttpResponse subjectResponse = execute(getMethod)) {
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
    public void testRoundTripReplaceGraph() throws IOException {

        final String subjectURI = getLocation(postObjMethod());
        final HttpGet getObjMethod = new HttpGet(subjectURI);
        getObjMethod.addHeader(ACCEPT, "text/turtle");
        getObjMethod.addHeader("Prefer", "return=representation; omit=\"" + PREFER_SERVER_MANAGED + "\"");

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
    public void testBinaryEtags() throws IOException, InterruptedException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final String binaryLocation = serverAddress + id + "/binary";
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
        final HttpGet get1 = new HttpGet(binaryLocation);
        get1.addHeader("If-None-Match", binaryEtag1);
        assertEquals("Expected 304 Not Modified", NOT_MODIFIED.getStatusCode(), getStatus(get1));

        final HttpGet get2 = new HttpGet(binaryLocation);
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
        final HttpHead head1 = new HttpHead(binaryLocation);
        try (final CloseableHttpResponse response = execute(head1)) {
            binaryEtag2 = response.getFirstHeader("ETag").getValue();
            binaryLastModed2 = response.getFirstHeader("Last-Modified").getValue();
        }

        assertEquals("ETags should be the same", binaryEtag1, binaryEtag2);
        assertEquals("Last-Modified should be the same", binaryLastModed1, binaryLastModed2);

        final HttpGet get6 = new HttpGet(binaryLocation);
        get6.addHeader("If-Match", binaryEtag1);
        assertEquals("Expected 200", OK.getStatusCode(), getStatus(get6));

        final HttpGet get7 = new HttpGet(binaryLocation);
        get7.addHeader("If-Unmodified-Since", binaryLastModed1);
        assertEquals("Expected 200", OK.getStatusCode(), getStatus(get7));

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
        final HttpPut method2 = new HttpPut(binaryLocation);
        assertFalse("Expected strong ETag", binaryEtag2.startsWith("W/"));
        method2.addHeader("If-Match", binaryEtag2);
        method2.setEntity(new StringEntity("foobar"));
        try (final CloseableHttpResponse response = execute(method2)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            binaryEtag3 = response.getFirstHeader("ETag").getValue();
            binaryLastModed3 = response.getFirstHeader("Last-Modified").getValue();
        }

        final HttpGet get10 = new HttpGet(binaryLocation);
        get10.addHeader("If-None-Match", binaryEtag2);
        assertEquals("Expected 200 OK", OK.getStatusCode(), getStatus(get10));

        final HttpGet get11 = new HttpGet(binaryLocation);
        get11.addHeader("If-Modified-Since", binaryLastModed1);
        assertEquals("Expected 200 OK", OK.getStatusCode(), getStatus(get11));

        assertNotEquals("ETags should have changed", binaryEtag1, binaryEtag3);
        assertNotEquals("Last-Modified should have changed", binaryLastModed1, binaryLastModed3);

        // Next, check headers for the description; they should not have changed
        final HttpHead head3 = new HttpHead(descLocation);
        try (final CloseableHttpResponse response = execute(head3)) {
            descEtag3 = response.getFirstHeader("ETag").getValue();
            descLastModed3 = response.getFirstHeader("Last-Modified").getValue();
        }

        assertEquals("ETags should have changed", descEtag2, descEtag3);
        assertEquals("Last-Modified should have changed", descLastModed2, descLastModed3);
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

        // If the child is created and deleted in the same second the eTag would be the same.
        // Wait to delete.
        TimeUnit.SECONDS.sleep(1);

        assertEquals("Child resource not deleted!", NO_CONTENT.getStatusCode(),
                getStatus(new HttpDelete(serverAddress + child)));
        final String etag2;
        try (final CloseableHttpResponse response = execute(get)) {
            etag2 = response.getFirstHeader("ETag").getValue();
        }

        assertNotEquals("ETag didn't change!", etag1, etag2);
    }

    @Test
    public void testContainmentHashChanges() throws Exception {
        final String parentUri;
        final String createdEtag;
        // Create a resource and note its ETag
        try (final var response = execute(postObjMethod())) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            parentUri = getLocation(response);
            createdEtag = getEtag(response);
        }

        // Create one child and see the parent's ETag changes
        final String oneChildEtag;
        assertEquals(CREATED.getStatusCode(), getStatus(new HttpPost(parentUri)));
        try (final var response = execute(new HttpGet(parentUri))) {
            oneChildEtag = getEtag(response);
            assertEquals(OK.getStatusCode(), getStatus(response));
            assertNotEquals(createdEtag, oneChildEtag);
        }

        // We use the last created time for the eTag, if 2 children are created in the same second there is no
        // difference, so we wait.
        TimeUnit.SECONDS.sleep(1);

        // Create another child
        final String otherChild;
        try (final var response = execute(new HttpPost(parentUri))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            otherChild = getLocation(response);
        }
        // See the parent's ETag changes again.
        final var currentETag = getEtag(parentUri);
        assertNotEquals(createdEtag, currentETag);
        assertNotEquals(oneChildEtag, currentETag);

        TimeUnit.SECONDS.sleep(1);

        // Delete the second child.
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(otherChild)));
        // See the parent's ETag change again.
        final var currentETag2 = getEtag(parentUri);
        assertNotEquals(createdEtag, currentETag2);
        assertNotEquals(oneChildEtag, currentETag2);
        assertNotEquals(currentETag, currentETag2);

        // Request parent with containment omitted, etag should change
        final var httpGetOmitContainment = new HttpGet(parentUri);
        final String preferHeader2 = "return=representation; omit=\"" +
                RdfLexicon.PREFER_CONTAINMENT.getURI() + "\"";
        httpGetOmitContainment.setHeader("Prefer", preferHeader2);
        final String omitEtag = getEtag(httpGetOmitContainment);
        assertEquals("Etag should match the etag before adding a child", createdEtag, omitEtag);
        assertNotEquals(oneChildEtag, omitEtag);
        assertNotEquals(currentETag, omitEtag);
        assertNotEquals(currentETag2, omitEtag);
    }

    @Test
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
    public void testIngestWithNewAndSparqlQuery() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "application/sparql-update");
        method.setEntity(new StringEntity(
                "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"title\" } WHERE {}"));
        assertEquals(UNSUPPORTED_MEDIA_TYPE.getStatusCode(), getStatus(method));

        final String id = getRandomUniqueId();
        final HttpPut putMethod = putObjMethod(id);
        putMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
        putMethod.setEntity(new StringEntity(
                "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"title\" } WHERE {}"));
        assertEquals(UNSUPPORTED_MEDIA_TYPE.getStatusCode(), getStatus(putMethod));
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
    public void testIngestWithBinaryAndTwoValidHeadersDigestHeaders() throws Exception {
        final HttpPost method = postObjMethod();
        final File img = new File("src/test/resources/test-objects/img.png");
        final var md5 = "6668675a91f39ca1afe46c084e8406ba";
        final var sha256 = "7b115a72978fe138287c1a6dfe6cc1afce4720fb3610a81d32e4ad518700c923";
        method.addHeader(CONTENT_TYPE, "application/octet-stream");
        method.addHeader("Digest", "md5=" + md5 + "," +
                " sha-256=" + sha256);
        method.setEntity(new FileEntity(img));
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);

        final String location;
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Should be a 201 Created!", CREATED.getStatusCode(), getStatus(method));
            location = getLocation(response);
        }

        // verify that both headers are returned in the metadata.
        try (final var response = execute(new HttpGet(location + "/" + FCR_METADATA))) {
            try (final var dataset = getDataset(response)) {
                final var graph = dataset.asDatasetGraph();
                final var model = createModelForGraph(graph.getDefaultGraph());
                final var nodeUri = createResource(location);
                final var stmts = model.listStatements(nodeUri, HAS_MESSAGE_DIGEST, (String) null);
                final var digests = new ArrayList<String>();
                while (stmts.hasNext()) {
                    digests.add(stmts.nextStatement().getObject().asResource().getURI());
                }
                assertTrue("contains md5", digests.contains("urn:md5:" + md5));
                assertTrue("contains sha-256", digests.contains("urn:sha-256:" + sha256));
            }
        }
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
        final String contentType = "application/png";
        method.addHeader(CONTENT_TYPE, contentType);
        method.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        method.setEntity(new FileEntity(img));
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);

        // Create a binary resource with content-disposition
        final String location;
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Should be a 201 Created!", CREATED.getStatusCode(), getStatus(response));
            location = getLocation(response);
        }
        verifyContentType(location, contentType);

        // Change binary headers via PUT headers
        final HttpPut putMethod = new HttpPut(location);
        final String filename2 = "some-other.png";
        final String contentType2 = "image/png";
        putMethod.addHeader(CONTENT_TYPE, contentType2);
        putMethod.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename2 + "\"");
        putMethod.setEntity(new FileEntity(img));

        assertEquals(location, NO_CONTENT.getStatusCode(), getStatus(putMethod));

        // Retrieve the new resource and verify the content-disposition
        verifyContentDispositionFilename(location, filename2);
        verifyContentType(location, contentType2);
    }

    @Test
    public void testContentDispositionHeaderEncoding() throws ParseException, IOException {
        final HttpPost method = postObjMethod();
        final File img = new File("src/test/resources/test-objects/img.png");
        final String filename = "浅顶软呢帽岩石.png";
        final String filenamePart = "UTF-8''" + URLEncoder.encode(filename, UTF_8);
        method.addHeader(CONTENT_TYPE, "application/png");
        method.addHeader(CONTENT_DISPOSITION, "attachment; filename*=" + filenamePart);
        method.setEntity(new FileEntity(img));
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);

        // Create a binary resource with content-disposition
        final String location;
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Should be a 201 Created!", CREATED.getStatusCode(), getStatus(response));
            location = getLocation(response);
        }

        // Retrieve the new resource and verify the content-disposition
        verifyContentDispositionFilename(location, filenamePart);

        final HttpGet get = new HttpGet(location + "/" + FCR_METADATA);
        try (final CloseableHttpResponse response = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            try (final CloseableDataset dataset = getDataset(response)) {
                final DatasetGraph graph = dataset.asDatasetGraph();
                assertFalse(dataset.isEmpty());
                assertTrue(graph.contains(ANY, createURI(location), HAS_ORIGINAL_NAME.asNode(),
                        createLiteral(filename)));
            }
        }
    }

    @Test
    public void testUpdateBinaryHeadersViaPut() throws Exception {
        final HttpPost method = postObjMethod();
        final File img = new File("src/test/resources/test-objects/img.png");
        final String filename = "some-file.png";
        method.addHeader(CONTENT_TYPE, "application/octet-stream");
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

        final String filename2 = "new-file.png";
        final String contentType2 = "image/png";
        final HttpPut replaceMethod = new HttpPut(location + "/" + FCR_METADATA);
        replaceMethod.addHeader(CONTENT_TYPE, "text/turtle");
        replaceMethod.setEntity(new StringEntity(
                "<" + location + "> <" + HAS_ORIGINAL_NAME.getURI() + "> \"" + filename2 + "\" ;"
                + " <" + HAS_MIME_TYPE.getURI() + "> \"" + contentType2 + "\""));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(replaceMethod));

        // Verify the binary headers updated
        verifyContentType(location, contentType2);
        verifyContentDispositionFilename(location, filename2);
    }

    @Test
    public void testUpdateBinaryHeadersViaPutMultipleMimetypes() throws Exception {
        final HttpPost method = postObjMethod();
        final File img = new File("src/test/resources/test-objects/img.png");
        method.addHeader(CONTENT_TYPE, "application/octet-stream");
        method.setEntity(new FileEntity(img));
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        final String location = getLocation(method);

        final HttpPut replaceMethod = new HttpPut(location + "/" + FCR_METADATA);
        replaceMethod.addHeader(CONTENT_TYPE, "text/turtle");
        replaceMethod.setEntity(new StringEntity(
                "<" + location + "> <" + HAS_MIME_TYPE.getURI() + "> \"image/ping\" ;"
                        + " <" + HAS_MIME_TYPE.getURI() + "> \"image/pong\""));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(replaceMethod));

        // Verify the binary headers not updated
        verifyContentType(location, "application/octet-stream");
    }

    @Test
    public void testUpdateBinaryHeadersViaPutMultipleFilenames() throws Exception {
        final HttpPost method = postObjMethod();
        final String filename = "some-file.png";
        final File img = new File("src/test/resources/test-objects/img.png");
        method.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        method.setEntity(new FileEntity(img));
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        final String location = getLocation(method);

        final HttpPut replaceMethod = new HttpPut(location + "/" + FCR_METADATA);
        replaceMethod.addHeader(CONTENT_TYPE, "text/turtle");
        replaceMethod.setEntity(new StringEntity(
                "<" + location + "> <" + HAS_ORIGINAL_NAME.getURI() + "> \"image1.png\" ;"
                        + " <" + HAS_ORIGINAL_NAME.getURI() + "> \"image2.png\""));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(replaceMethod));

        // Verify the binary headers not updated
        verifyContentDispositionFilename(location, filename);
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

    private void verifyContentType(final String location, final String expectedType) throws IOException {
        try (final CloseableHttpResponse getResponse = execute(new HttpHead(location))) {
            assertEquals(expectedType, getResponse.getFirstHeader(CONTENT_TYPE).getValue());
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
    public void testDeleteWithBadEtag() throws IOException {
        try (final CloseableHttpResponse response = execute(postObjMethod())) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            final HttpDelete request = new HttpDelete(getLocation(response));
            request.addHeader("If-Match", "\"doesnt-match\"");
            assertEquals(PRECONDITION_FAILED.getStatusCode(), getStatus(request));
        }
    }

    @Test
    public void testGetDatastreamAttachment() throws IOException, ParseException {
        testGetDatastream(false);
    }

    @Test
    public void testGetDatastreamInline() throws IOException, ParseException {
        testGetDatastream(true);
    }

    private void testGetDatastream(final boolean inline) throws IOException, ParseException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createDatastream(id, "ds1", "foo");

        try (final CloseableHttpResponse response = execute(getDSMethod(id, "ds1", inline))) {
            assertEquals("Wasn't able to retrieve a datastream!", OK.getStatusCode(), getStatus(response));
            assertEquals(TEXT_PLAIN, response.getFirstHeader(CONTENT_TYPE).getValue());
            assertEquals("3", response.getFirstHeader(CONTENT_LENGTH).getValue());
            assertEquals("bytes", response.getFirstHeader("Accept-Ranges").getValue());
            final ContentDisposition disposition =
                    new ContentDisposition(response.getFirstHeader(CONTENT_DISPOSITION).getValue());
            if (inline) {
                assertEquals("inline", disposition.getType());
            } else {
                assertEquals("attachment", disposition.getType());
            }

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
                        createURI(location), CONTAINS.asNode(), createURI(location + "/c")));

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
                        createURI(location), CONTAINS.asNode(), createURI(location + "/c")));
            }
        }

        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteObjMethod(id + "/c")));

        try (final CloseableHttpResponse response1 = execute(getObjMethod(id))) {
            try (final CloseableDataset dataset = getDataset(response1)) {
                assertFalse("Found child node!", dataset.asDatasetGraph().contains(ANY,
                        createURI(location), CONTAINS.asNode(), createURI(location + "/c")));
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
    public void testPatchToCreateDirectContainerInSparqlUpdateFails() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        final String updateString =
                "INSERT DATA { <> a <" + DIRECT_CONTAINER.getURI() + "> ; <" + MEMBERSHIP_RESOURCE.getURI() +
                        "> <> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_MEMBER + "> .}";
        patch.setEntity(new StringEntity(updateString));
        assertEquals("Patch with sparql update created direct container from basic container!",
                CONFLICT.getStatusCode(), getStatus(patch));
    }

    @Test
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
        assertEquals("Changed the basic container ixn to NonRdfSource through PUT with RDF content!",
                CONFLICT.getStatusCode(), getStatus(put1));

        // attempt to change basic container to direct container
        final String ttl2 = "<> a <" + DIRECT_CONTAINER.getURI() + "> ; <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + resource + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_MEMBER + "> .";
        final HttpPut put2 = putObjMethod(pid + "/a", "text/turtle", ttl2);
        assertEquals("Changed the basic container ixn to Direct Container through PUT with RDF content!",
                CONFLICT.getStatusCode(), getStatus(put2));

        // create direct container
        final String ttl = "<> <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + resource + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_MEMBER + "> .";
        final HttpPut put = putObjMethod(pid + "/b", "text/turtle", ttl);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals(CREATED.getStatusCode(), getStatus(put));
        assertTrue(getLinkHeaders(getObjMethod(pid + "/b")).contains(DIRECT_CONTAINER_LINK_HEADER));

        // successful update the properties with the interaction mode
        final String ttla = "<> <" + MEMBERSHIP_RESOURCE + "> <" + container + ">;\n"
                + "<" + HAS_MEMBER_RELATION + "> <info:some/relation> .\n";
        final HttpPut puta = putObjMethod(pid + "/b", "text/turtle", ttla);
        puta.addHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(puta));

        // attempt to change direct container to basic container
        final String ttl3 = "<> a <" + BASIC_CONTAINER.getURI() +
                "> ; <" + title + "> \"this is a title\".";
        final HttpPut put3 = putObjMethod(pid + "/b", "text/turtle", ttl3);
        assertEquals("Changed the direct container ixn to basic container through PUT with RDF content!",
                CONFLICT.getStatusCode(), getStatus(put3));

        // attempt to change direct container to indirect container
        final String ttl4 = "<> a <" + INDIRECT_CONTAINER.getURI()
                + "> ; <" + MEMBERSHIP_RESOURCE + "> <" + container + ">;\n"
                + "<" + HAS_MEMBER_RELATION + "> <info:some/relation>;\n"
                + "<" + INSERTED_CONTENT_RELATION + "> <info:proxy/for> .\n";
        final HttpPut put4 = putObjMethod(pid + "/b", "text/turtle", ttl4);
        assertEquals("Changed the direct container ixn to indirect container through PUT with RDF content!",
                CONFLICT.getStatusCode(), getStatus(put4));
    }

    @Test
    public void testChangeInteractionModelWithPut() throws IOException {
        final String pid = getRandomUniqueId();
        final String resource = serverAddress + pid;
        final String container = serverAddress + pid + "/c";

        createObjectAndClose(pid);
        createObjectAndClose(pid + "/a");
        createObjectAndClose(pid + "/c");

        final String ttl1 = "<> <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + resource + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_MEMBER + "> .";
        final HttpPut put1 = putObjMethod(pid + "/a", "text/turtle", ttl1);
        put1.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        put1.addHeader("Prefer", "handling=lenient");
        assertEquals("Changed the basic container ixn to direct container!",
                CONFLICT.getStatusCode(), getStatus(put1));

        // create direct container
        final String ttl = "<> <" + MEMBERSHIP_RESOURCE.getURI() +
                "> <" + resource + "> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_MEMBER + "> .";
        final HttpPut put = putObjMethod(pid + "/b", "text/turtle", ttl);
        put.setHeader(LINK, DIRECT_CONTAINER_LINK_HEADER);
        assertEquals(CREATED.getStatusCode(), getStatus(put));
        assertTrue(getLinkHeaders(getObjMethod(pid + "/b")).contains(DIRECT_CONTAINER_LINK_HEADER));

        final String ttl2 = "<> <" + title + "> \"this is a title\"";
        final HttpPut put2 = putObjMethod(pid + "/b", "text/turtle", ttl2);
        put2.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        put2.addHeader("Prefer", "handling=lenient");
        assertEquals("Changed the direct container ixn to basic container",
                CONFLICT.getStatusCode(), getStatus(put2));

        final String ttl3 = "<> <" + MEMBERSHIP_RESOURCE + "> <" + container + ">;\n"
                + "<" + HAS_MEMBER_RELATION + "> <info:some/relation>;\n"
                + "<" + INSERTED_CONTENT_RELATION + "> <info:proxy/for> .\n";
        final HttpPut put3 = putObjMethod(pid + "/b", "text/turtle", ttl3);
        put3.setHeader(LINK, INDIRECT_CONTAINER_LINK_HEADER);
        put3.addHeader("Prefer", "handling=lenient");
        assertEquals("Changed the direct container ixn to indirect container!",
                CONFLICT.getStatusCode(), getStatus(put3));
    }

    @Test
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
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));

        final String withoutRefsEtag = getEtag(resourceb);

        final HttpGet getObjMethod = new HttpGet(resourceb);
        getObjMethod.addHeader("Prefer", INBOUND_REFERENCE_PREFER_HEADER);

        assertNotEquals(withoutRefsEtag, getEtag(getObjMethod));

        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(ANY,
                    createURI(resourcea), createURI("http://purl.org/dc/terms/isPartOf"), createURI(resourceb)));

            assertTrue(graph.contains(ANY,
                    createURI(resourcea), createURI("info:xyz#some-other-property"), createURI(resourceb)));
        }
    }

    @Test
    public void testInboundReferencesFromBinary() throws Exception {
        final Node referenceProp = NodeFactory.createURI("http://awoods.com/pointsAt");
        final HttpPost postContainer = postObjMethod();
        final String containerUri;
        try (final CloseableHttpResponse response = execute(postContainer)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            containerUri = getLocation(response);
        }

        final HttpPost postBinary = postObjMethod();
        postBinary.setHeader(CONTENT_TYPE, TEXT_PLAIN);
        postBinary.setEntity(new StringEntity("Test text"));
        final String binaryUri;
        try (final CloseableHttpResponse response = execute(postBinary)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            binaryUri = getLocation(response);
        }

        setProperty(binaryUri.replace(serverAddress, "") + "/" + FCR_METADATA, referenceProp.getURI(),
                URI.create(containerUri));

        final String withoutRefsEtag = getEtag(containerUri);

        final HttpGet getContainer = new HttpGet(containerUri);
        getContainer.setHeader("Prefer", INBOUND_REFERENCE_PREFER_HEADER);

        assertNotEquals(withoutRefsEtag, getEtag(getContainer));
        try (final CloseableDataset dataset = getDataset(getContainer)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(ANY, NodeFactory.createURI(binaryUri), referenceProp,
                    NodeFactory.createURI(containerUri)));
        }
    }

    /**
     * Create the same reference from two resources, check that one exists when the other is deleted.
     */
    @Test
    public void testMultipleReferences() throws Exception {
        // Need to use something different or they come out as a single triple in the serialization.
        final Node referenceProp1 = NodeFactory.createURI("http://awoods.com/pointsAt");
        final Node referenceProp2 = createURI("http://example.org/lookAt");
        final HttpPost postContainer = postObjMethod();
        final String targetUri;
        try (final CloseableHttpResponse response = execute(postContainer)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            targetUri = getLocation(response);
        }
        final Node targetNode = createURI(targetUri);

        final HttpPost post1 = postObjMethod();
        post1.setHeader(CONTENT_TYPE, "text/turtle");
        post1.setEntity(new StringEntity("<> <" + referenceProp1.getURI() + "> <" + targetUri + "> ."));
        final String firstContainerUri;
        try (final CloseableHttpResponse response = execute(post1)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            firstContainerUri = getLocation(response);
        }
        final Node subjectNode = createURI(firstContainerUri);

        final HttpPost post2 = postObjMethod();
        post2.setHeader(CONTENT_TYPE, "text/turtle");
        post2.setEntity(new StringEntity("<" + firstContainerUri + "> <" + referenceProp2.getURI() + "> <" + targetUri +
                "> ."));
        final String secondContainerUri;
        try (final CloseableHttpResponse response = execute(post2)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            secondContainerUri = getLocation(response);
        }

        final HttpGet getReferences = new HttpGet(targetUri);
        getReferences.setHeader("Prefer", INBOUND_REFERENCE_PREFER_HEADER);
        try (final CloseableDataset dataset = getDataset(getReferences)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(ANY, subjectNode, referenceProp1, targetNode));
            assertTrue(graph.contains(ANY, subjectNode, referenceProp2, targetNode));
        }
        // Delete the first resource.
        final HttpDelete deleteFirst = new HttpDelete(firstContainerUri);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteFirst));
        // Still one reference.
        final HttpGet getReferences2 = new HttpGet(targetUri);
        getReferences2.setHeader("Prefer", INBOUND_REFERENCE_PREFER_HEADER);
        try (final CloseableDataset dataset = getDataset(getReferences2)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertFalse(graph.contains(ANY, subjectNode, referenceProp1, targetNode));
            assertTrue(graph.contains(ANY, subjectNode, referenceProp2, targetNode));
        }
        // Delete the second resource
        final HttpDelete deleteSecond = new HttpDelete(secondContainerUri);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteSecond));
        // No more references.
        final HttpGet getReferences3 = new HttpGet(targetUri);
        getReferences3.setHeader("Prefer", INBOUND_REFERENCE_PREFER_HEADER);
        try (final CloseableDataset dataset = getDataset(getReferences3)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertFalse(graph.contains(ANY, subjectNode, referenceProp1, targetNode));
            assertFalse(graph.contains(ANY, subjectNode, referenceProp2, targetNode));
        }
    }

    @Test
    public void testReferenceRemovedOnDelete() throws Exception {
        final String id = getRandomUniqueId();
        final String resource = serverAddress + id;
        final String resourcea = resource + "/a";
        final String resourceb = resource + "/b";
        final String resourcec = resource + "/c";

        createObjectAndClose(id);
        createObjectAndClose(id + "/a");
        createObjectAndClose(id + "/b");
        createObjectAndClose(id + "/c");
        // Patch a to point to c.
        final HttpPatch updateObjectGraphMethodA = patchObjMethod(id + "/a");
        updateObjectGraphMethodA.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethodA.setEntity(new StringEntity("INSERT { <" +
                resourcea + "> <http://purl.org/dc/terms/isPartOf> <" + resourcec + "> . } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethodA));
        // Patch b to point to c as well.
        final HttpPatch updateObjectGraphMethodB = patchObjMethod(id + "/b");
        updateObjectGraphMethodB.addHeader(CONTENT_TYPE, "application/sparql-update");
        updateObjectGraphMethodB.setEntity(new StringEntity("INSERT { <" +
                resourceb + "> <http://purl.org/dc/terms/isPartOf> <" + resourcec + "> . } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethodB));

        // Verify the inbound references.
        final HttpGet getObjMethod = new HttpGet(resourcec);
        getObjMethod.addHeader("Prefer", INBOUND_REFERENCE_PREFER_HEADER);
        try (final CloseableDataset dataset = getDataset(getObjMethod)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(ANY,
                    createURI(resourcea), createURI("http://purl.org/dc/terms/isPartOf"), createURI(resourcec)));

            assertTrue(graph.contains(ANY,
                    createURI(resourceb), createURI("http://purl.org/dc/terms/isPartOf"), createURI(resourcec)));
        }
        // Delete a.
        final HttpDelete deleteReferrer = deleteObjMethod(id + "/a");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteReferrer));
        // Verify only inbound reference from b
        final HttpGet getObjMethod2 = new HttpGet(resourcec);
        getObjMethod2.addHeader("Prefer", INBOUND_REFERENCE_PREFER_HEADER);
        try (final CloseableDataset dataset = getDataset(getObjMethod2)) {
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertFalse(graph.contains(ANY,
                    createURI(resourcea), createURI("http://purl.org/dc/terms/isPartOf"), createURI(resourceb)));

            assertTrue(graph.contains(ANY,
                    createURI(resourceb), createURI("http://purl.org/dc/terms/isPartOf"), createURI(resourcec)));
        }
    }

    @Test
    @Ignore("Membership/containment not included as inbound references - FCREPO-3589")
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
    public void testNoReferentialIntegrity() throws IOException {
        final HttpPatch patch = new HttpPatch(getLocation(postObjMethod()));
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity("INSERT { " +
                "<> <http://some-vocabulary#isMemberOfCollection> <" + serverAddress + "non-existant> } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch));
    }

    @Test
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
    public void testPutResourceBadRdf() throws IOException {
        final HttpPut httpPut = new HttpPut(serverAddress + getRandomUniqueId());
        httpPut.setHeader(CONTENT_TYPE, "text/turtle");
        httpPut.setEntity(new StringEntity("<> a \"still image\"."));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(httpPut));
    }

    @Test
    public void testCreateResourceWithoutContentType() {
        assertEquals(CREATED.getStatusCode(), getStatus(new HttpPut(serverAddress + getRandomUniqueId())));
    }

    @Test
    public void testUpdateObjectWithoutContentType() throws IOException {
        final HttpPut httpPut = new HttpPut(getLocation(postObjMethod()));
        // use a bytestream-based entity to avoid settin a content type
        httpPut.setEntity(new ByteArrayEntity("bogus content".getBytes(UTF_8)));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(httpPut));
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

    @Test
    public void testBinarySetBadMimeType() throws Exception {
        final String binId = getRandomUniqueId();
        final String subjectURI = serverAddress + binId;
        final HttpPut createMethod = new HttpPut(subjectURI);
        createMethod.addHeader(CONTENT_TYPE, "text/plain");
        createMethod.setEntity(new StringEntity("Some text here."));
        createMethod.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);

        assertEquals(CREATED.getStatusCode(), getStatus(createMethod));

        final HttpPatch patch = new HttpPatch(subjectURI + "/fcr:metadata");
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "PREFIX ebucore: <http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#> " +
                "INSERT { <" + subjectURI + "> ebucore:hasMimeType \"-- invalid syntax! --\" } WHERE {}")
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
        final Model model = getModel(binId + "/fcr:metadata");
        final Resource binResc = model.getResource(subjectURI);
        final Statement mimetypeStmt = binResc.getProperty(HAS_MIME_TYPE);
        assertEquals("Expected binary description to retain previous mimetype",
                "text/plain", mimetypeStmt.getString());
    }

    @Test
    public void testBinarySetBadMimeTypeViaHeader() throws Exception {
        final String binId = getRandomUniqueId();
        final String subjectURI = serverAddress + binId;
        final HttpPut createMethod = new HttpPut(subjectURI);
        createMethod.addHeader(CONTENT_TYPE, "-- invalid syntax! --");
        createMethod.setEntity(new StringEntity("Some text here."));
        createMethod.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);

        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(createMethod));
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
    public void testRoundTripReplaceGraphForDatastreamDescription() throws IOException {
        final String id = getRandomUniqueId();
        final String subjectURI = serverAddress + id + "/ds1";
        createDatastream(id, "ds1", "some-content");

        final HttpGet getObjMethod = new HttpGet(subjectURI + "/" + FCR_METADATA);
        getObjMethod.addHeader(ACCEPT, "text/turtle");
        getObjMethod.addHeader("Prefer", "return=representation; omit=\"" + PREFER_SERVER_MANAGED + "\"");
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
    public void testDefaultBinaryDescriptionTriples() throws Exception {
        final String id = getRandomUniqueId();
        final HttpPost post = postObjMethod();
        final String binaryUri = serverAddress + id;
        final Node binaryNode = createURI(binaryUri);

        post.setHeader("Slug", id);
        post.setHeader(CONTENT_TYPE, TEXT_PLAIN);
        post.setEntity(new StringEntity("some text", UTF_8));
        post.setHeader(CONTENT_DISPOSITION, "attachment; filename=\"mytest.txt\"");
        assertEquals(CREATED.getStatusCode(), getStatus(post));

        final HttpGet get = getObjMethod(id + "/" + FCR_METADATA);
        try (final CloseableHttpResponse response = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            try (final CloseableDataset dataset = getDataset(response)) {
                final DatasetGraph graph = dataset.asDatasetGraph();
                // Not checking for CreatedBy and LastModifiedBy as they require authentication.
                assertFalse(dataset.isEmpty());
                assertTrue(graph.contains(ANY, binaryNode, CREATED_DATE.asNode(), ANY));
                assertTrue(graph.contains(ANY, binaryNode, LAST_MODIFIED_DATE.asNode(), ANY));
                assertTrue(graph.contains(ANY, binaryNode, type.asNode(), NON_RDF_SOURCE.asNode()));
                assertTrue(graph.contains(ANY, binaryNode, type.asNode(), RESOURCE.asNode()));
                assertTrue(graph.contains(ANY, binaryNode, type.asNode(), FEDORA_RESOURCE.asNode()));
                assertTrue(graph.contains(ANY, binaryNode, type.asNode(), FEDORA_BINARY.asNode()));
                assertTrue(graph.contains(ANY, binaryNode, HAS_SIZE.asNode(), createLiteral("9", XSDlong)));
                assertTrue(graph.contains(ANY, binaryNode, HAS_MESSAGE_DIGEST.asNode(), ANY));
                assertTrue(graph.contains(ANY, binaryNode, HAS_FIXITY_SERVICE.asNode(),
                        createURI(binaryUri + "/" + FCR_FIXITY)));
                assertTrue(graph.contains(ANY, binaryNode, HAS_ORIGINAL_NAME.asNode(),
                        createLiteral("mytest.txt")));
            }
        }

        // Now get with ServerManaged omitted
        final HttpGet get2 = getObjMethod(id + "/" + FCR_METADATA);
        get2.setHeader("Prefer", "return=representation; omit=\"" + PREFER_SERVER_MANAGED + "\"");
        try (final CloseableHttpResponse response = execute(get2)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            try (final CloseableDataset dataset = getDataset(response)) {
                assertTrue(dataset.isEmpty());
            }
        }
    }

    @Test
    public void binaryDescMementoShouldHaveHistoricFileAttributes() throws Exception {
        final String id = getRandomUniqueId();
        final HttpPost post = postObjMethod();
        final String binaryUri = serverAddress + id;
        final Node binaryNode = createURI(binaryUri);

        final String originalName = "mytest.txt";
        final String updatedName = "updated.txt";
        final String originalSize = "9";
        final String updatedSize = "17";

        post.setHeader("Slug", id);
        post.setHeader(CONTENT_TYPE, TEXT_PLAIN);
        post.setEntity(new StringEntity("some text", UTF_8));
        post.setHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"");
        assertEquals(CREATED.getStatusCode(), getStatus(post));

        // create binary
        HttpGet get = getObjMethod(id + "/" + FCR_METADATA);
        try (final CloseableHttpResponse response = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            try (final CloseableDataset dataset = getDataset(response)) {
                final DatasetGraph graph = dataset.asDatasetGraph();
                assertFalse(dataset.isEmpty());
                assertTrue(graph.contains(ANY, binaryNode, HAS_SIZE.asNode(), createLiteral(originalSize, XSDlong)));
                assertTrue(graph.contains(ANY, binaryNode, HAS_ORIGINAL_NAME.asNode(),
                        createLiteral(originalName)));
            }
        }

        // get memento url
        final HttpPost createVersionMethod = new HttpPost(binaryUri + "/" + FCR_VERSIONS);
        final String binaryMementoUrl;
        try (final CloseableHttpResponse response = execute(createVersionMethod)) {
            binaryMementoUrl = response.getFirstHeader(LOCATION).getValue();
        }
        final var binDescMementoUrl = binaryUri + "/" + FCR_METADATA + "/"
                + binaryMementoUrl.substring(binaryMementoUrl.indexOf("/fcr:"));

        TimeUnit.SECONDS.sleep(1);

        final var put = putObjMethod(id);
        put.setHeader("Slug", id);
        put.setHeader(CONTENT_TYPE, TEXT_PLAIN);
        put.setEntity(new StringEntity("some updated text", UTF_8));
        put.setHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + updatedName + "\"");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(put));

        // update binary
        get = getObjMethod(id + "/" + FCR_METADATA);
        try (final CloseableHttpResponse response = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            try (final CloseableDataset dataset = getDataset(response)) {
                final DatasetGraph graph = dataset.asDatasetGraph();
                assertFalse(dataset.isEmpty());
                assertTrue(graph.contains(ANY, binaryNode, HAS_SIZE.asNode(), createLiteral(updatedSize, XSDlong)));
                assertTrue(graph.contains(ANY, binaryNode, HAS_ORIGINAL_NAME.asNode(),
                        createLiteral(updatedName)));
            }
        }

        //
        try (final CloseableHttpResponse response = execute(new HttpGet(binDescMementoUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            try (final CloseableDataset dataset = getDataset(response)) {
                final DatasetGraph graph = dataset.asDatasetGraph();
                assertFalse(dataset.isEmpty());
                assertTrue(graph.contains(ANY, binaryNode, HAS_SIZE.asNode(), createLiteral(originalSize, XSDlong)));
                assertTrue(graph.contains(ANY, binaryNode, HAS_ORIGINAL_NAME.asNode(),
                        createLiteral(originalName)));
            }
        }
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

    /**
     * Without the asterisks on the GET/HEAD methods Jersey will fail to serve all other content types for binaries.
     */
    @Test
    public void testBinaryAcceptHeaders() throws IOException {
        final HttpPost post = postObjMethod();
        post.setHeader(CONTENT_TYPE, "application/pdf");
        post.setEntity(new StringEntity("Some fake content", UTF_8));
        final String objectUrl;
        try (final CloseableHttpResponse response = execute(post)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            objectUrl = getLocation(response);
        }
        final HttpGet httpGet = new HttpGet(objectUrl);
        httpGet.setHeader(ACCEPT, "image/tiff");
        assertEquals(NOT_ACCEPTABLE.getStatusCode(), getStatus(httpGet));

        final HttpGet httpGet2 = new HttpGet(objectUrl);
        httpGet2.setHeader(ACCEPT, "application/pdf");
        assertEquals(OK.getStatusCode(), getStatus(httpGet2));

        final HttpGet httpGet3 = new HttpGet(objectUrl);
        httpGet3.addHeader(ACCEPT, "*/*");
        try (final var response = execute(httpGet3)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            checkContentTypeMatches(response, "application/pdf");
        }

        final HttpGet httpGet4 = new HttpGet(objectUrl);
        try (final var response = execute(httpGet4)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            checkContentTypeMatches(response, "application/pdf");
        }

        final HttpHead httpHead = new HttpHead(objectUrl);
        httpHead.setHeader(ACCEPT, "image/tiff");
        assertEquals(NOT_ACCEPTABLE.getStatusCode(), getStatus(httpHead));

        final HttpHead httpHead2 = new HttpHead(objectUrl);
        httpHead2.setHeader(ACCEPT, "application/pdf");
        assertEquals(OK.getStatusCode(), getStatus(httpHead2));

        final HttpHead httpHead3 = new HttpHead(objectUrl);
        httpHead3.setHeader(ACCEPT, "*/*");
        try (final var response = execute(httpHead3)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            checkContentTypeMatches(response, "application/pdf");
        }

        final HttpHead httpHead4 = new HttpHead(objectUrl);
        try (final var response = execute(httpHead4)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            checkContentTypeMatches(response, "application/pdf");
        }
    }

    private static void checkContentTypeMatches(final CloseableHttpResponse response, final String contentType) {
        final Collection<String> contentTypes = getHeader(response, CONTENT_TYPE);
        final String type = contentTypes.iterator().next();
        assertTrue("Didn't find LDP valid content-type header: " + type +
                "; expected result: " + contentType, type.contains(contentType));
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
    public void testUpdateObjectWithSpacesPost() throws IOException {
        final String id = getRandomUniqueId() + " 2";
        final String expectedUri = serverAddress + id.replace(" ", "%20");
        final String notExpectedUri = expectedUri.replace("%", "%25");
        try (final CloseableHttpResponse createResponse = createObject(id)) {
            final String subjectURI = getLocation(createResponse);
            assertEquals(expectedUri, subjectURI);
            final HttpPatch updateObjectGraphMethod = new HttpPatch(subjectURI);
            updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
            updateObjectGraphMethod.setEntity(new StringEntity(
                    "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"test\" } WHERE {}"));
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));
        }
        final HttpGet httpGet = new HttpGet(expectedUri);
        try (final var dataset = getDataset(httpGet)) {
            final var graph  = dataset.asDatasetGraph();
            // Ensure the graph has an encoded URI too.
            assertTrue(graph.contains(
                    ANY,
                    createURI(expectedUri),
                    ANY,
                    ANY
            ));
            // Ensure it does not have a double encoded URI.
            assertFalse(graph.contains(
                    ANY,
                    createURI(notExpectedUri),
                    ANY,
                    ANY
            ));
        }
    }

    @Test
    public void testUpdateObjectWithSpacesPut() throws IOException {
        final String id = getRandomUniqueId() + "%202";
        final String expectedUri = serverAddress + id;
        final String notExpectedUri = expectedUri.replace("%", "%25");
        try (final CloseableHttpResponse createResponse = execute(putObjMethod(id))) {
            final String subjectURI = getLocation(createResponse);
            assertEquals(expectedUri, subjectURI);
            final HttpPatch updateObjectGraphMethod = new HttpPatch(subjectURI);
            updateObjectGraphMethod.addHeader(CONTENT_TYPE, "application/sparql-update");
            updateObjectGraphMethod.setEntity(new StringEntity(
                    "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"test\" } WHERE {}"));
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));
        }
        final HttpGet httpGet = new HttpGet(expectedUri);
        try (final var dataset = getDataset(httpGet)) {
            final var graph  = dataset.asDatasetGraph();
            // Ensure the graph has an encoded URI too.
            assertTrue(graph.contains(
                    ANY,
                    createURI(expectedUri),
                    ANY,
                    ANY
            ));
            // Ensure it does not have a double encoded URI.
            assertFalse(graph.contains(
                    ANY,
                    createURI(notExpectedUri),
                    ANY,
                    ANY
            ));
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
                        getDateFromModel(model, nodeUri, CREATED_DATE);
                final Optional<Instant> lastmodDateTriples =
                        getDateFromModel(model, nodeUri, LAST_MODIFIED_DATE);
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
    public void testPostCreateDate() throws IOException {
        final HttpPost httpPost = postObjMethod("/");
        httpPost.addHeader("Slug", getRandomUniqueId());
        httpPost.addHeader(CONTENT_TYPE, "text/turtle");
        httpPost.setEntity(new StringEntity(getTTLThatUpdatesServerManagedTriples("fakeuser", null, null, null)));
        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Must not be able to update createdBy!", CONFLICT.getStatusCode(), getStatus(response));
            assertConstrainedByPresent(response);
            checkForLinkHeader(response, SERVER_MANAGED_PROPERTY_CONSTRAINT_URI, CONSTRAINED_BY.toString());
        }
    }

    @Test
    public void testPutRdfLenient() throws IOException {
        final String location = getLocation(postObjMethod());
        final Node uriResource = createURI(location);
        final HttpGet getObjMethod = new HttpGet(location);
        String createdDate = null;
        try (final CloseableHttpResponse response = execute(getObjMethod)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            try (final CloseableDataset dataset = getDataset(response)) {
                final DatasetGraph graph = dataset.asDatasetGraph();
                final var iterator = graph.find(ANY, uriResource, CREATED_DATE.asNode(), ANY);
                while (iterator.hasNext()) {
                    final var quad = iterator.next();
                    createdDate = quad.getObject().getLiteral().toString();
                }
            }
        }
        final HttpPut putMethod = new HttpPut(location);
        putMethod.setHeader(CONTENT_TYPE, "text/turtle");
        putMethod.setEntity(new StringEntity("<> <" + CREATED_DATE + "> \"1979-01-01T00:00:00\" ;" +
                "<" + DCTITLE + "> \"This is a new title\" . "));
        // Can't set the created date
        assertEquals(CONFLICT.getStatusCode(), getStatus(putMethod));

        // With lenient we ignore it.
        putMethod.setHeader("Prefer", "handling=\"lenient\"");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(putMethod));

        final HttpGet getObj2 = new HttpGet(location);
        try (final CloseableHttpResponse response = execute(getObj2)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            try (final CloseableDataset dataset = getDataset(response)) {
                String createdDate2 = null;
                String title = null;
                final DatasetGraph graph = dataset.asDatasetGraph();
                final var iterator = graph.find(ANY, uriResource, CREATED_DATE.asNode(), ANY);
                while (iterator.hasNext()) {
                    final var quad = iterator.next();
                    createdDate2 = quad.getObject().getLiteral().toString();
                }
                final var iterator2 = graph.find(ANY, uriResource, DCTITLE, ANY);
                while (iterator2.hasNext()) {
                    final var quad = iterator2.next();
                    title = quad.getObject().getLiteral().toString();
                }
                // Created date change did not take effect.
                assertEquals(createdDate, createdDate2);
                // But the new title is there.
                assertEquals("This is a new title", title);
            }
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
                    resource, CONTAINS.asNode(), createURI(location + "/a")));
            assertTrue("Expected to have container b", graphStore.contains(ANY,
                    resource, CONTAINS.asNode(), createURI(location + "/b")));
            assertTrue("Expected member relation", graphStore.contains(ANY,
                    resource, createURI("info:some/relation"), createURI(location + "/a/1")));
            assertTrue("Expected other member relation", graphStore.contains(ANY,
                    resource, createURI("info:some/another-relation"), createURI(location + "/b/1")));
        }
    }

    @Test
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
                + "<" + INSERTED_CONTENT_RELATION + "> <info:proxy/for> .\n";
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
                    createURI(container), CONTAINS.asNode(), createURI(indirectContainer)));

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
                        createURI(location + "#abc"), LAST_MODIFIED_DATE.asNode(), ANY));
                assertFalse(graphStore.contains(ANY,
                        createURI(location + "#abc"), rdfType, FEDORA_RESOURCE.asNode()));
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
        replaceMethod.addHeader("Prefer", "handling=lenient");
        replaceMethod.setEntity(new StringEntity("<> <" + DCTITLE.getURI() + "> \"xyz\""));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(replaceMethod));

        final HttpGet get = new HttpGet(subjectURI);
        get.addHeader("Prefer", "return=representation; include=\"" + PREFER_MINIMAL_CONTAINER + "\"");
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
    public void testPathWithEmptySegment() {
        // Ensure HttpClient does not remove empty paths
        final RequestConfig config = RequestConfig.custom().setNormalizeUri(false).build();
        final String badLocation = "test/me/mb/er/s//members/9528a300-22da-40f2-bf3c-5b345d71affb";
        final var getEmpty = headObjMethod(badLocation);
        getEmpty.setConfig(config);
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(getEmpty));
    }

    private static Optional<Instant> getDateFromModel(final Model model, final Resource subj, final Property pred)
            throws NoSuchElementException {
        final StmtIterator stmts = model.listStatements(subj, pred, (String) null);
        return Optional.ofNullable(
           stmts.hasNext() ? Instant.from(tripleFormat.parse(stmts.nextStatement().getString())) : null);
    }

    @Test
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
                NO_CONTENT.getStatusCode(),getStatus(updateObjectGraphMethod));
    }

    @Test
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
    public void testConcurrentPutsAsChildren() throws InterruptedException, IOException {
        final String parent = getRandomUniqueId();
        executeAndClose(putObjMethod(parent));
        final String first = parent + "/1";
        final String second = parent + "/2";
        final String third = parent + "/3";
        final String fourth = parent + "/4";

        final var phaser = new Phaser(5);

        final RequestThread[] threads = new RequestThread[] {
                new RequestThread(putObjMethod(first), phaser),
                new RequestThread(putObjMethod(second), phaser),
                new RequestThread(putObjMethod(third), phaser),
                new RequestThread(putObjMethod(fourth), phaser)
        };

        for (final RequestThread t : threads) {
            t.start();
        }

        phaser.arriveAndAwaitAdvance();

        final List<RequestThread> successfulThreads = new ArrayList<>();
        for (final RequestThread t : threads) {
            t.join(1000);
            assertFalse("Thread " + t.getId() + " could not perform its operation in time!", t.isAlive());
            final int status = t.response.getStatusLine().getStatusCode();
            LOGGER.info("{} received a {} status code.", t.getId(), status);
            if (status == 201) {
                successfulThreads.add(t);
            }
        }

        assertEquals("All four PUT requests should have been successful!", 4, successfulThreads.size());

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

    /**
     * Ghost nodes are special paths that become part of a single resource, so you cannot have the below situation of
     * /00/1 and /00/2.
     */
    @Test
    public void testConcurrentPutsWithGhostNodes() throws InterruptedException, IOException {
        final String parent = getRandomUniqueId();
        executeAndClose(putObjMethod(parent));
        final String first = parent + "/00/1";
        final String second = parent + "/00/2";
        final String third = parent + "/00/3";
        final String fourth = parent + "/00/4";

        final var phaser = new Phaser(5);

        final var tx1 = createTransaction();
        final var tx2 = createTransaction();
        final var tx3 = createTransaction();
        final var tx4 = createTransaction();

        final RequestThread[] threads = new RequestThread[] {
                new RequestThread(addTxTo(putObjMethod(first), tx1), phaser),
                new RequestThread(addTxTo(putObjMethod(second), tx2), phaser),
                new RequestThread(addTxTo(putObjMethod(third), tx3), phaser),
                new RequestThread(addTxTo(putObjMethod(fourth), tx4), phaser)
        };

        for (final RequestThread t : threads) {
            t.start();
        }

        phaser.arriveAndAwaitAdvance();

        final List<RequestThread> successfulThreads = new ArrayList<>();
        for (final RequestThread t : threads) {
            t.join(1000);
            assertFalse("Thread " + t.getId() + " could not perform its operation in time!", t.isAlive());
            final int status = t.response.getStatusLine().getStatusCode();
            LOGGER.info("{} received a {} status code.", t.getId(), status);
            if (status == 201) {
                successfulThreads.add(t);
            }
        }

        assertEquals("Only one PUT request should have been successful!", 1, successfulThreads.size());
    }

    @Test
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

        final var phaser = new Phaser(5);

        final RequestThread[] threads = new RequestThread[] {
                new RequestThread(putBinaryObjMethodIfMatch(path, binaryEtag, "thread 1"), phaser),
                new RequestThread(putBinaryObjMethodIfMatch(path, binaryEtag, "thread 2"), phaser),
                new RequestThread(putBinaryObjMethodIfMatch(path, binaryEtag, "thread 3"), phaser),
                new RequestThread(putBinaryObjMethodIfMatch(path, binaryEtag, "thread 4"), phaser)
        };

        for (final RequestThread t : threads) {
            t.start();
        }

        phaser.arriveAndAwaitAdvance();

        final List<RequestThread> successfulThreads = new ArrayList<>();
        for (final RequestThread t : threads) {
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

    private static class RequestThread extends Thread {

        private final HttpUriRequest request;

        private final Phaser phaser;

        private HttpResponse response;

        public RequestThread(final HttpUriRequest request, final Phaser phaser) {
            this.request = request;
            this.phaser = phaser;
        }

        @Override
        public void run() {
            try {
                phaser.arriveAndAwaitAdvance();
                response = execute(request);
            } catch (final IOException e) {
                LOGGER.error("Thread " + Thread.currentThread().getId() + ", failed to request!", e);
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
    public void testBinaryDescUpdateLastModified() throws Exception {
        final String objId = getRandomUniqueId();
        final String objURI = serverAddress + objId;
        final String binURI = objURI + "/binary";

        final Instant lastModified;
        final Instant lastModifiedUpdate;
        try (final CloseableHttpResponse response = execute(putDSMethod(objId, "binary", "test content"))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        try (final CloseableDataset dataset = getDataset(new HttpGet(binURI + "/fcr:metadata"))) {
            final var graph = dataset.asDatasetGraph();
            final var modifiedNode = graph.find(ANY, ANY, LAST_MODIFIED_DATE.asNode(), ANY).next().getObject();
            lastModified = Instant.from(tripleFormat.parse(modifiedNode.getLiteralValue().toString()));
        }

        // replace the binary and make sure the last modified is updated
        try (final CloseableHttpResponse response = execute(putDSMethod(objId, "binary", "updated content"))) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        try (final CloseableDataset dataset = getDataset(new HttpGet(binURI + "/fcr:metadata"))) {
            final var graph = dataset.asDatasetGraph();
            final var modifiedNode = graph.find(ANY, ANY, LAST_MODIFIED_DATE.asNode(), ANY).next().getObject();
            lastModifiedUpdate = Instant.from(tripleFormat.parse(modifiedNode.getLiteralValue().toString()));
        }

        assertTrue(lastModified.isBefore(lastModifiedUpdate));
    }

    @Test
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
    public void testPostFcrSlug() throws IOException {
        final HttpPost httpPost = postObjMethod();
        httpPost.addHeader("Slug", "fcr:path");
        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Must not be able to POST with fcr namespaced Slug!", CONFLICT.getStatusCode(),
                getStatus(response));
        }
    }

    @Test
    public void testPutFcrPath() throws IOException {
        final HttpPut httpPut = putObjMethod("/fcr:path");
        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Must not be able to PUT with fcr namespaced path!", CONFLICT.getStatusCode(),
                getStatus(response));
        }
    }

    @Test
    public void testPutFcrTx() throws IOException {
        final HttpPut httpPut = putObjMethod("hello/fcr:tx");
        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Must not be able to PUT with fcr namespaced path!", CONFLICT.getStatusCode(),
                getStatus(response));
        }
    }

    @Test
    public void testDeleteWithFcrPath() throws IOException {
        final String id = getRandomUniqueId() + "/fcr:delete";
        final HttpDelete httpDelete = deleteObjMethod(id);
        httpDelete.addHeader("Depth", "infinity");
        try (final CloseableHttpResponse response = execute(httpDelete)) {
            assertEquals("Must not be able to DELETE with fcr namespaced path!", CONFLICT.getStatusCode(),
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
            assertConstrainedByPresent(response);
            checkForLinkHeader(response, SERVER_MANAGED_TYPE_CONSTRAINT_URI, CONSTRAINED_BY.toString());
        }
    }

    @Test
    public void testPutCreateRDFSourceWithMementoNamespaceType() throws IOException {
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut putMethod = new HttpPut(subjectURI);
        putMethod.addHeader(CONTENT_TYPE, "text/turtle");
        putMethod.setEntity(new StringEntity("<> a <" + MEMENTO_TYPE + "> ."));
        try (final CloseableHttpResponse response = execute(putMethod)) {
            assertEquals("Must not be able to PUT RDF that contains an \"<> a  <" + MEMENTO_TYPE + ">\"",
                         CONFLICT.getStatusCode(),
                         getStatus(response));
            checkForLinkHeader(response, SERVER_MANAGED_TYPE_CONSTRAINT_URI, CONSTRAINED_BY.toString());
        }
    }

    @Test
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
            checkForLinkHeader(response, SERVER_MANAGED_TYPE_CONSTRAINT_URI, CONSTRAINED_BY.toString());
        }
    }

    @Test
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
            checkForLinkHeader(response, SERVER_MANAGED_PROPERTY_CONSTRAINT_URI, CONSTRAINED_BY.toString());
        }
    }

    @Test
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
            checkForLinkHeader(response, SERVER_MANAGED_PROPERTY_CONSTRAINT_URI, CONSTRAINED_BY.toString());
        }
    }

    @Test
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
            checkForLinkHeader(response, SERVER_MANAGED_PROPERTY_CONSTRAINT_URI, CONSTRAINED_BY.toString());
        }
    }

    @Test
    public void testPostCreateRDFSourceWithMementoTypeAndProperty() throws IOException {
        final String subjectURI = getRandomUniqueId();
        final HttpPost createMethod = new HttpPost(serverAddress);
        createMethod.addHeader(CONTENT_TYPE, "text/turtle");
        createMethod.addHeader("Slug", subjectURI);
        createMethod.setEntity(
                new StringEntity("<> a <" + MEMENTO_TYPE + "> ; " +
                        "<" + MEMENTO_NAMESPACE + "mementoDatetime" + "> \"Thu, 21 Jan 2010 00:09:40 GMT\""));
        try (final CloseableHttpResponse response = execute(createMethod)) {
            assertEquals("Must not be able to POST RDF that contains a memento type and namespace predicate",
                    CONFLICT.getStatusCode(),
                    getStatus(response));
            checkForLinkHeader(response, SERVER_MANAGED_PROPERTY_CONSTRAINT_URI, CONSTRAINED_BY.toString());
            checkForLinkHeader(response, SERVER_MANAGED_TYPE_CONSTRAINT_URI, CONSTRAINED_BY.toString());
        }
    }

    @Test
    public void testPutCreateRDFSourceWithMementoTypeAndProperty() throws IOException {
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut putMethod = new HttpPut(subjectURI);
        putMethod.addHeader(CONTENT_TYPE, "text/turtle");
        putMethod.setEntity(
                new StringEntity("<> a <" + MEMENTO_TYPE + "> ; " +
                        "<" + MEMENTO_NAMESPACE + "mementoDatetime" + "> \"Thu, 21 Jan 2010 00:09:40 GMT\""));
        try (final CloseableHttpResponse response = execute(putMethod)) {
            assertEquals("Must not be able to PUT RDF that contains a memento type and namespace predicate",
                    CONFLICT.getStatusCode(),
                    getStatus(response));
            checkForLinkHeader(response, SERVER_MANAGED_PROPERTY_CONSTRAINT_URI, CONSTRAINED_BY.toString());
            checkForLinkHeader(response, SERVER_MANAGED_TYPE_CONSTRAINT_URI, CONSTRAINED_BY.toString());
        }
    }

    @Test
    public void testPatchInsertMementoTypeAndProperty() throws IOException {
        final String pid = getRandomUniqueId();
        final String subjectURI = serverAddress + pid;
        createObjectAndClose(pid);
        final HttpPatch patch = new HttpPatch(subjectURI);
        patch.addHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity(
                "INSERT { <> a <" + MEMENTO_TYPE + "> ; <" + MEMENTO_NAMESPACE + "mementoDatetime" +
                        "> \"Thu, 21 Jan 2010 00:09:40 GMT\". } WHERE {}"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals("Must not be able to PATCH RDF that contains a memento type and namespace predicate",
                    CONFLICT.getStatusCode(), getStatus(response));
            checkForLinkHeader(response, SERVER_MANAGED_PROPERTY_CONSTRAINT_URI, CONSTRAINED_BY.toString());
            checkForLinkHeader(response, SERVER_MANAGED_TYPE_CONSTRAINT_URI, CONSTRAINED_BY.toString());
        }
    }

    @Test
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
    public void testPutUpdateBinaryWithType() throws Exception {
        final String objid = getRandomUniqueId();
        final String objURI = serverAddress + objid;
        final String binURI = objURI + "/binary1";
        final String descURI = objURI + "/binary1/fcr:metadata";

        try (final CloseableHttpResponse response = execute(putDSMethod(objid, "binary1", "some test content"))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        // Add type using PUT
        final Model model1 = getModel(objid + "/binary1/fcr:metadata", true);
        final Resource resc = model1.getResource(binURI);
        resc.addProperty(RDF.type, PCDM_FILE_TYPE);

        final HttpPut addMethod = new HttpPut(descURI);
        addMethod.addHeader(CONTENT_TYPE, "text/turtle");
        addMethod.setEntity(new InputStreamEntity(streamModel(model1, RDFFormat.TURTLE)));
        try (final CloseableHttpResponse response = execute(addMethod)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        final Model model2 = getModel(objid + "/binary1/fcr:metadata", true);
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
     * @throws Exception issue with http communication.
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

    @Test
    public void testGhostNodes() throws IOException {
        final String shallowId = getRandomUniqueId();
        final String ghostSlug = getRandomUniqueId();
        final String ghostId = shallowId + "/" + ghostSlug;
        final String deepId = ghostId + "/" + getRandomUniqueId();
        // Create 'a'
        assertEquals(CREATED.getStatusCode(), getStatus(putObjMethod(shallowId)));
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod(shallowId)));

        // Create 'a/b/c' ('b' is a ghost node)
        assertEquals(CREATED.getStatusCode(), getStatus(putObjMethod(deepId)));
        // Ensure the resource exists.
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod(deepId)));

        // Ensure the ghost node does not exist.
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getObjMethod(ghostId)));
        // Ensure you can't create at the ghost node location.
        assertEquals(CONFLICT.getStatusCode(), getStatus(putObjMethod(ghostId)));
        // Ensure you can't POST with a slug to get the ghost node location.
        final HttpPost ghostPost = postObjMethod(shallowId);
        ghostPost.setHeader("Slug", ghostSlug);
        try (final CloseableHttpResponse response = execute(ghostPost)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            assertFalse(getLocation(response).contains(ghostId));
        }
        // Ensure you can't create a child of the ghost node.
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(postObjMethod(ghostId)));

        // Delete 'a/b/c'
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteObjMethod(deepId)));

        // Assert 'a/b/c' is gone.
        assertEquals(GONE.getStatusCode(), getStatus(getObjMethod(deepId)));

        // Still can't create because there is a tombstone
        assertEquals(CONFLICT.getStatusCode(), getStatus(putObjMethod(ghostId)));

        // Delete the tombstone
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteObjMethod(deepId + "/" + FCR_TOMBSTONE)));

        // Now you can create a/b
        assertEquals(CREATED.getStatusCode(), getStatus(putObjMethod(ghostId)));

        // 'a' exists
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod(shallowId)));
        // 'a/b' exists
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod(ghostId)));
        // 'a/b/c' does not exist
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getObjMethod(deepId)));
    }

    @Test
    public void testRepositoryRootTypes() throws Exception {
        final HttpGet getRoot = new HttpGet(serverAddress);
        final Node rootNode = createURI(serverAddress);
        try (final CloseableHttpResponse response = execute(getRoot)) {
            final Dataset dataset = getDataset(response);
            final DatasetGraph graph = dataset.asDatasetGraph();
            assertTrue(graph.contains(ANY, rootNode, type.asNode(), REPOSITORY_ROOT.asNode()));
            assertTrue(graph.contains(ANY, rootNode, type.asNode(), RESOURCE.asNode()));
            assertTrue(graph.contains(ANY, rootNode, type.asNode(), FEDORA_RESOURCE.asNode()));
            assertTrue(graph.contains(ANY, rootNode, type.asNode(), RDF_SOURCE.asNode()));
            assertTrue(graph.contains(ANY, rootNode, type.asNode(), CONTAINER.asNode()));
            assertTrue(graph.contains(ANY, rootNode, type.asNode(), FEDORA_CONTAINER.asNode()));
            assertTrue(graph.contains(ANY, rootNode, type.asNode(), BASIC_CONTAINER.asNode()));
        }
    }

    private static String genLongUrl(final String prefix, final int numChars) {
        String ret = prefix;
        final Random rand = new Random();
        int cnt = prefix.length();

        while (cnt <= numChars) {
            ret = ret + "/" + (char)(rand.nextInt(26) + 'a');
            cnt = ret.length();
        }
        return ret;
    }

    @Test
    public void testLongIdentifier1() throws IOException {
        final String url = genLongUrl(serverAddress,476);
        final HttpPut putMethod = new HttpPut(url);
        assertEquals(CREATED.getStatusCode(), getStatus(putMethod));
        final HttpPost postMethod = new HttpPost(url);
        postMethod.setHeader("Slug", getRandomUniqueId());
        try (final CloseableHttpResponse response = execute(postMethod)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testLongIdentifier2() throws IOException {
        final String url = genLongUrl(serverAddress,477);
        final HttpPut putMethod = new HttpPut(url);
        assertEquals(CREATED.getStatusCode(), getStatus(putMethod));
        final HttpPost postMethod = new HttpPost(url);
        postMethod.setHeader("Slug", getRandomUniqueId());
        try (final CloseableHttpResponse response = execute(postMethod)) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testIdConstraints() throws IOException {
        assertIdStringConstraint(".fcrepo");
        assertIdStringConstraint("fcr-root");
        assertIdStringConstraint("fcr-container.nt");

        assertIdSuffixConstraint("~fcr-desc");
        assertIdSuffixConstraint("~fcr-acl");
        assertIdSuffixConstraint("~fcr-desc.nt");
        assertIdSuffixConstraint("~fcr-acl.nt");
    }

    @Test
    public void testMapRelativeUris() throws Exception {
        final HttpPost post = postObjMethod();
        final String body = "<> <http://example.org/related> </fcrepo/rest/test1> ; <http://example.org/related> " +
                "</rest/test2> ; <http://example.org/related> <http://something.else/outThere> ; " +
                "<http://example.org/related> <test3> .";
        post.setHeader(CONTENT_TYPE, "text/turtle");
        post.setEntity(new StringEntity(body, UTF_8));
        final String location;
        try (final var response = execute(post)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            location = getLocation(response);
        }
        final HttpGet get = new HttpGet(location);
        try (final var dataset = getDataset(get)) {
            final var graph = dataset.asDatasetGraph();
            final var nodeIter = graph.find(ANY, ANY, createURI("http://example.org/related"), ANY);
            int nodeCounter = 0;
            final List<String> expectedUris = List.of(serverAddress + "fcrepo/rest/test1",
                    serverAddress + "rest/test2", "http://something.else/outThere", serverAddress + "test3");
            while (nodeIter.hasNext()) {
                final var quad = nodeIter.next();
                nodeCounter += 1;
                assertTrue(expectedUris.contains(quad.getObject().getURI()));
            }
            assertEquals(4, nodeCounter);
        }
    }

    @Test
    public void testMapRelativeUrisDeeper() throws Exception {
        final String location1;
        final HttpPost post = postObjMethod();
        try (final var response = execute(post)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            location1 = getLocation(response);
        }
        final String location2;
        final HttpPost post2 = new HttpPost(location1);
        try (final var response = execute(post2)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            location2 = getLocation(response);
        }
        final HttpPost post3 = new HttpPost(location2);
        final String body = "<> <http://example.org/related> </fcrepo/rest/test1> ; <http://example.org/related> " +
                "</rest/test2> ; <http://example.org/related> <http://something.else/outThere> ; " +
                "<http://example.org/related> <test3> .";
        post3.setHeader(CONTENT_TYPE, "text/turtle");
        post3.setEntity(new StringEntity(body, UTF_8));
        final String location3;
        try (final var response = execute(post3)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            location3 = getLocation(response);
        }
        final HttpGet get = new HttpGet(location3);
        try (final var dataset = getDataset(get)) {
            final var graph = dataset.asDatasetGraph();
            final var nodeIter = graph.find(ANY, ANY, createURI("http://example.org/related"), ANY);
            int nodeCounter = 0;
            final List<String> expectedUris = List.of(serverAddress + "fcrepo/rest/test1",
                    serverAddress + "rest/test2", "http://something.else/outThere", location2 + "/test3");
            while (nodeIter.hasNext()) {
                final var quad = nodeIter.next();
                nodeCounter += 1;
                assertTrue(expectedUris.contains(quad.getObject().getURI()));
            }
            assertEquals(4, nodeCounter);
        }
    }

    @Test
    public void testNonExistantFcrMetadata() throws Exception {
        final var id = getRandomUniqueId();
        // Ensure the object does not exist
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getObjMethod(id)));
        // Ensure the metadata of a non-existant object also doesn't exist.
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getObjMethod(id + "/" + FCR_METADATA)));
        // Ensure you can't PUT at non-existant binary.
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(putObjMethod(id + "/" + FCR_METADATA)));
    }

    @Test
    public void createObjectWithUnicodeInSlug() throws IOException {
        final var id = "☞☕☜☑";
        final var encodedId = URLEncoder.encode(id, UTF_8);

        final String location;

        try (final var response = createObject(id)) {
            assertEquals(SC_CREATED, response.getStatusLine().getStatusCode());
            location = getLocation(response);
            assertTrue(String.format("Expected location to end with '/%s'. Found: %s", encodedId, location),
                    location.endsWith("/" + encodedId));
        }

        try (final var response = execute(new HttpGet(location))) {
            assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void postToNonexistantResourceFails() throws IOException {
        final var id = getRandomUniqueId();
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(getObjMethod(id)));
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(postObjMethod(id)));
    }

    @Test
    public void testBinaryDescriptionWithOtherSubject() throws IOException {
        final var schemaName = ResourceFactory.createProperty("http://schema.org/name");
        final var schemaAbout = ResourceFactory.createProperty("http://schema.org/about");
        final var wikiCat = ResourceFactory.createResource("http://en.wikipedia.org/Cat");

        final var id = getRandomUniqueId();
        final String location;
        final HttpPut putBinary = new HttpPut(serverAddress + id);
        putBinary.setHeader(CONTENT_TYPE, "text/x-ascii-art");
        putBinary.setHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        putBinary.setEntity(new StringEntity("\\    /\\\n )  ( `)\n(  /  )\n \\(__)|", UTF_8));
        // Make a binary
        try (final var response = execute(putBinary)) {
            assertEquals(SC_CREATED, getStatus(response));
            location = getLocation(response);
        }
        final HttpPut putDesc = new HttpPut(location + "/" + FCR_METADATA);
        putDesc.setHeader(CONTENT_TYPE, "text/turtle");
        putDesc.setEntity(new StringEntity("@prefix ldp:   <http://www.w3.org/ns/ldp#> .\n" +
                "         @prefix schema: <http://schema.org/> .\n" +
                "         @prefix wiki: <http://en.wikipedia.org/> .\n" +
                "         <>  schema:name \"Ashley\";\n" +
                "             schema:about wiki:Cat;\n" +
                "              .\n" +
                "         <" + location + "/" + FCR_METADATA + "> schema:name \"The description\".\n" +
                "\n" +
                "         wiki:Cat schema:name \"Cat\".", UTF_8));
        // Put description
        assertEquals(SC_NO_CONTENT, getStatus(putDesc));
        try (final var dataset = getDataset(getObjMethod(id + "/" + FCR_METADATA))) {
            final Model model = dataset.getDefaultModel();
            assertTrue(model.contains(createResource(location), schemaName, createPlainLiteral("Ashley")));
            assertTrue(model.contains(createResource(location), schemaAbout, wikiCat));
            assertTrue(model.contains(createResource(location), schemaName, createPlainLiteral("The description")));
            assertTrue(model.contains(wikiCat, schemaName, createPlainLiteral("Cat")));
        }
    }

    private void assertIdStringConstraint(final String id) throws IOException {
        assertInvalidId(id);
        assertValidId(id + "-suffix");
        assertValidId("prefix-" + id);
    }

    private void assertIdSuffixConstraint(final String suffix) throws IOException {
        assertInvalidId("prefix" + suffix);
        assertValidId(suffix);
        assertValidId(suffix + "-suffix");
    }

    private void assertInvalidId(final String id) throws IOException {
        final HttpPost postMethod = postObjMethod();
        postMethod.setHeader("Slug", id);
        try (final CloseableHttpResponse response = execute(postMethod)) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
        }
    }

    private void assertValidId(final String id) throws IOException {
        final HttpPost postMethod = postObjMethod();
        postMethod.setHeader("Slug", id);
        try (final CloseableHttpResponse response = execute(postMethod)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }
    }

}
