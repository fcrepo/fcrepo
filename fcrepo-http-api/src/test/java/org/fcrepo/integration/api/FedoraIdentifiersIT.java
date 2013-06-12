
package org.fcrepo.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.fcrepo.RdfLexicon;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Test;

import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;

public class FedoraIdentifiersIT extends AbstractResourceIT {

    @Test
    public void testGetNextPidResponds() throws Exception {
        final HttpPost method = new HttpPost(serverAddress + "fcr:pid");
        logger.debug("Executed testGetNextPidResponds()");
        assertEquals(HttpServletResponse.SC_OK, getStatus(method));
    }

    @Test
    public void testGetNextHasAPid() throws IOException {
        final HttpPost method =
                new HttpPost(serverAddress + "fcr:pid");
        final HttpResponse response = client.execute(method);
        logger.debug("Executed testGetNextHasAPid()");
        final GraphStore graphStore = TestHelpers.parseTriples(response.getEntity().getContent());
        assertTrue("Didn't find a single dang PID!", graphStore.contains(Node.ANY, ResourceFactory.createResource(serverAddress + "fcr:pid").asNode(), RdfLexicon.HAS_MEMBER_OF_RESULT.asNode(), Node.ANY));

    }

    @Test
    public void testGetNextHasTwoPids() throws IOException {
        final HttpPost method =
                new HttpPost(serverAddress + "fcr:pid?numPids=2");
        final HttpResponse response = client.execute(method);
        logger.debug("Executed testGetNextHasTwoPids()");
        final GraphStore graphStore = TestHelpers.parseTriples(response.getEntity().getContent());
        assertEquals("Didn't find two dang PIDs!", 2, Iterators.size(graphStore.find(Node.ANY, ResourceFactory.createResource(serverAddress + "fcr:pid").asNode(), RdfLexicon.HAS_MEMBER_OF_RESULT.asNode(), Node.ANY)));

    }

    @Test
    public void testGetNextPidRespondsWithPath() throws Exception {
        final HttpPost method = new HttpPost(serverAddress + "objects/fcr:pid");
        logger.debug("Executed testGetNextPidRespondsWithPath()");
        assertEquals(HttpServletResponse.SC_OK, getStatus(method));
    }

    @Test
    public void testGetNextHasAPidWithPath() throws IOException {
        final HttpPost method =
                new HttpPost(serverAddress + "objects/fcr:pid");
        final HttpResponse response = client.execute(method);
        logger.debug("Executed testGetNextHasAPidWithPath()");
        final GraphStore graphStore = TestHelpers.parseTriples(response.getEntity().getContent());
        assertTrue("Didn't find a single dang PID!", graphStore.contains(Node.ANY, ResourceFactory.createResource(serverAddress + "objects/fcr:pid").asNode(), RdfLexicon.HAS_MEMBER_OF_RESULT.asNode(), Node.ANY));

    }

    @Test
    public void testGetNextHasTwoPidsWithPath() throws IOException {
        final HttpPost method =
                new HttpPost(serverAddress + "objects/fcr:pid?numPids=2");
        final HttpResponse response = client.execute(method);
        logger.debug("Executed testGetNextHasTwoPidsWithPath()");
        final GraphStore graphStore = TestHelpers.parseTriples(response.getEntity().getContent());
        assertEquals("Didn't find two dang PIDs!", 2, Iterators.size(graphStore.find(Node.ANY, ResourceFactory.createResource(serverAddress + "objects/fcr:pid").asNode(), RdfLexicon.HAS_MEMBER_OF_RESULT.asNode(), Node.ANY)));

    }
}
