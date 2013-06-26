
package org.fcrepo.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.fcrepo.RdfLexicon;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;

public class FedoraFieldSearchIT extends AbstractResourceIT {

    @Test
    public void testSearchHtml() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "fcr:search");
        method.setHeader("Accept", "text/html");

        HttpResponse resp = execute(method);

        String content = IOUtils.toString(resp.getEntity().getContent());
        logger.debug("Got search form: {}", content);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertTrue(content.contains("<form"));

    }

    @Test
    public void testSearchResultsHtml() throws Exception {
        final HttpGet method = new HttpGet(serverAddress + "fcr:search");
        method.setHeader("Accept", "text/html");
        URI uri =
                new URIBuilder(method.getURI()).addParameter("q", "testobj")
                        .addParameter("offset", "0").addParameter("limit", "1")
                        .build();

        method.setURI(uri);

        HttpResponse resp = execute(method);

        String content = IOUtils.toString(resp.getEntity().getContent());
        logger.debug("Got search results: {}", content);
        assertEquals(200, resp.getStatusLine().getStatusCode());

    }

    @Test
    public void testSearchRdf() throws Exception {
        /* first post an object which can be used for the search */
        HttpPost postObj = postObjMethod("testobj");
        HttpResponse postResp = execute(postObj);
        postObj.releaseConnection();
        assertEquals(201, postResp.getStatusLine().getStatusCode());

        /* and add a dc title to the object so the query returns a result */
        HttpPost postDc = new HttpPost(serverAddress + "objects/testobj");
        postDc.setHeader("Content-Type", "application/sparql-update");
        String updateString =
                "INSERT { <" + serverAddress +
                        "objects/testobj> <http://purl.org/dc/terms/title> \"testobj\" } WHERE { }";
        postDc.setEntity(new StringEntity(updateString));
        HttpResponse dcResp = execute(postDc);
        assertEquals(dcResp.getStatusLine().toString(), 204, dcResp
                .getStatusLine().getStatusCode());
        postDc.releaseConnection();

        final HttpGet method = new HttpGet(serverAddress + "fcr:search");
        method.setHeader("Accept", "application/n3");
        URI uri =
                new URIBuilder(method.getURI()).addParameter("q", "testobj")
                        .addParameter("offset", "0").addParameter("limit", "1")
                        .build();

        method.setURI(uri);

        HttpResponse resp = execute(method);

        final GraphStore graphStore =
                TestHelpers.parseTriples(resp.getEntity().getContent());

        logger.debug("Got search results graph: {}", graphStore);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertTrue(graphStore
                .contains(
                        Node.ANY,
                        ResourceFactory
                                .createResource(
                                        serverAddress +
                                                "fcr:search?q=testobj&offset=0&limit=1")
                                .asNode(),
                        ResourceFactory
                                .createResource(
                                        "http://a9.com/-/spec/opensearch/1.1/totalResults")
                                .asNode(), ResourceFactory
                                .createTypedLiteral(1).asNode()));

    }

    @Test
    public void testSearchSubmitPaging() throws Exception {

        final HttpGet method = new HttpGet(serverAddress + "fcr:search");
        method.setHeader("Accept", "application/n3");
        URI uri =
                new URIBuilder(method.getURI()).addParameter("q", "testobj")
                        .addParameter("offset", "1").addParameter("limit", "1")
                        .build();

        method.setURI(uri);

        HttpResponse resp = execute(method);

        final GraphStore graphStore =
                TestHelpers.parseTriples(resp.getEntity().getContent());

        logger.debug("Got search results graph: {}", graphStore);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertFalse(graphStore
                .contains(
                        Node.ANY,
                        ResourceFactory
                                .createResource(
                                        serverAddress +
                                                "fcr:search?q=testobj&offset=1&limit=1")
                                .asNode(), RdfLexicon.HAS_MEMBER_OF_RESULT
                                .asNode(), Node.ANY));

    }
}
