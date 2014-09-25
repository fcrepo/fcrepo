/**
 * Copyright 2014 DuraSpace, Inc.
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

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.riot.Lang;
import org.junit.Test;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.WebContent.contentTypeN3;
import static org.apache.jena.riot.WebContent.contentTypeN3Alt1;
import static org.apache.jena.riot.WebContent.contentTypeN3Alt2;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.apache.jena.riot.WebContent.contentTypeTurtle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 */
public class FedoraLdpIT extends AbstractResourceIT {

    private static final Logger LOGGER = getLogger(FedoraLdpIT.class);


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
    public void testHeadDatastream() throws Exception {
        final String pid = getRandomUniquePid();

        createDatastream(pid, "x", pid);

        final String location = serverAddress + pid + "/x";
        final HttpHead headObjMethod = new HttpHead(location);
        assertEquals(200, getStatus(headObjMethod));
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

    private void assertContainerOptionsHeaders(final HttpResponse httpResponse) {
        assertRdfOptionsHeaders(httpResponse);
        final List<String> methods = headerValues(httpResponse,"Allow");
        assertTrue("Should allow POST", methods.contains(HttpPost.METHOD_NAME));

        final List<String> postTypes = headerValues(httpResponse,"Accept-Post");
        assertTrue("POST should support application/sparql-update", postTypes.contains(contentTypeSPARQLUpdate));
        assertTrue("POST should support text/turtle", postTypes.contains(contentTypeTurtle));
        assertTrue("POST should support text/rdf+n3", postTypes.contains(contentTypeN3));
        assertTrue("POST should support application/n3", postTypes.contains(contentTypeN3Alt1));
        assertTrue("POST should support text/n3", postTypes.contains(contentTypeN3Alt2));
        assertTrue("POST should support application/rdf+xml", postTypes.contains(contentTypeRDFXML));
        assertTrue("POST should support application/n-triples", postTypes.contains(contentTypeNTriples));
        assertTrue("POST should support multipart/form-data", postTypes.contains("multipart/form-data"));
    }

    private void assertRdfOptionsHeaders(final HttpResponse httpResponse) {
        final List<String> methods = headerValues(httpResponse,"Allow");
        assertTrue("Should allow PATCH", methods.contains(HttpPatch.METHOD_NAME));
        assertTrue("Should allow MOVE", methods.contains(HttpMove.METHOD_NAME));
        assertTrue("Should allow COPY", methods.contains(HttpCopy.METHOD_NAME));

        final List<String> patchTypes = headerValues(httpResponse,"Accept-Patch");
        assertTrue("PATCH should support application/sparql-update", patchTypes.contains(contentTypeSPARQLUpdate));
        assertResourceOptionsHeaders(httpResponse);
    }

    private void assertResourceOptionsHeaders(final HttpResponse httpResponse) {
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


        /**
         * @throws IllegalArgumentException if the uri is invalid.
         */
        public HttpCopy(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

    }

    @NotThreadSafe // HttpRequestBase is @NotThreadSafe
    private class HttpMove extends HttpRequestBase {

        public final static String METHOD_NAME = "MOVE";


        /**
         * @throws IllegalArgumentException if the uri is invalid.
         */
        public HttpMove(final String uri) {
            super();
            setURI(URI.create(uri));
        }

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
    public void testGetNonRDFSourceDescription() throws Exception {
        final String pid = getRandomUniquePid();

        createDatastream(pid, "x", "some content");

        final HttpGet httpGet = new HttpGet(serverAddress + pid + "/x/fcr:metadata");
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
        assertEquals("Object wasn't really deleted!", 404,
                getStatus(new HttpGet(location)));
    }


    @Test
    public void testDeleteBinary() throws Exception {
        final String pid = getRandomUniquePid();

        createDatastream(pid, "x", "some content");

        final String location = serverAddress + pid + "/x";
        assertEquals(204, getStatus(new HttpDelete(location)));
        assertEquals("Object wasn't really deleted!", 404,
                getStatus(new HttpGet(location)));
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
    public void testReplaceGraph() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final String subjectURI = serverAddress + pid;
        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("<" + subjectURI + "> <info:rubydora#label> \"asdfg\"")
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
    public void testRoundTripReplaceGraph() throws Exception {

        final String pid = getRandomUniquePid();

        createObject(pid);

        final String subjectURI = serverAddress + pid;

        final HttpGet getObjMethod = new HttpGet(subjectURI);
        getObjMethod.addHeader("Accept", "text/turtle");
        getObjMethod.addHeader("Prefer", "return=minimal");
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
    public void testPutDatastream() throws Exception {

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
    }

    @Test
    public void testPutDatastreamContentOnObject() throws Exception {
        final String content = "foo";
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPut put = new HttpPut(serverAddress + pid);
        put.setEntity(new StringEntity(content));
        final HttpResponse response = execute(put);
        assertEquals("Expected 415 response code when PUTing content on an object (as opposed to a datastream).",
                415, response.getStatusLine().getStatusCode());
    }

}