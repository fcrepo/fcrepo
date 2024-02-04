/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Calendar.getInstance;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.TimeZone.getTimeZone;
import static java.util.stream.StreamSupport.stream;
import static jakarta.ws.rs.core.HttpHeaders.ALLOW;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.HttpHeaders.LINK;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createLiteralByValue;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ModelFactory.createModelForGraph;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.http.commons.test.util.TestHelpers.parseTriples;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.PREFER_SERVER_MANAGED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Link;
import jakarta.xml.bind.DatatypeConverter;

import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.http.commons.test.util.CloseableDataset;
import org.fcrepo.kernel.api.utils.GraphDifferencer;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.datatypes.xsd.impl.XSDDateTimeType;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.RDF;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mike Durbin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraRelaxedLdpIT extends AbstractResourceIT {

    private final String providedUsername = "provided-username";
    private Calendar providedDate;

    public FedoraRelaxedLdpIT() {
        providedDate = nowUTC();
        providedDate.add(Calendar.YEAR, -20);
    }

    @Before
    public void switchToRelaxedMode() {
        propsConfig.setServerManagedPropsMode(ServerManagedPropsMode.RELAXED);
    }


    @Test
    public void testBasicPutRoundtrip() throws IOException {
        final String subjectURI;
        try (final CloseableHttpResponse response = execute(postObjMethod())) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        final String body;
        final HttpGet get = new HttpGet(subjectURI);
        try (final CloseableHttpResponse response = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            body = EntityUtils.toString(response.getEntity());
        }

        final HttpPut put = new HttpPut(subjectURI);
        put.setEntity(new StringEntity(body));
        put.setHeader(CONTENT_TYPE, "text/turtle");
        put.addHeader("Prefer", "handling=lenient");
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(put));
    }

    @Test
    public void testCreateResourceWithSpecificCreationInformationIsAllowed() throws IOException {
        assertEquals(ServerManagedPropsMode.RELAXED, propsConfig.getServerManagedPropsMode()); // sanity check
        final String subjectURI;
        try (final CloseableHttpResponse response = postResourceWithTTL(
                getTTLThatUpdatesServerManagedTriples(providedUsername, providedDate, null, null))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            triples(subjectURI, dataset)
                    .mustHave(CREATED_BY.asNode(), createLiteral(providedUsername))
                    .mustHave(CREATED_DATE.asNode(), createDateTime(providedDate));
        }
    }

    @Test
    public void testUpdateNonRdfResourceWithSpecificInformationIsAllowed() throws IOException {
        assertEquals(ServerManagedPropsMode.RELAXED, propsConfig.getServerManagedPropsMode()); // sanity check

        final String subjectURI;
        try (final CloseableHttpResponse response = postBinaryResource(serverAddress, "this is the binary")) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        final String describedByURI = subjectURI + "/fcr:metadata";

        try (final CloseableHttpResponse response = putResourceWithTTL(describedByURI,
                getTTLThatUpdatesServerManagedTriples(providedUsername, providedDate,
                                                      providedUsername, providedDate))) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        try (final CloseableDataset dataset = getDataset(new HttpGet(describedByURI))) {
            triples(subjectURI, dataset)
                    .mustHave(CREATED_BY.asNode(), createLiteral(providedUsername))
                    .mustHave(CREATED_DATE.asNode(), createDateTime(providedDate))
                    .mustHave(LAST_MODIFIED_BY.asNode(), createLiteral(providedUsername))
                    .mustHave(LAST_MODIFIED_DATE.asNode(), createDateTime(providedDate));
        }
    }

    @Test
    public void testValidSparqlUpdate() throws IOException {
        assertEquals(ServerManagedPropsMode.RELAXED, propsConfig.getServerManagedPropsMode()); // sanity check

        final String subjectURI;
        try (final CloseableHttpResponse response = postResourceWithTTL(
                getTTLThatUpdatesServerManagedTriples(providedUsername, providedDate, null, null))) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        final Calendar updatedDate = Calendar.getInstance(getTimeZone("UTC"));
        final String sparqlUpdate = "PREFIX fedora: <http://fedora.info/definitions/v4/repository#>\n" +
                "\n" +
                "DELETE { <> fedora:created \"" + DatatypeConverter.printDateTime(providedDate)
                        + "\"^^<http://www.w3.org/2001/XMLSchema#dateTime> }\n" +
                "INSERT { <> fedora:created \"" + DatatypeConverter.printDateTime(updatedDate)
                        + "\"^^<http://www.w3.org/2001/XMLSchema#dateTime> }\n" +
                "WHERE { }";
        try (final CloseableHttpResponse response = patchResource(subjectURI, sparqlUpdate)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            triples(subjectURI, dataset)
                    .mustHave(CREATED_BY.asNode(), createLiteral(providedUsername))
                    .mustHave(CREATED_DATE.asNode(), createDateTime(updatedDate));
        }
    }

    @Test
    public void testInvalidSparqlUpdate() throws IOException {
        assertEquals(ServerManagedPropsMode.RELAXED, propsConfig.getServerManagedPropsMode()); // sanity check

        final String subjectURI;
        try (final CloseableHttpResponse response = execute(postObjMethod())) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        final Calendar updatedDate = Calendar.getInstance(getTimeZone("UTC"));
        final String sparqlUpdate = "PREFIX fedora: <http://fedora.info/definitions/v4/repository#>\n" +
                "\n" +
                "INSERT { <> fedora:created \"" + DatatypeConverter.printDateTime(updatedDate) +
                "\"^^<http://www.w3.org/2001/XMLSchema#dateTime> .\n" +
                " <> fedora:created \"" + DatatypeConverter.printDateTime(providedDate) +
                "\"^^<http://www.w3.org/2001/XMLSchema#dateTime> }\n" +
                "WHERE { }";
        try (final CloseableHttpResponse response = patchResource(subjectURI, sparqlUpdate)) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
        }

        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            triples(subjectURI, dataset)
                    .mustNotHave(CREATED_BY.asNode(), createLiteral(providedUsername))
                    .mustNotHave(CREATED_DATE.asNode(), createDateTime(updatedDate));
        }
    }

    @Test
    public void testUpdateResourceWithSpecificModificationInformationIsAllowed() throws IOException {
        assertEquals(ServerManagedPropsMode.RELAXED, propsConfig.getServerManagedPropsMode()); // sanity check

        final String subjectURI;
        try (final CloseableHttpResponse response = createObject()) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            subjectURI = getLocation(response);
        }

        providedDate = nowUTC();
        providedDate.add(Calendar.MONTH, 1);

        try (final CloseableHttpResponse response = putResourceWithTTL(subjectURI,
                getTTLThatUpdatesServerManagedTriples(null, null, providedUsername, providedDate))) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        try (final CloseableDataset dataset = getDataset(new HttpGet(subjectURI))) {
            triples(subjectURI, dataset)
                    .mustHave(LAST_MODIFIED_BY.asNode(), createLiteral(providedUsername))
                    .mustHave(LAST_MODIFIED_DATE.asNode(), createDateTime(providedDate));
        }
    }

    /**
     * Tests a lossless roundtrip of a resource.
     * @throws IOException if an error occurs while reading or writing to repository over HTTP
     */
    @Test
    public void testRoundtripping() throws IOException {
        assertEquals(ServerManagedPropsMode.RELAXED, propsConfig.getServerManagedPropsMode()); // sanity check

        // POST a resource with one user-managed triple
        final String containerURI;
        try (final CloseableHttpResponse response
                     = postResourceWithTTL("<> <http://purl.org/dc/elements/1.1/title> 'title' .")) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            containerURI = getLocation(response);
        }

        // POST a non-rdf child resource
        final String containedBinaryURI;
        final String containedBinaryDescriptionURI;
        try (final CloseableHttpResponse response = postBinaryResource(containerURI, "content")) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            containedBinaryURI = getLocation(response);
            containedBinaryDescriptionURI = containedBinaryURI + "/fcr:metadata";
        }

        // export the RDF of the container
        final String containerBody;
        try (final CloseableHttpResponse response = execute(new HttpGet(containerURI))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            containerBody = EntityUtils.toString(response.getEntity());
        }

        // export the RDF of the child
        final String containedBinaryDescriptionBody;
        try (final CloseableHttpResponse response = execute(new HttpGet(containedBinaryDescriptionURI))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            containedBinaryDescriptionBody = EntityUtils.toString(response.getEntity());
        }


        // delete the container and its tombstone
        try (final CloseableHttpResponse response = execute(new HttpDelete(containerURI))) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
            try (final CloseableHttpResponse r2 = execute(new HttpGet(containerURI))) {
                final Link tombstone = Link.valueOf(r2.getFirstHeader(LINK).getValue());
                assertEquals("hasTombstone", tombstone.getRel());
                assertEquals(NO_CONTENT.getStatusCode(), getStatus(new HttpDelete(tombstone.getUri())));
            }
        }


        // post the container from the export
        final String containerRdf = filterRdf(containerBody, CREATED_BY, CREATED_DATE, LAST_MODIFIED_BY,
                LAST_MODIFIED_DATE, createProperty("http://purl.org/dc/elements/1.1/title"));
        try (final CloseableHttpResponse response = postResourceWithTTL(containerURI, containerRdf)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }

        // post the binary then patch its metadata to match the export
        try (final CloseableHttpResponse response = postBinaryResource(containerURI,
                containedBinaryURI.substring(containerURI.length() + 1), "content")) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
        }
        final String sparqlUpdate;
        try (final CloseableDataset d = getDataset(new HttpGet(containedBinaryDescriptionURI))) {
            final Model model = createDefaultModel();
            model.read(new ByteArrayInputStream(containedBinaryDescriptionBody.getBytes()), "", "N3");
            final Stream<Statement> statements =
                    stream(spliteratorUnknownSize(d.getDefaultModel().listStatements(), IMMUTABLE), false);
            final GraphDifferencer diff = new GraphDifferencer(model, statements.map(Statement::asTriple));
            sparqlUpdate = buildSparqlUpdate(diff.difference(), diff.notCommon(), CREATED_BY, CREATED_DATE,
                    LAST_MODIFIED_BY, LAST_MODIFIED_DATE);
        }

        try (final CloseableHttpResponse response = patchResource(containedBinaryDescriptionURI, sparqlUpdate)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        // Because the creation of the contained resource resulted in an implicit modification to
        // the triples on the parent, we need to overwrite them again...  This is done doing a PUT
        // but could be achieved using a sparql update.
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(putResourceWithTTL(containerURI, containerRdf)));

        assertIdentical(containerURI, containerBody);
        assertIdentical(containedBinaryDescriptionURI, containedBinaryDescriptionBody);
    }

    @Test
    public void testChangeInteractionModelPutRdf() throws Exception {
        final var postBinary = postObjMethod();
        postBinary.setHeader(CONTENT_TYPE, "text/plain");
        postBinary.setEntity(new StringEntity("some test data"));
        final String binaryUri;
        // Create a binary
        try (final var response = execute(postBinary)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            binaryUri = getLocation(response);
        }
        final Node binaryNode = createURI(binaryUri);
        final String oldDate;
        // Get the binary description
        try (final var dataset = getDataset(new HttpGet(binaryUri + "/" + FCR_METADATA))) {
            final var graph = dataset.asDatasetGraph();
            // Assert it has a NonRDFSource type
            assertTrue(graph.contains(ANY, binaryNode, RDF.type.asNode(), NON_RDF_SOURCE.asNode()));
            final var iter = graph.find(ANY, binaryNode, CREATED_DATE.asNode(), ANY);
            assertTrue(iter.hasNext());
            final var stmt = iter.next();
            // We'll change the createdDate
            oldDate = stmt.getObject().getLiteralValue().toString();
        }
        // Now GET without server managed properties
        final ByteArrayOutputStream newModel = new ByteArrayOutputStream();
        final var newDate = "2000-01-01T00:00:00Z";
        // Instant one day after the day we are trying to set.
        final var newDatePlusOneInstant = Instant.from(ISO_INSTANT.parse(newDate))
                .plus(1, ChronoUnit.DAYS);
        final var getDesc = new HttpGet(binaryUri + "/" + FCR_METADATA);
        getDesc.addHeader("Prefer", preferLink(null, PREFER_SERVER_MANAGED.getURI()));
        try (final var dataset = getDataset(getDesc)) {
            final var graph = dataset.asDatasetGraph();
            // Now add some fields to the new model
            final var newGraph = graph.getDefaultGraph();
            newGraph.add(Triple.create(binaryNode, RDF.type.asNode(), DIRECT_CONTAINER.asNode()));
            newGraph.add(Triple.create(binaryNode, CREATED_DATE.asNode(),
                    createLiteral(newDate, XSDDateTimeType.XSDdateTime)));
            RDFDataMgr.write(newModel, newGraph, RDFFormat.NTRIPLES_UTF8);
        }
        // Now PUT this new graph back
        final var putBinaryDescription = new HttpPut(binaryUri + "/" + FCR_METADATA);
        putBinaryDescription.setHeader(CONTENT_TYPE, "application/n-triples");
        putBinaryDescription.setEntity(new ByteArrayEntity(newModel.toByteArray()));
        assertEquals(CONFLICT.getStatusCode(), getStatus(putBinaryDescription));
        // Check that the date didn't change either.
        try (final var dataset = getDataset(new HttpGet(binaryUri + "/" + FCR_METADATA))) {
            final var graph = dataset.asDatasetGraph();
            // Assert it has a NonRDFSource type
            assertTrue(graph.contains(ANY, binaryNode, RDF.type.asNode(), NON_RDF_SOURCE.asNode()));
            final var dateIter = graph.find(ANY, binaryNode, CREATED_DATE.asNode(), ANY);
            // Assert at least one date
            assertTrue(dateIter.hasNext());
            final var graphDate = dateIter.next();
            // Assert only one date
            assertFalse(dateIter.hasNext());
            final var graphDateInstant =
                    Instant.from(ISO_INSTANT.parse(graphDate.getObject().getLiteralValue().toString()));
            // Assert the date is after the date we tried to set
            assertTrue(graphDateInstant.isAfter(newDatePlusOneInstant));
            assertTrue(graph.contains(ANY, binaryNode, CREATED_DATE.asNode(),
                    createLiteral(oldDate, XSDDateTimeType.XSDdateTime)));
            assertFalse(graph.contains(ANY, binaryNode, RDF.type.asNode(), DIRECT_CONTAINER.asNode()));
            assertFalse(graph.contains(ANY, binaryNode, CREATED_DATE.asNode(),
                    createLiteral(newDate, XSDDateTimeType.XSDdateTime)));
        }
    }

    @Test
    public void testChangeInteractionModelPatchRdf() throws Exception {
        final var postBinary = postObjMethod();
        postBinary.setHeader(CONTENT_TYPE, "text/plain");
        postBinary.setEntity(new StringEntity("some test data"));
        final String binaryUri;
        // Create a binary
        try (final var response = execute(postBinary)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            binaryUri = getLocation(response);
        }
        final Node binaryNode = createURI(binaryUri);
        // Get the binary description
        try (final var dataset = getDataset(new HttpGet(binaryUri + "/" + FCR_METADATA))) {
            final var graph = dataset.asDatasetGraph();
            // Assert it has a NonRDFSource type
            assertTrue(graph.contains(ANY, binaryNode, RDF.type.asNode(), NON_RDF_SOURCE.asNode()));
            assertTrue(graph.contains(ANY, binaryNode, CREATED_DATE.asNode(), ANY));
        }
        // Now PUT this new graph back
        final var newDate = "2000-01-01T00:00:00Z";
        // Instant one day after the day we are trying to set.
        final var newDatePlusOneInstant = Instant.from(ISO_INSTANT.parse(newDate))
                .plus(1, ChronoUnit.DAYS);
                final var patchBinaryDescription = new HttpPatch(binaryUri + "/" + FCR_METADATA);
        patchBinaryDescription.setHeader(CONTENT_TYPE, "application/sparql-update");
        patchBinaryDescription.setEntity(new StringEntity("DELETE { <> <" + RDF.type + "> ?type ; " +
                "<" + CREATED_DATE + "> ?date . } INSERT { <> <" + RDF.type + "> <" + DIRECT_CONTAINER + "> ; " +
                "<" + CREATED_DATE + "> \"" + newDate + "\"^^<" +
                XSDDateTimeType.XSDdateTime.getURI() + "> . } WHERE { <> <" + RDF.type + "> ?type ; " +
                "<" + CREATED_DATE + "> ?date . } "));
        assertEquals(CONFLICT.getStatusCode(), getStatus(patchBinaryDescription));
        // Check that the date didn't change either.
        try (final var dataset = getDataset(new HttpGet(binaryUri + "/" + FCR_METADATA))) {
            final var graph = dataset.asDatasetGraph();
            // Assert it has a NonRDFSource type
            assertTrue(graph.contains(ANY, binaryNode, RDF.type.asNode(), NON_RDF_SOURCE.asNode()));
            assertFalse(graph.contains(ANY, binaryNode, RDF.type.asNode(), DIRECT_CONTAINER.asNode()));
            final var dateIter = graph.find(ANY, binaryNode, CREATED_DATE.asNode(), ANY);
            // Assert at least one date
            assertTrue(dateIter.hasNext());
            final var graphDate = dateIter.next();
            // Assert only one date
            assertFalse(dateIter.hasNext());
            final var graphDateInstant =
                    Instant.from(ISO_INSTANT.parse(graphDate.getObject().getLiteralValue().toString()));
            // Assert the date is after the date we tried to set
            assertTrue(graphDateInstant.isAfter(newDatePlusOneInstant));
        }
    }

    @Test
    public void testPutNonRelaxablePredicates() throws Exception {
        final var postBinary = postObjMethod();
        postBinary.setHeader(CONTENT_TYPE, "text/plain");
        postBinary.setEntity(new StringEntity("some test data"));
        final String binaryUri;
        // Create a binary
        try (final var response = execute(postBinary)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            binaryUri = getLocation(response);
        }
        final String binaryDescription = binaryUri + "/" + FCR_METADATA;
        // Get the original body.
        final var getObj = new HttpGet(binaryDescription);
        getObj.addHeader(ALLOW, "application/n-triples");
        final String originalBody;
        try (final var response = execute(getObj)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            originalBody = IOUtils.toString(response.getEntity().getContent(), UTF_8);
        }
        // Can't set a non-relaxable property, even in relaxed mode.
        final var put = new HttpPut(binaryDescription);
        put.setHeader(CONTENT_TYPE, "application/n-triples");
        put.setEntity(new StringEntity("<" + binaryUri + "> <" + HAS_MESSAGE_DIGEST + "> \"not a real digest\" .\n" +
                "<" + binaryUri + "> <" + CREATED_BY + "> \"some-test-user\" ."));
        assertEquals(CONFLICT.getStatusCode(), getStatus(put));

        // Get the body again.
        final String newBody;
        try (final var response = execute(getObj)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            newBody = IOUtils.toString(response.getEntity().getContent(), UTF_8);
        }
        confirmResponseBodyNTriplesAreEqual(originalBody, newBody);
    }

    @Test
    public void testPatchNonRelaxablePredicates() throws Exception {
        final var postBinary = postObjMethod();
        postBinary.setHeader(CONTENT_TYPE, "text/plain");
        postBinary.setEntity(new StringEntity("some test data"));
        final String binaryUri;
        // Create a binary
        try (final var response = execute(postBinary)) {
            assertEquals(CREATED.getStatusCode(), getStatus(response));
            binaryUri = getLocation(response);
        }
        final String binaryDescription = binaryUri + "/" + FCR_METADATA;
        // Get the original body.
        final var getObj = new HttpGet(binaryDescription);
        getObj.addHeader(ALLOW, "application/n-triples");
        final String originalBody;
        try (final var response = execute(getObj)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            originalBody = IOUtils.toString(response.getEntity().getContent(), UTF_8);
        }

        // Can't patch a non-relaxable property, even in relaxed mode.
        final var patch = new HttpPatch(binaryDescription);
        patch.setHeader(CONTENT_TYPE, "application/sparql-update");
        patch.setEntity(new StringEntity("DELETE { <> <" + HAS_MESSAGE_DIGEST + "> ?digest } INSERT { " +
                "<> <" + HAS_MESSAGE_DIGEST + "> \"fake-digest\". } WHERE { <> <" + HAS_MESSAGE_DIGEST +
                "> ?digest }"));
        assertEquals(CONFLICT.getStatusCode(), getStatus(patch));

        // Get the body again.
        final String newBody;
        try (final var response = execute(getObj)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            newBody = IOUtils.toString(response.getEntity().getContent(), UTF_8);
        }
        confirmResponseBodyNTriplesAreEqual(originalBody, newBody);
    }

    @After
    public void switchToStrictMode() {
        propsConfig.setServerManagedPropsMode(ServerManagedPropsMode.STRICT);
    }

    private void assertIdentical(final String uri, final String originalRdf) throws IOException {
        try (final CloseableDataset roundtripped = getDataset(new HttpGet(uri))) {
            try (final CloseableDataset original = parseTriples(new ByteArrayInputStream(originalRdf.getBytes()))) {
                final DatasetGraph originalGraph = original.asDatasetGraph();
                final DatasetGraph roundtrippedGraph = roundtripped.asDatasetGraph();
                final Iterator<Quad> originalQuadIt = originalGraph.find();
                while (originalQuadIt.hasNext()) {
                    final Quad q = originalQuadIt.next();
                    if (!assertQuadsAreSimilar(q, roundtrippedGraph)) {
                        fail(q + " should be preserved through a roundtrip! \nOriginal RDF: " + originalRdf
                                + "\nRoundtripped Graph:\n" + roundtrippedGraph);
                    }
                    roundtrippedGraph.delete(q);
                }
                assertTrue("Roundtripped graph had extra quads! " + roundtrippedGraph, roundtrippedGraph.isEmpty());
            }
        }
    }

    /**
     * In roundtripping XSDDateTimes get truncated to 3 digits of microseconds. This checks that if the quad doesn't
     * match and is a XSDDateTime that it is within .001 of a second.
     *
     * @param testQuad the quad to check
     * @param checkGraph the graph to verify against
     * @return true if the quad is exact or very close, false if not.
     */
    private boolean assertQuadsAreSimilar(final Quad testQuad, final DatasetGraph checkGraph) {
        if (checkGraph.contains(testQuad)) {
            return true;
        } else if (testQuad.getObject().isLiteral() &&
                testQuad.getObject().getLiteral().getDatatype() instanceof XSDDateTimeType) {
            final var testLiteral = (XSDDateTime)testQuad.getObject().getLiteralValue();
            if (checkGraph.contains(ANY, testQuad.getSubject(), testQuad.getPredicate(), ANY)) {
                final var roundTripIter =
                        checkGraph.find(ANY, testQuad.getSubject(), testQuad.getPredicate(), ANY);
                while (roundTripIter.hasNext()) {
                    final var dateStmt = roundTripIter.next();
                    final var comparisonVal = (XSDDateTime) dateStmt.getObject().getLiteralValue();
                    if (testLiteral.getYears() == comparisonVal.getYears() &&
                        testLiteral.getMonths() == comparisonVal.getMonths() &&
                        testLiteral.getDays() == comparisonVal.getDays() &&
                        testLiteral.getHours() == comparisonVal.getHours() &&
                        testLiteral.getMinutes() == comparisonVal.getMinutes() &&
                        testLiteral.getFullSeconds() == comparisonVal.getFullSeconds() &&
                        Math.abs(testLiteral.getSeconds() - comparisonVal.getSeconds()) < .001) {
                            // Delete here as it won't actually match the quad
                            checkGraph.delete(dateStmt);
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private String buildSparqlUpdate(final Stream<Triple> toRemove, final Stream<Triple> toAdd,
                                     final Property ... predicates) {
        final Set<Property> allowedPredicates = new HashSet<>(Arrays.asList(predicates));
        final StringBuilder r = new StringBuilder();
        r.append("DELETE { \n");
        toRemove.filter(t -> allowedPredicates.contains(createProperty(t.getPredicate().getURI())))
                .forEach(triple -> r.append(" <> <" + triple.getPredicate().toString() + "> "
                        + wrapLiteral(triple.getObject()) + " .\n"));
        r.append("}\nINSERT { ");
        toAdd.filter(t -> allowedPredicates.contains(createProperty(t.getPredicate().getURI())))
                .forEach(triple -> r.append(" <> <" + triple.getPredicate().toString() + "> "
                        + wrapLiteral(triple.getObject()) + " .\n"));
        r.append("} WHERE {}");
        return r.toString();
    }

    private String wrapLiteral(final Node node) {
        return "\"" + node.getLiteralLexicalForm() + "\"^^<" + node.getLiteralDatatype().getURI() + ">";
    }

    /**
     * Parses the provided triples and filters it to just include statements with the
     * specified predicates.
     * @param triples to filter
     * @param predicates to filter on
     * @return filtered triples
     */
    private String filterRdf(final String triples, final Property ... predicates) {
        try (final CloseableDataset original = parseTriples(new ByteArrayInputStream(triples.getBytes()))) {
            final Model m = original.getDefaultModel();
            final StmtIterator it = m.listStatements();
            while (it.hasNext()) {
                final Statement stmt = it.nextStatement();
                boolean keep = false;
                for (final Property p : predicates) {
                    if (stmt.getPredicate().equals(p)) {
                        keep = true;
                        break;
                    }
                }
                if (!keep) {
                    it.remove();
                }
            }
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.write(baos, "N3");
            return baos.toString();
        }
    }

    private CloseableHttpResponse patchResource(final String uri, final String sparqlUpdate) throws IOException {
        final HttpPatch patch = new HttpPatch(uri);
        patch.setHeader("Content-type", "application/sparql-update");
        patch.setEntity(new StringEntity(sparqlUpdate));
        return execute(patch);
    }

    private CloseableHttpResponse postBinaryResource(final String parentURI, final String content) throws IOException {
        return postBinaryResource(parentURI, null, content);
    }

    private CloseableHttpResponse postBinaryResource(final String parentURI, final String slug,
                                                     final String content) throws IOException {
        final HttpPost post = new HttpPost(parentURI);
        if (slug != null) {
            post.setHeader("Slug", slug);
        }
        post.setEntity(new StringEntity(content == null ? "" : content));
        post.setHeader(CONTENT_TYPE, TEXT_PLAIN);
        post.setHeader(LINK, "<" + NON_RDF_SOURCE.toString() + ">; rel=\"type\"");
        return execute(post);
    }

    private CloseableHttpResponse postResourceWithTTL(final String ttl) throws IOException {
        return this.postResourceWithTTL(null, ttl);
    }

    private CloseableHttpResponse postResourceWithTTL(final String uri, final String ttl) throws IOException {
        final HttpPost httpPost = new HttpPost(serverAddress);
        httpPost.addHeader("Slug", uri == null ? getRandomUniqueId() : uri.substring(serverAddress.length()));
        httpPost.addHeader(CONTENT_TYPE, "text/turtle");
        httpPost.addHeader(LINK, "<" + BASIC_CONTAINER.toString() + ">; rel=\"type\"");
        httpPost.setEntity(new StringEntity(ttl));
        return execute(httpPost);
    }

    private CloseableHttpResponse putResourceWithTTL(final String uri, final String ttl)
            throws IOException {
        final HttpPut httpPut = new HttpPut(uri);
        httpPut.addHeader(CONTENT_TYPE, "text/turtle");
        httpPut.setEntity(new StringEntity(ttl));
        return execute(httpPut);
    }

    private ResultGraphWrapper triples(final String uri, final Dataset dataset) {
        return new ResultGraphWrapper(uri, dataset.asDatasetGraph());
    }

    private static Calendar nowUTC() {
        return getInstance(getTimeZone("UTC"));
    }

    private class ResultGraphWrapper {

        private final DatasetGraph graph;
        private final Node nodeUri;
        private final String statements;

        public ResultGraphWrapper(final String subjectURI, final DatasetGraph graph) {
            this.graph = graph;
            final Model model = createModelForGraph(graph.getDefaultGraph());
            nodeUri = createURI(subjectURI);

            final StringBuilder statements = new StringBuilder();
            final StmtIterator it = model.listStatements();
            while (it.hasNext()) {
                statements.append(it.next() + "\n");
            }
            this.statements = statements.toString();
        }

        public ResultGraphWrapper mustHave(final Node predicate, final Node object) {
            assertTrue(predicate.getLocalName() + " should have been set to " + object.getLiteral().toString()
                    + "!\nStatements:\n" + statements, graph.contains(ANY, nodeUri, predicate, object));
            return this;
        }

        public ResultGraphWrapper mustNotHave(final Node predicate, final Node object) {
            assertFalse(predicate.getLocalName() + " should not have been set to " + object.getLiteral().toString()
                    + "!", graph.contains(ANY, nodeUri, predicate, object));
            return this;
        }

    }

    private Node createDateTime(final Calendar cal) {
        return createLiteralByValue(DatatypeConverter.printDateTime(cal), XSDDatatype.XSDdateTime);
    }

}
