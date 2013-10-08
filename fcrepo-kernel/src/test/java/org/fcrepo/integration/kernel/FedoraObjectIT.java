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
package org.fcrepo.integration.kernel;

import static com.hp.hpl.jena.update.UpdateAction.parseExecute;
import static java.util.regex.Pattern.compile;
import static org.fcrepo.kernel.RdfLexicon.RELATIONS_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.RESTAPI_NAMESPACE;
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

import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.kernel.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.hp.hpl.jena.query.Dataset;

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
        final FedoraObject obj =
            objectService.getObject(session, "/testObject");
        assertNotNull("Couldn't find object!", obj);
    }

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

    @Test
    public void testObjectGraph() throws Exception {
        final Session session = repo.login();
        final FedoraObject object =
            objectService.createObject(session, "/graphObject");
        final Dataset graphStore = object.getPropertiesDataset(new DefaultGraphSubjects(session));

        final String graphSubject = RESTAPI_NAMESPACE + "/graphObject";

        assertFalse("Graph store should not contain JCR prefixes",
                    compile("jcr").matcher(graphStore.toString()).find());
        assertFalse("Graph store should contain our fcrepo prefix",
                    compile("fcrepo")
                    .matcher(graphStore.toString()).find());

        parseExecute("PREFIX dc: <http://purl.org/dc/terms/>\n" +
                          "INSERT { <http://example/egbook> dc:title " +
                          "\"This is an example of an update that will be " +
                          "ignored\" } WHERE {}", graphStore);

        parseExecute("PREFIX dc: <http://purl.org/dc/terms/>\n" +
                          "INSERT { <" + graphSubject + "> dc:title " +
                          "\"This is an example title\" } WHERE {}",
                          graphStore);

        assertTrue(object.getNode().getProperty("dc:title").getValues()[0]
                   .getString(),
                   object.getNode().getProperty("dc:title").getValues()[0]
                   .getString().equals("This is an example title"));


        parseExecute("PREFIX myurn: <info:myurn/>\n" +
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

        parseExecute("PREFIX fedora-rels-ext: <"
                + RELATIONS_NAMESPACE + ">\n" +
                "INSERT { <" + graphSubject + "> fedora-rels-ext:" +
                "isPartOf <" + graphSubject + "> } WHERE {}", graphStore);

        assertTrue(object.getNode().getProperty("fedorarelsext:isPartOf")
                   .getValues()[0].getString(),
                   object.getNode().getProperty("fedorarelsext:isPartOf")
                   .getValues()[0].getString()
                   .equals(object.getNode().getIdentifier()));


        parseExecute("PREFIX dc: <http://purl.org/dc/terms/>\n" +
                          "DELETE { <" + graphSubject + "> dc:title " +
                          "\"This is an example title\" } WHERE {}",
                          graphStore);

        assertFalse("Found unexpected dc:title",
                    object.getNode().hasProperty("dc:title"));

        parseExecute("PREFIX fedora-rels-ext: <" +
                    RELATIONS_NAMESPACE + ">\n" +
                    "DELETE { <" + graphSubject + "> " +
                    "fedora-rels-ext:isPartOf <" + graphSubject + "> " +
                    "} WHERE {}", graphStore);
        assertFalse("found unexpected reference",
                    object.getNode().hasProperty("fedorarelsext:isPartOf"));

        session.save();

    }
}
