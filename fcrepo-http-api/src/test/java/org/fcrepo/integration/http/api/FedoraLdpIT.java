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

package org.fcrepo.integration.http.api;

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createModelForGraph;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.TimeZone.getTimeZone;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.Status.TEMPORARY_REDIRECT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;
import static nu.validator.htmlparser.common.DoctypeExpectation.NO_DOCTYPE_ERRORS;
import static nu.validator.htmlparser.common.XmlViolationPolicy.ALLOW;
import static org.apache.http.impl.client.cache.CacheConfig.DEFAULT;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.WebContent.contentTypeN3;
import static org.apache.jena.riot.WebContent.contentTypeN3Alt2;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.apache.jena.riot.WebContent.contentTypeTurtle;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING;
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_VARIANTS;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.api.FedoraJcrTypes.FEDORA_PAIRTREE;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONSTRAINED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_ORIGINAL_NAME;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_PRIMARY_IDENTIFIER;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.JCR_NT_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.MIX_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

import nu.validator.htmlparser.sax.HtmlParser;
import nu.validator.saxtree.TreeBuilder;

import org.fcrepo.http.commons.test.util.CloseableGraphStore;

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
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.vocabulary.DC_11;

/**
 * @author cabeer
 * @author ajs6f
 */
public class FedoraLdpIT extends AbstractResourceIT {

    private static final Node DC_IDENTIFIER = DC_11.identifier.asNode();

    private static final String LDP_RESOURCE_LINK_HEADER = "<" + LDP_NAMESPACE + "Resource>;rel=\"type\"";

    private static final Node rdfType = type.asNode();

    private static final Node DCTITLE = DC_TITLE.asNode();

    private static final String INDIRECT_CONTAINER_LINK_HEADER = "<" + INDIRECT_CONTAINER.getURI() + ">;rel=\"type\"";

    private static final String BASIC_CONTAINER_LINK_HEADER = "<" + BASIC_CONTAINER.getURI() + ">;rel=\"type\"";

    private static final String DIRECT_CONTAINER_LINKHEADER = "<" + DIRECT_CONTAINER.getURI() + ">;rel=\"type\"";

    private static final String TEST_ACTIVATION_PROPERTY = "RUN_TEST_CREATE_MANY";

    private static final Logger LOGGER = getLogger(FedoraLdpIT.class);

    private static SimpleDateFormat headerFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    private static SimpleDateFormat tripleFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    static {
        headerFormat.setTimeZone(getTimeZone("GMT"));
        tripleFormat.setTimeZone(getTimeZone("GMT"));
    }

    @Test
    public void testHeadRepositoryGraph() {
        final HttpHead headObjMethod = new HttpHead(serverAddress);
        assertEquals(OK.getStatusCode(), getStatus(headObjMethod));
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
            assertTrue("Didn't find LDP container link header!", getLinkHeaders(response).contains(
                    BASIC_CONTAINER_LINK_HEADER));
        }
    }

    @Test
    public void testHeadBasicContainer() throws IOException {
        final String id = getRandomUniqueId();

        createObjectAndClose(id);
        addMixin(id, BASIC_CONTAINER.getURI());

        final HttpHead headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            final Collection<String> links = getLinkHeaders(response);
            assertTrue("Didn't find LDP container link header!", links.contains(BASIC_CONTAINER_LINK_HEADER));
        }
    }

    @Test
    public void testHeadDirectContainer() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        addMixin(id, DIRECT_CONTAINER.getURI());

        final HttpHead headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            final Collection<String> links = getLinkHeaders(response);
            assertTrue("Didn't find LDP container link header!", links.contains(DIRECT_CONTAINER_LINKHEADER));
        }
    }

    @Test
    public void testHeadIndirectContainer() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        addMixin(id, INDIRECT_CONTAINER.getURI());

        final HttpHead headObjMethod = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            final Collection<String> links = getLinkHeaders(response);
            assertTrue("Didn't find LDP container link header!", links.contains(INDIRECT_CONTAINER_LINK_HEADER));
        }
    }

    @Test
    public void testHeadDatastream() throws IOException, ParseException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "123");

        final HttpHead headObjMethod = headObjMethod(id + "/x");
        try (final CloseableHttpResponse response = execute(headObjMethod)) {
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertEquals(TEXT_PLAIN, response.getFirstHeader("Content-Type").getValue());
            assertEquals("3", response.getFirstHeader("Content-Length").getValue());
            assertEquals("bytes", response.getFirstHeader("Accept-Ranges").getValue());
            final ContentDisposition disposition =
                    new ContentDisposition(response.getFirstHeader("Content-Disposition").getValue());
            assertEquals("attachment", disposition.getType());
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
            assertEquals("0", optionsResponse.getFirstHeader("Content-Length").getValue());
        }
    }

    @Test
    public void testOptionsBinaryMetadata() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", null);

        final HttpOptions optionsRequest = new HttpOptions(serverAddress + id + "/x/fcr:metadata");
        try (final CloseableHttpResponse optionsResponse = execute(optionsRequest)) {
            assertEquals(OK.getStatusCode(), optionsResponse.getStatusLine().getStatusCode());
            assertRdfOptionsHeaders(optionsResponse);
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
            assertRdfOptionsHeaders(optionsResponse);
        }
    }

    private static void assertContainerOptionsHeaders(final HttpResponse httpResponse) {
        assertRdfOptionsHeaders(httpResponse);
        final List<String> methods = headerValues(httpResponse, "Allow");
        assertTrue("Should allow POST", methods.contains(HttpPost.METHOD_NAME));

        final List<String> postTypes = headerValues(httpResponse, "Accept-Post");
        assertTrue("POST should support application/sparql-update", postTypes.contains(contentTypeSPARQLUpdate));
        assertTrue("POST should support text/turtle", postTypes.contains(contentTypeTurtle));
        assertTrue("POST should support text/rdf+n3", postTypes.contains(contentTypeN3));
        assertTrue("POST should support text/n3", postTypes.contains(contentTypeN3Alt2));
        assertTrue("POST should support application/rdf+xml", postTypes.contains(contentTypeRDFXML));
        assertTrue("POST should support application/n-triples", postTypes.contains(contentTypeNTriples));
        assertTrue("POST should support multipart/form-data", postTypes.contains("multipart/form-data"));
    }

    private static void assertRdfOptionsHeaders(final HttpResponse httpResponse) {
        final List<String> methods = headerValues(httpResponse, "Allow");
        assertTrue("Should allow PATCH", methods.contains(HttpPatch.METHOD_NAME));
        assertTrue("Should allow MOVE", methods.contains("MOVE"));
        assertTrue("Should allow COPY", methods.contains("COPY"));

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

    private static List<String> headerValues(final HttpResponse response, final String headerName) {
        return stream(response.getHeaders(headerName)).map(Header::getValue).map(s -> s.split(",")).flatMap(
                Arrays::stream).map(String::trim).collect(toList());
    }

    @Test
    public void testGetRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        try (final CloseableHttpResponse response = execute(new HttpGet(serverAddress + id))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final HttpEntity entity = response.getEntity();
            final String mediaType = MediaType.valueOf(entity.getContentType().getValue()).toString();
            assertNotNull("Entity is not an RDF serialization!", contentTypeToLang(mediaType));
        }
    }

    @Test
    public void testGetNonRDFSource() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "some content");

        try (final CloseableHttpResponse response = execute(getDSMethod(id, "x"))) {
            final HttpEntity entity = response.getEntity();
            final String content = EntityUtils.toString(entity);
            assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());
            assertEquals("some content", content);
        }
    }

    @Test
    public void testGetNonRDFSourceDescription() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "some content");
        try (final CloseableHttpResponse response = execute(getDSDescMethod(id, "x"));
                final CloseableGraphStore graph = getGraphStore(response)) {
            final Node correctDSSubject = createURI(serverAddress + id + "/x");
            assertTrue("Binary should be a ldp:NonRDFSource", graph.contains(ANY,
                    correctDSSubject, rdfType, NON_RDF_SOURCE.asNode()));
            // every triple in the response should have a subject of the actual resource described
            logger.info("Found graph:\n{}", graph);
            graph.find().forEachRemaining(quad -> {
                assertEquals("Found a triple with incorrect subject!", correctDSSubject, quad.getSubject());
            });
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
    public void testDeleteHierarchy() {
        final String id = getRandomUniqueId();
        createObjectAndClose(id + "/foo");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteObjMethod(id)));
        assertDeleted(id);
        assertDeleted(id + "/foo");
    }

    @Test
    public void testDeleteBinary() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "some content");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(deleteObjMethod(id + "/x")));
        assertDeleted(id + "/x");
    }

    @Test
    public void testDeleteObjectAndTombstone() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(serverAddress + id)));
        assertDeleted(id);
        final HttpGet httpGet = getObjMethod(id);
        final Link tombstone;
        try (final CloseableHttpResponse response = execute(httpGet)) {
            tombstone = Link.valueOf(response.getFirstHeader("Link").getValue());
        }
        assertEquals("hasTombstone", tombstone.getRel());
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(tombstone.getUri())));
        assertEquals(NOT_FOUND.getStatusCode(), getStatus(httpGet));
    }

    @Test
    public void testEmptyPatch() {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final HttpPatch patch = patchObjMethod(id);
        patch.addHeader("Content-Type", "application/sparql-update");
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patch));
    }

    @Test
    public void testUpdateObjectGraph() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final String location = serverAddress + id;
        final HttpPatch updateObjectGraphMethod = new HttpPatch(location);
        updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity("INSERT { <" + location + "> " +
                "<http://purl.org/dc/elements/1.1/identifier> \"this is an identifier\" } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));
    }

    @Test
    public void testPatchBinary() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "some content");

        final HttpPatch patch = patchObjMethod(id + "/x");
        patch.addHeader("Content-Type", "application/sparql-update");
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
        patch.addHeader("Content-Type", "application/sparql-update");
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
        patch.addHeader("Content-Type", "application/sparql-update");
        patch.setEntity(new StringEntity("INSERT { <" +
                resource + "/x>" + " <" + DC_IDENTIFIER + "> \"identifier\" } WHERE {}"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            try (final CloseableGraphStore graph = getGraphStore(new HttpGet(location))) {
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
        patch.addHeader("Content-Type", "application/sparql-update");
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
            try (final CloseableGraphStore graphStore = getGraphStore(new HttpGet(location))) {
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
    public void testPatchWithBlankNode() throws Exception {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final String location = serverAddress + id;
        final HttpPatch updateObjectGraphMethod = patchObjMethod(id);
        updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity("INSERT { <" +
                location + "> <info:some-predicate> _:a .\n " +
                "_:a <http://purl.org/dc/elements/1.1/title> \"this is a title\"\n" + " } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));

        try (final CloseableGraphStore graphStore = getGraphStore(new HttpGet(location))) {
            assertTrue(graphStore.contains(ANY, createURI(location), createURI("info:some-predicate"), ANY));
            final Node bnode = graphStore.find(ANY,
                    createURI(location), createURI("info:some-predicate"), ANY).next().getObject();
            try (final CloseableGraphStore bnodeGraphStore = getGraphStore(new HttpGet(bnode.getURI()))) {
                assertTrue(bnodeGraphStore.contains(ANY, bnode, DCTITLE, createLiteral("this is a title")));
            }
        }
    }

    @Test
    public void testReplaceGraph() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final String subjectURI = serverAddress + id;
        final String initialContent;
        try (final CloseableHttpResponse subjectResponse = execute(getObjMethod(id))) {
            initialContent = EntityUtils.toString(subjectResponse.getEntity());
        }
        final HttpPut replaceMethod = putObjMethod(id);
        replaceMethod.addHeader("Content-Type", "application/n3");
        replaceMethod
                .setEntity(new StringEntity(initialContent + "\n<" + subjectURI + "> <info:test#label> \"foo\""));
        try (final CloseableHttpResponse response = execute(replaceMethod)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
            assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));
        }
        try (final CloseableGraphStore graph = getGraphStore(getObjMethod(id))) {
            assertTrue("Didn't find a triple we tried to create!", graph.contains(ANY,
                    createURI(subjectURI), createURI("info:test#label"), createLiteral("foo")));
        }
    }

    @Test
    public void testCreateGraph() throws IOException {
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut createMethod = new HttpPut(subjectURI);
        createMethod.addHeader("Content-Type", "application/n3");
        createMethod.setEntity(new StringEntity("<" + subjectURI + "> <info:test#label> \"foo\""));
        assertEquals(CREATED.getStatusCode(), getStatus(createMethod));

        try (final CloseableGraphStore graph = getGraphStore(new HttpGet(subjectURI))) {
            assertTrue("Didn't find a triple we tried to create!", graph.contains(ANY,
                    createURI(subjectURI), createURI("info:test#label"), createLiteral("foo")));
        }
    }

    @Test
    public void testCreateGraphWithBlanknodes() throws IOException {
        final String subjectURI = serverAddress + getRandomUniqueId();
        final HttpPut createMethod = new HttpPut(subjectURI);
        createMethod.addHeader("Content-Type", "application/n3");
        createMethod.setEntity(new StringEntity("<" + subjectURI + "> <info:some-predicate> _:a ." +
                "_:a <info:test#label> \"asdfg\""));
        assertEquals(CREATED.getStatusCode(), getStatus(createMethod));

        final HttpGet getObjMethod = new HttpGet(subjectURI);
        getObjMethod.addHeader("Accept", "application/rdf+xml");
        try (final CloseableGraphStore graph = getGraphStore(getObjMethod)) {
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
        getObjMethod.addHeader("Accept", "text/turtle");

        final Model model = createDefaultModel();
        try (final CloseableHttpResponse getResponse = execute(getObjMethod)) {
            model.read(getResponse.getEntity().getContent(), subjectURI, "TURTLE");
        }
        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "application/n-triples");
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
            final Link link = Link.valueOf(response.getFirstHeader("Link").getValue());
            assertEquals("describedby", link.getRel());
        }
    }

    @Test
    public void testPutDatastreamContentOnObject() throws IOException {
        final String content = "foo";
        final String id = getRandomUniqueId();
        createObjectAndClose(id);

        final HttpPut put = putObjMethod(id);
        put.setEntity(new StringEntity(content));
        put.setHeader("Content-Type", "application/octet-stream");
        assertEquals(
                "Expected UNSUPPORTED MEDIA TYPE response when PUTing content to an object (as opposed to datastream)",
                UNSUPPORTED_MEDIA_TYPE.getStatusCode(), getStatus(put));
    }

    @Test
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
        put.setHeader("Content-Type", "text/plain");
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
        method.addHeader("Content-Type", "application/sparql-update");
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
            try (final CloseableGraphStore graphStore = getGraphStore(new HttpGet(location))) {
                assertTrue(graphStore.contains(ANY, createURI(location), DCTITLE, createLiteral("title")));
            }
        }
    }

    @Test
    public void testIngestWithSparqlQueryBadNS() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader("Content-Type", "application/sparql-update");
        method.setEntity(new StringEntity("PREFIX fcr: <http://xmlns.com/my-fcr/> "
                + "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"this is a title\" } WHERE {}"));
        assertNotEquals("Should not get a CREATED response with bad namspace prefix!",
                CREATED.getStatusCode(), getStatus(method));
    }

    @Test
    public void testIngestWithNewAndGraph() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader("Content-Type", "application/n3");
        method.setEntity(new StringEntity("<> <http://purl.org/dc/elements/1.1/title> \"title\"."));

        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
            final String lastmod = response.getFirstHeader("Last-Modified").getValue();
            assertNotNull("Should set Last-Modified for new nodes", lastmod);
            assertNotEquals("Last-Modified should not be blank for new nodes", lastmod.trim(), "");
            final String location = getLocation(response);
            try (final CloseableGraphStore graphStore = getGraphStore(new HttpGet(location))) {
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
        method.addHeader("Content-Type", "application/octet-stream");
        method.setEntity(new StringEntity("xyz"));

        try (final CloseableHttpResponse response = execute(method)) {
            final String content = EntityUtils.toString(response.getEntity());
            final int status = getStatus(response);
            assertEquals("Didn't get a CREATED response! Got content:\n" + content, CREATED.getStatusCode(), status);
            assertIdentifierness(content);
            final String location = getLocation(response);
            assertNotEquals(serverAddress + "/objects", location);
            assertEquals("Object wasn't created!", OK.getStatusCode(), getStatus(new HttpGet(location)));
            final Link link = Link.valueOf(response.getFirstHeader("Link").getValue());

            assertEquals("describedby", link.getRel());
            assertTrue("Expected an anchor to the newly created resource", link.getParams().containsKey("anchor"));
            assertEquals("Expected anchor at the newly created resource", location, link.getParams().get("anchor"));
            assertEquals("Expected describedBy link", location + "/" + FCR_METADATA, link.getUri().toString());
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
    public void testIngestOnPairtree() throws IOException {
        //  Following the approach undertaken for FedoraExportIT#shouldRoundTripOnePairtree
        final String objName = getLocation(postObjMethod());
        final String pairtreeName = objName.substring(serverAddress.length(), objName.lastIndexOf('/'));

        try (final CloseableGraphStore graphStore = getGraphStore(getObjMethod(pairtreeName))) {
        assertTrue("Resource \"" + objName + " " + pairtreeName + "\" must be pairtree.", graphStore.contains(ANY,
                createURI(serverAddress + pairtreeName),  createURI(REPOSITORY_NAMESPACE + "mixinTypes"),
                createLiteral(FEDORA_PAIRTREE)));
        }
        // Attempting to POST to the child of the pairtree node...
        final int status = getStatus(postObjMethod(pairtreeName));
        assertEquals("Created an Object under a pairtree node!", FORBIDDEN.getStatusCode(), status);
    }

    @Test
    public void testIngestWithRDFLang() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader("Content-Type", "application/n3");
        method.setEntity(new StringEntity("<> <http://purl.org/dc/elements/1.1/title> \"french title\"@fr ."
                + "<> <http://purl.org/dc/elements/1.1/title> \"english title\"@en ."));

        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            final String location = getLocation(response);
            try (final CloseableGraphStore graphStore = getGraphStore(new HttpGet(location))) {
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
            logger.info("Not running testCreateManyObjects because system property TEST_ACTIVATION_PROPERTY not set.");
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
            assertEquals(TEXT_PLAIN, response.getFirstHeader("Content-Type").getValue());
            assertEquals("3", response.getFirstHeader("Content-Length").getValue());
            assertEquals("bytes", response.getFirstHeader("Accept-Ranges").getValue());
            final ContentDisposition disposition =
                    new ContentDisposition(response.getFirstHeader("Content-Disposition").getValue());
            assertEquals("attachment", disposition.getType());

            final Collection<String> links = getLinkHeaders(response);
            final String describedByHeader =
                    "<" + serverAddress + id + "/ds1/" + FCR_METADATA + ">; rel=\"describedby\"";
            assertTrue("Didn't find 'describedby' link header!", links.contains(describedByHeader));
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
    public void testGetRepositoryGraph() throws IOException {
        try (final CloseableGraphStore graph = getGraphStore(getObjMethod(""))) {
            logger.trace("Retrieved repository graph:\n" + graph);
            assertTrue("Should find the root type", graph.contains(ANY,
                    ANY, HAS_PRIMARY_TYPE.asNode(), createLiteral(ROOT)));
        }
    }

    @Test
    public void testGetObjectGraphHtml() throws IOException {
        final HttpGet getObjMethod = new HttpGet(getLocation(postObjMethod()));
        getObjMethod.addHeader("Accept", "text/html");
        assertEquals(OK.getStatusCode(), getStatus(getObjMethod));
    }

    @Test
    public void testGetObjectGraphVariants() throws IOException {
        final String location = getLocation(postObjMethod());
        for (final Variant variant : POSSIBLE_RDF_VARIANTS) {
            final HttpGet getObjMethod = new HttpGet(location);
            final String type = variant.getMediaType().getType();
            getObjMethod.addHeader("Accept", type);
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
            assertTrue("Didn't find LDP link header!", getLinkHeaders(response).contains(LDP_RESOURCE_LINK_HEADER));
            try (final CloseableGraphStore results = getGraphStore(response)) {
                assertTrue("Didn't find any type triples!", results.contains(ANY,
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
        addMixin(id, MIX_NAMESPACE + "versionable");

        try (final CloseableGraphStore graph = getGraphStore(getObjMethod(id))) {

            final Node resource = createURI(serverAddress + id);
            verifyResource(graph, resource, REPOSITORY_NAMESPACE, "Container");
            verifyResource(graph, resource, REPOSITORY_NAMESPACE, "Resource");
            verifyResource(graph, resource, MIX_NAMESPACE, "created");
            verifyResource(graph, resource, MIX_NAMESPACE, "lastModified");
            verifyResource(graph, resource, MIX_NAMESPACE, "referenceable");
            verifyResource(graph, resource, MIX_NAMESPACE, "simpleVersionable");
            verifyResource(graph, resource, MIX_NAMESPACE, "versionable");
            verifyResource(graph, resource, JCR_NT_NAMESPACE, "base");
            verifyResource(graph, resource, JCR_NT_NAMESPACE, "folder");
            verifyResource(graph, resource, JCR_NT_NAMESPACE, "hierarchyNode");
        }
        logger.trace("Leaving verifyFullSetOfRdfTypes()...");
    }

    private static void verifyResource(final GraphStore g, final Node subject, final String ns, final String type) {
        assertTrue("Should find type: " + ns + type, g.contains(ANY, subject, rdfType, createURI(ns + type)));
    }

    @Test
    public void testGetObjectGraphWithChild() throws IOException {
        final String id = getRandomUniqueId();
        final String location = getLocation(createObject(id));
        createObjectAndClose(id + "/c");

        try (final CloseableHttpResponse response = execute(getObjMethod(id))) {
            try (final CloseableGraphStore graph = getGraphStore(response)) {
                assertTrue("Didn't find child node!", graph.contains(ANY,
                        createURI(location), createURI(LDP_NAMESPACE + "contains"), createURI(location + "/c")));
                final Collection<String> links = getLinkHeaders(response);
                assertTrue("Didn't find LDP resource link header!", links.contains(LDP_RESOURCE_LINK_HEADER));
            }
        }
    }

    @Test
    public void testGetObjectGraphMinimal() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        addMixin(id, BASIC_CONTAINER.getURI());
        createObjectAndClose(id + "/a");
        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader("Prefer", "return=minimal");
        try (final CloseableGraphStore graph = getGraphStore(getObjMethod)) {
            final Node resource = createURI(serverAddress + id);
            assertFalse("Didn't expect members", graph.find(ANY, resource, HAS_CHILD.asNode(), ANY).hasNext());
            assertFalse("Didn't expect members", graph.find(ANY, resource, CONTAINS.asNode(), ANY).hasNext());
        }
    }

    @Test
    public void testGetObjectOmitMembership() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        addMixin(id, BASIC_CONTAINER.getURI());
        createObjectAndClose(id + "/a");
        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod.addHeader("Prefer", "return=representation; "
                + "omit=\"http://www.w3.org/ns/ldp#PreferContainment http://www.w3.org/ns/ldp#PreferMembership\"");
        try (final CloseableGraphStore graph = getGraphStore(getObjMethod)) {
            assertFalse("Didn't expect inlined member resources", graph.find(ANY,
                    createURI(serverAddress + id), HAS_CHILD.asNode(), ANY).hasNext());
        }
    }

    @Test
    public void testGetObjectOmitContainment() throws IOException {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final HttpPatch patch = patchObjMethod(id);
        patch.setHeader("Content-Type", "application/sparql-update");
        final String updateString =
                "INSERT DATA { <> a <" + DIRECT_CONTAINER.getURI() + "> ; <" + MEMBERSHIP_RESOURCE.getURI() +
                        "> <> ; <" + HAS_MEMBER_RELATION + "> <" + LDP_NAMESPACE + "member> .}";
        patch.setEntity(new StringEntity(updateString));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(patch));

        createObjectAndClose(id + "/a");
        final HttpGet getObjMethod = getObjMethod(id);
        getObjMethod
                .addHeader("Prefer", "return=representation; omit=\"http://www.w3.org/ns/ldp#PreferContainment\"");
        try (final CloseableGraphStore graph = getGraphStore(getObjMethod)) {
            final Node resource = createURI(serverAddress + id);
            assertTrue("Didn't find member resources", graph.find(ANY, resource, LDP_MEMBER.asNode(), ANY).hasNext());
            assertFalse("Expected nothing contained", graph.find(ANY, resource, CONTAINS.asNode(), ANY).hasNext());
        }
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
        updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity("INSERT { <" +
                resourcea + "> <http://purl.org/dc/terms/isPartOf> <" + resourceb + "> . \n <" +
                resourcea + "> <info:xyz#some-other-property> <" + resourceb + "> } WHERE {}"));
        executeAndClose(updateObjectGraphMethod);

        final HttpGet getObjMethod = new HttpGet(resourceb);

        getObjMethod.addHeader("Prefer", "return=representation; include=\"" + INBOUND_REFERENCES + "\"");
        try (final CloseableGraphStore graph = getGraphStore(getObjMethod)) {
            assertTrue(graph.contains(ANY,
                    createURI(resourcea), createURI("http://purl.org/dc/terms/isPartOf"), createURI(resourceb)));

            assertTrue(graph.contains(ANY,
                    createURI(resourcea), createURI("info:xyz#some-other-property"), createURI(resourceb)));
        }
    }

    @Test
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
        createContainer.addHeader("Content-Type", "text/turtle");
        final String membersRDF = "<> a <http://www.w3.org/ns/ldp#IndirectContainer>; "
            + "<http://www.w3.org/ns/ldp#hasMemberRelation> <" + memberRelation + ">; "
            + "<http://www.w3.org/ns/ldp#insertedContentRelation> <http://www.openarchives.org/ore/terms/proxyFor>; "
            + "<http://www.w3.org/ns/ldp#membershipResource> <" + serverAddress + pid1 + "> . ";
        createContainer.setEntity(new StringEntity(membersRDF));
        assertEquals(CREATED.getStatusCode(), getStatus(createContainer));

        // create proxies for the children in the indirect container
        createProxy(pid1, pid2);
        createProxy(pid1, pid3);

        // retrieve the parent and verify the outbound triples exist
        final HttpGet getParent =  new HttpGet(serverAddress + pid1);
        getParent.addHeader("Accept", "application/n-triples");
        try (final CloseableGraphStore parentGraph = getGraphStore(getParent)) {
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
        getContainer.addHeader("Accept", "application/n-triples");
        try (final CloseableGraphStore containerGraph = getGraphStore(getContainer)) {
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
        getMember.addHeader("Accept", "application/n-triples");
        try (final CloseableGraphStore memberGraph = getGraphStore(getMember)) {
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
    private void createProxy(final String parent, final String child) throws Exception {
        final HttpPost createProxy = new HttpPost(serverAddress + parent + "/members");
        createProxy.addHeader("Content-Type", "text/turtle");
        final String proxyRDF = "<> <http://www.openarchives.org/ore/terms/proxyFor> <" + serverAddress + child + ">;"
            + " <http://www.openarchives.org/ore/terms/proxyIn> <" + serverAddress + parent + "> .";
        createProxy.setEntity(new StringEntity(proxyRDF));
        assertEquals(CREATED.getStatusCode(), getStatus(createProxy));
    }

    @Test
    public void testGetObjectGraphLacksUUID() throws Exception {
        final String location = getLocation(postObjMethod());
        final HttpGet getObjMethod = new HttpGet(location);
        try (final CloseableGraphStore graphStore = getGraphStore(getObjMethod)) {
            final Iterator<Quad> iterator =
                    graphStore.find(ANY, createURI(location), HAS_PRIMARY_IDENTIFIER.asNode(), ANY);
            assertFalse("Graph should not contain a UUID!", iterator.hasNext());
        }
    }

    @Test
    public void testLinkToNonExistent() throws IOException {
        final HttpPatch patch = new HttpPatch(getLocation(postObjMethod()));
        patch.addHeader("Content-Type", "application/sparql-update");
        patch.setEntity(new StringEntity("INSERT { " +
                "<> <http://some-vocabulary#isMemberOfCollection> <" + serverAddress + "non-existant> } WHERE {}"));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patch));
    }

    @Test
    public void testUpdateAndReplaceObjectGraph() throws IOException {
        final String subjectURI = getLocation(postObjMethod());
        final HttpPatch updateObjectGraphMethod = new HttpPatch(subjectURI);
        updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity("INSERT {<" + subjectURI + "> <info:test#label> \"foo\"}" +
                " WHERE {}"));
        executeAndClose(updateObjectGraphMethod);
        updateObjectGraphMethod.setEntity(new StringEntity("DELETE { <" + subjectURI + "> <info:test#label> ?p}\n" +
                "INSERT {<" + subjectURI + "> <info:test#label> \"qwerty\"} \n" +
                "WHERE { <" + subjectURI + "> <info:test#label> ?p}"));

        try (final CloseableHttpResponse response = execute(updateObjectGraphMethod)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
            assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));
        }
        try (CloseableGraphStore graph = getGraphStore(new HttpGet(subjectURI))) {
            assertFalse("Found a triple we thought we deleted.", graph.contains(ANY,
                    createURI(subjectURI), createURI("<info:test#label>"), createLiteral("foo")));
        }
    }

    @Test
    // TODO there is no actual use of the JCR namespace in this test-- what is it testing?
            public
            void testUpdateWithSparqlQueryJcrNS() throws IOException {
        final String subjectURI = getLocation(postObjMethod());
        final HttpPatch updateObjectGraphMethod = new HttpPatch(subjectURI);
        updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity("PREFIX fcr: <http://xmlns.com/my-fcr/> "
                + "INSERT { <" + subjectURI + "> <info:test#label> \"asdfg\" } WHERE {}"));
        assertNotEquals("Got updated response with jcr namspace prefix!\n",
                NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));
    }

    @Test
    public void testUpdateObjectGraphWithProblems() throws IOException {
        final String subjectURI = getLocation(postObjMethod());
        final Link ex = fromUri(URI.create(serverAddress + "static/constraints/ServerManagedPropertyException.rdf"))
                .rel(CONSTRAINED_BY.getURI()).build();

        final HttpPatch patchObjMethod = new HttpPatch(subjectURI);
        patchObjMethod.addHeader("Content-Type", "application/sparql-update");
        patchObjMethod.setEntity(new StringEntity("INSERT { <" +
                subjectURI + "> <" + REPOSITORY_NAMESPACE + "uuid> \"value-doesn't-matter\" } WHERE {}\n"));
        try (final CloseableHttpResponse response = execute(patchObjMethod)) {
            assertEquals(CONFLICT.getStatusCode(), getStatus(response));
            assertEquals(ex.toString(), response.getFirstHeader("Link").getValue().toString());
        }
    }

    @Test
    public void testPutResourceBadRdf() throws IOException {
        final HttpPut httpPut = new HttpPut(serverAddress + getRandomUniqueId());
        httpPut.setHeader("Content-Type", "text/turtle");
        httpPut.setEntity(new StringEntity("<> a \"still image\"."));
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(httpPut));
    }

    @Test
    public void testRepeatedPut() {
        final String id = getRandomUniqueId();
        assertEquals(CREATED.getStatusCode(), getStatus(new HttpPut(serverAddress + id)));

        final HttpPut secondPut = new HttpPut(serverAddress + id);
        secondPut.setHeader("Content-Type", "text/turtle");
        assertEquals(CONFLICT.getStatusCode(), getStatus(secondPut));
    }

    @Test
    public void testCreateResourceWithoutContentType() {
        assertEquals(CREATED.getStatusCode(), getStatus(new HttpPut(serverAddress + getRandomUniqueId())));
    }

    @Test
    public void testUpdateObjectWithoutContentType() throws IOException {
        final HttpPut httpPut = new HttpPut(getLocation(postObjMethod()));
        // use a bytestream-based entity to avoid settin a content type
        httpPut.setEntity(new ByteArrayEntity("bogus content".getBytes()));
        assertEquals(UNSUPPORTED_MEDIA_TYPE.getStatusCode(), getStatus(httpPut));
    }

    @Test
    public void testUpdateBinaryWithoutContentType() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "xyz");
        final HttpPut httpPut = new HttpPut(serverAddress + id + "/x");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(httpPut));
    }

    @Test
    public void testRoundTripReplaceGraphForDatastreamDescription() throws IOException {
        final String id = getRandomUniqueId();
        final String subjectURI = serverAddress + id + "/ds1";
        createDatastream(id, "ds1", "some-content");

        final HttpGet getObjMethod = new HttpGet(subjectURI + "/" + FCR_METADATA);
        getObjMethod.addHeader("Accept", "text/turtle");
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
            replaceMethod.setEntity(new StringEntity(w.toString()));
            logger.trace("Transmitting object graph for testRoundTripReplaceGraphForDatastream():\n {}", w);
        }
        replaceMethod.addHeader("Content-Type", "application/n-triples");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(replaceMethod));
    }

    @Test
    public void testResponseContentTypes() throws Exception {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        for (final String type : POSSIBLE_RDF_RESPONSE_VARIANTS_STRING) {
            final HttpGet method = new HttpGet(serverAddress + id);
            method.addHeader("Accept", type);
            assertEquals(type, getContentType(method));
        }
    }

    /**
     * Given a directory at: test-FileSystem1/ /ds1 /ds2 /TestSubdir/ and a projection of test-objects as
     * fedora:/files, then I should be able to retrieve an object from fedora:/files/FileSystem1 that lists a child
     * object at fedora:/files/FileSystem1/TestSubdir and lists datastreams ds and ds2
     *
     * @throws IOException thrown during this function
     */
    @Test
    public void testGetProjectedNode() throws IOException {
        final HttpGet method = new HttpGet(serverAddress + "files/FileSystem1");
        try (final CloseableGraphStore result = getGraphStore(method)) {
            final Node subjectURI = createURI(serverAddress + "files/FileSystem1");
            assertTrue("Didn't find the first datastream! ", result.contains(ANY,
                    subjectURI, ANY, createURI(subjectURI + "/ds1")));
            assertTrue("Didn't find the second datastream! ", result.contains(ANY,
                    subjectURI, ANY, createURI(subjectURI + "/ds2")));
            assertTrue("Didn't find the first object! ", result.contains(ANY,
                    subjectURI, ANY, createURI(subjectURI + "/TestSubdir")));
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
        getMethod.addHeader("Accept", "text/html");
        try (final CloseableHttpResponse response = execute(getMethod)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final String content = EntityUtils.toString(response.getEntity());
            logger.trace("Retrieved HTML view:\n" + content);
            final HtmlParser htmlParser = new HtmlParser(ALLOW);
            htmlParser.setDoctypeExpectation(NO_DOCTYPE_ERRORS);
            htmlParser.setErrorHandler(new HTMLErrorHandler());
            htmlParser.setContentHandler(new TreeBuilder());
            try (final InputStream htmlStream = new ByteArrayInputStream(content.getBytes())) {
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

    /**
     * I should be able to create two subdirectories of a non-existent parent directory.
     *
     * @throws IOException thrown during this function
     **/
    @Ignore("Enabled once the FedoraFileSystemConnector becomes readable/writable")
    // TODO
            public
            void testBreakFederation() throws IOException {
        final String id = getRandomUniqueId();
        testGetRepositoryGraph();
        createObjectAndClose("files/a0/" + id + "b0");
        createObjectAndClose("files/a0/" + id + "b1");
        testGetRepositoryGraph();
    }

    /**
     * I should be able to upload a file to a read/write federated filesystem.
     *
     * @throws IOException thrown during this function
     **/
    @Ignore("Enabled once the FedoraFileSystemConnector becomes readable/writable")
    // TODO
            public
            void testUploadToProjection() throws IOException {
        // upload file to federated filesystem using rest api
        final String id = getRandomUniqueId();
        final String uploadLocation = serverAddress + "files/" + id + "/ds1";
        final String uploadContent = "abc123";
        logger.debug("Uploading to federated filesystem via rest api: " + uploadLocation);
        // final HttpResponse response = createDatastream("files/" + pid, "ds1", uploadContent);
        // final String actualLocation = response.getFirstHeader("Location").getValue();
        // assertEquals("Wrong URI in Location header", uploadLocation, actualLocation);

        // validate content
        try (final CloseableHttpResponse getResponse = execute(new HttpGet(uploadLocation))) {
            final String actualContent = EntityUtils.toString(getResponse.getEntity());
            assertEquals(OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
            assertEquals("Content doesn't match", actualContent, uploadContent);
        }
        // validate object profile
        try (final CloseableHttpResponse objResponse = execute(new HttpGet(serverAddress + "files/" + id))) {
            assertEquals(OK.getStatusCode(), objResponse.getStatusLine().getStatusCode());
        }
    }

    /**
     * I should be able to link to content on a federated filesystem.
     *
     * @throws IOException in case of IOException
     **/
    @Test
    public void testFederatedDatastream() throws IOException {
        final String federationAddress = serverAddress + "files/FileSystem1/ds1";
        final String linkingAddress = getLocation(postObjMethod());

        // link from the object to the content of the file on the federated filesystem
        final HttpPatch patch = new HttpPatch(linkingAddress);
        patch.addHeader("Content-Type", "application/sparql-update");
        patch.setEntity(new ByteArrayEntity(("INSERT DATA { <> <http://some-vocabulary#hasExternalContent> "
                + "<" + federationAddress + "> . }").getBytes()));
        assertEquals("Couldn't link to external datastream!", NO_CONTENT.getStatusCode(), getStatus(patch));
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
        patch.addHeader("Content-Type", "application/sparql-update");
        patch.setEntity(new ByteArrayEntity(sparql.getBytes()));
        assertEquals("Couldn't link resources!", NO_CONTENT.getStatusCode(), getStatus(patch));
        assertEquals("Error deleting linked-to!", NO_CONTENT.getStatusCode(), getStatus(deleteObjMethod(linkedTo)));
        assertEquals("Linked to should still exist!", OK.getStatusCode(), getStatus(getObjMethod(linkedFrom)));
    }

    /**
     * When I make changes to a resource in a federated filesystem, the parent folder's Last-Modified header should be
     * updated.
     *
     * @throws IOException in case of IOException
     **/
    @Test
    public void testLastModifiedUpdatedAfterUpdates() throws IOException  {

        // create directory containing a file in filesystem
        final File fed = new File("target/test-classes/test-objects");
        final String id = getRandomUniqueId();
        final File dir = new File(fed, id);
        final File child = new File(dir, "child");
        final long timestamp1 = currentTimeMillis();
        dir.mkdir();
        child.mkdir();
        // TODO this seems really brittle
        try {
            sleep(2000);
        } catch (final InterruptedException e) {
        }

        // check Last-Modified header is current
        final long lastmod1;
        try (final CloseableHttpResponse resp1 = execute(headObjMethod("files/" + id))) {
            assertEquals(OK.getStatusCode(), getStatus(resp1));
            lastmod1 = headerFormat.parse(resp1.getFirstHeader("Last-Modified").getValue()).getTime();
            assertTrue((timestamp1 - lastmod1) < 1000); // because rounding

            // remove the file and wait for the TTL to expire
            final long timestamp2 = currentTimeMillis();
            child.delete();
            try {
                sleep(2000);
            } catch (final InterruptedException e) {
            }

            // check Last-Modified header is updated
            try (final CloseableHttpResponse resp2 = execute(headObjMethod("files/" + id))) {
                assertEquals(OK.getStatusCode(), getStatus(resp2));
                final long lastmod2 = headerFormat.parse(resp2.getFirstHeader("Last-Modified").getValue()).getTime();
                assertTrue((timestamp2 - lastmod2) < 1000); // because rounding
                assertFalse("Last-Modified headers should have changed", lastmod1 == lastmod2);
            } catch (final ParseException e) {
                fail();
            }
        } catch (final ParseException e) {
            fail();
        }
    }

    @Test
    public void testUpdateObjectWithSpaces() throws IOException {
        final String id = getRandomUniqueId() + " 2";
        try (final CloseableHttpResponse createResponse = createObject(id)) {
            final String subjectURI = getLocation(createResponse);
            final HttpPatch updateObjectGraphMethod = new HttpPatch(subjectURI);
            updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");
            updateObjectGraphMethod.setEntity(new StringEntity(
                    "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"test\" } WHERE {}"));
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(updateObjectGraphMethod));
        }
    }

    @Test
    public void testCreatedAndModifiedDates() throws IOException, ParseException {
        final String location = getLocation(postObjMethod());
        final HttpGet getObjMethod = new HttpGet(location);
        try (final CloseableHttpResponse response = execute(getObjMethod)) {
            try (final CloseableGraphStore results = getGraphStore(response)) {
                final Model model = createModelForGraph(results.getDefaultGraph());
                final Resource nodeUri = createResource(location);
                final String lastmodString = response.getFirstHeader("Last-Modified").getValue();
                headerFormat.parse(lastmodString);
                final Optional<Date> createdDateTriples =
                        getDateFromModel(model, nodeUri, createProperty(REPOSITORY_NAMESPACE + "created"));
                final Optional<Date> lastmodDateTriples =
                        getDateFromModel(model, nodeUri, createProperty(REPOSITORY_NAMESPACE + "lastModified"));
                assertTrue(createdDateTriples.isPresent());
                assertTrue(lastmodDateTriples.isPresent());
                assertEquals(lastmodString, headerFormat.format(createdDateTriples.get()));
                assertEquals(lastmodString, headerFormat.format(lastmodDateTriples.get()));
            }
        }
    }

    @Test
    public void testLdpContainerInteraction() throws IOException {

        final String id = getRandomUniqueId();
        final String location;
        try (final CloseableHttpResponse createResponse = createObject(id)) {
            location = getLocation(createResponse);
        }
        createObjectAndClose(id + "/t");
        addMixin(id + "/t", DIRECT_CONTAINER.getURI());
        final HttpPatch patch = patchObjMethod(id + "/t");
        final String sparql = "INSERT DATA { "
                + "<> <" + MEMBERSHIP_RESOURCE + "> <" + location + "> .\n"
                + "<> <" + HAS_MEMBER_RELATION + "> <info:some/relation> .\n"
                + " }";
        patch.setEntity(new StringEntity(sparql));
        patch.addHeader("Content-Type", "application/sparql-update");
        assertEquals("Expected patch to succeed", NO_CONTENT.getStatusCode(), getStatus(patch));

        createObjectAndClose(id + "/b");
        addMixin(id + "/b", DIRECT_CONTAINER.getURI());
        final HttpPatch bPatch = patchObjMethod(id + "/b");
        bPatch.addHeader("Content-Type", "application/sparql-update");
        final String bSparql = "INSERT DATA { "
                + "<> <" + MEMBERSHIP_RESOURCE + "> <" + location + "> .\n"
                + "<> <" + HAS_MEMBER_RELATION + "> <info:some/another-relation> .\n"
                + " }";
        bPatch.setEntity(new StringEntity(bSparql));
        assertEquals("Expected patch to succeed", NO_CONTENT.getStatusCode(), getStatus(bPatch));

        createObject(id + "/t/1");
        createObject(id + "/b/1");
        try (final CloseableGraphStore graphStore = getGraphStore(new HttpGet(location))) {
            final Node resource = createURI(location);
            assertTrue("Expected to have container t", graphStore.contains(ANY,
                    resource, createURI(LDP_NAMESPACE + "contains"), createURI(location + "/t")));
            assertTrue("Expected to have container b", graphStore.contains(ANY,
                    resource, createURI(LDP_NAMESPACE + "contains"), createURI(location + "/b")));
            assertTrue("Expected member relation", graphStore.contains(ANY,
                    resource, createURI("info:some/relation"), createURI(location + "/t/1")));
            assertTrue("Expected other member relation", graphStore.contains(ANY,
                    resource, createURI("info:some/another-relation"), createURI(location + "/b/1")));
        }
    }

    @Test
    public void testLdpIndirectContainerInteraction() throws IOException {

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
        try (final CloseableHttpResponse createResponse = createObject(indirectContainerId)) {
            indirectContainer = getLocation(createResponse);
        }
        addMixin(indirectContainerId, INDIRECT_CONTAINER.getURI());

        // Add LDP properties to indirect container
        final HttpPatch patch = patchObjMethod(indirectContainerId);
        patch.addHeader("Content-Type", "application/sparql-update");
        final String sparql = "INSERT DATA { "
                + "<> <" + MEMBERSHIP_RESOURCE + "> <" + container + "> .\n"
                + "<> <" + HAS_MEMBER_RELATION + "> <info:some/relation> .\n"
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
        // Ensure container has been updated with relationship... indirectly
        try (final CloseableHttpResponse getResponse = execute(new HttpGet(container));
                final CloseableGraphStore graphStore = getGraphStore(getResponse)) {
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
                final CloseableGraphStore graphStore1 = getGraphStore(getResponse1);) {
            assertFalse("Expected NOT to have resource: " + graphStore1, graphStore1.contains(ANY,
                    createURI(container), createURI("info:some/relation"), createURI(resource)));
        }
    }

    @Test
    public void testWithHashUris() throws IOException {
        final HttpPost method = postObjMethod();
        method.addHeader("Content-Type", "text/turtle");
        method.setEntity(new StringEntity("<> <info:some-predicate> <#abc> .\n"
                + "<#abc> <info:test#label> \"asdfg\" ."));
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            final String location = getLocation(response);
            try (final CloseableGraphStore graphStore = getGraphStore(new HttpGet(location))) {
                assertTrue(graphStore.contains(ANY,
                        createURI(location), createURI("info:some-predicate"), createURI(location + "#abc")));
                assertTrue(graphStore.contains(ANY,
                        createURI(location + "#abc"), createURI("info:test#label"), createLiteral("asdfg")));
            }
        }
    }

    @Test
    public void testCreateAndReplaceGraphMinimal() throws IOException {
        LOGGER.trace("Entering testCreateAndReplaceGraphMinimal()...");
        final HttpPost httpPost = postObjMethod("/");
        httpPost.addHeader("Slug", getRandomUniqueId());
        httpPost.addHeader("Content-Type", "text/turtle");
        httpPost.setEntity(new StringEntity("<> <" + DCTITLE.getURI() + "> \"abc\""));
        final String subjectURI;
        try (final CloseableHttpResponse response = execute(httpPost)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }
        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "text/turtle");
        replaceMethod.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
        replaceMethod.setEntity(new StringEntity("<> <" + DCTITLE.getURI() + "> \"xyz\""));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(replaceMethod));

        final HttpGet get = new HttpGet(subjectURI);
        get.addHeader("Prefer", "return=minimal");
        try (final CloseableGraphStore graphStore = getGraphStore(get)) {
            assertTrue(graphStore.contains(ANY, ANY, DCTITLE, createLiteral("xyz")));
        }
        LOGGER.trace("Done with testCreateAndReplaceGraphMinimal().");
    }

    @Test
    @Ignore("This test needs manual intervention to decide how \"good\" the graph looks")
    // TODO Do we have any way to proceed with this kind of aesthetic goal?
            public
            void testGraphShouldNotBeTooLumpy() throws IOException {

        final HttpPut httpPut = putObjMethod(getRandomUniqueId());
        httpPut.addHeader("Content-Type", "text/turtle");
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
    public void testEmbeddedChildResources() throws IOException {
        final String id = getRandomUniqueId();
        final String binaryId = "binary0";

        assertEquals(CREATED.getStatusCode(), getStatus(putObjMethod(id)));
        assertEquals(CREATED.getStatusCode(), getStatus(putDSMethod(id, binaryId, "some test content")));

        final HttpPatch httpPatch = patchObjMethod(id + "/" + binaryId + "/fcr:metadata");
        httpPatch.addHeader("Content-Type", "application/sparql-update");
        httpPatch.setEntity(new StringEntity(
                "INSERT { <> <http://purl.org/dc/elements/1.1/title> 'this is a title' } WHERE {}"));
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(httpPatch));

        final HttpGet httpGet = getObjMethod(id);
        httpGet.setHeader("Prefer",
                "return=representation; include=\"http://fedora.info/definitions/v4/repository#EmbedResources\"");
        try (final CloseableGraphStore graphStore = getGraphStore(httpGet)) {
            assertTrue("Property on child binary should be found!" + graphStore, graphStore.contains(ANY,
                    createURI(serverAddress + id + "/" + binaryId),
                    createURI("http://purl.org/dc/elements/1.1/title"), createLiteral("this is a title")));
        }
    }

    @Test
    public void testExternalMessageBody() throws IOException {

        // we need a client that won't automatically follow redirects
        try (final CloseableHttpClient noFollowClient = HttpClientBuilder.create().disableRedirectHandling().build()) {

            final String id = getRandomUniqueId();
            final HttpPut httpPut = putObjMethod(id);
            httpPut.addHeader("Content-Type", "message/external-body; access-type=URL; " +
                    "URL=\"http://www.example.com/file\"");

            try (final CloseableHttpResponse response = execute(httpPut)) {
                assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
                final HttpGet get = new HttpGet(getLocation(response));
                try (final CloseableHttpResponse getResponse = noFollowClient.execute(get)) {
                    assertEquals(TEMPORARY_REDIRECT.getStatusCode(), getStatus(getResponse));
                    assertEquals("http://www.example.com/file", getLocation(getResponse));
                }
            }
        }
    }

    @Test
    public void testJsonLdProfile() throws IOException {
        // Create a resource
        final HttpPost method = postObjMethod();
        method.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity entity = new BasicHttpEntity();
        final String rdf = "<> <http://purl.org/dc/elements/1.1/title> \"this is a french title\"@fr ." +
                "<> <http://purl.org/dc/elements/1.1/title> \"this is an english title\"@en .";
        entity.setContent(new ByteArrayInputStream(rdf.getBytes()));
        method.setEntity(entity);

        final String location;
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), getStatus(response));
            location = response.getFirstHeader("Location").getValue();
        }
        // GET the resource with a JSON profile
        final HttpGet httpGet = new HttpGet(location);
        httpGet.setHeader("Accept", "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#flattened\"");
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
    public void testPathWithEmptySegment() {
        final String badLocation = "test/me/mb/er/s//members/9528a300-22da-40f2-bf3c-5b345d71affb";
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(headObjMethod(badLocation)));
    }

    private static Optional<Date> getDateFromModel(final Model model, final Resource subj, final Property pred)
            throws NoSuchElementException, ParseException {
        final StmtIterator stmts = model.listStatements(subj, pred, (String) null);
        return Optional.ofNullable(stmts.hasNext() ? tripleFormat.parse(stmts.nextStatement().getString()) : null);
    }

    private static Collection<String> getLinkHeaders(final HttpResponse response) {
        return stream(response.getHeaders("Link")).map(Header::getValue).collect(toList());
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
        updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");
        updateObjectGraphMethod.setEntity(new StringEntity("INSERT { <" + location +
                "> <http://purl.org/dc/elements/1.1/identifier> \"this is an identifier\". " + "<" + otherLocation +
                "> <http://purl.org/dc/elements/1.1/identifier> \"this is an identifier\"" + " } WHERE {}"));
        assertEquals("It ought not be possible to use PATCH to create non-local triples!",
                FORBIDDEN.getStatusCode(),getStatus(updateObjectGraphMethod));
    }

    @Test
    public void testPutMalformedHeader() throws IOException {
        // Create a resource
        final String id = getRandomUniqueId();
        executeAndClose(putObjMethod(id));

        // Get the resource's etag
        String etag;
        final HttpHead httpHead = headObjMethod(id);
        try (final CloseableHttpResponse response = execute(httpHead)) {
            etag = response.getFirstHeader("ETag").getValue();
            assertNotNull("ETag was missing!?", etag);
        }

        // PUT properly formatted etag
        final HttpPut httpPut = putObjMethod(id);
        httpPut.addHeader("If-Match", etag);

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Should be a 409 Conflict!", CONFLICT.getStatusCode(), getStatus(response));
        }

        // PUT improperly formatted etag ... not quoted.
        final HttpPut httpPut2 = putObjMethod(id);
        httpPut2.addHeader("If-Match", etag.replace("\"", ""));

        try (final CloseableHttpResponse response = execute(httpPut2)) {
            assertEquals("Should be a 400 BAD REQUEST!", BAD_REQUEST.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testPutEmptyBody() throws IOException {
        final HttpPut httpPut = putObjMethod(getRandomUniqueId());
        httpPut.addHeader("Content-Type", "application/ld+json");

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals("Should be a client error", BAD_REQUEST.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testPutOgg() throws IOException {
        final String id = getRandomUniqueId();
        createDatastream(id, "x", "OggS");
    }

    @Test
    public void testPutReferenceRoot() throws Exception {
        final HttpPut httpPut = putObjMethod(getRandomUniqueId());
        httpPut.addHeader("Content-Type", "text/turtle");
        httpPut.setEntity(new StringEntity("@prefix acl: <http://www.w3.org/ns/auth/acl#> . " +
                "<> a acl:Authorization ; " +
                "acl:agent \"smith123\" ; " +
                "acl:mode acl:Read ;" +
                "acl:accessTo <" + serverAddress + "> ."));

        try (final CloseableHttpResponse response = execute(httpPut)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }
    }

}