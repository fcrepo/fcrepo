
package org.fcrepo.integration.api;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.cache.CachingHttpClient;
import org.apache.http.util.EntityUtils;
import org.fcrepo.RdfLexicon;
import org.fcrepo.test.util.TestHelpers;
import org.fcrepo.utils.FedoraJcrTypes;
import org.junit.Test;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;

public class FedoraNodesIT extends AbstractResourceIT {

    @Test
    public void testIngest() throws Exception {
        final HttpPost method = postObjMethod("FedoraObjectsTest1");
        final HttpResponse response = client.execute(method);
        assertEquals(201, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals("Got wrong Location header for ingest!", serverAddress +
                OBJECT_PATH + "/FedoraObjectsTest1", location);
    }

    @Test
    public void testIngestWithNew() throws Exception {
        final HttpPost method = postObjMethod("fcr:new");
        final HttpResponse response = client.execute(method);
        final String content = EntityUtils.toString(response.getEntity());
        final int status = response.getStatusLine().getStatusCode();
        if (201 != status) {
            logger.error(content);
        }
        assertEquals(201, status);
        assertTrue("Response wasn't a PID", compile("[a-z]+").matcher(content)
                .find());
        assertTrue("new object did not mint a PID", !content
                .endsWith("/fcr:new"));

        assertEquals("Object wasn't created!", 200, getStatus(new HttpGet(
                serverAddress + content)));
    }

    @Test
    public void testDeleteObject() throws Exception {
        assertEquals(201, getStatus(postObjMethod("FedoraObjectsTest3")));
        assertEquals(204, getStatus(new HttpDelete(serverAddress +
                "objects/FedoraObjectsTest3")));
        assertEquals("Object wasn't really deleted!", 404,
                getStatus(new HttpGet(serverAddress +
                        "objects/FedoraObjectsTest3")));
    }

    @Test
    public void testGetDatastream() throws Exception {
        execute(postObjMethod("FedoraDatastreamsTest4"));

        assertEquals(404, getStatus(new HttpGet(serverAddress +
                "objects/FedoraDatastreamsTest4/ds1")));
        assertEquals(201, getStatus(postDSMethod("FedoraDatastreamsTest4",
                "ds1", "foo")));
        final HttpResponse response =
                execute(new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest4/ds1"));
        assertEquals(EntityUtils.toString(response.getEntity()), 200, response
                .getStatusLine().getStatusCode());
    }

    @Test
    public void testDeleteDatastream() throws Exception {
        execute(postObjMethod("FedoraDatastreamsTest5"));

        final HttpPost method =
                postDSMethod("FedoraDatastreamsTest5", "ds1", "foo");
        assertEquals(201, getStatus(method));

        final HttpGet method_2 =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest5/ds1");
        assertEquals(200, getStatus(method_2));

        final HttpDelete dmethod =
                new HttpDelete(serverAddress +
                        "objects/FedoraDatastreamsTest5/ds1");
        assertEquals(204, getStatus(dmethod));

        final HttpGet method_test_get =
                new HttpGet(serverAddress +
                        "objects/FedoraDatastreamsTest5/ds1");
        assertEquals(404, getStatus(method_test_get));
    }

    @Test
    public void testGetRepositoryGraph() throws Exception {
        final HttpGet getObjMethod = new HttpGet(serverAddress + "");
        getObjMethod.addHeader("Accept", "application/n-triples");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final GraphStore graphStore =
                TestHelpers.parseTriples(response.getEntity().getContent());
        logger.debug("Retrieved repository graph:\n" + graphStore.toString());

        assertTrue(
                "expected to find the root node data",
                graphStore
                        .contains(
                                Node.ANY,
                                Node.ANY,
                                RdfLexicon.HAS_PRIMARY_TYPE.asNode(),
                                Node.createLiteral(FedoraJcrTypes.ROOT)));

    }

    @Test
    public void testGetObjectGraphHtml() throws Exception {
        client.execute(postObjMethod("FedoraDescribeTestGraph"));
        final HttpGet getObjMethod =
                new HttpGet(serverAddress + "objects/FedoraDescribeTestGraph");
        getObjMethod.addHeader("Accept", "text/html");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        logger.info(content);
    }

    @Test
    public void testGetObjectGraph() throws Exception {
        client.execute(postObjMethod("FedoraDescribeTestGraph"));
        final HttpGet getObjMethod =
                new HttpGet(serverAddress + "objects/FedoraDescribeTestGraph");
        getObjMethod.addHeader("Accept", "application/n-triples");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        logger.debug("Retrieved object graph:\n" + content);

        assertTrue(
                "Didn't find an expected ntriple",
                compile(
                        "<" + serverAddress
                                + "objects/FedoraDescribeTestGraph> <info:fedora/fedora-system:def/internal#mixinTypes> \"fedora:object\" \\.",
                        DOTALL).matcher(content).find());

        assertTrue(
                "Didn't find an expected ntriple",
                compile(
                        "<" + serverAddress + "objects/FedoraDescribeTestGraph> <info:fedora/fedora-system:def/internal#mixinTypes> \"fedora:object\" \\.",
                        DOTALL).matcher(content).find());


    }


    @Test
    public void testGetObjectGraphByUUID() throws Exception {
        client.execute(postObjMethod("FedoraDescribeTestGraphByUuid"));

        final HttpGet getObjMethod =
                new HttpGet(serverAddress + "objects/FedoraDescribeTestGraphByUuid");
        getObjMethod.addHeader("Accept", "application/n3");
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final GraphStore graphStore = TestHelpers.parseTriples(response.getEntity().getContent());
        final Iterator<Quad> iterator = graphStore.find(Node.ANY, Node.createURI(serverAddress + "objects/FedoraDescribeTestGraphByUuid"), RdfLexicon.HAS_PRIMARY_IDENTIFIER.asNode(), Node.ANY);

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
        client.execute(postObjMethod("FedoraDescribeTestGraphUpdate"));
        final HttpPost updateObjectGraphMethod =
                new HttpPost(serverAddress +
                        "objects/FedoraDescribeTestGraphUpdate");
        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + serverAddress + "objects/FedoraDescribeTestGraphUpdate> <http://purl.org/dc/terms/identifier> \"this is an identifier\" } WHERE {}")
                        .getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response
                .getStatusLine().getStatusCode());

    }

    @Test
    public void testUpdateAndReplaceObjectGraph() throws Exception {
        client.execute(postObjMethod("FedoraDescribeTestGraphReplace"));
        final String subjectURI =
                serverAddress + "objects/FedoraDescribeTestGraphReplace";
        final HttpPost updateObjectGraphMethod =

        new HttpPost(subjectURI);

        new HttpPost(serverAddress + "objects/FedoraDescribeTestGraphReplace");

        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");

        BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(

                ("INSERT { <" + subjectURI + "> <info:rubydora#label> \"asdfg\" } WHERE {}")
                        .getBytes()));

        updateObjectGraphMethod.setEntity(e);
        client.execute(updateObjectGraphMethod);

        e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(

        ("DELETE { <" + subjectURI + "> <info:rubydora#label> ?p}\n" +
                "INSERT {<" + subjectURI +
                "> <info:rubydora#label> \"qwerty\"} \n" + "WHERE { <" +
                subjectURI + "> <info:rubydora#label> ?p}").getBytes()));

        updateObjectGraphMethod.setEntity(e);

        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response
                .getStatusLine().getStatusCode());

        final HttpGet getObjMethod =

        new HttpGet(subjectURI);

        getObjMethod.addHeader("Accept", "application/n-triples");
        final HttpResponse getResponse = client.execute(getObjMethod);
        assertEquals(200, getResponse.getStatusLine().getStatusCode());
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
                serverAddress + "objects/FedoraDescribeTestGraphUpdateBad";
        final HttpPost getObjMethod = new HttpPost(subjectURI);
        getObjMethod.addHeader("Content-Type", "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT { <" + subjectURI + "> <info:fedora/fedora-system:def/internal#uuid> \"00e686e2-24d4-40c2-92ce-577c0165b158\" } WHERE {}\n")
                        .getBytes()));
        getObjMethod.setEntity(e);
        final HttpResponse response = client.execute(getObjMethod);
        assertEquals(403, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        logger.debug("Got update response:\n" + content);

    }

    @Test
    public void testDescribeSize() throws Exception {
        final HttpGet describeMethod = new HttpGet(serverAddress + "");
        describeMethod.addHeader("Accept", "text/n3");
        HttpResponse response = client.execute(describeMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        GraphStore graphStore =
                TestHelpers.parseTriples(response.getEntity().getContent());
        logger.debug("For testDescribeSize() first size retrieved repository graph:\n" +
                graphStore.toString());

        Iterator<Triple> iterator =
                graphStore
                        .getDefaultGraph()
                        .find(Node.ANY, RdfLexicon.HAS_OBJECT_SIZE.asNode(),
                                Node.ANY);

        final Integer oldSize =
                (Integer) iterator.next().getObject().getLiteralValue();

        assertEquals(201, getStatus(postObjMethod("sizeNode")));
        assertEquals(201, getStatus(postDSMethod("sizeNode", "asdf", "1234")));

        response = client.execute(describeMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        graphStore =
                TestHelpers.parseTriples(response.getEntity().getContent());
        logger.debug("For testDescribeSize() new size retrieved repository graph:\n" +
                graphStore.toString());

        iterator =
                graphStore
                        .getDefaultGraph()
                        .find(Node.ANY, RdfLexicon.HAS_OBJECT_SIZE.asNode(),
                                Node.ANY);

        final Integer newSize =
                (Integer) iterator.next().getObject().getLiteralValue();

        logger.debug("Old size was: " + oldSize + " and new size was: " +
                newSize);
        assertTrue("No increment in size occurred when we expected one!",
                oldSize < newSize);
    }

    @Test
    public void testDescribeCount() throws Exception {
        final HttpGet describeMethod = new HttpGet(serverAddress + "");
        describeMethod.addHeader("Accept", "text/n3");
        HttpResponse response = client.execute(describeMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        GraphStore graphStore =
                TestHelpers.parseTriples(response.getEntity().getContent());
        logger.debug("For testDescribeCount() first count retrieved repository graph:\n" +
                graphStore.toString());

        Iterator<Triple> iterator =
                graphStore
                        .getDefaultGraph()
                        .find(Node.ANY, RdfLexicon.HAS_OBJECT_COUNT.asNode(),
                                Node.ANY);

        final Integer oldSize =
                (Integer) iterator.next().getObject().getLiteralValue();

        assertEquals(201, getStatus(postObjMethod("countNode")));
        assertEquals(201, getStatus(postDSMethod("countNode", "asdf", "1234")));

        response = client.execute(describeMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        graphStore =
                TestHelpers.parseTriples(response.getEntity().getContent());
        logger.debug("For testDescribeCount() first count repository graph:\n" +
                graphStore.toString());

        iterator =
                graphStore
                        .getDefaultGraph()
                        .find(Node.ANY,
                                     RdfLexicon.HAS_OBJECT_COUNT.asNode(),
                                Node.ANY);

        final Integer newSize =
                (Integer) iterator.next().getObject().getLiteralValue();

        logger.debug("Old size was: " + oldSize + " and new size was: " +
                newSize);
        assertTrue("No increment in count occurred when we expected one!",
                oldSize < newSize);
    }

    /**
     * Given a directory at:
     *  test-objects/FileSystem1/
     *                          /ds1
     *                          /ds2
     *                          /TestSubdir/
     * and a projection of test-objects as fedora:/files,
     * then I should be able to retrieve an object from fedora:/files/FileSystem1
     * that lists a child object at fedora:/files/FileSystem1/TestSubdir
     * and lists datastreams ds1 and ds2
     */
    @Test
    public void testGetProjectedNode() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "files/FileSystem1");
        method.setHeader("Accept", "text/n3");
        final HttpResponse response = execute(method);

        assertEquals(200, response.getStatusLine().getStatusCode());

        final String subjectURI = serverAddress + "files/FileSystem1";
        final Graph result =
                TestHelpers.parseTriples(response.getEntity().getContent())
                        .getDefaultGraph();
        logger.debug("For testGetProjectedNode() retrieved graph:\n" +
                result.toString());
        assertTrue("Didn't find the first datastream! ", result.contains(Node
                .createURI(subjectURI), Node.ANY, Node.createURI(subjectURI +
                "/ds1")));
        assertTrue("Didn't find the second datastream! ", result.contains(Node
                .createURI(subjectURI), Node.ANY, Node.createURI(subjectURI +
                "/ds2")));
        assertTrue("Didn't find the first object! ", result.contains(Node
                .createURI(subjectURI), Node.ANY, Node.createURI(subjectURI +
                "/TestSubdir")));

    }

    public void testDescribeRdfCached() throws RepositoryException, IOException {
        final CachingHttpClient specialClient = new CachingHttpClient(client);

        final String pid = "FedoraObjectsRdfTest2";
        final String path = "objects/" + pid;
        specialClient.execute(new HttpPost(serverAddress + path));
        final HttpGet getObjMethod = new HttpGet(serverAddress + path);
        HttpResponse response = specialClient.execute(getObjMethod);
        assertEquals("Client didn't return a OK!", OK.getStatusCode(), response
                .getStatusLine().getStatusCode());
        final String lastModed =
                response.getFirstHeader("Last-Modified").getValue();
        final HttpGet getObjMethod2 = new HttpGet(serverAddress + path);
        getObjMethod2.setHeader("If-Modified-Since", lastModed);
        response = specialClient.execute(getObjMethod2);

        assertEquals("Client didn't return a NOT_MODIFIED!", NOT_MODIFIED
                .getStatusCode(), response.getStatusLine().getStatusCode());

    }

}
