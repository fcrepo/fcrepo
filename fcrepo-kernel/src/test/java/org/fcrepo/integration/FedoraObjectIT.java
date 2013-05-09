
package org.fcrepo.integration;

import java.io.IOException;
import java.util.Iterator;

import javax.inject.Inject;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.UpdateAction;
import org.fcrepo.FedoraObject;
import org.fcrepo.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import static java.util.regex.Pattern.compile;
import static org.junit.Assert.*;

@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraObjectIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    ObjectService objectService;

    @Test
    public void testCreatedObject() throws RepositoryException, IOException {
        Session session = repo.login();
        objectService.createObject(session, "/testObject");
        session.save();
        session.logout();
        session = repo.login();
        final FedoraObject obj = objectService.getObject(session, "/testObject");
        assertNotNull("Couldn't find object!", obj);
    }

	@Test
	public void testGetSizeWhenInATree() throws Exception {

		final Session session = repo.login();
		final FedoraObject object = objectService.createObject(session, "/parentObject");
		final long originalSize = object.getSize();
		objectService.createObject(session, "/parentObject/testChildObject");

		session.save();

		assertTrue(objectService.getObject(session, "/parentObject").getSize() > originalSize);

	}

	@Test
	public void testObjectGraph() throws Exception {
		final Session session = repo.login();
		final FedoraObject object = objectService.createObject(session, "/graphObject");
		final GraphStore graphStore = object.getGraphStore();

		assertFalse("Graph store should not contain JCR prefixes", compile("jcr").matcher(graphStore.toString()).find());
		assertFalse("Graph store should contain our fedora-internal prefix", compile("fedora-internal").matcher(graphStore.toString()).find());
		assertEquals("info:fedora/graphObject", object.getGraphSubject().toString());

		UpdateAction.parseExecute("PREFIX dc: <http://purl.org/dc/terms/>\n" +
										  "INSERT { <http://example/egbook> dc:title  \"This is an example of an update that will be ignored\" } WHERE {}", graphStore);


		UpdateAction.parseExecute("PREFIX dc: <http://purl.org/dc/terms/>\n" +
										  "INSERT { <" + object.getGraphSubject().toString() + "> dc:title  \"This is an example title\" } WHERE {}", graphStore);

		assertTrue(object.getNode().getProperty("dc:title").getValues()[0].getString(), object.getNode().getProperty("dc:title").getValues()[0].getString().equals("This is an example title"));


		UpdateAction.parseExecute("PREFIX myurn: <info:myurn/>\n" +
										  "INSERT { <" + object.getGraphSubject().toString() + "> myurn:info  \"This is some example data\";" +
										  "					myurn:info  \"And so it this\"	 } WHERE {}", graphStore);

		final Value[] values = object.getNode().getProperty(object.getNode().getSession().getNamespacePrefix("info:myurn/") + ":info").getValues();

		assertEquals("This is some example data", values[0].getString());
		assertEquals("And so it this", values[1].getString());


		UpdateAction.parseExecute("PREFIX fedora-rels-ext: <info:fedora/fedora-system:def/relations-external#>\n" +
										  "INSERT { <" + object.getGraphSubject().toString() + "> fedora-rels-ext:isPartOf <" + object.getGraphSubject().toString() + "> } WHERE {}", graphStore);
		assertTrue(object.getNode().getProperty("fedorarelsext:isPartOf").getValues()[0].getString(), object.getNode().getProperty("fedorarelsext:isPartOf").getValues()[0].getString().equals(object.getNode().getIdentifier()));


		UpdateAction.parseExecute("PREFIX dc: <http://purl.org/dc/terms/>\n" +
										  "DELETE { <" + object.getGraphSubject().toString() + "> dc:title  \"This is an example title\" } WHERE {}", graphStore);

		assertFalse("Found unexpected dc:title", object.getNode().hasProperty("dc:title"));

		UpdateAction.parseExecute("PREFIX fedora-rels-ext: <info:fedora/fedora-system:def/relations-external#>\n" +
										  "DELETE { <" + object.getGraphSubject().toString() + "> fedora-rels-ext:isPartOf <" + object.getGraphSubject().toString() + "> } WHERE {}", graphStore);
		assertFalse("found unexpected reference", object.getNode().hasProperty("fedorarelsext:isPartOf"));


		session.save();

	}
}
