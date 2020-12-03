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
package org.fcrepo.http.api.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import javax.ws.rs.core.MediaType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import org.junit.runner.RunWith;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.RdfStream;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

/**
 * Unit tests for HttpRdfService
 * @author bseeger
 * @since 2019-11-08
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class HttpRdfServiceTest {

    private static final Logger log = getLogger(HttpRdfService.class);

    @Mock
    HttpIdentifierConverter idTranslator;

    @Mock
    FedoraResource resource;

    @InjectMocks
    private HttpRdfService httpRdfService;

    private static final String FEDORA_URI_1 = "http://www.example.com/fedora/rest/resource1";
    private static final Resource FEDORA_URI_1_RESOURCE = ResourceFactory.createResource(FEDORA_URI_1);
    private static final String FEDORA_URI_2 = "http://www.example.com/fedora/rest/resource2";
    private static final Resource FEDORA_URI_2_RESOURCE = ResourceFactory.createResource(FEDORA_URI_2);
    private static final FedoraId FEDORA_ID_1 = FedoraId.create("info:fedora/resource1");
    private static final Resource FEDORA_ID_1_RESOURCE = ResourceFactory.createResource(FEDORA_ID_1.getFullId());
    private static final FedoraId FEDORA_ID_2 = FedoraId.create("info:fedora/resource2");
    private static final Resource FEDORA_ID_2_RESOURCE = ResourceFactory.createResource(FEDORA_ID_2.getFullId());
    private static final String NON_FEDORA_URI = "http://www.otherdomain.org/resource5";
    private static final Resource NON_FEDORA_URI_RESOURCE = ResourceFactory.createResource(NON_FEDORA_URI);
    private static final String RDF =
                "@prefix dc: <"  + DC.getURI() + "> ." +
                "@prefix dcterms: <"  + DCTerms.getURI() + "> ." +
                "<" + FEDORA_URI_1 + "> dc:title 'fancy title' ;" +
                "    dcterms:isPartOf <" + NON_FEDORA_URI + "> ;" +
                "    dcterms:isPartOf <" + FEDORA_URI_2 + "> .";
    private static final String INTERNAL_RDF =
                "@prefix dc: <"  + DC.getURI() + "> ." +
                "@prefix dcterms: <"  + DCTerms.getURI() + "> ." +
                "<" + FEDORA_ID_1 + "> dc:title 'fancy title' ;" +
                "    dcterms:isPartOf <" + NON_FEDORA_URI + "> ;" +
                "    dcterms:isPartOf <" + FEDORA_ID_2 + "> .";
    private static final MediaType CONTENT_TYPE = new MediaType("text", "turtle");

    @Before
    public void setup() {
        when(idTranslator.toInternalId(FEDORA_URI_1)).thenReturn(FEDORA_ID_1.getFullId());
        when(idTranslator.toInternalId(FEDORA_URI_2)).thenReturn(FEDORA_ID_2.getFullId());
        when(idTranslator.inExternalDomain(FEDORA_URI_1)).thenReturn(true);
        when(idTranslator.inExternalDomain(FEDORA_URI_2)).thenReturn(true);
        when(idTranslator.inExternalDomain(NON_FEDORA_URI)).thenReturn(false);

        when(idTranslator.toExternalId(FEDORA_ID_1.getFullId())).thenReturn(FEDORA_URI_1);
        when(idTranslator.toExternalId(FEDORA_ID_2.getFullId())).thenReturn(FEDORA_URI_2);
        when(idTranslator.inInternalDomain(FEDORA_ID_1.getFullId())).thenReturn(true);
        when(idTranslator.inInternalDomain(FEDORA_ID_2.getFullId())).thenReturn(true);
        when(idTranslator.inInternalDomain(NON_FEDORA_URI)).thenReturn(false);

        when(resource.getId()).thenReturn(FEDORA_ID_1.getFullId());

        log.debug("Rdf is: {}", RDF);
    }

    @Test
    public void testGetModelFromInputStream() {
        final InputStream requestBodyStream = new ByteArrayInputStream(RDF.getBytes());
        final Model model = httpRdfService.bodyToInternalModel(FEDORA_ID_1, requestBodyStream,
            CONTENT_TYPE, idTranslator, false);

        assertFalse(model.isEmpty());

        verifyTriples(model);
    }

    @Test
    public void testConvertInternalToExternalStream() {
        final InputStream requestBodyStream = new ByteArrayInputStream(INTERNAL_RDF.getBytes());
        final Node topic = NodeFactory.createURI(FEDORA_ID_1.getEncodedFullId());
        final Model internalGraph = ModelFactory.createDefaultModel();
        internalGraph.read(requestBodyStream, FEDORA_ID_1.getEncodedFullId(), "TURTLE");
        final RdfStream stream = fromModel(topic, internalGraph);

        final RdfStream converted = httpRdfService.bodyToExternalStream(FEDORA_ID_1.getFullId(), stream,
                idTranslator);

        final Model model = converted.collect(toModel());
        assertFalse(model.contains(FEDORA_ID_1_RESOURCE, DCTerms.isPartOf, NON_FEDORA_URI_RESOURCE));
        assertTrue(model.contains(FEDORA_URI_1_RESOURCE, DCTerms.isPartOf, NON_FEDORA_URI_RESOURCE));

        assertFalse(model.contains(FEDORA_ID_1_RESOURCE, DCTerms.isPartOf, FEDORA_ID_2_RESOURCE));
        assertTrue(model.contains(FEDORA_URI_1_RESOURCE, DCTerms.isPartOf, FEDORA_URI_2_RESOURCE));

        assertFalse(model.containsResource(FEDORA_ID_1_RESOURCE));
        assertFalse(model.containsResource(FEDORA_ID_2_RESOURCE));
    }

    @Test
    public void testPatchToInternal_EmptySubjectUpdate() {
        final String rdf = "prefix test: <http://example.org#> INSERT { <> test:pointer <http://other.com/resource>" +
                " } WHERE {}";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_1, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> <http://other.com/resource>",
                translated);
    }

    @Test
    public void testPatchToInternal_EmptySubjectInsert() {
        final String rdf = "prefix test: <http://example.org#> INSERT DATA { <> test:pointer " +
                "<http://other.com/resource> }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_1, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> <http://other.com/resource>",
                translated);
    }

    @Test
    public void testPatchToInternal_EmptySubjectDelete() {
        final String rdf = "prefix test: <http://example.org#> DELETE DATA { <> test:pointer " +
                "<http://other.com/resource> }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_1, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> <http://other.com/resource>",
                translated);
    }

    @Test
    public void testPatchToInternal_ExplicitSubjectUpdate() {
        final String rdf = "prefix test: <http://example.org#> INSERT { <" + FEDORA_URI_1 + "> test:pointer " +
                "<http://other.com/resource> } WHERE {}";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_1, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> <http://other.com/resource>",
                translated);
    }

    @Test
    public void testPatchToInternal_ExplicitSubjectInsert() {
        final String rdf = "prefix test: <http://example.org#> INSERT DATA { <" + FEDORA_URI_1 + "> test:pointer " +
                "<http://other.com/resource> }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_1, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> <http://other.com/resource>",
                translated);
    }

    @Test
    public void testPatchToInternal_ExplicitSubjectDelete() {
        final String rdf = "prefix test: <http://example.org#> DELETE DATA { <" + FEDORA_URI_1 + "> test:pointer " +
                "<http://other.com/resource> }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_1, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> <http://other.com/resource>",
                translated);
    }

    @Test
    public void testPatchToInternal_EmptySubjectInsertDeleteWhere() {
        final String rdf = "prefix test: <http://example.org#> DELETE { <> test:pointer ?o } INSERT { <> test:pointer" +
                " <http://other.com/resource> } WHERE { <> test:pointer ?o }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_1, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> ?o ", translated);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> <http://other.com/resource>",
                translated);
    }

    @Test
    public void testPatchToInternal_EmptySubjectObjectInsert() {
        final String rdf = "prefix test: <http://example.org#> INSERT DATA { <> test:pointer " +
                "<" + FEDORA_URI_2 + "> }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_1, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> <" + FEDORA_ID_2 + ">",
                translated);
    }

    @Test
    public void testPatchToInternal_ExplicitSubjectObjectInsert() {
        final String rdf = "prefix test: <http://example.org#> INSERT DATA { <" + FEDORA_URI_1 + "> " +
                "test:pointer <" + FEDORA_URI_2 + "> }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_1, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> <" + FEDORA_ID_2 + ">",
                translated);
    }

    @Test
    public void testPatchToInternal_EmptySubjectExplicitObjectDelete() {
        final String rdf = "prefix test: <http://example.org#> DELETE DATA { <> test:pointer <" + FEDORA_URI_2 + "> }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_1, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> <" + FEDORA_ID_2 + ">",
                translated);
    }

    @Test
    public void testPatchToInternal_ExplicitSubjectObjectDelete() {
        final String rdf = "prefix test: <http://example.org#> DELETE DATA { <" + FEDORA_URI_1 + "> test:pointer <" +
                FEDORA_URI_2 + "> }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_1, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> <" + FEDORA_ID_2 + ">",
                translated);
    }

    @Test
    public void testPatchToInternal_EmptySubjectExplicitObjectUpdate() {
        final String rdf = "prefix test: <http://example.org#> DELETE { <> test:pointer ?o } INSERT { <> test:pointer" +
                " <" + FEDORA_URI_2 + "> } WHERE { <> test:pointer ?o }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_1, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> <" + FEDORA_ID_2 + ">",
                translated);
        assertStringMatch("<" + FEDORA_ID_1 + "> <http://example.org#pointer> ?o", translated);
    }

    @Test
    public void testPatchToInternal_ExplicitSubjectObjectUpdate() {
        final String rdf = "prefix test: <http://example.org#> DELETE { <" + FEDORA_URI_2 + "> " +
                "test:pointer ?o } INSERT { <" + FEDORA_URI_2 + "> test:pointer" +
                " <" + FEDORA_URI_1 + "> } WHERE { <" + FEDORA_URI_2 + "> test:pointer ?o }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_2, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_2 + "> <http://example.org#pointer> <" + FEDORA_ID_1 + ">",
                translated);
        assertStringMatch("<" + FEDORA_ID_2 + "> <http://example.org#pointer> ?o", translated);
    }

    @Test
    public void testPatchToInternal_ExplicitSubjectObjectUpdateWhereSubject() {
        final String rdf = "prefix test: <http://example.org#> INSERT { <" + FEDORA_URI_2 + "> test:pointer" +
                " <" + FEDORA_URI_1 + "> } WHERE { <" + FEDORA_URI_2 + "> test:pointer ?o }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_2, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_2 + "> <http://example.org#pointer> " +
                "<" + FEDORA_ID_1 + ">", translated);
        assertStringMatch("<" + FEDORA_ID_2 + "> <http://example.org#pointer> ?o", translated);
    }

    @Test
    public void testPatchToInternal_ExplicitSubjectObjectUpdateWhereObject() {
        final String rdf = "prefix test: <http://example.org#> INSERT { <" + FEDORA_URI_2 + "> test:pointer" +
                " <" + FEDORA_URI_1 + "> } WHERE { ?o test:pointer <" + FEDORA_URI_2 + "> }";
        final String translated = httpRdfService.patchRequestToInternalString(FEDORA_ID_2, rdf, idTranslator);
        assertStringMatch("<" + FEDORA_ID_2 + "> <http://example.org#pointer> " +
                "<" + FEDORA_ID_1 + ">", translated);
        assertStringMatch("?o <http://example.org#pointer> <" + FEDORA_ID_2 + ">", translated);
    }

    /**
     * Assert 2 strings match regardless of whitespace.
     * @param expected the expected string.
     * @param comparison the string to test.
     */
    private void assertStringMatch(final String expected, final String comparison) {
        final String cleanedExpected = expected.replaceAll("[\t\n]", "")
                .replaceAll(" {2,}", " ");
        final String cleanedTest = comparison.replaceAll("[\t\n]", "")
                .replaceAll(" {2,}", " ");
        assertEquals(cleanedExpected, cleanedExpected);
    }

    private void verifyTriples(final Model model)  {

        final Resource fedoraResource = model.createResource(FEDORA_ID_1.getEncodedFullId());

        // make sure it changed the fedora uri to id, but didn't touch the non fedora domain
        assertTrue(model.contains(fedoraResource, DCTerms.isPartOf, model.createResource(NON_FEDORA_URI)));

        // make sure it translated the fedora uri to fedora id for this triple in both the subject and object
        assertTrue(model.contains(fedoraResource, DCTerms.isPartOf,
                model.createResource(FEDORA_ID_2.getEncodedFullId())));

        // make sure there are no triples that have the subject of the fedora uri
        assertFalse(model.containsResource(model.createResource(FEDORA_URI_1)));
    }
}
