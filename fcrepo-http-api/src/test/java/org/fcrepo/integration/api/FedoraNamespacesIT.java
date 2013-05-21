
package org.fcrepo.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.fcrepo.test.util.TestHelpers;
import org.fcrepo.utils.JcrRdfTools;
import org.junit.Test;

import java.io.ByteArrayInputStream;

public class FedoraNamespacesIT extends AbstractResourceIT {

    @Test
    public void testGet() throws Exception {

        HttpGet get = new HttpGet(serverAddress + "fcr:namespaces");
        get.addHeader("Accept", "application/n-triples");
        HttpResponse response = execute(get);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(200, status);

        final GraphStore graphStore = TestHelpers.parseTriples(response.getEntity().getContent());

        logger.debug("Got store {}", graphStore);
        assertTrue("expected to find nt property in response", graphStore.contains(Node.ANY, ResourceFactory.createResource("http://www.jcp.org/jcr/nt/1.0").asNode(), ResourceFactory.createProperty(JcrRdfTools.HAS_NAMESPACE_PREDICATE).asNode(), ResourceFactory.createPlainLiteral("nt").asNode()));
    }

    @Test
    public void testCreate() throws Exception {
        HttpPost post = new HttpPost(serverAddress + "fcr:namespaces");
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(("INSERT { <http://example.com/namespace/abc> <" + JcrRdfTools.HAS_NAMESPACE_PREDICATE + "> \"abc\"} WHERE { }").getBytes()));

        post.setEntity(entity);

        execute(post);


        HttpGet get = new HttpGet(serverAddress + "fcr:namespaces");
        get.addHeader("Accept", "application/n-triples");
        HttpResponse response = execute(get);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(200, status);

        final GraphStore graphStore = TestHelpers.parseTriples(response.getEntity().getContent());

        logger.debug("Got store {}", graphStore);
        assertTrue("expected to find our new property in response", graphStore.contains(Node.ANY, ResourceFactory.createResource("http://example.com/namespace/abc").asNode(), ResourceFactory.createProperty(JcrRdfTools.HAS_NAMESPACE_PREDICATE).asNode(), ResourceFactory.createPlainLiteral("abc").asNode()));
    }

    @Test
    public void testUpdatePrefix() throws Exception {
        HttpPost post = new HttpPost(serverAddress + "fcr:namespaces");
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(("INSERT { <http://example.com/namespace/abc> <" + JcrRdfTools.HAS_NAMESPACE_PREDICATE + "> \"abc\"} WHERE { }").getBytes()));

        post.setEntity(entity);

        execute(post);
        post = new HttpPost(serverAddress + "fcr:namespaces");
        entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(("INSERT { <http://example.com/namespace/abc> <" + JcrRdfTools.HAS_NAMESPACE_PREDICATE + "> \"cba\"} WHERE { }").getBytes()));

        post.setEntity(entity);
        execute(post);

        HttpGet get = new HttpGet(serverAddress + "fcr:namespaces");
        get.addHeader("Accept", "application/n-triples");
        HttpResponse response = execute(get);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(200, status);

        final GraphStore graphStore = TestHelpers.parseTriples(response.getEntity().getContent());

        logger.debug("Got store {}", graphStore);
        assertTrue("expected to find our updated property in response", graphStore.contains(Node.ANY, ResourceFactory.createResource("http://example.com/namespace/abc").asNode(), ResourceFactory.createProperty(JcrRdfTools.HAS_NAMESPACE_PREDICATE).asNode(), ResourceFactory.createPlainLiteral("cba").asNode()));
    }

    @Test
    public void testUpdateNamespace() throws Exception {
        HttpPost post = new HttpPost(serverAddress + "fcr:namespaces");
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(("INSERT { <http://example.com/namespace/abc> <" + JcrRdfTools.HAS_NAMESPACE_PREDICATE + "> \"abc\"} WHERE { }").getBytes()));

        post.setEntity(entity);
        execute(post);

        post = new HttpPost(serverAddress + "fcr:namespaces");
        entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(("INSERT { <http://example.com/moved/to/abc> <" + JcrRdfTools.HAS_NAMESPACE_PREDICATE + "> \"abc\"} WHERE { }").getBytes()));

        post.setEntity(entity);
        execute(post);

        HttpGet get = new HttpGet(serverAddress + "fcr:namespaces");
        get.addHeader("Accept", "application/n-triples");
        HttpResponse response = execute(get);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(200, status);

        final GraphStore graphStore = TestHelpers.parseTriples(response.getEntity().getContent());

        logger.debug("Got store {}", graphStore);
        assertTrue("expected to find our updated property in response", graphStore.contains(Node.ANY, ResourceFactory.createResource("http://example.com/moved/to/abc").asNode(), ResourceFactory.createProperty(JcrRdfTools.HAS_NAMESPACE_PREDICATE).asNode(), ResourceFactory.createPlainLiteral("abc").asNode()));
    }


    @Test
    public void testDeleteNamespace() throws Exception {
        HttpPost post = new HttpPost(serverAddress + "fcr:namespaces");
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(("INSERT { <http://example.com/namespace/abc> <" + JcrRdfTools.HAS_NAMESPACE_PREDICATE + "> \"abc\"} WHERE { }").getBytes()));

        post.setEntity(entity);
        execute(post);

        post = new HttpPost(serverAddress + "fcr:namespaces");
        entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(("DELETE { <http://example.com/namespace/abc> <" + JcrRdfTools.HAS_NAMESPACE_PREDICATE + "> \"abc\"} WHERE { }").getBytes()));

        post.setEntity(entity);
        execute(post);

        HttpGet get = new HttpGet(serverAddress + "fcr:namespaces");
        get.addHeader("Accept", "application/n-triples");
        HttpResponse response = execute(get);
        int status = response.getStatusLine().getStatusCode();
        assertEquals(200, status);

        final GraphStore graphStore = TestHelpers.parseTriples(response.getEntity().getContent());

        logger.debug("Got store {}", graphStore);
        assertFalse("should not find deleted property in response", graphStore.contains(Node.ANY, ResourceFactory.createResource("http://example.com/namespace/abc").asNode(), ResourceFactory.createProperty(JcrRdfTools.HAS_NAMESPACE_PREDICATE).asNode(), ResourceFactory.createPlainLiteral("abc").asNode()));
    }

}
