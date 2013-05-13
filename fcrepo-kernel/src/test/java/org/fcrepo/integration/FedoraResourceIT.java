package org.fcrepo.integration;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_Literal;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.util.NodeFactory;
import org.fcrepo.Datastream;
import org.fcrepo.FedoraResource;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraResourceIT extends AbstractIT {

	protected Logger logger;

	@Inject
	Repository repo;

	@Inject
	NodeService nodeService;

	@Inject
	ObjectService objectService;


	@Inject
	DatastreamService datastreamService;

	@Before
	public void setLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void testRandomNodeGraph() throws IOException, RepositoryException {
		Session session = repo.login();

		final FedoraResource object = nodeService.findOrCreateObject(session, "/testNodeGraph");

		assertEquals("info:fedora/testNodeGraph", object.getGraphSubject().toString());

		logger.warn(object.getGraphStore().toString());
		Node s = Node.createURI("info:fedora/testNodeGraph");
		Node p = Node.createURI("info:fedora/fedora-system:def/internal#primaryType");
		Node o = Node.createLiteral("nt:unstructured");
		assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));
		session.logout();
	}

	@Test
	public void testObjectGraph() throws IOException, RepositoryException {
		Session session = repo.login();

		final FedoraResource object = objectService.createObject(session, "/testObjectGraph");

		assertEquals("info:fedora/testObjectGraph", object.getGraphSubject().toString());

		logger.warn(object.getGraphStore().toString());

		// jcr property
		Node s = Node.createURI("info:fedora/testObjectGraph");
		Node p = Node.createURI("info:fedora/fedora-system:def/internal#uuid");
		Node o = Node.createLiteral(object.getNode().getIdentifier());
		assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));


		// multivalued property
		p = Node.createURI("info:fedora/fedora-system:def/internal#mixinTypes");
		o = Node.createLiteral("fedora:resource");
		assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));

		o = Node.createLiteral("fedora:object");
		assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));

		session.logout();
	}


	@Test
	public void testDatastreamGraph() throws IOException, RepositoryException, InvalidChecksumException {
		Session session = repo.login();

		objectService.createObject(session, "/testDatastreamGraphParent");

		datastreamService.createDatastreamNode(session, "/testDatastreamGraph", "text/plain", new ByteArrayInputStream("123456789test123456789".getBytes()));


		final FedoraResource object = nodeService.getObject(session, "/testDatastreamGraph");

		object.getNode().setProperty("fedorarelsext:isPartOf", session.getNode("/testDatastreamGraphParent"));

		assertEquals("info:fedora/testDatastreamGraph", object.getGraphSubject().toString());

		logger.warn(object.getGraphStore().toString());

		// jcr property
		Node s = Node.createURI("info:fedora/testDatastreamGraph");
		Node p = Node.createURI("info:fedora/fedora-system:def/internal#uuid");
		Node o = Node.createLiteral(object.getNode().getIdentifier());
		assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));


		// multivalued property
		p = Node.createURI("info:fedora/fedora-system:def/internal#mixinTypes");
		o = Node.createLiteral("fedora:resource");
		assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));

		o = Node.createLiteral("fedora:datastream");
		assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));

        // structure
        p = Node.createURI("info:fedora/fedora-system:def/internal#numberOfChildren");
        RDFDatatype long_datatype = ResourceFactory.createTypedLiteral(0L).getDatatype();
        o = Node.createLiteral("0", long_datatype);

        assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));
		// relations
		p = Node.createURI("info:fedora/fedora-system:def/relations-external#isPartOf");
		o = Node.createURI("info:fedora/testDatastreamGraphParent");
		assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));

		p = Node.createURI("info:fedora/fedora-system:def/internal#hasContent");
		o = Node.createURI("info:fedora/testDatastreamGraph/fcr:content");
		assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));

		// content properties
		s = Node.createURI("info:fedora/testDatastreamGraph/fcr:content");
		p = Node.createURI("info:fedora/fedora-system:def/internal#mimeType");
		o = Node.createLiteral("text/plain");
		assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));

		p = Node.createURI("info:fedora/size");
		o = Node.createLiteral("22", ModelFactory.createDefaultModel().createTypedLiteral(22L).getDatatype());
		assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));



		session.logout();
	}
}
