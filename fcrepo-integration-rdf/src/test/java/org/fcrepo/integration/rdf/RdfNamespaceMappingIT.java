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
package org.fcrepo.integration.rdf;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author bbpennel
 */
@Ignore // TODO FIX THESE TESTS
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

            assertFalse("Should not contain unregistered namespace mapping",
                    model.getNsPrefixMap().containsKey("asdf"));

            final Resource resc = model.getResource(subjectURI);
            assertTrue("Must return property from unregistered namespace",
                    resc.hasLiteral(createProperty("http://asdf.org/foo"), "bar"));
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

            assertTrue("Should contain fedora namespace prefix",
                    model.getNsPrefixMap().containsKey("fedora"));
            assertTrue("Should contain rdf namespace prefix",
                    model.getNsPrefixMap().containsKey("rdf"));

            final Resource resc = model.getResource(subjectURI);
            assertTrue("Must contain property using register namespaces",
                    resc.hasProperty(RDF_TYPE, FEDORA_CONTAINER));
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

            assertTrue("Should contain fedora namespace prefix",
                    model.getNsPrefixMap().containsKey("fedora"));

            assertFalse("Must not contain prefix for registered but unused namespace",
                    model.getNsPrefixMap().containsKey("unused"));
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

            assertTrue("No namespaces should be returned",
                    model.getNsPrefixMap().isEmpty());

            final Resource resc = model.getResource(subjectURI);
            assertTrue("Must contain property using register namespaces",
                    resc.hasProperty(RDF_TYPE, FEDORA_CONTAINER));
        }
    }
}
