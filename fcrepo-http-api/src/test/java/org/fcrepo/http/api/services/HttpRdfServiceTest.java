/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api.services;

import static org.fcrepo.config.ServerManagedPropsMode.STRICT;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.runner.RunWith;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.RdfStream;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for HttpRdfService
 * @author bseeger
 * @since 2019-11-08
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class HttpRdfServiceTest {

    private HttpIdentifierConverter idTranslator;

    @Mock
    private FedoraResource resource;

    private FedoraPropsConfig fedoraPropsConfig = new FedoraPropsConfig();

    @InjectMocks
    private HttpRdfService httpRdfService;

    private static final String HTTP_BASE_URI = "http://www.example.com/fedora/rest/";
    private static final String FEDORA_URI_1 = HTTP_BASE_URI + "resource1";
    private static final Resource FEDORA_URI_1_RESOURCE = ResourceFactory.createResource(FEDORA_URI_1);
    private static final String FEDORA_URI_2 = HTTP_BASE_URI + "resource2";
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

    public HttpRdfServiceTest() {
        fedoraPropsConfig.setServerManagedPropsMode(STRICT);
        idTranslator = new HttpIdentifierConverter(UriBuilder.fromUri(HTTP_BASE_URI + "{path: .*}"));
    }

    @Before
    public void setUp() {
        setField(httpRdfService, "fedoraPropsConfig", fedoraPropsConfig);
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
     * Test that binary descriptions URIs in subjects are converted to the binary IDs.
     */
    @Test
    public void testPutToInternal_BinaryDescriptionSubject() {
        final var binaryUri = HTTP_BASE_URI + UUID.randomUUID();
        final var binaryDescUri = binaryUri + "/" + FCR_METADATA;
        final var descriptionId = FedoraId.create(idTranslator.toInternalId(binaryDescUri));
        // Predicate to filter for a resource with the full binary description ID (i.e. ending in /fcr:metadata)
        final Predicate<Resource> keepDescId = a -> a.isURIResource() && a.hasURI(descriptionId.getFullId());
        final var rdf = "@prefix dc: <"  + DC.getURI() + "> ." +
                "@prefix dcterms: <"  + DCTerms.getURI() + "> ." +
                "<" + binaryDescUri + "> dc:title 'fancy title' ;" +
                "    dcterms:isPartOf <" + binaryUri + "> .";
        final InputStream requestBodyStream = new ByteArrayInputStream(rdf.getBytes());
        final Resource binaryRes = ResourceFactory.createResource(descriptionId.getFullDescribedId());
        final var translated = httpRdfService.bodyToInternalModel(descriptionId, requestBodyStream, CONTENT_TYPE,
                idTranslator, false);
        assertTrue(translated.contains(binaryRes, DCTerms.isPartOf, binaryRes));
        assertTrue(translated.contains(binaryRes, DC.title, ResourceFactory.createPlainLiteral("fancy title")));
        assertFalse(translated.listSubjects().filterKeep(keepDescId).hasNext());
    }

    /**
     * Test we don't touch binary description URIs in the object of a triple.
     */
    @Test
    public void testPutToInternal_BinaryDescriptionObject() {
        final var binaryUri = HTTP_BASE_URI + UUID.randomUUID();
        final var binaryDescUri = binaryUri + "/" + FCR_METADATA;
        final var descriptionId = FedoraId.create(idTranslator.toInternalId(binaryDescUri));
        // Predicate to filter for a resource with the full binary description ID (i.e. ending in /fcr:metadata)
        final Predicate<Resource> keepDescId = a -> a.isURIResource() && a.hasURI(descriptionId.getFullId());
        final var rdf = "@prefix dc: <"  + DC.getURI() + "> ." +
                "@prefix dcterms: <"  + DCTerms.getURI() + "> ." +
                "<" + binaryUri + "> dc:title 'fancy title' ;" +
                "    dcterms:isPartOf <" + binaryDescUri + "> .";
        final InputStream requestBodyStream = new ByteArrayInputStream(rdf.getBytes());
        final Resource binaryRes = ResourceFactory.createResource(descriptionId.getFullDescribedId());
        final Resource binaryDescRes = ResourceFactory.createResource(descriptionId.getFullId());
        final var translated = httpRdfService.bodyToInternalModel(descriptionId, requestBodyStream, CONTENT_TYPE,
                idTranslator, false);
        assertTrue(translated.contains(binaryRes, DCTerms.isPartOf, binaryDescRes));
        assertTrue(translated.contains(binaryRes, DC.title, ResourceFactory.createPlainLiteral("fancy title")));
        assertFalse(translated.listSubjects().filterKeep(keepDescId).hasNext());
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
