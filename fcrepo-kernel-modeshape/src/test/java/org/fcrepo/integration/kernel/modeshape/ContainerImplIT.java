/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.integration.kernel.modeshape;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getReferencePropertyName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.jcr.AccessDeniedException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.modeshape.rdf.impl.PropertiesRdfContext;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * <p>{@link ContainerImplIT} class.</p>
 *
 * @author ksclarke
 * @author ajs6f
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class ContainerImplIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    ContainerService containerService;

    private Session session;

    private DefaultIdentifierTranslator subjects;

    @Before
    public void setUp() throws RepositoryException {
        session = repo.login();
        subjects = new DefaultIdentifierTranslator(session);

    }

    @Test
    public void testCreatedObject() throws RepositoryException {
        containerService.findOrCreate(session, "/testObject");
        session.save();
        session.logout();
        session = repo.login();
        final Container obj =
            containerService.findOrCreate(session, "/testObject");
        assertNotNull("Couldn't find object!", obj);
    }

    @Test
    public void testObjectGraph() throws Exception {
        final Container object =
            containerService.findOrCreate(session, "/graphObject");
        final Model model = object.getTriples(subjects, PropertiesRdfContext.class).asModel();

        final Resource graphSubject = subjects.reverse().convert(object);

        assertFalse("Graph store should not contain JCR prefixes",
                    compile("jcr").matcher(model.toString()).find());
        assertFalse("Graph store should contain our fcrepo prefix",
                    compile("fcrepo")
                    .matcher(model.toString()).find());

        object.updateProperties(subjects, "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "INSERT { <" + graphSubject + "> dc:title " +
            "\"This is an example title\" } WHERE {}", object.getTriples(subjects, PropertiesRdfContext.class));


        final Value[] values = object.getNode().getProperty("dc:title").getValues();
        assertTrue(values.length > 0);

        assertTrue(values[0]
                       .getString(),
                      values[0]
                          .getString().equals("This is an example title"));


        object.updateProperties(subjects, "PREFIX myurn: <info:myurn/>\n" +
                "INSERT { <" + graphSubject + "> myurn:info " +
                "\"This is some example data\"} WHERE {}", object.getTriples(subjects, PropertiesRdfContext.class));

        final Value value =
            object.getNode().getProperty(object.getNode().getSession()
                                         .getNamespacePrefix("info:myurn/") +
                                         ":info").getValues()[0];

        assertEquals("This is some example data", value.getString());

        object.updateProperties(subjects, "PREFIX dcterms: <http://purl.org/dc/terms/>\n" +
                "INSERT { <" + graphSubject + "> dcterms:" +
                "isPartOf <" + graphSubject + "> } WHERE {}", object.getTriples(subjects, PropertiesRdfContext.class));

        final Value refValue = object.getNode().getProperty("dcterms:isPartOf_ref").getValues()[0];
        assertTrue(refValue.getString(), refValue.getString().equals(object.getNode().getIdentifier()));


        object.updateProperties(subjects, "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "DELETE { <" + graphSubject + "> dc:title " +
                "\"This is an example title\" } WHERE {}", object.getTriples(subjects, PropertiesRdfContext.class));

        assertFalse("Found unexpected dc:title",
                    object.getNode().hasProperty("dc:title"));

        object.updateProperties(subjects, "PREFIX dcterms: <http://purl.org/dc/terms/>\n" +
                "DELETE { <" + graphSubject + "> " +
                "dcterms:isPartOf <" + graphSubject + "> " +
                "} WHERE {}", object.getTriples(subjects, PropertiesRdfContext.class));
        assertFalse("found unexpected reference",
                    object.getNode().hasProperty("dcterms:isPartOf"));

        session.save();

    }

    @Test
    public void testObjectGraphWithUriProperty() throws RepositoryException {
        final Container object =
            containerService.findOrCreate(session, "/graphObject");
        final Resource graphSubject = subjects.reverse().convert(object);

        object.updateProperties(subjects, "PREFIX some: <info:some#>\n" +
                "INSERT { <" + graphSubject + "> some:urlProperty " +
                "<" + graphSubject + "> } WHERE {}", object.getTriples(subjects, PropertiesRdfContext.class));

        final String prefix = session.getWorkspace().getNamespaceRegistry().getPrefix("info:some#");
        final String propertyName = prefix + ":urlProperty";
        final String referencePropertyName = getReferencePropertyName(propertyName);

        assertTrue(object.getNode().hasProperty(referencePropertyName));
        assertFalse(object.getNode().hasProperty(propertyName));

        assertEquals(object.getNode(), session.getNodeByIdentifier(
                object.getNode().getProperty(prefix + ":urlProperty_ref").getValues()[0].getString()));

        object.updateProperties(subjects, "PREFIX some: <info:some#>\n" +
                "DELETE { <" + graphSubject + "> some:urlProperty " +
                "<" + graphSubject + "> } WHERE {}", object.getTriples(subjects, PropertiesRdfContext.class));


        assertFalse(object.getNode().hasProperty(referencePropertyName));
        assertFalse(object.getNode().hasProperty(propertyName));

        object.updateProperties(subjects, "PREFIX some: <info:some#>\n" +
                "INSERT DATA { <" + graphSubject + "> some:urlProperty <" + graphSubject + ">;\n" +
                "       some:urlProperty <info:somewhere/else> . }",
                object.getTriples(subjects, PropertiesRdfContext.class));


        assertTrue(object.getNode().hasProperty(referencePropertyName));
        assertTrue(object.getNode().hasProperty(propertyName));

        assertEquals(1, object.getNode().getProperty(prefix + ":urlProperty_ref").getValues().length);
        assertEquals(object.getNode(), session.getNodeByIdentifier(
                object.getNode().getProperty(prefix + ":urlProperty_ref").getValues()[0].getString()));

    }

    @Test
    public void testUpdatingObjectGraphWithErrors() throws AccessDeniedException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, pid);

        MalformedRdfException e = null;
        try {
            object.updateProperties(subjects, "INSERT DATA { <> <info:some-property> <relative-url> . \n" +
                    "<> <info:some-other-property> <another-relative-url> }", new RdfStream());
        } catch (final MalformedRdfException ex) {
            e = ex;
        }

        assertNotNull("Expected an exception to get thrown", e);
        assertEquals("Excepted two nested exceptions", 2, e.getMessage().split("\n").length);
        assertTrue(e.getMessage().contains("/relative-url"));
        assertTrue(e.getMessage().contains("/another-relative-url"));
    }

    @Test
    public void testReplaceObjectGraphWithErrors() {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, pid);

        final Model model = createDefaultModel().read(
                toInputStream("<> <info:some-property> <relative-url> . \n" +
                                      "<> <info:some-other-property> <another-relative-url>"),
                subjects.reverse().convert(object).toString(),
                "TTL");
        MalformedRdfException e = null;
        try {
            object.replaceProperties(subjects, model, new RdfStream());
        } catch (final MalformedRdfException ex) {
            e = ex;
        }

        assertNotNull("Expected an exception to get thrown", e);
        assertEquals("Excepted two nested exceptions", 2, e.getMessage().split("\n").length);
        assertTrue(e.getMessage().contains("/relative-url"));
        assertTrue(e.getMessage().contains("/another-relative-url"));
    }

    @Test(expected = MalformedRdfException.class)
    public void testUpdatingObjectGraphWithOutOfDomainSubjects() throws AccessDeniedException, MalformedRdfException {
        final Container object =
            containerService.findOrCreate(session, "/graphObject");

        object.updateProperties(subjects, "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
          "INSERT { <http://example/egbook> dc:title " + "\"This is an example of an update that will be " +
          "ignored\" } WHERE {}", object.getTriples(subjects, PropertiesRdfContext.class));
    }

}
