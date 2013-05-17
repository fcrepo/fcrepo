package org.fcrepo.integration;

import static org.fcrepo.utils.FedoraTypesUtils.getVersionHistory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

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

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

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
    public void testGetRootNode() throws IOException, RepositoryException {
        Session session = repo.login();

        final FedoraResource object = nodeService.getObject(session, "/");

        session.logout();
    }

	@Test
	public void testRandomNodeGraph() throws IOException, RepositoryException {
		Session session = repo.login();

		final FedoraResource object = nodeService.findOrCreateObject(session, "/testNodeGraph");

		logger.warn(object.getGraphStore().toString());
		Node s = Node.createURI("info:fedora/testNodeGraph");
		Node p = Node.createURI("info:fedora/fedora-system:def/internal#primaryType");
		Node o = Node.createLiteral("nt:unstructured");
		assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));
		session.logout();
	}

    @Test
    public void testRepositoryRootGraph() throws IOException, RepositoryException {
        Session session = repo.login();

        final FedoraResource object = nodeService.getObject(session, "/");

        logger.warn(object.getGraphStore().toString());
        Node s = Node.createURI("info:fedora/");
        Node p = Node.createURI("info:fedora/fedora-system:def/internal#primaryType");
        Node o = Node.createLiteral("mode:root");
        assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));


        p = Node.createURI("info:fedora/fedora-system:def/internal#repository/jcr.repository.vendor.url");
        o = Node.createLiteral("http://www.modeshape.org");
        assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));

        p = Node.createURI("info:fedora/fedora-system:def/internal#hasNodeType");
        o = Node.createLiteral("fedora:resource");
        assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));

        session.logout();
    }

	@Test
	public void testObjectGraph() throws IOException, RepositoryException {
		Session session = repo.login();

		final FedoraResource object = objectService.createObject(session, "/testObjectGraph");

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

        // location

        p = Node.createURI("info:fedora/fedora-system:def/internal#hasLocation");
        o = Node.ANY;

        assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));


		session.logout();
	}

    @Test
    public void testUpdatingObjectGraph() throws RepositoryException {

        Session session = repo.login();

        final FedoraResource object = objectService.createObject(session, "/testObjectGraphUpdates");

        object.updateGraph("INSERT { <info:fedora/testObjectGraphUpdates> <info:fcrepo/zyx> \"a\" } WHERE {} ");

        // jcr property
        Node s = Node.createURI("info:fedora/testObjectGraphUpdates");
        Node p = Node.createURI("info:fcrepo/zyx");
        Node o = Node.createLiteral("a");
        assertTrue(object.getGraphStore().getDefaultGraph().contains(s, p, o));

        object.updateGraph("DELETE { <info:fedora/testObjectGraphUpdates> <info:fcrepo/zyx> ?o }\nINSERT { <info:fedora/testObjectGraphUpdates> <info:fcrepo/zyx> \"b\" } WHERE { <info:fedora/testObjectGraphUpdates> <info:fcrepo/zyx> ?o } ");

        assertFalse("found value we should have removed", object.getGraphStore().getDefaultGraph().contains(s, p, o));
        o = Node.createLiteral("b");
        assertTrue("could not find new value", object.getGraphStore().getDefaultGraph().contains(s, p, o));

        session.logout();
    }

    @Test
    public void testAddVersionLabel() throws RepositoryException {

        Session session = repo.login();

        final FedoraResource object = objectService.createObject(session, "/testObjectVersionLabel");

        session.save();

        object.addVersionLabel("v0.0.1");

        session.save();


        assertTrue(Arrays.asList(getVersionHistory(object.getNode()).getVersionLabels()).contains("v0.0.1"));

        session.logout();
    }

    @Test
    public void testGetObjectVersionGraph() throws RepositoryException {

        Session session = repo.login();

        final FedoraResource object = objectService.createObject(session, "/testObjectVersionGraph");

        session.save();

        object.addVersionLabel("v0.0.1");

        session.save();


        final GraphStore graphStore = object.getVersionGraphStore();

        logger.info(graphStore.toString());

        // go querying for the version URI
        Node s = Node.createURI("info:fedora/testObjectVersionGraph");
        Node p = Node.createURI("info:fedora/fedora-system:def/internal#hasVersion");
        final ExtendedIterator<Triple> triples = graphStore.getDefaultGraph().find(Triple.createMatch(s, p, Node.ANY));

        List<Triple> list = triples.toList();
        assertEquals(1, list.size());

        // make sure it matches the label
        s = list.get(0).getMatchObject();
        p = Node.createURI("info:fedora/fedora-system:def/internal#hasVersionLabel");
        Node o = Node.createLiteral("v0.0.1");

        assertTrue(graphStore.getDefaultGraph().contains(s, p, o));

        session.logout();
    }
}
