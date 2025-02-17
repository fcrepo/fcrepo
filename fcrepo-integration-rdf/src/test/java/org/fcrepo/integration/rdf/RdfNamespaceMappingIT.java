/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.rdf;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.jupiter.api.Test;

/**
 * @author bbpennel
 */
public class RdfNamespaceMappingIT extends AbstractResourceIT {

    public static final Property RDF_TYPE = createProperty(RDF_NAMESPACE + "type");

    @Test
    public void testUnregisteredNamespace() throws Exception {
        final String pid = getRandomUniqueId();
        final String subjectURI = serverAddress + pid;

        // create object with rdf that contains a namespace declaration
        final String content = "@prefix asdf: <http://asdf.org/> . <> asdf:foo 'bar' .";
        try (final CloseableHttpResponse response = execute(putObjMethod(pid, "text/turtle", content))) {
            assertEquals(201, response.getStatusLine().getStatusCode());
        }

        // that namespace should come back without a prefix
        final HttpGet httpGet = getObjMethod(pid);
        httpGet.addHeader(ACCEPT, "text/turtle");
        final Model model = createDefaultModel();
        try (final CloseableHttpResponse getResponse = execute(httpGet)) {
            model.read(getResponse.getEntity().getContent(), subjectURI, "TURTLE");

            assertFalse(model.getNsPrefixMap().containsKey("asdf"),
                    "Should not contain unregistered namespace mapping");

            final Resource resc = model.getResource(subjectURI);
            assertTrue(resc.hasLiteral(createProperty("http://asdf.org/foo"), "bar"),
                    "Must return property from unregistered namespace");
        }
    }

    @Test
    public void testRegisteredNamespace() throws Exception {
        final String pid = getRandomUniqueId();
        final String subjectURI = serverAddress + pid;
        createObject(pid);

        final HttpGet httpGet = getObjMethod(pid);
        httpGet.addHeader(ACCEPT, "text/turtle");
        final Model model = createDefaultModel();
        try (final CloseableHttpResponse response = execute(httpGet)) {
            model.read(response.getEntity().getContent(), subjectURI, "TURTLE");

            assertTrue(model.getNsPrefixMap().containsKey("fedora"),
                    "Should contain fedora namespace prefix");
            assertTrue(model.getNsPrefixMap().containsKey("rdf"),
                    "Should contain rdf namespace prefix");

            final Resource resc = model.getResource(subjectURI);
            assertTrue(resc.hasProperty(RDF_TYPE, FEDORA_CONTAINER),
                    "Must contain property using register namespaces");
        }
    }

    @Test
    public void testUnusedNamespaceNotReturned() throws Exception {
        verifyUnusedNamespaceNotReturned("text/turtle", "TURTLE");
    }

    // Testing a serialization that cannot be block streamed
    @Test
    public void testUnusedNamespaceNotReturnedRdfXML() throws Exception {
        verifyUnusedNamespaceNotReturned("application/rdf+xml", "RDF/XML");
    }

    private void verifyUnusedNamespaceNotReturned(final String acceptType, final String rdfLang) throws Exception {
        final String pid = getRandomUniqueId();
        final String subjectURI = serverAddress + pid;
        createObject(pid);

        final HttpGet httpGet = getObjMethod(pid);
        httpGet.addHeader(ACCEPT, acceptType);
        final Model model = createDefaultModel();
        try (final CloseableHttpResponse response = execute(httpGet)) {
            model.read(response.getEntity().getContent(), subjectURI, rdfLang);

            assertTrue(model.getNsPrefixMap().containsKey("fedora"),
                    "Should contain fedora namespace prefix");

            assertFalse(model.getNsPrefixMap().containsKey("unused"),
                    "Must not contain prefix for registered but unused namespace");
        }
    }

    @Test
    public void testUnprefixedSerialization() throws Exception {
        final String pid = getRandomUniqueId();
        final String subjectURI = serverAddress + pid;
        createObject(pid);

        final HttpGet httpGet = getObjMethod(pid);
        httpGet.addHeader(ACCEPT, "application/n-triples");
        final Model model = createDefaultModel();
        try (final CloseableHttpResponse response = execute(httpGet)) {
            model.read(response.getEntity().getContent(), subjectURI, "NTRIPLES");

            assertTrue(model.getNsPrefixMap().isEmpty(),
                    "No namespaces should be returned");

            final Resource resc = model.getResource(subjectURI);
            assertTrue(resc.hasProperty(RDF_TYPE, FEDORA_CONTAINER),
                    "Must contain property using register namespaces");
        }
    }
}
