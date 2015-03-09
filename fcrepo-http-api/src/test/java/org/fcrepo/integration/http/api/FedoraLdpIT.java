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

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createModelForGraph;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createLangLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.TimeZone.getTimeZone;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.TEMPORARY_REDIRECT;
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
import static org.fcrepo.http.commons.domain.RDFMediaType.POSSIBLE_RDF_VARIANTS;
import static org.fcrepo.kernel.FedoraJcrTypes.FCR_METADATA;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.FIRST_PAGE;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.RdfLexicon.HAS_OBJECT_COUNT;
import static org.fcrepo.kernel.RdfLexicon.HAS_OBJECT_SIZE;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_IDENTIFIER;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.RdfLexicon.INBOUND_REFERENCES;
import static org.fcrepo.kernel.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.RdfLexicon.JCR_NT_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.MIX_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.NEXT_PAGE;
import static org.fcrepo.kernel.RdfLexicon.RDF_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
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
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

import nu.validator.htmlparser.sax.HtmlParser;
import nu.validator.saxtree.TreeBuilder;

import org.fcrepo.http.commons.domain.RDFMediaType;
import org.fcrepo.kernel.RdfLexicon;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.jena.riot.Lang;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
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

    private static final String TEST_ACTIVATION_PROPERTY = "RUN_TEST_CREATE_MANY";

    private static final Logger LOGGER = getLogger(FedoraLdpIT.class);

    private SimpleDateFormat headerFormat;
    private SimpleDateFormat tripleFormat;

    @Before
    public void setup() {
        headerFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        headerFormat.setTimeZone(getTimeZone("GMT"));
        tripleFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        tripleFormat.setTimeZone(getTimeZone("GMT"));
    }


    @Test
    public void testHeadRepositoryGraph() throws Exception {
        final HttpHead headObjMethod = new HttpHead(serverAddress);
        assertEquals(200, getStatus(headObjMethod));
    }

    @Test
    public void testHeadObject() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final String location = serverAddress + pid;
        final HttpHead headObjMethod = new HttpHead(location);
        assertEquals(200, getStatus(headObjMethod));
    }

    @Test
    public void testHeadDefaultContainer() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final String location = serverAddress + pid;
        final HttpHead headObjMethod = new HttpHead(location);
        final HttpResponse response = client.execute(headObjMethod);

        final Collection<String> links = getLinkHeaders(response);
        assertTrue("Didn't find LDP container link header!", links.contains("<" + BASIC_CONTAINER.getURI() + ">;" +
                "rel=\"type\""));
    }

    @Test
    public void testHeadBasicContainer() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        addMixin(pid, BASIC_CONTAINER.getURI());

        final String location = serverAddress + pid;
        final HttpHead headObjMethod = new HttpHead(location);
        final HttpResponse response = client.execute(headObjMethod);

        final Collection<String> links = getLinkHeaders(response);
        assertTrue("Didn't find LDP container link header!", links.contains("<" + BASIC_CONTAINER.getURI() + ">;" +
                "rel=\"type\""));
    }


    @Test
    public void testHeadDirectContainer() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        addMixin(pid, DIRECT_CONTAINER.getURI());

        final String location = serverAddress + pid;
        final HttpHead headObjMethod = new HttpHead(location);
        final HttpResponse response = client.execute(headObjMethod);

        final Collection<String> links = getLinkHeaders(response);
        assertTrue("Didn't find LDP container link header!", links.contains("<" + DIRECT_CONTAINER.getURI() + ">;" +
                "rel=\"type\""));
    }

    @Test
    public void testHeadIndirectContainer() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        addMixin(pid, INDIRECT_CONTAINER.getURI());

        final String location = serverAddress + pid;
        final HttpHead headObjMethod = new HttpHead(location);
        final HttpResponse response = client.execute(headObjMethod);

        final Collection<String> links = getLinkHeaders(response);
        assertTrue("Didn't find LDP container link header!", links.contains("<" + INDIRECT_CONTAINER.getURI() + ">;" +
                "rel=\"type\""));
    }



    @Test
    public void testHeadDatastream() throws Exception {
        final String pid = getRandomUniquePid();

        createDatastream(pid, "x", "123");

        final String location = serverAddress + pid + "/x";
        final HttpHead headObjMethod = new HttpHead(location);
        final HttpResponse response = client.execute(headObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine().getStatusCode());

        assertEquals(TEXT_PLAIN, response.getFirstHeader("Content-Type").getValue());
        assertEquals("3", response.getFirstHeader("Content-Length").getValue());
        assertEquals("bytes", response.getFirstHeader("Accept-Ranges").getValue());
        final ContentDisposition disposition
                = new ContentDisposition(response.getFirstHeader("Content-Disposition").getValue());
        assertEquals("attachment", disposition.getType());
    }

    @Test
    public void testOptions() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final String location = serverAddress + pid;

        final HttpOptions optionsRequest = new HttpOptions(location);
        final HttpResponse optionsResponse = client.execute(optionsRequest);
        assertEquals(OK.getStatusCode(), optionsResponse.getStatusLine().getStatusCode());

        assertContainerOptionsHeaders(optionsResponse);
    }

    @Test
    public void testOptionsBinary() throws Exception {
        final String pid = getRandomUniquePid();

        createDatastream(pid, "x", pid);

        final String location = serverAddress + pid + "/x";

        final HttpOptions optionsRequest = new HttpOptions(location);
        final HttpResponse optionsResponse = client.execute(optionsRequest);
        assertEquals(OK.getStatusCode(), optionsResponse.getStatusLine().getStatusCode());

        assertResourceOptionsHeaders(optionsResponse);

        assertEquals("0", optionsResponse.getFirstHeader("Content-Length").getValue());
    }

    @Test
    public void testOptionsBinaryMetadata() throws Exception {
        final String pid = getRandomUniquePid();

        createDatastream(pid, "x", pid);

        final String location = serverAddress + pid + "/x/fcr:metadata";

        final HttpOptions optionsRequest = new HttpOptions(location);
        final HttpResponse optionsResponse = client.execute(optionsRequest);
        assertEquals(OK.getStatusCode(), optionsResponse.getStatusLine().getStatusCode());

        assertRdfOptionsHeaders(optionsResponse);
    }

    @Test
    public void testOptionsBinaryMetadataWithUriEncoding() throws Exception {
        final String pid = getRandomUniquePid();

        createDatastream(pid, "x", pid);

        final String location = serverAddress + pid + "/x/fcr%3Ametadata";

        final HttpOptions optionsRequest = new HttpOptions(location);
        final HttpResponse optionsResponse = client.execute(optionsRequest);
        assertEquals(OK.getStatusCode(), optionsResponse.getStatusLine().getStatusCode());

        assertRdfOptionsHeaders(optionsResponse);
    }

    private static void assertContainerOptionsHeaders(final HttpResponse httpResponse) {
        assertRdfOptionsHeaders(httpResponse);
        final List<String> methods = headerValues(httpResponse,"Allow");
        assertTrue("Should allow POST", methods.contains(HttpPost.METHOD_NAME));

        final List<String> postTypes = headerValues(httpResponse,"Accept-Post");
        assertTrue("POST should support application/sparql-update", postTypes.contains(contentTypeSPARQLUpdate));
        assertTrue("POST should support text/turtle", postTypes.contains(contentTypeTurtle));
        assertTrue("POST should support text/rdf+n3", postTypes.contains(contentTypeN3));
        assertTrue("POST should support text/n3", postTypes.contains(contentTypeN3Alt2));
        assertTrue("POST should support application/rdf+xml", postTypes.contains(contentTypeRDFXML));
        assertTrue("POST should support application/n-triples", postTypes.contains(contentTypeNTriples));
        assertTrue("POST should support multipart/form-data", postTypes.contains("multipart/form-data"));
    }

    private static void assertRdfOptionsHeaders(final HttpResponse httpResponse) {
        final List<String> methods = headerValues(httpResponse,"Allow");
        assertTrue("Should allow PATCH", methods.contains(HttpPatch.METHOD_NAME));
        assertTrue("Should allow MOVE", methods.contains(HttpMove.METHOD_NAME));
        assertTrue("Should allow COPY", methods.contains(HttpCopy.METHOD_NAME));

        final List<String> patchTypes = headerValues(httpResponse,"Accept-Patch");
        assertTrue("PATCH should support application/sparql-update", patchTypes.contains(contentTypeSPARQLUpdate));
        assertResourceOptionsHeaders(httpResponse);
    }

    private static void assertResourceOptionsHeaders(final HttpResponse httpResponse) {
        final List<String> methods = headerValues(httpResponse,"Allow");
        assertTrue("Should allow GET", methods.contains(HttpGet.METHOD_NAME));
        assertTrue("Should allow PUT", methods.contains(HttpPut.METHOD_NAME));
        assertTrue("Should allow DELETE", methods.contains(HttpDelete.METHOD_NAME));
        assertTrue("Should allow OPTIONS", methods.contains(HttpOptions.METHOD_NAME));

    }

    private static List<String> headerValues( final HttpResponse response, final String headerName ) {
        final List<String> values = new ArrayList<>();
        for (final Header header : response.getHeaders(headerName)) {
            for (final String elem : header.getValue().split(",")) {
                values.add(elem.trim());
            }
        }
        return values;
    }

    @NotThreadSafe // HttpRequestBase is @NotThreadSafe
    private class HttpCopy extends HttpRequestBase {

        public final static String METHOD_NAME = "COPY";


        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

    }

    @NotThreadSafe // HttpRequestBase is @NotThreadSafe
    private class HttpMove extends HttpRequestBase {

        public final static String METHOD_NAME = "MOVE";


        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

    }


    @Test
    public void testGetRDFSource() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final HttpGet httpGet = new HttpGet(serverAddress + pid);
        final HttpResponse response = execute(httpGet);

        final HttpEntity entity = response.getEntity();
        final String content = EntityUtils.toString(entity);

        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());

        final MediaType mediaType = MediaType.valueOf(entity.getContentType().getValue());
        final Lang lang = contentTypeToLang(mediaType.toString());
        assertNotNull("Entity is not an RDF serialization", lang);
        LOGGER.warn(content);

    }

    @Test
    public void testGetNonRDFSource() throws Exception {
        final String pid = getRandomUniquePid();

        createDatastream(pid, "x", "some content");

        final HttpGet httpGet = new HttpGet(serverAddress + pid + "/x");
        final HttpResponse response = execute(httpGet);

        final HttpEntity entity = response.getEntity();
        final String content = EntityUtils.toString(entity);

        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        assertEquals("some content", content);
        LOGGER.warn(content);

    }

    @Test
    public void testGetNonRDFSourceDescription() throws IOException {
        final String pid = getRandomUniquePid();

        createDatastream(pid, "x", "some content");

        final HttpGet httpGet = new HttpGet(serverAddress + pid + "/x/" + FCR_METADATA);
        final HttpResponse response = execute(httpGet);

        final HttpEntity entity = response.getEntity();
        final String content = EntityUtils.toString(entity);

        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());

        final MediaType mediaType = MediaType.valueOf(entity.getContentType().getValue());
        final Lang lang = contentTypeToLang(mediaType.toString());
        assertNotNull("Entity is not an RDF serialization", lang);
        LOGGER.warn(content);

    }

    @Test
    public void testDeleteObject() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final String location = serverAddress + pid;
        assertEquals(204, getStatus(new HttpDelete(location)));
        assertDeleted(location);
    }

    @Test
    public void testDeleteHierarchy() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid + "/foo");

        final String location = serverAddress + pid;
        assertEquals(204, getStatus(new HttpDelete(location)));
        assertDeleted(location);
        assertDeleted(location + "/foo");
    }

    @Test
    public void testDeleteBinary() throws Exception {
        final String pid = getRandomUniquePid();

        createDatastream(pid, "x", "some content");

        final String location = serverAddress + pid + "/x";
        assertEquals(204, getStatus(new HttpDelete(location)));
        assertDeleted(location);
    }

    @Test
    public void testDeleteObjectAndTombstone() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final String location = serverAddress + pid;
        assertEquals(204, getStatus(new HttpDelete(location)));
        assertDeleted(location);
        final HttpGet httpGet = new HttpGet(location);
        final HttpResponse response = execute(httpGet);
        final Link tombstone = Link.valueOf(response.getFirstHeader("Link").getValue());

        assertEquals("hasTombstone", tombstone.getRel());

        final HttpResponse tombstoneResponse = execute(new HttpDelete(tombstone.getUri()));
        assertEquals(204, tombstoneResponse.getStatusLine().getStatusCode());

        assertEquals(404, getStatus(httpGet));
    }

    @Test
    public void testEmptyPatch() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final String location = serverAddress + pid;
        final HttpPatch patch = new HttpPatch(location);
        patch.addHeader("Content-Type", "application/sparql-update");
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patch));
    }

    @Test
    public void testUpdateObjectGraph() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final String location = serverAddress + pid;
        final HttpPatch updateObjectGraphMethod = new HttpPatch(location);
        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + location +
                        "> <http://purl.org/dc/elements/1.1/identifier> \"this is an identifier\" } WHERE {}")
                        .getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());

    }

    @Test
    public void testPatchBinary() throws Exception {
        final String pid = getRandomUniquePid();


        createDatastream(pid, "x", "some content");

        final String location = serverAddress + pid + "/x";
        final HttpPatch patch = new HttpPatch(location);
        patch.addHeader("Content-Type", "application/sparql-update");
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patch));
    }

    @Test
    public void testPatchBinaryDescription() throws Exception {
        final String pid = getRandomUniquePid();


        createDatastream(pid, "x", "some content");

        final String location = serverAddress + pid + "/x/fcr:metadata";
        final HttpPatch patch = new HttpPatch(location);
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + location +
                        "> <http://purl.org/dc/elements/1.1/identifier> \"this is an identifier\" } WHERE {}")
                        .getBytes()));
        patch.setEntity(e);
        final HttpResponse response = client.execute(patch);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());
    }

    @Test
    public void testPatchBinaryDescriptionWithBinaryProperties() throws Exception {
        final String pid = getRandomUniquePid();


        createDatastream(pid, "x", "some content");

        final String location = serverAddress + pid + "/x/fcr:metadata";
        final HttpPatch patch = new HttpPatch(location);
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + serverAddress + pid + "/x>" +
                        " <" + DC_11.identifier + "> \"this is an identifier\" } WHERE {}")
                        .getBytes()));
        patch.setEntity(e);
        final HttpResponse response = client.execute(patch);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());

        final GraphStore graphStore = getGraphStore(new HttpGet(location));
        assertTrue(graphStore.contains(ANY,
                createURI(serverAddress + pid + "/x"),
                DC_11.identifier.asNode(),
                createLiteral("this is an identifier")));
    }

    @Test
    public void testPatchWithBlankNode() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final String location = serverAddress + pid;
        final HttpPatch updateObjectGraphMethod = new HttpPatch(location);
        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + location +
                        "> <info:some-predicate> _:a .\n" +
                        "_:a <http://purl.org/dc/elements/1.1/title> \"this is a title\"\n" +
                        " } WHERE {}")
                        .getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());


        final HttpGet httpGet = new HttpGet(location);

        final GraphStore graphStore = getGraphStore(httpGet);
        LOGGER.debug("Recovered graph: {}", graphStore);
        assertTrue(graphStore.contains(ANY, createResource(location).asNode(),
                createProperty("info:some-predicate").asNode(), ANY));

        final Node bnode = graphStore.find(ANY, createResource(location).asNode(),
                createProperty("info:some-predicate").asNode(), ANY).next().getObject();
        LOGGER.debug("Received node: {}, checking for blankness.", bnode);
        assertTrue(bnode.isBlank());

    }

    @Test
    public void testReplaceGraph() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final String subjectURI = serverAddress + pid;

        final String initialContent = EntityUtils.toString(execute(new HttpGet(subjectURI)).getEntity());

        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                (initialContent + "\n<" + subjectURI + "> <info:rubydora#label> \"asdfg\"")
                        .getBytes()));
        replaceMethod.setEntity(e);
        final HttpResponse response = client.execute(replaceMethod);
        assertEquals(204, response.getStatusLine().getStatusCode());
        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));


        final HttpGet getObjMethod = new HttpGet(subjectURI);

        getObjMethod.addHeader("Accept", "application/rdf+xml");
        final HttpResponse getResponse = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
        final Model model = createDefaultModel();
        model.read(getResponse.getEntity().getContent(), null);
        try (final Writer w = new StringWriter()) {
            model.write(w);
            logger.trace(
                    "Retrieved object graph for testReplaceGraph():\n {}", w);
        }
        assertTrue("Didn't find a triple we tried to create!", model.contains(
                createResource(subjectURI),
                createProperty("info:rubydora#label"),
                createPlainLiteral("asdfg")));
    }


    @Test
    public void testCreateGraph() throws Exception {
        final String pid = getRandomUniquePid();
        final String subjectURI = serverAddress + pid;
        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("<" + subjectURI + "> <info:rubydora#label> \"asdfg\"")
                        .getBytes()));
        replaceMethod.setEntity(e);
        final HttpResponse response = client.execute(replaceMethod);
        assertEquals(201, response.getStatusLine().getStatusCode());


        final HttpGet getObjMethod = new HttpGet(subjectURI);

        getObjMethod.addHeader("Accept", "application/rdf+xml");
        final HttpResponse getResponse = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine()
                .getStatusCode());
        final Model model = createDefaultModel();
        model.read(getResponse.getEntity().getContent(), null);
        try (final Writer w = new StringWriter()) {
            model.write(w);
            logger.trace("Retrieved object graph for testCreateGraph():\n {}",
                    w);
        }
        assertTrue("Didn't find a triple we tried to create!", model.contains(
                createResource(subjectURI),
                createProperty("info:rubydora#label"),
                createPlainLiteral("asdfg")));
    }

    @Test
    public void testCreateGraphWithBlanknodes() throws IOException {
        final String pid = getRandomUniquePid();
        final String subjectURI = serverAddress + pid;
        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("<" + subjectURI + "> <info:some-predicate> _:a ." +
                        "_:a <info:rubydora#label> \"asdfg\"").getBytes()));
        replaceMethod.setEntity(e);
        final HttpResponse response = client.execute(replaceMethod);
        assertEquals(201, response.getStatusLine().getStatusCode());


        final HttpGet getObjMethod = new HttpGet(subjectURI);

        getObjMethod.addHeader("Accept", "application/rdf+xml");
        final HttpResponse getResponse = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine()
                .getStatusCode());
        final Model model = createDefaultModel();
        model.read(getResponse.getEntity().getContent(), null);
        try (final Writer w = new StringWriter()) {
            model.write(w);
            logger.trace("Retrieved object graph for testCreateGraph():\n {}",
                    w);
        }
        final NodeIterator nodeIterator = model.listObjectsOfProperty(createResource(subjectURI),
                createProperty("info:some-predicate"));
        assertTrue("Didn't find blank node assertion", nodeIterator.hasNext());
        final Resource blankNode = nodeIterator.nextNode().asResource();
        assertTrue(blankNode.isAnon());
        assertTrue("Didn't find a triple we tried to create!", model.contains(
                blankNode,
                createProperty("info:rubydora#label"),
                createPlainLiteral("asdfg")));
    }

    @Test
    public void testRoundTripReplaceGraph() throws Exception {

        final String pid = getRandomUniquePid();

        createObject(pid);

        final String subjectURI = serverAddress + pid;

        final HttpGet getObjMethod = new HttpGet(subjectURI);
        getObjMethod.addHeader("Accept", "text/turtle");
        final HttpResponse getResponse = client.execute(getObjMethod);

        final BasicHttpEntity e = new BasicHttpEntity();

        final Model model = createDefaultModel();
        model.read(getResponse.getEntity().getContent(), subjectURI, "TURTLE");

        try (final StringWriter w = new StringWriter()) {
            model.write(w, "TURTLE");
            e.setContent(new ByteArrayInputStream(w.toString().getBytes()));
            logger.trace("Retrieved object graph for testRoundTripReplaceGraph():\n {}",
                    w);
        }

        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "text/turtle");

        replaceMethod.setEntity(e);
        final HttpResponse response = client.execute(replaceMethod);
        assertEquals(204, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testPutBinary() throws Exception {

        final String pid = getRandomUniquePid();
        createObject(pid);

        final String location = serverAddress + pid + "/x";
        final HttpPut method = new HttpPut(location);
        method.setEntity(new StringEntity("foo"));
        final HttpResponse response = execute(method);
        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));

        final String receivedLocation = response.getFirstHeader("Location").getValue();
        assertEquals(201, response.getStatusLine().getStatusCode());
        assertEquals(
                "Got wrong URI in Location header for datastream creation!",
                location, receivedLocation);

        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        final String lastmod = response.getFirstHeader("Last-Modified").getValue();
        assertNotNull("Should set Last-Modified for new nodes", lastmod);
        assertNotEquals("Last-Modified should not be blank for new nodes", lastmod.trim(), "");
        final Link link = Link.valueOf(response.getFirstHeader("Link").getValue());
        assertEquals("describedby", link.getRel());
    }

    @Test
    public void testPutDatastreamContentOnObject() throws Exception {
        final String content = "foo";
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPut put = new HttpPut(serverAddress + pid);
        put.setEntity(new StringEntity(content));
        put.setHeader("Content-Type", "application/octet-stream");
        final HttpResponse response = execute(put);
        assertEquals("Expected 415 response code when PUTing content on an object (as opposed to a datastream).",
                415, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testEmptyPutToExistingObject() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPut put = new HttpPut(serverAddress + pid);
        final HttpResponse response = execute(put);
        assertEquals("Expected 409 response code when doing empty PUT on an existing object.",
                409, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testPutMalformedRDFOnObject() throws Exception {
        final String content = "foo";
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPut put = new HttpPut(serverAddress + pid);
        put.setEntity(new StringEntity(content));
        put.setHeader("Content-Type", "text/plain");
        final HttpResponse response = execute(put);
        assertEquals("Expected 400 response code when PUTing malformed RDF on an object",
                BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
    }

    @Test
    public void testIngest() throws Exception {
        final String pid = getRandomUniquePid();

        final HttpResponse response = createObject(pid);

        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));
        assertTrue("Didn't find Location header!", response.containsHeader("Location"));
    }

    @Test
    public void testIngestWithNewAndSparqlQuery() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(
                ("INSERT { <> <http://purl.org/dc/elements/1.1/title> \"this is a title\" } WHERE {}")
                        .getBytes()));
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);
        final String location = response.getFirstHeader("Location").getValue();

        final HttpGet httpGet = new HttpGet(location);

        final GraphStore graphStore = getGraphStore(httpGet);

        assertTrue(graphStore.contains(ANY, createResource(location).asNode(),
                DC_TITLE.asNode(), createPlainLiteral("this is a title")
                        .asNode()));

        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        final String lastmod = response.getFirstHeader("Last-Modified").getValue();
        assertNotNull("Should set Last-Modified for new nodes", lastmod);
        assertNotEquals("Last-Modified should not be blank for new nodes", lastmod.trim(), "");
    }

    @Test
    public void testIngestWithSparqlQueryJcrNS() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(
                ("PREFIX fcr: <http://xmlns.com/my-fcr/> "
                        + "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"this is a title\" } WHERE {}")
                        .getBytes()));
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        final int status = response.getStatusLine().getStatusCode();
        assertFalse("Got a CREATED response with jcr namspace prefix!",
                CREATED.getStatusCode() == status);
    }

    @Test
    public void testIngestWithNewAndGraph() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity entity = new BasicHttpEntity();
        final String rdf = "<> <http://purl.org/dc/elements/1.1/title> \"this is a title\".";
        entity.setContent(new ByteArrayInputStream(rdf.getBytes()));
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);

        final String location = response.getFirstHeader("Location").getValue();

        final HttpGet httpGet = new HttpGet(location);

        final GraphStore graphStore = getGraphStore(httpGet);

        assertTrue(graphStore.contains(ANY, createResource(location).asNode(),
                DC_TITLE.asNode(), createPlainLiteral("this is a title")
                        .asNode()));

        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        final String lastmod = response.getFirstHeader("Last-Modified").getValue();
        assertNotNull("Should set Last-Modified for new nodes", lastmod);
        assertNotEquals("Last-Modified should not be blank for new nodes", lastmod.trim(), "");
    }

    @Test
    public void testIngestWithSlug() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Slug", getRandomUniquePid());
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        final String location = response.getFirstHeader("Location").getValue();
        assertNotEquals(serverAddress + "/objects", location);

        assertEquals("Object wasn't created!", OK.getStatusCode(),
                getStatus(new HttpGet(location)));
    }

    @Test
    public void testIngestWithRepeatedSlug() throws Exception {
        final String pid = getRandomUniquePid();
        final HttpPut put = new HttpPut(serverAddress + pid);
        assertEquals(201, getStatus(put));

        final HttpPost method = postObjMethod("");
        method.addHeader("Slug", pid);
        assertEquals(201, getStatus(method));
    }

    @Test
    public void testIngestWithBinary() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Content-Type", "application/octet-stream");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream("xyz".getBytes()));
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        final String location = response.getFirstHeader("Location").getValue();
        assertNotEquals(serverAddress + "/objects", location);
        assertEquals("Object wasn't created!", OK.getStatusCode(),
                getStatus(new HttpGet(location)));

        final Link link = Link.valueOf(response.getFirstHeader("Link").getValue());

        assertEquals("describedby", link.getRel());
        assertTrue("Expected an anchor to the newly created resource", link.getParams().containsKey("anchor"));
        assertEquals("Expected anchor to point at the newly created resource",
                location, link.getParams().get("anchor"));
        assertEquals("Expected describedby link to point at the description",
                location + "/" + FCR_METADATA, link.getUri().toString());
    }

    @Test
    public void testIngestOnSubtree() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);


        final HttpPost method = postObjMethod(pid);
        method.addHeader("Slug", "x");
        final HttpResponse response = client.execute(method);
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(serverAddress + pid + "/x", location);

    }

    @Test
    public void testIngestWithRDFLang() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity entity = new BasicHttpEntity();
        final String rdf = "<> <http://purl.org/dc/elements/1.1/title> \"this is a french title\"@fr ." +
                "<> <http://purl.org/dc/elements/1.1/title> \"this is an english title\"@en .";
        entity.setContent(new ByteArrayInputStream(rdf.getBytes()));
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);

        final String location = response.getFirstHeader("Location").getValue();

        final HttpGet httpGet = new HttpGet(location);

        final GraphStore graphStore = getGraphStore(httpGet);

        assertTrue(graphStore.contains(ANY, createResource(location).asNode(),
                DC_TITLE.asNode(), createLangLiteral("this is an english title", "en")
                        .asNode()));

        assertTrue(graphStore.contains(ANY, createResource(location).asNode(),
                DC_TITLE.asNode(), createLangLiteral("this is a french title", "fr")
                        .asNode()));

    }


    @Test
    public void testCreateManyObjects() throws Exception {
        if (System.getProperty(TEST_ACTIVATION_PROPERTY) == null) {
            logger.info("Not running test because system property not set: {}", TEST_ACTIVATION_PROPERTY);
            return;
        }

        final int manyObjects = 2000;
        for ( int i = 0; i < manyObjects; i++ ) {
            Thread.sleep(10); // needed to prevent overloading
            final HttpResponse response = createObject("");
            logger.debug( response.getFirstHeader("Location").getValue() );
        }
    }

    @Test
    public void testDeleteWithBadEtag() throws Exception {

        final HttpPost method = postObjMethod("");
        final HttpResponse response = client.execute(method);
        assertEquals(CREATED.getStatusCode(), response.getStatusLine()
                .getStatusCode());

        final String location = response.getFirstHeader("Location").getValue();
        final HttpDelete request = new HttpDelete(location);
        request.addHeader("If-Match", "\"doesnt-match\"");
        final HttpResponse deleteResponse = client.execute(request);
        assertEquals(412, deleteResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void testGetDatastream() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        createDatastream(pid, "ds1", "foo");

        final HttpResponse response =
                execute(new HttpGet(serverAddress + pid + "/ds1"));
        assertEquals(EntityUtils.toString(response.getEntity()), 200, response
                .getStatusLine().getStatusCode());

        assertEquals(TEXT_PLAIN, response.getFirstHeader("Content-Type").getValue());
        assertEquals("3", response.getFirstHeader("Content-Length").getValue());
        assertEquals("bytes", response.getFirstHeader("Accept-Ranges").getValue());
        final ContentDisposition disposition
                = new ContentDisposition(response.getFirstHeader("Content-Disposition").getValue());
        assertEquals("attachment", disposition.getType());

        final Collection<String> links = getLinkHeaders(response);
        assertTrue("Didn't find 'describedby' link header!",
                links.contains("<" + serverAddress + pid + "/ds1/" + FCR_METADATA + ">; rel=\"describedby\""));

    }

    @Test
    public void testDeleteDatastream() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        createDatastream(pid, "ds1", "foo");

        final HttpDelete dmethod =
                new HttpDelete(serverAddress + pid + "/ds1");
        assertEquals(204, getStatus(dmethod));

        assertDeleted(serverAddress + pid + "/ds1");
    }

    @Test
    public void testGetRepositoryGraph() throws Exception {
        final HttpGet getObjMethod = new HttpGet(serverAddress);
        final GraphStore graphStore = getGraphStore(getObjMethod);
        logger.trace("Retrieved repository graph:\n" + graphStore.toString());

        assertTrue("expected to find the root node data", graphStore.contains(
                ANY, ANY, HAS_PRIMARY_TYPE.asNode(), createLiteral(ROOT)));

    }

    @Test
    public void testGetObjectGraphHtml() throws Exception {
        final HttpResponse createResponse = createObject("");

        final String location = createResponse.getFirstHeader("Location").getValue();

        final HttpGet getObjMethod = new HttpGet(location);
        getObjMethod.addHeader("Accept", "text/html");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.trace("Retrieved: {}", content);
    }

    @Test
    public void testGetObjectGraphVariants() throws Exception {
        final HttpResponse createResponse = createObject("");

        final String location = createResponse.getFirstHeader("Location").getValue();

        for (final Variant variant : POSSIBLE_RDF_VARIANTS) {

            final HttpGet getObjMethod =
                    new HttpGet(location);

            final String type = variant.getMediaType().getType();

            getObjMethod.addHeader("Accept", type);
            final HttpResponse response = client.execute(getObjMethod);

            final int expected = OK.getStatusCode();
            final int found = response.getStatusLine().getStatusCode();

            assertEquals("Expected: " + expected + ", recieved: " + found + ", " + type, expected, found);
        }
    }

    @Test
    public void testGetObjectGraph() throws Exception {
        logger.debug("Entering testGetObjectGraph()...");
        final HttpResponse createResponse = createObject("");

        final String location = createResponse.getFirstHeader("Location").getValue();

        final HttpGet getObjMethod =
                new HttpGet(location);
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());

        assertResourceOptionsHeaders(response);

        final Collection<String> links = getLinkHeaders(response);
        assertTrue("Didn't find LDP link header!", links
                .contains("<" + LDP_NAMESPACE + "Resource>;rel=\"type\""));
        final GraphStore results = getGraphStore(getObjMethod);
        final Model model = createModelForGraph(results.getDefaultGraph());

        final Resource nodeUri = createResource(location);

        assertTrue("Didn't find an expected triple!", model.contains(nodeUri,
                createProperty(REPOSITORY_NAMESPACE + "mixinTypes"),
                createPlainLiteral(FEDORA_CONTAINER)));

        logger.debug("Leaving testGetObjectGraph()...");
    }

    @Test
    public void verifyFullSetOfRdfTypes() throws Exception {
        logger.debug("Entering verifyFullSetOfRdfTypes()...");
        final String pid = getRandomUniquePid();
        createObject(pid);
        addMixin( pid, MIX_NAMESPACE + "versionable" );

        final HttpGet getObjMethod =
                new HttpGet(serverAddress + pid);
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        final GraphStore results = getGraphStore(getObjMethod);
        final Model model = createModelForGraph(results.getDefaultGraph());
        final Resource nodeUri = createResource(serverAddress + pid);
        final Property rdfType = createProperty(RDF_NAMESPACE + "type");

        verifyResource(model, nodeUri, rdfType, REPOSITORY_NAMESPACE, "Container");
        verifyResource(model, nodeUri, rdfType, REPOSITORY_NAMESPACE, "Resource");
        verifyResource(model, nodeUri, rdfType, MIX_NAMESPACE, "created");
        verifyResource(model, nodeUri, rdfType, MIX_NAMESPACE, "lastModified");
        verifyResource(model, nodeUri, rdfType, MIX_NAMESPACE, "referenceable");
        verifyResource(model, nodeUri, rdfType, MIX_NAMESPACE, "simpleVersionable");
        verifyResource(model, nodeUri, rdfType, MIX_NAMESPACE, "versionable");
        verifyResource(model, nodeUri, rdfType, JCR_NT_NAMESPACE, "base");
        verifyResource(model, nodeUri, rdfType, JCR_NT_NAMESPACE, "folder");
        verifyResource(model, nodeUri, rdfType, JCR_NT_NAMESPACE, "hierarchyNode");

        logger.debug("Leaving verifyFullSetOfRdfTypes()...");
    }

    private static void verifyResource(final Model model,
                                       final Resource nodeUri,
                                       final Property rdfType,
                                       final String namespace,
                                       final String resource) {
        assertTrue("Didn't find rdfType " + namespace + resource,
                model.contains(nodeUri, rdfType, createResource(namespace + resource)));
    }


    @Test
    public void testGetObjectGraphWithChildren() throws Exception {
        final String pid = getRandomUniquePid();
        final HttpResponse createResponse = createObject(pid);
        final String location = createResponse.getFirstHeader("Location").getValue();

        createObject(pid + "/a");
        createObject(pid + "/b");
        createObject(pid + "/c");
        final HttpGet getObjMethod = new HttpGet(serverAddress + pid);
        getObjMethod.addHeader("Accept", "application/rdf+xml");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        final Model model = createDefaultModel();
        model.read(response.getEntity().getContent(), null);
        try (final Writer w = new StringWriter()) {
            model.write(w);
            logger.trace(
                    "Retrieved object graph:\n {}",
                    w);
        }

        final Resource subjectUri = createResource(location);
        assertTrue(
                "Didn't find child node!",
                model.contains(
                        subjectUri,
                        createProperty(LDP_NAMESPACE + "contains"),
                        createResource(location + "/c")));
        final Collection<String> links = getLinkHeaders(response);
        assertTrue("Didn't find LDP resource link header!",
                links.contains("<" + LDP_NAMESPACE + "Resource>;rel=\"type\""));
    }

    @Test
    public void testGetObjectGraphMinimal() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);
        addMixin(pid, BASIC_CONTAINER.getURI());
        createObject(pid + "/a");
        final HttpGet getObjMethod =
                new HttpGet(serverAddress + pid);
        getObjMethod.addHeader("Prefer", "return=minimal");
        getObjMethod.addHeader("Accept", "application/n-triples");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        logger.trace("Retrieved object graph:\n" + content);

        assertFalse(
                "Didn't expect member resources",
                compile(
                        "<"
                                + serverAddress
                                + pid + "> <" + HAS_CHILD + ">",
                        DOTALL).matcher(content).find());

        assertFalse("Didn't expect contained member resources",
                compile(
                        "<"
                                + serverAddress
                                + pid + "> <" + CONTAINS + ">",
                        DOTALL).matcher(content).find());
    }

    @Test
    public void testGetObjectOmitMembership() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);
        addMixin(pid, BASIC_CONTAINER.getURI());
        createObject(pid + "/a");
        final HttpGet getObjMethod =
                new HttpGet(serverAddress + pid);
        getObjMethod.addHeader("Prefer",
                "return=representation; " +
                        "omit=\"http://www.w3.org/ns/ldp#PreferContainment " +
                        "http://www.w3.org/ns/ldp#PreferMembership\"");
        getObjMethod.addHeader("Accept", "application/n-triples");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        logger.trace("Retrieved object graph:\n" + content);

        assertFalse(
                "Didn't expect inlined member resources",
                compile(
                        "<"
                                + serverAddress
                                + pid + "> <" + HAS_CHILD + ">",
                        DOTALL).matcher(content).find());

    }

    @Test
    public void testGetObjectOmitContainment() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);
        final HttpPatch patch = new HttpPatch(serverAddress + pid);

        patch.setHeader("Content-Type", "application/sparql-update");
        final String updateString =
                "INSERT DATA { <> a <" + DIRECT_CONTAINER.getURI() + "> ;" +
                        "    <" + MEMBERSHIP_RESOURCE.getURI() + "> <> ;" +
                        "    <" + HAS_MEMBER_RELATION + "> <" + LDP_NAMESPACE + "member> ." +
                        "}";
        patch.setEntity(new StringEntity(updateString));
        assertEquals(204, getStatus(patch));

        createObject(pid + "/a");
        final HttpGet getObjMethod =
                new HttpGet(serverAddress + pid);
        getObjMethod.addHeader("Prefer", "return=representation; omit=\"http://www.w3.org/ns/ldp#PreferContainment\"");
        getObjMethod.addHeader("Accept", "application/n-triples");
        final HttpResponse response = client.execute(getObjMethod);

        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        logger.trace("Retrieved object graph:\n" + content);

        assertTrue("Didn't find member resources",
                compile(
                        "<"
                                + serverAddress
                                + pid + "> <" + LDP_MEMBER + ">",
                        DOTALL).matcher(content).find());

        assertFalse("Didn't expect contained resources",
                compile(
                        "<"
                                + serverAddress
                                + pid + "> <" + CONTAINS + ">",
                        DOTALL).matcher(content).find());

    }

    @Test
    public void testGetObjectReferences() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);
        createObject(pid + "/a");
        createObject(pid + "/b");

        final HttpPatch updateObjectGraphMethod = new HttpPatch(serverAddress + pid + "/a");

        updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");

        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(
                new ByteArrayInputStream(
                        ("INSERT { " +
                                "<" + serverAddress + pid + "/a"
                                + "> <http://purl.org/dc/terms/isPartOf> <"
                                + serverAddress + pid + "/b" + "> . \n" +
                                "<" + serverAddress + pid + "/a" + "> <info:xyz#some-other-property> <"
                                + serverAddress + pid + "/b"
                                + "> " + "} WHERE {}").getBytes()));

        updateObjectGraphMethod.setEntity(e);
        client.execute(updateObjectGraphMethod);

        final HttpGet getObjMethod =  new HttpGet(serverAddress + pid + "/b");

        getObjMethod.addHeader("Prefer", "return=representation; include=\"" + INBOUND_REFERENCES.toString() + "\"");
        getObjMethod.addHeader("Accept", "application/n-triples");

        final GraphStore graphStore = getGraphStore(getObjMethod);

        assertTrue(graphStore.contains(Node.ANY,
                NodeFactory.createURI(serverAddress + pid + "/a"),
                NodeFactory.createURI("http://purl.org/dc/terms/isPartOf"),
                NodeFactory.createURI(serverAddress + pid + "/b")
        ));

        assertTrue(graphStore.contains(Node.ANY,
                NodeFactory.createURI(serverAddress + pid + "/a"),
                NodeFactory.createURI("info:xyz#some-other-property"),
                NodeFactory.createURI(serverAddress + pid + "/b")
        ));

    }

    @Test
    public void testGetObjectGraphByUUID() throws Exception {
        final HttpResponse createResponse = createObject("");

        final String location = createResponse.getFirstHeader("Location").getValue();

        final HttpGet getObjMethod = new HttpGet(location);
        final GraphStore graphStore = getGraphStore(getObjMethod);
        final Iterator<Quad> iterator =
                graphStore.find(ANY, createURI(location),
                        HAS_PRIMARY_IDENTIFIER.asNode(), ANY);

        assertTrue("Expected graph to contain a UUID", iterator.hasNext());

        final String uuid = iterator.next().getObject().getLiteralLexicalForm();

        final HttpGet getObjMethodByUuid =
                new HttpGet(serverAddress + "%5B" + uuid + "%5D");
        getObjMethodByUuid.addHeader("Accept", "application/n3");
        final HttpResponse uuidResponse = client.execute(getObjMethod);
        assertEquals(200, uuidResponse.getStatusLine().getStatusCode());

    }

    @Test
    public void testLinkToNonExistent() throws Exception {
        final HttpResponse createResponse = createObject("");
        final String subjectURI = createResponse.getFirstHeader("Location").getValue();
        final HttpPatch patch = new HttpPatch(subjectURI);
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <> <http://some-vocabulary#isMemberOfCollection> " +
                        "<" + serverAddress + "non-existant> } WHERE {}").getBytes()));
        patch.setEntity(e);
        assertEquals(BAD_REQUEST.getStatusCode(), getStatus(patch));
    }

    @Test
    public void testUpdateAndReplaceObjectGraph() throws Exception {
        final HttpResponse createResponse = createObject("");
        final String subjectURI = createResponse.getFirstHeader("Location").getValue();
        final HttpPatch updateObjectGraphMethod = new HttpPatch(subjectURI);

        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");

        BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + subjectURI + "> <info:rubydora#label> \"asdfg\" } WHERE {}")
                        .getBytes()));

        updateObjectGraphMethod.setEntity(e);
        client.execute(updateObjectGraphMethod);

        e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(("DELETE { <" + subjectURI
                + "> <info:rubydora#label> ?p}\n" + "INSERT {<" + subjectURI
                + "> <info:rubydora#label> \"qwerty\"} \n" + "WHERE { <"
                + subjectURI + "> <info:rubydora#label> ?p}").getBytes()));

        updateObjectGraphMethod.setEntity(e);

        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());

        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));

        final HttpGet getObjMethod = new HttpGet(subjectURI);

        getObjMethod.addHeader("Accept", "application/rdf+xml");
        final HttpResponse getResponse = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine()
                .getStatusCode());
        final String content = EntityUtils.toString(getResponse.getEntity());
        logger.trace("Retrieved object graph:\n" + content);

        assertFalse("Found a triple we thought we deleted.", compile(
                "<" + subjectURI + "> <info:rubydora#label> \"asdfg\" \\.",
                DOTALL).matcher(content).find());

    }

    @Test
    public void testUpdateWithSparqlQueryJcrNS() throws Exception {
        final HttpResponse createResponse = createObject("");
        final String subjectURI = createResponse.getFirstHeader("Location").getValue();
        final HttpPatch updateObjectGraphMethod = new HttpPatch(subjectURI);

        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");

        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("PREFIX fcr: <http://xmlns.com/my-fcr/> "
                        + "INSERT { <" + subjectURI + "> <info:rubydora#label> \"asdfg\" } WHERE {}")
                        .getBytes()));

        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        final int status = response.getStatusLine().getStatusCode();
        assertFalse("Got updated response with jcr namspace prefix!\n",
                NO_CONTENT.getStatusCode() == status);
    }

    @Test
    public void testUpdateObjectGraphWithProblems() throws Exception {

        final HttpResponse createResponse = createObject("");
        final String subjectURI = createResponse.getFirstHeader("Location").getValue();

        final HttpPatch patchObjMethod = new HttpPatch(subjectURI);
        patchObjMethod.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + subjectURI + "> <" + REPOSITORY_NAMESPACE +
                        "uuid> \"00e686e2-24d4-40c2-92ce-577c0165b158\" } WHERE {}\n")
                        .getBytes()));
        patchObjMethod.setEntity(e);
        final HttpResponse response = client.execute(patchObjMethod);

        if (response.getStatusLine().getStatusCode() != BAD_REQUEST.getStatusCode()
                && response.getEntity() != null) {
            final String content = EntityUtils.toString(response.getEntity());
            logger.trace("Got unexpected update response:\n" + content);
        }
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());

    }

    @Test
    public void testPutResourceBadRdf() throws Exception {
        final String pid = getRandomUniquePid();
        final HttpPut httpPut = new HttpPut(serverAddress + "/test/" + pid);
        httpPut.setHeader("Content-Type", "text/turtle");
        httpPut.setEntity(new StringEntity("<> a \"still image\"."));

        final HttpResponse response = client.execute(httpPut);

        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
    }

    @Test
    public void testRepeatedPut() throws Exception {
        final String pid = getRandomUniquePid();
        final HttpPut firstPut = new HttpPut(serverAddress + pid);
        assertEquals(201, getStatus(firstPut));

        final HttpPut secondPut = new HttpPut(serverAddress + pid);
        secondPut.setHeader("Content-Type", "text/turtle");
        assertEquals(400, getStatus(secondPut));
    }

    @Test
    public void testCreateResourceWithoutContentType() throws Exception {
        final String pid = getRandomUniquePid();

        final HttpPut httpPut = new HttpPut(serverAddress + pid);
        assertEquals(201, getStatus(httpPut));
    }

    @Test
    public void testUpdateObjectWithoutContentType() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPut httpPut = new HttpPut(serverAddress + pid);
        httpPut.setEntity(new ByteArrayEntity("bogus content".getBytes()));
        assertEquals(415, getStatus(httpPut));
    }

    @Test
    public void testUpdateBinaryWithoutContentType() throws Exception {
        final String pid = getRandomUniquePid();
        createDatastream(pid, "x", "xyz");

        final HttpPut httpPut = new HttpPut(serverAddress + pid + "/x");
        assertEquals(204, getStatus(httpPut));
    }

    @Test
    public void testRoundTripReplaceGraphForDatastream() throws Exception {

        final String pid = getRandomUniquePid();
        final String subjectURI = serverAddress + pid + "/ds1";

        createDatastream(pid, "ds1", "some-content");

        final HttpGet getObjMethod = new HttpGet(subjectURI + "/" + FCR_METADATA);
        getObjMethod.addHeader("Accept", "text/turtle");
        final HttpResponse getResponse = client.execute(getObjMethod);

        final BasicHttpEntity e = new BasicHttpEntity();

        final Model model = createDefaultModel();
        model.read(getResponse.getEntity().getContent(), subjectURI, "TURTLE");

        try (final StringWriter w = new StringWriter()) {
            model.write(w, "TURTLE");
            e.setContent(new ByteArrayInputStream(w.toString().getBytes()));
            logger.trace("Retrieved object graph for testRoundTripReplaceGraphForDatastream():\n {}",
                    w);
        }

        final HttpPut replaceMethod = new HttpPut(subjectURI + "/" + FCR_METADATA);
        replaceMethod.addHeader("Content-Type", "text/turtle");

        replaceMethod.setEntity(e);
        final HttpResponse response = client.execute(replaceMethod);
        assertEquals(204, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testResponseContentTypes() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        for (final String type : RDFMediaType.POSSIBLE_RDF_RESPONSE_VARIANTS_STRING) {
            final HttpGet method =
                    new HttpGet(serverAddress + pid);

            method.addHeader("Accept", type);
            assertEquals(type, getContentType(method));
        }
    }

    @Ignore("pending https://www.pivotaltracker.com/story/show/78647248")
    @Test
    public void testDescribeSize() throws Exception {

        final String sizeNode = getRandomUniquePid();

        GraphStore graphStore = getGraphStore(new HttpGet(serverAddress + ""));
        logger.trace("For testDescribeSize() first size retrieved repository graph:\n"
                + graphStore.toString());

        Iterator<Triple> iterator =
                graphStore.getDefaultGraph().find(ANY, HAS_OBJECT_SIZE.asNode(),
                        ANY);

        final String oldSize = (String) iterator.next().getObject().getLiteralValue();

        createObject(sizeNode);
        createDatastream(sizeNode, "asdf", "1234");

        graphStore = getGraphStore(new HttpGet(serverAddress + ""));
        logger.trace("For testDescribeSize() new size retrieved repository graph:\n"
                + graphStore.toString());

        iterator =
                graphStore.getDefaultGraph().find(ANY, HAS_OBJECT_SIZE.asNode(),
                        ANY);

        final String newSize = (String) iterator.next().getObject().getLiteralValue();

        logger.trace("Old size was: " + oldSize + " and new size was: "
                + newSize);
        assertTrue("No increment in size occurred when we expected one!",
                Integer.parseInt(oldSize) < Integer.parseInt(newSize));
    }

    @Ignore("pending https://www.pivotaltracker.com/story/show/78647248")
    @Test
    public void testDescribeCount() throws Exception {
        logger.trace("Entering testDescribeCount()...");
        GraphStore graphStore = getGraphStore(new HttpGet(serverAddress + ""));
        logger.trace("For testDescribeCount() first count retrieved repository graph:\n"
                + graphStore.toString());

        Iterator<Triple> iterator =
                graphStore.getDefaultGraph().find(ANY, HAS_OBJECT_COUNT.asNode(),
                        ANY);

        final String oldSize = (String) iterator.next().getObject().getLiteralValue();

        createObject("");
        final String countNode = getRandomUniquePid();
        createDatastream(countNode, "asdf", "1234");

        graphStore = getGraphStore(new HttpGet(serverAddress + ""));
        logger.trace("For testDescribeCount() first count repository graph:\n"
                + graphStore.toString());

        iterator =
                graphStore.getDefaultGraph().find(ANY, HAS_OBJECT_COUNT.asNode(),
                        ANY);

        final String newSize =
                (String) iterator.next().getObject().getLiteralValue();

        logger.debug("Old size was: " + oldSize + " and new size was: " +
                newSize);
        assertTrue("No increment in count occurred when we expected one!",
                Integer.parseInt(oldSize) < Integer.parseInt(newSize));
    }

    /**
     * Given a directory at: test-FileSystem1/ /ds1 /ds2 /TestSubdir/
     * and a projection of test-objects as fedora:/files, then I should be able
     * to retrieve an object from fedora:/files/FileSystem1 that lists a child
     * object at fedora:/files/FileSystem1/TestSubdir and lists datastreams ds1
     * and ds2
     */
    @Test
    public void testGetProjectedNode() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "files/FileSystem1");
        final Graph result = getGraphStore(method).getDefaultGraph();

        final String subjectURI = serverAddress + "files/FileSystem1";
        logger.trace("For testGetProjectedNode() retrieved graph:\n"
                + result.toString());
        assertTrue("Didn't find the first datastream! ", result.contains(
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds1")));
        assertTrue("Didn't find the second datastream! ", result.contains(
                createURI(subjectURI), ANY, createURI(subjectURI + "/ds2")));
        assertTrue("Didn't find the first object! ", result.contains(
                createURI(subjectURI), ANY, createURI(subjectURI
                        + "/TestSubdir")));

    }

    @Test
    public void testDescribeRdfCached() throws IOException {
        try (final CloseableHttpClient cachingClient =
                     CachingHttpClientBuilder.create().setCacheConfig(DEFAULT).build()) {

            final HttpResponse createResponse = createObject("");
            final String location = createResponse.getFirstHeader("Location").getValue();
            final HttpGet getObjMethod = new HttpGet(location);

            HttpResponse response = cachingClient.execute(getObjMethod);
            assertEquals("Client didn't return a OK!", OK.getStatusCode(), response
                    .getStatusLine().getStatusCode());
            logger.debug("Found HTTP headers:\n{}", Joiner.on('\n').join(
                    response.getAllHeaders()));
            assertTrue("Didn't find Last-Modified header!", response
                    .containsHeader("Last-Modified"));
            final String lastModed =
                    response.getFirstHeader("Last-Modified").getValue();
            final String etag = response.getFirstHeader("ETag").getValue();
            final HttpGet getObjMethod2 = new HttpGet(location);
            getObjMethod2.setHeader("If-Modified-Since", lastModed);
            getObjMethod2.setHeader("If-None-Match", etag);
            response = cachingClient.execute(getObjMethod2);
            assertEquals("Client didn't return a NOT_MODIFIED!", NOT_MODIFIED
                    .getStatusCode(), response.getStatusLine().getStatusCode());

        }
    }

    @Test
    public void testValidHTMLForRepo() throws Exception {
        validateHTML("");
    }

    @Test
    public void testValidHTMLForObject() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        validateHTML(pid);
    }

    @Test
    public void testValidHTMLForDS() throws Exception {
        final String pid = getRandomUniquePid();
        createDatastream(pid, "ds", "content");
        validateHTML(pid + "/ds/" + FCR_METADATA);
    }

    private static void validateHTML(final String path) throws Exception {
        final HttpGet getMethod = new HttpGet(serverAddress + path);
        getMethod.addHeader("Accept", "text/html");
        final HttpResponse response = client.execute(getMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.trace("Retrieved HTML view:\n" + content);

        final HtmlParser htmlParser = new HtmlParser(ALLOW);
        htmlParser.setDoctypeExpectation(NO_DOCTYPE_ERRORS);
        htmlParser.setErrorHandler(new HTMLErrorHandler());
        htmlParser.setContentHandler(new TreeBuilder());
        try (
                final InputStream htmlStream =
                        new ByteArrayInputStream(content.getBytes())) {
            htmlParser.parse(new InputSource(htmlStream));
        }
        logger.debug("HTML found to be valid.");
    }

    public static class HTMLErrorHandler implements ErrorHandler {

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
     * I should be able to create two subdirectories of a non-existent parent
     * directory.
     **/
    @Ignore("Enabled once the FedoraFileSystemConnector becomes readable/writable")
    public void testBreakFederation() throws Exception {
        final String pid = getRandomUniquePid();
        testGetRepositoryGraph();
        createObject("files/a0/" + pid + "b0");
        createObject("files/a0/" + pid + "b1");
        testGetRepositoryGraph();
    }

    /**
     * I should be able to upload a file to a read/write federated filesystem.
     **/
    @Ignore("Enabled once the FedoraFileSystemConnector becomes readable/writable")
    public void testUploadToProjection() throws IOException {
        // upload file to federated filesystem using rest api
        final String pid = getRandomUniquePid();
        final String uploadLocation = serverAddress + "files/" + pid + "/ds1";
        final String uploadContent = "abc123";
        logger.debug("Uploading to federated filesystem via rest api: " + uploadLocation);
        final HttpResponse response = createDatastream("files/" + pid, "ds1", uploadContent);
        final String actualLocation = response.getFirstHeader("Location").getValue();
        assertEquals("Wrong URI in Location header", uploadLocation, actualLocation);

        // validate content
        final HttpGet get = new HttpGet(uploadLocation);
        final HttpResponse getResponse = client.execute(get);
        final String actualContent = EntityUtils.toString( getResponse.getEntity() );
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
        assertEquals("Content doesn't match", actualContent, uploadContent);

        // validate object profile
        final HttpGet objGet = new HttpGet(serverAddress + "files/" + pid);
        final HttpResponse objResponse = client.execute(objGet);
        assertEquals(OK.getStatusCode(), objResponse.getStatusLine().getStatusCode());
    }


    /**
     * I should be able to link to content on a federated filesystem.
     **/
    @Test
    public void testFederatedDatastream() throws IOException {
        final String federationAddress = serverAddress + "files/FileSystem1/ds1";
        final String repoObj = getRandomUniquePid();
        final String linkingAddress = serverAddress + repoObj;

        // create an object in the repository
        final HttpPut put = new HttpPut(linkingAddress);
        assertEquals(201, getStatus(put));

        // link from the object to the content of the file on the federated filesystem
        final String sparql = "insert data { <> <http://some-vocabulary#hasExternalContent> "
                + "<" + federationAddress + "> . }";
        final HttpPatch patch = new HttpPatch(serverAddress + repoObj);
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(sparql.getBytes()));
        patch.setEntity(e);
        assertEquals("Couldn't link to external datastream!", 204, getStatus(patch));
    }


    @Test
    @Ignore("https://www.pivotaltracker.com/story/show/59240160")
    public void testPaging() throws Exception {
        // create a node with 4 children
        final String pid = getRandomUniquePid();
        final Node parent = createResource(serverAddress + pid).asNode();
        createObject(pid);
        createObject(pid + "/child1");
        createObject(pid + "/child2");
        createObject(pid + "/child3");
        createObject(pid + "/child4");

        // get first page
        final HttpGet firstGet = new HttpGet(serverAddress + pid + "?limit=2");
        final HttpResponse firstResponse = execute(firstGet);
        final GraphStore firstGraph = getGraphStore(firstResponse);

        // count children in response graph
        int firstChildCount = 0;
        Iterator<Quad> it = firstGraph.find(ANY,parent,HAS_CHILD.asNode(),ANY);
        for ( ; it.hasNext(); firstChildCount++ ) {
            logger.debug( "Found child: {}", it.next() );
        }
        assertEquals("Should have two children!", 2, firstChildCount);


        // count children in response graph
        int firstContainsCount = 0;
        it = firstGraph.find(ANY,parent,CONTAINS.asNode(),ANY);
        for ( ; it.hasNext(); firstContainsCount++ ) {
            logger.debug( "Found child: {}", it.next() );
        }
        assertEquals("Should have two children!", 2, firstContainsCount);

        // collect link headers
        final Collection<String> firstLinks = getLinkHeaders(firstResponse);

        // it should have a first page link
        assertTrue("Didn't find first page header!",firstLinks.contains("<" + serverAddress + pid
                + "?limit=2&amp;offset=0>;rel=\"first\""));
        assertTrue("Didn't find first page triple!", firstGraph.contains(ANY, ANY, FIRST_PAGE.asNode(),
                createResource(serverAddress + pid + "?limit=2&amp;offset=0").asNode()));

        // it should have a next page link
        assertTrue("Didn't find next page header!", firstLinks.contains("<" + serverAddress + pid
                + "?limit=2&amp;offset=2>;rel=\"next\""));
        assertTrue("Didn't find next page triple!", firstGraph.contains(ANY, ANY, NEXT_PAGE.asNode(),
                createResource(serverAddress + pid + "?limit=2&amp;offset=2").asNode()));


        // get second page
        final HttpGet nextGet = new HttpGet(serverAddress + pid + "?limit=2&offset=2");
        final HttpResponse nextResponse = execute(nextGet);
        final GraphStore nextGraph = getGraphStore(nextResponse);

        // it should have two inlined resources
        int nextChildCount = 0;
        for (it = nextGraph.find(ANY,parent,HAS_CHILD.asNode(),ANY); it.hasNext(); nextChildCount++ ) {
            logger.debug( "Found child: {}", it.next() );
        }
        assertEquals("Should have two children!", 2, nextChildCount);

        // collect link headers
        final Collection<String> nextLinks = getLinkHeaders(nextResponse);

        // it should have a first page link
        assertTrue("Didn't find first page header!", nextLinks.contains("<" + serverAddress + pid
                + "?limit=2&amp;offset=0>;rel=\"first\""));
        assertTrue("Didn't find first page triple!", nextGraph.contains(ANY, ANY, FIRST_PAGE.asNode(),
                createResource(serverAddress + pid + "?limit=2&amp;offset=0").asNode()));

        // it should not have a next page link
        for ( final String link : nextLinks ) {
            assertFalse("Should not have next page header!", link.contains("rel=\"next\""));
        }
        assertFalse("Should not have next pagiple!", nextGraph.contains(ANY, ANY, NEXT_PAGE.asNode(), ANY));
    }

    @Test
    public void testLinkedDeletion() throws Exception {
        final String linkedFrom = UUID.randomUUID().toString();
        final String linkedTo = UUID.randomUUID().toString();
        createObject(linkedFrom);
        createObject(linkedTo);

        final String sparql = "insert data { <" + serverAddress + linkedFrom + "> "
                + "<http://some-vocabulary#isMemberOfCollection> "
                + "<" + serverAddress + linkedTo + "> . }";
        final HttpPatch patch = new HttpPatch(serverAddress + linkedFrom);
        patch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(sparql.getBytes()));
        patch.setEntity(e);
        assertEquals("Couldn't link resources!", 204, getStatus(patch));

        final HttpDelete delete = new HttpDelete(serverAddress + linkedTo);
        assertEquals("Error deleting linked-to!", 204, getStatus(delete));

        final HttpGet get = new HttpGet(serverAddress + linkedFrom);
        assertEquals("Linked to should still exist!", 200, getStatus(get));
    }

    /**
     * When I make changes to a resource in a federated filesystem, the parent
     * folder's Last-Modified header should be updated.
     **/
    @Test
    public void testLastModifiedUpdatedAfterUpdates() throws Exception {

        // create directory containing a file in filesystem
        final File fed = new File("target/test-classes/test-objects");
        final String id = getRandomUniquePid();
        final File dir = new File( fed, id );
        final File child = new File( dir, "child" );
        final long timestamp1 = System.currentTimeMillis();
        dir.mkdir();
        child.mkdir();
        Thread.sleep(2000);

        // check Last-Modified header is current
        final HttpHead head1 = new HttpHead(serverAddress + "files/" + id);
        final HttpResponse resp1 = client.execute(head1);
        assertEquals( 200, resp1.getStatusLine().getStatusCode() );
        final long lastmod1 = headerFormat.parse(resp1.getFirstHeader("Last-Modified").getValue()).getTime();
        assertTrue( (timestamp1 - lastmod1) < 1000 ); // because rounding

        // remove the file and wait for the TTL to expire
        final long timestamp2 = System.currentTimeMillis();
        child.delete();
        Thread.sleep(2000);

        // check Last-Modified header is updated
        final HttpHead head2 = new HttpHead(serverAddress + "files/" + id);
        final HttpResponse resp2 = client.execute(head2);
        assertEquals( 200, resp2.getStatusLine().getStatusCode() );
        final long lastmod2 = headerFormat.parse(resp2.getFirstHeader("Last-Modified").getValue()).getTime();
        assertTrue( (timestamp2 - lastmod2) < 1000 ); // because rounding

        assertFalse("Last-Modified headers should have changed", lastmod1 == lastmod2);
    }

    @Test
    public void testUpdateObjectWithSpaces() throws Exception {
        final String id = getRandomUniquePid() + " 2";
        final HttpResponse createResponse = createObject(id);
        final String subjectURI = createResponse.getFirstHeader("Location").getValue();
        final HttpPatch updateObjectGraphMethod = new HttpPatch(subjectURI);
        updateObjectGraphMethod.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"test\" } WHERE {}".getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
    }

    @Test
    public void testCreatedAndModifiedDates() throws Exception {
        final HttpResponse createResponse = createObject("");
        final String location = createResponse.getFirstHeader("Location").getValue();
        final HttpGet getObjMethod = new HttpGet(location);
        final HttpResponse response = client.execute(getObjMethod);
        final GraphStore results = getGraphStore(response);
        final Model model = createModelForGraph(results.getDefaultGraph());
        final Resource nodeUri = createResource(location);

        final String lastmodString = response.getFirstHeader("Last-Modified").getValue();
        headerFormat.parse(lastmodString);
        final Date createdDateTriples = getDateFromModel( model, nodeUri,
                createProperty(REPOSITORY_NAMESPACE + "created"));
        final Date lastmodDateTriples = getDateFromModel( model, nodeUri,
                createProperty(REPOSITORY_NAMESPACE + "lastModified"));
        assertNotNull( createdDateTriples );
        assertEquals( lastmodString, headerFormat.format(createdDateTriples) );
        assertNotNull( lastmodDateTriples );
        assertEquals( lastmodString, headerFormat.format(lastmodDateTriples) );
    }

    @Test
    public void testLdpContainerInteraction() throws Exception {

        final String id = getRandomUniquePid();
        final HttpResponse object = createObject(id);
        final String location = object.getFirstHeader("Location").getValue();
        createObject(id + "/t");
        addMixin(id + "/t", DIRECT_CONTAINER.getURI());

        final HttpPatch patch = new HttpPatch(location + "/t");
        final String sparql = "INSERT DATA { "
                + "<> <" + MEMBERSHIP_RESOURCE + "> <" + location + "> .\n"
                + "<> <" + HAS_MEMBER_RELATION + "> <info:some/relation> .\n"
                + " }";
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(IOUtils.toInputStream(sparql));

        patch.setEntity(entity);

        assertEquals("Expected patch to succeed", 204, getStatus(patch));

        createObject(id + "/b");
        addMixin(id + "/b", DIRECT_CONTAINER.getURI());

        final HttpPatch bPatch = new HttpPatch(location + "/b");
        final String bSparql = "INSERT DATA { "
                + "<> <" + MEMBERSHIP_RESOURCE + "> <" + location + "> .\n"
                + "<> <" + HAS_MEMBER_RELATION + "> <info:some/another-relation> .\n"
                + " }";
        final BasicHttpEntity bEntity = new BasicHttpEntity();
        bEntity.setContent(IOUtils.toInputStream(bSparql));

        bPatch.setEntity(bEntity);


        assertEquals("Expected patch to succeed", 204, getStatus(bPatch));

        createObject(id + "/t/1");
        createObject(id + "/b/1");

        final HttpGet getObjMethod = new HttpGet(location);
        final HttpResponse response = client.execute(getObjMethod);
        final GraphStore graphStore = getGraphStore(response);

        assertTrue("Expected to have container t", graphStore.contains(Node.ANY,
                NodeFactory.createURI(location),
                NodeFactory.createURI(RdfLexicon.LDP_NAMESPACE + "contains"),
                NodeFactory.createURI(location + "/t")
        ));

        assertTrue("Expected to have container b", graphStore.contains(Node.ANY,
                NodeFactory.createURI(location),
                NodeFactory.createURI(RdfLexicon.LDP_NAMESPACE + "contains"),
                NodeFactory.createURI(location + "/b")
        ));

        assertTrue("Expected member relation", graphStore.contains(Node.ANY,
                NodeFactory.createURI(location),
                NodeFactory.createURI("info:some/relation"),
                NodeFactory.createURI(location + "/t/1")
                ));

        assertTrue("Expected other member relation", graphStore.contains(Node.ANY,
                NodeFactory.createURI(location),
                NodeFactory.createURI("info:some/another-relation"),
                NodeFactory.createURI(location + "/b/1")
        ));

    }

    @Test
    public void testLdpIndirectContainerInteraction() throws Exception {

        // Create resource (object)
        final String resourceId = getRandomUniquePid();
        final HttpResponse rResponse = createObject(resourceId);
        final String resource = rResponse.getFirstHeader("Location").getValue();

        // Create container (c0)
        final String containerId = getRandomUniquePid();
        final HttpResponse cResponse = createObject(containerId);
        final String container = cResponse.getFirstHeader("Location").getValue();

        // Create indirect container (c0/members)
        final String indirectContainerId = containerId + "/t";
        final HttpResponse icResponse = createObject(indirectContainerId);
        final String indirectContainer = icResponse.getFirstHeader("Location").getValue();
        addMixin(indirectContainerId, INDIRECT_CONTAINER.getURI());

        // Add LDP properties to indirect container
        final HttpPatch patch = new HttpPatch(indirectContainer);
        final String sparql = "INSERT DATA { "
                + "<> <" + MEMBERSHIP_RESOURCE + "> <" + container + "> .\n"
                + "<> <" + HAS_MEMBER_RELATION + "> <info:some/relation> .\n"
                + "<> <" + LDP_NAMESPACE + "insertedContentRelation> <info:proxy/for> .\n"
                + " }";
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(IOUtils.toInputStream(sparql));

        patch.setEntity(entity);

        assertEquals("Expected patch to succeed", 204, getStatus(patch));

        // Add indirect resource to indirect container
        final HttpPost postIndirectResource = postObjMethod(indirectContainerId);
        final String irRdf =
                "<> <info:proxy/in>  <" + container + "> ;\n" +
                "   <info:proxy/for> <" + resource  + "> .";
        final BasicHttpEntity irEntity = new BasicHttpEntity();
        irEntity.setContent(IOUtils.toInputStream(irRdf));
        postIndirectResource.setEntity(irEntity);
        postIndirectResource.setHeader("Content-Type", "text/turtle");

        final HttpResponse postResponse = client.execute(postIndirectResource);
        final String indirectResource = postResponse.getFirstHeader("Location").getValue();

        assertEquals("Expected post to succeed", 201, postResponse.getStatusLine().getStatusCode());

        // Ensure container has been updated with relationship... indirectly
        final HttpGet getContainer0 = new HttpGet(container);
        final HttpResponse getResponse = client.execute(getContainer0);
        final GraphStore graphStore = getGraphStore(getResponse);

        assertTrue("Expected to have indirect container", graphStore.contains(Node.ANY,
                NodeFactory.createURI(container),
                NodeFactory.createURI(RdfLexicon.LDP_NAMESPACE + "contains"),
                NodeFactory.createURI(indirectContainer)
        ));

        assertTrue("Expected to have resource: " + graphStore.toString(), graphStore.contains(Node.ANY,
                NodeFactory.createURI(container),
                NodeFactory.createURI("info:some/relation"),
                NodeFactory.createURI(resource)
        ));

        // Remove indirect resource
        final HttpDelete delete = new HttpDelete(indirectResource);
        final HttpResponse deleteResponse = client.execute(delete);

        assertEquals("Expected delete to succeed", 204, deleteResponse.getStatusLine().getStatusCode());

        // Ensure container has been updated with relationship... indirectly
        final HttpGet getContainer1 = new HttpGet(container);
        final HttpResponse getResponse1 = client.execute(getContainer1);
        final GraphStore graphStore1 = getGraphStore(getResponse1);

        assertFalse("Expected NOT to have resource: " + graphStore1, graphStore1.contains(Node.ANY,
                NodeFactory.createURI(container),
                NodeFactory.createURI("info:some/relation"),
                NodeFactory.createURI(resource)
        ));

    }

    @Test
    public void testWithHashUris() throws IOException {
        final HttpPost method = postObjMethod("");
        method.addHeader("Content-Type", "text/turtle");
        final BasicHttpEntity entity = new BasicHttpEntity();
        final String rdf = "<> <info:some-predicate> <#abc> .\n" +
                "<#abc> <info:rubydora#label> \"asdfg\" .";
        entity.setContent(new ByteArrayInputStream(rdf.getBytes()));
        method.setEntity(entity);
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);

        final String location = response.getFirstHeader("Location").getValue();

        final HttpGet httpGet = new HttpGet(location);

        final GraphStore graphStore = getGraphStore(httpGet);

        assertTrue(graphStore.contains(ANY, createResource(location).asNode(),
                createProperty("info:some-predicate").asNode(), createResource(location + "#abc").asNode()));

        assertTrue(graphStore.contains(ANY, createResource(location + "#abc").asNode(),
                createProperty("info:rubydora#label").asNode(), createLiteral("asdfg")));


    }

    @Test
    public void testCreateAndReplaceGraphMinimal() throws Exception {
        LOGGER.trace("Entering testCreateAndReplaceGraphMinimal()...");
        final String pid = getRandomUniquePid();

        final HttpPost httpPost = postObjMethod("/");
        httpPost.addHeader("Slug", pid);
        httpPost.addHeader("Content-Type", "text/turtle");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(IOUtils.toInputStream("<> <" + DC_11.title.toString() + "> \"abc\""));
        httpPost.setEntity(e);
        final HttpResponse response = client.execute(httpPost);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);

        final String subjectURI = response.getFirstHeader("Location").getValue();

        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "text/turtle");
        replaceMethod.addHeader("Prefer", "handling=lenient; received=\"minimal\"");

        final BasicHttpEntity replacement = new BasicHttpEntity();
        replacement.setContent(IOUtils.toInputStream("<> <" + DC_11.title.toString() + "> \"xyz\""));
        replaceMethod.setEntity(replacement);
        final HttpResponse replaceResponse = client.execute(replaceMethod);
        assertEquals(204, replaceResponse.getStatusLine().getStatusCode());

        final HttpGet get = new HttpGet(subjectURI);
        get.addHeader("Prefer", "return=minimal");
        final GraphStore graphStore = getGraphStore(get);
        assertTrue(graphStore.contains(ANY, ANY, DC_11.title.asNode(), createLiteral("xyz")));
        LOGGER.trace("Done with testCreateAndReplaceGraphMinimal().");
    }

    @Test
    @Ignore("This test needs manual intervention to decide how \"good\" the graph looks")
    public void testGraphShouldNotBeTooLumpy() throws Exception {

        final String pid = getRandomUniquePid();

        final HttpPut httpPut = putObjMethod(pid);
        httpPut.addHeader("Content-Type", "text/turtle");
        httpPut.setEntity(new StringEntity("<> a <" + DIRECT_CONTAINER.getURI() + ">;" +
                "    <" + MEMBERSHIP_RESOURCE.getURI() + "> <> ;" +
                "    <" + HAS_MEMBER_RELATION.getURI() + "> <" + LDP_NAMESPACE + "member> ;" +
                "    <info:x> <#hash-uri> ;" +
                "    <info:x> [ <" + DC_11.title + "> \"xyz\" ] . " +
                "<#hash-uri>  <" + DC_11.title + "> \"some-hash-uri\" ."));

       /* final HttpResponse response = client.execute(httpPut);
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), status);

        final String subjectURI = response.getFirstHeader("Location").getValue();

        final HttpGet get = new HttpGet(subjectURI);
        final HttpResponse getResponse = client.execute(get);

        final String s = EntityUtils.toString(getResponse.getEntity());*/

    }

    @Test
    public void testEmbeddedChildResources() throws Exception {
        final String pid = getRandomUniquePid();
        final String binaryId = "binary0";

        final HttpPut httpPutContainer = putObjMethod(pid);
        final HttpResponse responseContainer = client.execute(httpPutContainer);
        assertEquals(201, responseContainer.getStatusLine().getStatusCode());

        final HttpPut httpPutBinary = putDSMethod(pid, binaryId, "some test content");
        final HttpResponse responseBinary = client.execute(httpPutBinary);
        assertEquals(201, responseBinary.getStatusLine().getStatusCode());

        final HttpPatch httpPatch = patchObjMethod(pid + "/" + binaryId + "/fcr:metadata");
        httpPatch.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <> <http://purl.org/dc/elements/1.1/title> 'this is a title' } WHERE {}").getBytes()));
        httpPatch.setEntity(e);

        final HttpResponse responsePatch = client.execute(httpPatch);
        assertEquals(NO_CONTENT.getStatusCode(), responsePatch.getStatusLine().getStatusCode());

        final HttpGet httpGet = getObjMethod(pid);
        httpGet.setHeader("Prefer",
                "return=representation; include=\"http://fedora.info/definitions/v4/repository#EmbedResources\"");

        final GraphStore graphStore = getGraphStore(httpGet);
        assertTrue("Property on child binary should be found!" + graphStore, graphStore.contains(
                ANY,
                createResource(serverAddress + pid + "/" + binaryId + "/fcr:metadata").asNode(),
                createProperty("http://purl.org/dc/elements/1.1/title").asNode(),
                createLiteral("this is a title")));
    }

    @Test
    public void testExternalMessageBody() throws Exception {

        // we need a client that won't automatically follow redirects
        final HttpClient client = HttpClientBuilder.create().disableRedirectHandling().build();

        final String pid = getRandomUniquePid();

        final HttpPut httpPut = putObjMethod(pid);
        httpPut.addHeader("Content-Type", "message/external-body; access-type=URL; " +
                "URL=\"http://www.example.com/file\"");

        final HttpResponse response = client.execute(httpPut);
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response!", CREATED.getStatusCode(), status);

        final String subjectURI = response.getFirstHeader("Location").getValue();

        final HttpGet get = new HttpGet(subjectURI);
        final HttpResponse getResponse = client.execute(get);

        LOGGER.error(EntityUtils.toString(getResponse.getEntity()));
        assertEquals(TEMPORARY_REDIRECT.getStatusCode(), getResponse.getStatusLine().getStatusCode());
        assertEquals("http://www.example.com/file", getResponse.getFirstHeader("Location").getValue());
    }

    private Date getDateFromModel(final Model model, final Resource subj, final Property pred) throws Exception {
        final StmtIterator stmts = model.listStatements(subj, pred, (String) null);
        if (stmts.hasNext()) {
            return tripleFormat.parse(stmts.nextStatement().getString());
        }
        return null;
    }


    private static Collection<String> getLinkHeaders(final HttpResponse response) {
        return transform(copyOf(response.getHeaders("Link")), new Function<Header, String>() {

            @Override
            public String apply(final Header h) {
                return h.getValue();
            }
        });
    }


}
