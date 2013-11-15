/**
 * Copyright 2013 DuraSpace, Inc.
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
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.UUID.randomUUID;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static nu.validator.htmlparser.common.DoctypeExpectation.NO_DOCTYPE_ERRORS;
import static nu.validator.htmlparser.common.XmlViolationPolicy.ALLOW;
import static org.apache.http.impl.client.cache.CacheConfig.DEFAULT;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.jcr.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.RdfLexicon.HAS_OBJECT_COUNT;
import static org.fcrepo.kernel.RdfLexicon.HAS_OBJECT_SIZE;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_IDENTIFIER;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Iterator;
import java.util.UUID;

import javax.jcr.RepositoryException;

import nu.validator.htmlparser.sax.HtmlParser;
import nu.validator.saxtree.TreeBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;

public class FedoraNodesIT extends AbstractResourceIT {

    @Test
    public void testIngest() throws Exception {

        final String pid = randomUUID().toString();

        final HttpResponse response = createObject(pid);

        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals("Got wrong Location header for ingest!", serverAddress
                + pid, location);
    }

    @Test
    public void testIngestWithNew() throws Exception {
        final HttpResponse response = createObject("");
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        assertEquals("Didn't get a CREATED response! Got content:\n" + content,
                CREATED.getStatusCode(), status);
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        assertTrue("new object did not mint a PID", !content
                .endsWith("/fcr:new"));
        final String location = response.getFirstHeader("Location").getValue();
        assertNotEquals(serverAddress + "/objects", location);

        assertEquals("Object wasn't created!", OK.getStatusCode(),
                getStatus(new HttpGet(location)));
    }

    @Test
    public void testIngestWithNewAndSparqlQuery() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(
                ("INSERT { <> <http://purl.org/dc/terms/title> \"this is a title\" } WHERE {}")
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
    }

    @Test
    public void testIngestWithNewAndGraph() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity entity = new BasicHttpEntity();
        final String rdf = "<> <http://purl.org/dc/terms/title> \"this is a title\".";
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

    }

    @Test
    public void testIngestWithSlug() throws Exception {
        final HttpPost method = postObjMethod("");
        method.addHeader("Slug", randomUUID().toString());
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
        assertTrue(location.endsWith("fcr:content"));
        assertEquals("Object wasn't created!", OK.getStatusCode(),
                getStatus(new HttpGet(location)));
    }

    @Test
    public void testDeleteObject() throws Exception {
        assertEquals(CREATED.getStatusCode(),
                getStatus(postObjMethod("FedoraObjectsTest3")));
        assertEquals(204, getStatus(new HttpDelete(serverAddress
                + "FedoraObjectsTest3")));
        assertEquals("Object wasn't really deleted!", 404,
                getStatus(new HttpGet(serverAddress + "FedoraObjectsTest3")));
    }

    @Test
    public void testGetDatastream() throws Exception {

        final String pid = UUID.randomUUID().toString();

        createObject(pid);
        createDatastream(pid, "ds1", "foo");

        final HttpResponse response =
            execute(new HttpGet(serverAddress + pid + "/ds1"));
        assertEquals(EntityUtils.toString(response.getEntity()), 200, response
                .getStatusLine().getStatusCode());
        assertEquals(TURTLE, response.getFirstHeader("Content-Type").getValue());
    }

    @Test
    public void testDeleteDatastream() throws Exception {
        final String pid = UUID.randomUUID().toString();

        createObject(pid);
        createDatastream(pid, "ds1", "foo");

        final HttpDelete dmethod =
            new HttpDelete(serverAddress + pid + "/ds1");
        assertEquals(204, getStatus(dmethod));

        final HttpGet method_test_get =
            new HttpGet(serverAddress +  pid + "/ds1");
        assertEquals(404, getStatus(method_test_get));
    }

    @Test
    public void testGetRepositoryGraph() throws Exception {
        final HttpGet getObjMethod = new HttpGet(serverAddress);
        final GraphStore graphStore = getGraphStore(getObjMethod);
        logger.debug("Retrieved repository graph:\n" + graphStore.toString());

        assertTrue("expected to find the root node data", graphStore.contains(
                ANY, ANY, HAS_PRIMARY_TYPE.asNode(), createLiteral(ROOT)));

    }

    @Test
    public void testGetObjectGraphHtml() throws Exception {
        createObject("FedoraDescribeTestGraph");
        final HttpGet getObjMethod =
                new HttpGet(serverAddress + "FedoraDescribeTestGraph");
        getObjMethod.addHeader("Accept", "text/html");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved: {}", content);
    }

    @Test
    public void testGetObjectGraph() throws Exception {
        logger.debug("Entering testGetObjectGraph()...");
        createObject("FedoraDescribeTestGraph");

        final HttpGet getObjMethod =
                new HttpGet(serverAddress + "FedoraDescribeTestGraph");
        getObjMethod.addHeader("Accept", "application/rdf+xml");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());

        final Model model = createDefaultModel();
        model.read(response.getEntity().getContent(), null);
        try (final Writer w = new StringWriter()) {
            model.write(w);
            logger.debug(
                    "Retrieved object graph for testGetObjectGraph():\n {}", w);
        }

        assertEquals("application/sparql-update", response.getFirstHeader("Accept-Patch").getValue());
        assertEquals("http://www.w3.org/ns/ldp/Resource;rel=\"type\"", response.getFirstHeader("Link").getValue());
        final Resource nodeUri = createResource(serverAddress + "FedoraDescribeTestGraph");
        assertTrue("Didn't find inlined resources!", model.contains(nodeUri,
                createProperty("http://www.w3.org/ns/ldp#inlinedResource")));

        assertTrue("Didn't find an expected triple!", model.contains(nodeUri,
                createProperty(REPOSITORY_NAMESPACE + "mixinTypes"),
                createPlainLiteral("fedora:object"))
                );

        logger.debug("Leaving testGetObjectGraph()...");
    }

    @Test
    public void testGetObjectGraphWithChildren() throws Exception {
        createObject("FedoraDescribeWithChildrenTestGraph");
        createObject("FedoraDescribeWithChildrenTestGraph/a");
        createObject("FedoraDescribeWithChildrenTestGraph/b");
        createObject("FedoraDescribeWithChildrenTestGraph/c");
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + "FedoraDescribeWithChildrenTestGraph");
        getObjMethod.addHeader("Accept", "application/rdf+xml");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                                             .getStatusCode());
        final Model model = createDefaultModel();
        model.read(response.getEntity().getContent(), null);
        try (final Writer w = new StringWriter()) {
            model.write(w);
            logger.debug(
                    "Retrieved object graph for testGetObjectGraphWithChildren():\n {}",
                    w);
        }
        final Resource subjectUri =
            createResource(serverAddress
                    + "FedoraDescribeWithChildrenTestGraph");
        assertTrue(
                "Didn't find child node!",
                model.contains(
                        subjectUri,
                createProperty(REPOSITORY_NAMESPACE + "hasChild"),
                createResource(serverAddress
                        + "FedoraDescribeWithChildrenTestGraph/c")));
        logger.debug("Found Link header: {}", response.getFirstHeader("Link")
                .getValue());
        assertEquals("http://www.w3.org/ns/ldp/Resource;rel=\"type\"", response
                .getFirstHeader("Link").getValue());
    }

    @Test
    public void testGetObjectGraphNonMemberProperties() throws Exception {
        createObject("FedoraDescribeTestGraph");
        final HttpGet getObjMethod =
            new HttpGet(serverAddress + "FedoraDescribeTestGraph?non-member-properties");
        getObjMethod.addHeader("Accept", "application/n-triples");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                                             .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        logger.debug("Retrieved object graph:\n" + content);

        assertFalse(
                "Didn't expect inlined resources",
                compile(
                        "<"
                                + serverAddress
                                + "FedoraDescribeTestGraph> <http://www.w3.org/ns/ldp#inlinedResource>",
                        DOTALL).matcher(content).find());

    }

    @Test
    public void testGetObjectGraphByUUID() throws Exception {
        createObject("FedoraDescribeTestGraphByUuid");

        final HttpGet getObjMethod =
                new HttpGet(serverAddress +
                        "FedoraDescribeTestGraphByUuid");
        final GraphStore graphStore = getGraphStore(getObjMethod);
        final Iterator<Quad> iterator =
            graphStore.find(ANY, createURI(serverAddress +
                        "FedoraDescribeTestGraphByUuid"),
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
    public void testUpdateObjectGraph() throws Exception {
        createObject("FedoraDescribeTestGraphUpdate");
        final HttpPatch updateObjectGraphMethod =
            new HttpPatch(serverAddress + "FedoraDescribeTestGraphUpdate");
        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + serverAddress + "FedoraDescribeTestGraphUpdate> <http://purl.org/dc/terms/identifier> \"this is an identifier\" } WHERE {}")
                        .getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());

    }

    @Test
    public void testUpdateAndReplaceObjectGraph() throws Exception {
        createObject("FedoraDescribeTestGraphReplace");
        final String subjectURI =
            serverAddress + "FedoraDescribeTestGraphReplace";
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

        final HttpGet getObjMethod =

        new HttpGet(subjectURI);

        getObjMethod.addHeader("Accept", "application/n-triples");
        final HttpResponse getResponse = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine()
                .getStatusCode());
        final String content = EntityUtils.toString(getResponse.getEntity());
        logger.debug("Retrieved object graph:\n" + content);

        assertFalse("Found a triple we thought we deleted.", compile(
                "<" + subjectURI + "> <info:rubydora#label> \"asdfg\" \\.",
                DOTALL).matcher(content).find());

    }

    @Test
    public void testUpdateObjectGraphWithProblems() throws Exception {
        client.execute(postObjMethod("FedoraDescribeTestGraphUpdateBad"));
        final String subjectURI =
                serverAddress + "FedoraDescribeTestGraphUpdateBad";
        final HttpPatch getObjMethod = new HttpPatch(subjectURI);
        getObjMethod.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + subjectURI + "> <" + REPOSITORY_NAMESPACE + "uuid> \"00e686e2-24d4-40c2-92ce-577c0165b158\" } WHERE {}\n")
                        .getBytes()));
        getObjMethod.setEntity(e);
        final HttpResponse response = client.execute(getObjMethod);
        if (response.getStatusLine().getStatusCode() != 403
                && response.getEntity() != null) {
            final String content = EntityUtils.toString(response.getEntity());
            logger.error("Got unexpected update response:\n" + content);
        }
        assertEquals(403, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testReplaceGraph() throws Exception {
        client.execute(postObjMethod("FedoraReplaceGraph"));
        final String subjectURI =
            serverAddress + "FedoraReplaceGraph";
        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("<" + subjectURI + "> <info:rubydora#label> \"asdfg\"")
                        .getBytes()));
        replaceMethod.setEntity(e);
        final HttpResponse response = client.execute(replaceMethod);
        assertEquals(204, response.getStatusLine().getStatusCode());


        final HttpGet getObjMethod = new HttpGet(subjectURI);

        getObjMethod.addHeader("Accept", "application/rdf+xml");
        final HttpResponse getResponse = client.execute(getObjMethod);
        assertEquals(OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
        final Model model = createDefaultModel();
        model.read(getResponse.getEntity().getContent(), null);
        try (final Writer w = new StringWriter()) {
            model.write(w);
            logger.debug(
                    "Retrieved object graph for testReplaceGraph():\n {}", w);
        }
        assertTrue("Didn't find a triple we tried to create!", model.contains(
                createResource(subjectURI),
                createProperty("info:rubydora#label"),
                createPlainLiteral("asdfg")));


    }

    @Test
    @Ignore("waiting on MODE-1998")
    public void testRoundTripReplaceGraph() throws Exception {
        createObject("FedoraRoundTripGraph");

        final String subjectURI =
            serverAddress + "FedoraRoundTripGraph";

        final HttpGet getObjMethod = new HttpGet(subjectURI);
        getObjMethod.addHeader("Accept", "application/n3");
        final HttpResponse getResponse = client.execute(getObjMethod);

        final HttpPut replaceMethod = new HttpPut(subjectURI);
        replaceMethod.addHeader("Content-Type", "application/n3");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(getResponse.getEntity().getContent());
        replaceMethod.setEntity(e);
        final HttpResponse response = client.execute(replaceMethod);
        assertEquals(204, response.getStatusLine().getStatusCode());

    }

    @Test
    public void testDescribeSize() throws Exception {

        final String sizeNode = randomUUID().toString();

        GraphStore graphStore = getGraphStore(new HttpGet(serverAddress + ""));
        logger.debug("For testDescribeSize() first size retrieved repository graph:\n"
                + graphStore.toString());

        Iterator<Triple> iterator =
            graphStore.getDefaultGraph().find(ANY, HAS_OBJECT_SIZE.asNode(),
                    ANY);

        final String oldSize = (String) iterator.next().getObject().getLiteralValue();


        assertEquals(CREATED.getStatusCode(),
                getStatus(postObjMethod(sizeNode)));
        assertEquals(CREATED.getStatusCode(), getStatus(postDSMethod(sizeNode,
                "asdf", "1234")));

        graphStore = getGraphStore(new HttpGet(serverAddress + ""));
        logger.debug("For testDescribeSize() new size retrieved repository graph:\n"
                + graphStore.toString());

        iterator =
            graphStore.getDefaultGraph().find(ANY, HAS_OBJECT_SIZE.asNode(),
                    ANY);

        final String newSize = (String) iterator.next().getObject().getLiteralValue();

        logger.debug("Old size was: " + oldSize + " and new size was: "
                + newSize);
        assertTrue("No increment in size occurred when we expected one!",
                Integer.parseInt(oldSize) < Integer.parseInt(newSize));
    }

    @Test
    public void testDescribeCount() throws Exception {
        GraphStore graphStore = getGraphStore(new HttpGet(serverAddress + ""));
        logger.debug("For testDescribeCount() first count retrieved repository graph:\n"
                + graphStore.toString());

        Iterator<Triple> iterator =
            graphStore.getDefaultGraph().find(ANY, HAS_OBJECT_COUNT.asNode(),
                    ANY);

        final String oldSize = (String) iterator.next().getObject().getLiteralValue();

        assertEquals(CREATED.getStatusCode(),
                getStatus(postObjMethod("countNode")));
        assertEquals(CREATED.getStatusCode(), getStatus(postDSMethod(
                "countNode", "asdf", "1234")));

        graphStore = getGraphStore(new HttpGet(serverAddress + ""));
        logger.debug("For testDescribeCount() first count repository graph:\n"
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
        logger.debug("For testGetProjectedNode() retrieved graph:\n"
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
    public void testDescribeRdfCached() throws RepositoryException, IOException {
        final CloseableHttpClient specialClient =
            CachingHttpClientBuilder.create().setCacheConfig(DEFAULT).build();
        final String pid = "FedoraObjectsRdfTest2";
        final String path = "" + pid;
        specialClient.execute(new HttpPost(serverAddress + path));
        final HttpGet getObjMethod = new HttpGet(serverAddress + path);
        HttpResponse response = specialClient.execute(getObjMethod);
        assertEquals("Client didn't return a OK!", OK.getStatusCode(), response
                .getStatusLine().getStatusCode());
        final String lastModed =
            response.getFirstHeader("Last-Modified").getValue();
        final String etag = response.getFirstHeader("ETag").getValue();
        final HttpGet getObjMethod2 = new HttpGet(serverAddress + path);
        getObjMethod2.setHeader("If-Modified-Since", lastModed);
        getObjMethod2.setHeader("If-None-Match", etag);
        response = specialClient.execute(getObjMethod2);

        assertEquals("Client didn't return a NOT_MODIFIED!", NOT_MODIFIED
                .getStatusCode(), response.getStatusLine().getStatusCode());

    }

    @Test
    public void testValidHTMLForRepo() throws Exception {
        validateHTML("");
    }

    @Test
    public void testValidHTMLForObject() throws Exception {
        client.execute(new HttpPost(serverAddress + "testValidHTMLForObject"));
        validateHTML("testValidHTMLForObject");
    }

    @Test
    public void testValidHTMLForDS() throws Exception {
        client.execute(new HttpPost(serverAddress
                + "testValidHTMLForDS/ds/fcr:content"));
        validateHTML("testValidHTMLForDS/ds");
    }

    @Test
    public void testCopy() throws Exception {

        final String pid = randomUUID().toString();

        final HttpPost method = postObjMethod("");
        final HttpResponse response = client.execute(method);
        assertEquals(CREATED.getStatusCode(), response.getStatusLine()
                                                  .getStatusCode());

        final String location = response.getFirstHeader("Location").getValue();

        final HttpCopy request = new HttpCopy(location);
        request.addHeader("Destination", serverAddress + pid);
        client.execute(request);

        final HttpGet httpGet = new HttpGet(serverAddress + pid);

        final HttpResponse copiedResult = client.execute(httpGet);
        assertEquals(OK.getStatusCode(), copiedResult.getStatusLine().getStatusCode());

        final HttpResponse originalResult = client.execute(new HttpGet(location));
        assertEquals(OK.getStatusCode(), originalResult.getStatusLine().getStatusCode());
    }

    @Test
    public void testMove() throws Exception {

        final String pid = randomUUID().toString();

        final HttpPost method = postObjMethod("");
        final HttpResponse response = client.execute(method);
        assertEquals(CREATED.getStatusCode(), response.getStatusLine()
                                                  .getStatusCode());

        final String location = response.getFirstHeader("Location").getValue();

        final HttpMove request = new HttpMove(location);
        request.addHeader("Destination", serverAddress + pid);
        client.execute(request);

        final HttpGet httpGet = new HttpGet(serverAddress + pid);

        final HttpResponse copiedResult = client.execute(httpGet);
        assertEquals(OK.getStatusCode(), copiedResult.getStatusLine().getStatusCode());

        final HttpResponse originalResult = client.execute(new HttpGet(location));
        assertEquals(NOT_FOUND.getStatusCode(), originalResult.getStatusLine().getStatusCode());
    }

    private void validateHTML(final String path) throws Exception {
        final HttpGet getMethod = new HttpGet(serverAddress + path);
        getMethod.addHeader("Accept", "text/html");
        final HttpResponse response = client.execute(getMethod);
        assertEquals(OK.getStatusCode(), response.getStatusLine()
                .getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.debug("Retrieved HTML view:\n" + content);

        final HtmlParser htmlParser = new HtmlParser(ALLOW);
        htmlParser.setDoctypeExpectation(NO_DOCTYPE_ERRORS);
        htmlParser.setErrorHandler(new HTMLErrorHandler());
        htmlParser.setContentHandler(new TreeBuilder());
        try (
            final InputStream htmlStream =
                new ByteArrayInputStream(content.getBytes())) {
            htmlParser.parse(new InputSource(htmlStream));
        }
        logger.info("HTML found to be valid.");
    }

    public static class HTMLErrorHandler implements ErrorHandler {

        @Override
        public void warning(final SAXParseException e) throws SAXException {
            fail(e.toString());
        }

        @Override
        public void error(final SAXParseException e) throws SAXException {
            fail(e.toString());
        }

        @Override
        public void fatalError(final SAXParseException e) throws SAXException {
            fail(e.toString());
        }
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
}
