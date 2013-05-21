package org.fcrepo.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Test;

public class FedoraFixityIT extends AbstractResourceIT {
	@Test
	public void testCheckDatastreamFixity() throws Exception {
		final HttpPost objMethod = postObjMethod("FedoraDatastreamsTest11");
		assertEquals(201, getStatus(objMethod));
		final HttpPost method1 =
				postDSMethod("FedoraDatastreamsTest11", "zxc", "foo");
		assertEquals(201, getStatus(method1));
		final HttpGet method2 =
				new HttpGet(serverAddress +
									"objects/FedoraDatastreamsTest11/zxc/fcr:fixity");
		final HttpResponse response = execute(method2);
		assertEquals(200, response.getStatusLine().getStatusCode());
		final HttpEntity entity = response.getEntity();
        final GraphStore graphStore = TestHelpers.parseTriples(entity.getContent());

        logger.info("Got triples {}", graphStore);

        assertTrue(graphStore.contains(Node.ANY, Node.ANY, ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#isFixityResultOf").asNode(), ResourceFactory.createResource(serverAddress + "objects/FedoraDatastreamsTest11/zxc").asNode()));
        assertTrue(graphStore.contains(Node.ANY, Node.ANY, ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#status").asNode(), ResourceFactory.createPlainLiteral("SUCCESS").asNode()));

        assertTrue(graphStore.contains(Node.ANY, Node.ANY, ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#computedChecksum").asNode(),ResourceFactory.createResource("urn:sha1:0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33").asNode()));
        assertTrue(graphStore.contains(Node.ANY, Node.ANY, ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#storedChecksum").asNode(), ResourceFactory.createResource("urn:sha1:0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33").asNode()));
        assertTrue(graphStore.contains(Node.ANY, Node.ANY, ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#computedSize").asNode(),ResourceFactory.createTypedLiteral(3).asNode()));
        assertTrue(graphStore.contains(Node.ANY, Node.ANY, ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#storedSize").asNode(), ResourceFactory.createTypedLiteral(3).asNode()));
	}
}
