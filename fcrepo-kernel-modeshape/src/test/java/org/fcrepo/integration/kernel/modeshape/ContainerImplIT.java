/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDstring;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FIELD_DELIMITER;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getReferencePropertyName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;

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
    FedoraRepository repo;

    @Inject
    ContainerService containerService;

    private FedoraSession session;

    private DefaultIdentifierTranslator subjects;

    @Before
    public void setUp() throws RepositoryException {
        session = repo.login();
        subjects = new DefaultIdentifierTranslator(getJcrSession(session));

    }

    @Test
    public void testCreatedObject() throws RepositoryException {
        containerService.findOrCreate(session, "/testObject");
        session.commit();
        session.expire();
        session = repo.login();
        final Container obj =
            containerService.findOrCreate(session, "/testObject");
        assertNotNull("Couldn't find object!", obj);
    }

    @Test
    public void testObjectGraph() throws Exception {
        final Container object =
            containerService.findOrCreate(session, "/graphObject");
        final Model model = object.getTriples(subjects, PROPERTIES).collect(toModel());

        final Resource graphSubject = subjects.reverse().convert(object);

        assertFalse("Graph store should not contain JCR prefixes",
                    compile("jcr").matcher(model.toString()).find());
        assertFalse("Graph store should contain our fcrepo prefix",
                    compile("fcrepo")
                    .matcher(model.toString()).find());

        object.updateProperties(subjects, "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
            "INSERT { <" + graphSubject + "> dc:title " +
            "\"This is an example title\" } WHERE {}", object.getTriples(subjects, PROPERTIES));


        final Value[] values = getJcrNode(object).getProperty("dc:title").getValues();
        assertTrue(values.length > 0);

        assertTrue(values[0]
                       .getString(),
                      values[0]
                          .getString().equals("This is an example title" + FIELD_DELIMITER + XSDstring.getURI()));


        object.updateProperties(subjects, "PREFIX myurn: <info:myurn/>\n" +
                "INSERT { <" + graphSubject + "> myurn:info " +
                "\"This is some example data\"} WHERE {}", object.getTriples(subjects, PROPERTIES));

        final Value value = getJcrNode(object).getProperty(getJcrNode(object).getSession()
                                         .getNamespacePrefix("info:myurn/") +
                                         ":info").getValues()[0];

        assertEquals("This is some example data" + FIELD_DELIMITER + XSDstring.getURI(), value.getString());

        object.updateProperties(subjects, "PREFIX dcterms: <http://purl.org/dc/terms/>\n" +
                "INSERT { <" + graphSubject + "> dcterms:" +
                "isPartOf <" + graphSubject + "> } WHERE {}", object.getTriples(subjects, PROPERTIES));

        final Value refValue = getJcrNode(object).getProperty("dcterms:isPartOf_ref").getValues()[0];
        assertTrue(refValue.getString(), refValue.getString().equals(getJcrNode(object).getIdentifier()));


        object.updateProperties(subjects, "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "DELETE { <" + graphSubject + "> dc:title " +
                "\"This is an example title\" } WHERE {}", object.getTriples(subjects, PROPERTIES));

        assertFalse("Found unexpected dc:title", getJcrNode(object).hasProperty("dc:title"));

        object.updateProperties(subjects, "PREFIX dcterms: <http://purl.org/dc/terms/>\n" +
                "DELETE { <" + graphSubject + "> " +
                "dcterms:isPartOf <" + graphSubject + "> " +
                "} WHERE {}", object.getTriples(subjects, PROPERTIES));
        assertFalse("found unexpected reference", getJcrNode(object).hasProperty("dcterms:isPartOf"));

        session.commit();

    }

    @Test
    public void testObjectGraphWithUriProperty() throws RepositoryException {
        final Container object =
            containerService.findOrCreate(session, "/graphObject");
        final Resource graphSubject = subjects.reverse().convert(object);
        final Session jcrSession = getJcrSession(session);

        object.updateProperties(subjects, "PREFIX some: <info:some#>\n" +
                "INSERT { <" + graphSubject + "> some:urlProperty " +
                "<" + graphSubject + "> } WHERE {}", object.getTriples(subjects, PROPERTIES));

        final String prefix = jcrSession.getWorkspace().getNamespaceRegistry().getPrefix("info:some#");
        final String propertyName = prefix + ":urlProperty";
        final String referencePropertyName = getReferencePropertyName(propertyName);

        assertTrue(getJcrNode(object).hasProperty(referencePropertyName));
        assertFalse(getJcrNode(object).hasProperty(propertyName));

        assertEquals(getJcrNode(object), jcrSession.getNodeByIdentifier(
                getJcrNode(object).getProperty(prefix + ":urlProperty_ref").getValues()[0].getString()));

        object.updateProperties(subjects, "PREFIX some: <info:some#>\n" +
                "DELETE { <" + graphSubject + "> some:urlProperty " +
                "<" + graphSubject + "> } WHERE {}", object.getTriples(subjects, PROPERTIES));


        assertFalse(getJcrNode(object).hasProperty(referencePropertyName));
        assertFalse(getJcrNode(object).hasProperty(propertyName));

        object.updateProperties(subjects, "PREFIX some: <info:some#>\n" +
                "INSERT DATA { <" + graphSubject + "> some:urlProperty <" + graphSubject + ">;\n" +
                "       some:urlProperty <info:somewhere/else> . }",
                object.getTriples(subjects, PROPERTIES));


        assertTrue(getJcrNode(object).hasProperty(referencePropertyName));
        assertTrue(getJcrNode(object).hasProperty(propertyName));

        assertEquals(1, getJcrNode(object).getProperty(prefix + ":urlProperty_ref").getValues().length);
        assertEquals(getJcrNode(object), jcrSession.getNodeByIdentifier(
                getJcrNode(object).getProperty(prefix + ":urlProperty_ref").getValues()[0].getString()));

    }

    @Test
    public void testUpdatingObjectGraphWithErrors() {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, pid);

        MalformedRdfException e = null;
        try {
            object.updateProperties(subjects, "INSERT DATA { <> <info:some-property> <relative-url> . \n" +
                    "<> <info:some-other-property> <another-relative-url> }",
                    object.getTriples(subjects, emptySet()));
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
                                      "<> <info:some-other-property> <another-relative-url>", UTF_8),
                subjects.reverse().convert(object).toString(),
                "TTL");
        MalformedRdfException e = null;
        try {
            object.replaceProperties(subjects, model, object.getTriples(subjects, emptySet()));
        } catch (final MalformedRdfException ex) {
            e = ex;
        }

        assertNotNull("Expected an exception to get thrown", e);
        assertEquals("Excepted two nested exceptions", 2, e.getMessage().split("\n").length);
        assertTrue(e.getMessage().contains("/relative-url"));
        assertTrue(e.getMessage().contains("/another-relative-url"));
    }

    @Test(expected = MalformedRdfException.class)
    public void testUpdatingObjectGraphWithOutOfDomainSubjects() throws MalformedRdfException {
        final Container object =
            containerService.findOrCreate(session, "/graphObject");

        object.updateProperties(subjects, "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
          "INSERT { <http://example/egbook> dc:title " + "\"This is an example of an update that will be " +
          "ignored\" } WHERE {}", object.getTriples(subjects, PROPERTIES));
    }

}
