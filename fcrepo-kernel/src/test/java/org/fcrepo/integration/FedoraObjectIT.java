/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.integration;

import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import com.hp.hpl.jena.query.Dataset;
import org.fcrepo.FedoraObject;
import org.fcrepo.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.hp.hpl.jena.update.UpdateAction;

/**
 * @todo Add Documentation.
 * @author fasseg
 * @date Mar 20, 2013
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraObjectIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    ObjectService objectService;

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testCreatedObject() throws RepositoryException, IOException {
        Session session = repo.login();
        objectService.createObject(session, "/testObject");
        session.save();
        session.logout();
        session = repo.login();
        final FedoraObject obj =
            objectService.getObject(session, "/testObject");
        assertNotNull("Couldn't find object!", obj);
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetSizeWhenInATree() throws Exception {

        final Session session = repo.login();
        final FedoraObject object =
            objectService.createObject(session, "/parentObject");
        final long originalSize = object.getSize();
        objectService.createObject(session, "/parentObject/testChildObject");

        session.save();

        assertTrue(objectService
                   .getObject(session, "/parentObject")
                   .getSize() > originalSize);

    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testObjectGraph() throws Exception {
        final Session session = repo.login();
        final FedoraObject object =
            objectService.createObject(session, "/graphObject");
        final Dataset graphStore = object.getPropertiesDataset();

        final String graphSubject = "info:fedora/graphObject";

        assertFalse("Graph store should not contain JCR prefixes",
                    compile("jcr").matcher(graphStore.toString()).find());
        assertFalse("Graph store should contain our fedora-internal prefix",
                    compile("fedora-internal")
                    .matcher(graphStore.toString()).find());

        UpdateAction
            .parseExecute("PREFIX dc: <http://purl.org/dc/terms/>\n" +
                          "INSERT { <http://example/egbook> dc:title " +
                          "\"This is an example of an update that will be " +
                          "ignored\" } WHERE {}", graphStore);

        UpdateAction
            .parseExecute("PREFIX dc: <http://purl.org/dc/terms/>\n" +
                          "INSERT { <" + graphSubject + "> dc:title " +
                          "\"This is an example title\" } WHERE {}",
                          graphStore);

        assertTrue(object.getNode().getProperty("dc:title").getValues()[0]
                   .getString(),
                   object.getNode().getProperty("dc:title").getValues()[0]
                   .getString().equals("This is an example title"));


        UpdateAction
            .parseExecute("PREFIX myurn: <info:myurn/>\n" +
                          "INSERT { <" + graphSubject + "> myurn:info " +
                          "\"This is some example data\";" +
                          "myurn:info  \"And so it this\"     } WHERE {}",
                          graphStore);

        final Value[] values =
            object.getNode().getProperty(object.getNode().getSession()
                                         .getNamespacePrefix("info:myurn/") +
                                         ":info").getValues();

        assertEquals("This is some example data", values[0].getString());
        assertEquals("And so it this", values[1].getString());


        UpdateAction
            .parseExecute("PREFIX fedora-rels-ext: <info:fedora/fedora-system" +
                          ":def/relations-external#>\n" +
                          "INSERT { <" + graphSubject + "> fedora-rels-ext:" +
                          "isPartOf <" + graphSubject + "> } WHERE {}",
                          graphStore);
        assertTrue(object.getNode().getProperty("fedorarelsext:isPartOf")
                   .getValues()[0].getString(),
                   object.getNode().getProperty("fedorarelsext:isPartOf")
                   .getValues()[0].getString()
                   .equals(object.getNode().getIdentifier()));


        UpdateAction
            .parseExecute("PREFIX dc: <http://purl.org/dc/terms/>\n" +
                          "DELETE { <" + graphSubject + "> dc:title " +
                          "\"This is an example title\" } WHERE {}",
                          graphStore);

        assertFalse("Found unexpected dc:title",
                    object.getNode().hasProperty("dc:title"));

        UpdateAction
            .parseExecute("PREFIX fedora-rels-ext: <info:fedora/" +
                          "fedora-system:def/relations-external#>\n" +
                          "DELETE { <" + graphSubject + "> " +
                          "fedora-rels-ext:isPartOf <" + graphSubject + "> " +
                          "} WHERE {}",
                          graphStore);
        assertFalse("found unexpected reference",
                    object.getNode().hasProperty("fedorarelsext:isPartOf"));

        session.save();

    }
}
