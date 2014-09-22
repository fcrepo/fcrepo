/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.integration.kernel.impl;

import static com.hp.hpl.jena.update.UpdateAction.parseExecute;
import static java.util.regex.Pattern.compile;
import static org.fcrepo.kernel.RdfLexicon.RELATIONS_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.hp.hpl.jena.query.Dataset;

/**
 * <p>FedoraObjectImplIT class.</p>
 *
 * @author ksclarke
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraObjectImplIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    ObjectService objectService;

    @Test
    public void testCreatedObject() throws RepositoryException {
        Session session = repo.login();
        objectService.findOrCreateObject(session, "/testObject");
        session.save();
        session.logout();
        session = repo.login();
        final FedoraObject obj =
            objectService.findOrCreateObject(session, "/testObject");
        assertNotNull("Couldn't find object!", obj);
    }

    @Test
    public void testObjectGraph() throws Exception {
        final Session session = repo.login();
        final FedoraObject object =
            objectService.findOrCreateObject(session, "/graphObject");
        final IdentifierTranslator subjects = new DefaultIdentifierTranslator();
        final Dataset graphStore = object.getPropertiesDataset(subjects);

        final String graphSubject = subjects.getSubject("/graphObject").getURI();

        assertFalse("Graph store should not contain JCR prefixes",
                    compile("jcr").matcher(graphStore.toString()).find());
        assertFalse("Graph store should contain our fcrepo prefix",
                    compile("fcrepo")
                    .matcher(graphStore.toString()).find());

        parseExecute("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                          "INSERT { <http://example/egbook> dc:title " +
                          "\"This is an example of an update that will be " +
                          "ignored\" } WHERE {}", graphStore);

        parseExecute("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                          "INSERT { <" + graphSubject + "> dc:title " +
                          "\"This is an example title\" } WHERE {}",
                          graphStore);


        final Value[] values = object.getNode().getProperty("dc:title").getValues();
        assertTrue(values.length > 0);

        assertTrue(values[0]
                       .getString(),
                      values[0]
                          .getString().equals("This is an example title"));


        parseExecute("PREFIX myurn: <info:myurn/>\n" +
                          "INSERT { <" + graphSubject + "> myurn:info " +
                          "\"This is some example data\"} WHERE {}",
                          graphStore);

        final Value value =
            object.getNode().getProperty(object.getNode().getSession()
                                         .getNamespacePrefix("info:myurn/") +
                                         ":info").getValues()[0];

        assertEquals("This is some example data", value.getString());

        parseExecute("PREFIX fedora-rels-ext: <"
                + RELATIONS_NAMESPACE + ">\n" +
                "INSERT { <" + graphSubject + "> fedora-rels-ext:" +
                "isPartOf <" + graphSubject + "> } WHERE {}", graphStore);

        assertTrue(object.getNode().getProperty("fedorarelsext:isPartOf")
                   .getValues()[0].getString(),
                   object.getNode().getProperty("fedorarelsext:isPartOf")
                   .getValues()[0].getString()
                   .equals(object.getNode().getIdentifier()));


        parseExecute("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
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

    @Test
    public void testObjectGraphWithUriProperty() throws RepositoryException {
        final Session session = repo.login();
        final FedoraObject object =
            objectService.findOrCreateObject(session, "/graphObject");
        final IdentifierTranslator subjects = new DefaultIdentifierTranslator();
        final String graphSubject = subjects.getSubject("/graphObject").getURI();
        final Dataset graphStore = object.getPropertiesDataset(subjects);

        parseExecute("PREFIX some: <info:some#>\n" +
                         "INSERT { <" + graphSubject + "> some:urlProperty " +
                         "<" + graphSubject + "> } WHERE {}", graphStore);

        final String prefix = session.getWorkspace().getNamespaceRegistry().getPrefix("info:some#");

        assertNotNull(object.getNode().getProperty(prefix + ":urlProperty"));

        assertEquals(object.getNode(), session.getNodeByIdentifier(
                object.getNode().getProperty(prefix + ":urlProperty_ref").getValues()[0].getString()));

        parseExecute("PREFIX some: <info:some#>\n" +
                         "DELETE { <" + graphSubject + "> some:urlProperty " +
                         "<" + graphSubject + "> } WHERE {}", graphStore);

        assertFalse(object.getNode().hasProperty(prefix + ":urlProperty_ref"));


        parseExecute("PREFIX some: <info:some#>\n" +
                         "INSERT DATA { <" + graphSubject + "> some:urlProperty <" + graphSubject + ">;\n" +
                         "       some:urlProperty <info:somewhere/else> . }", graphStore);

        assertEquals(1, object.getNode().getProperty(prefix + ":urlProperty_ref").getValues().length);
        assertEquals(object.getNode(), session.getNodeByIdentifier(
                object.getNode().getProperty(prefix + ":urlProperty_ref").getValues()[0].getString()));

    }
}
