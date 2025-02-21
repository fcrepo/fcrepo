/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.config.ServerManagedPropsMode;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.identifiers.FedoraId;

import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.DC_11;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for SparqlTranslateVisitor
 * @author whikloj
 */
@ExtendWith(MockitoExtension.class)
public class SparqlTranslateVisitorTest {

    private static final String URI_BASE = "http://localhost:8080/rest/";

    private HttpIdentifierConverter identifierConverter;

    private String httpUri;

    private FedoraId resourceId;

    private final FedoraPropsConfig propsConfig = new FedoraPropsConfig();

    private SparqlTranslateVisitor visitor;

    public SparqlTranslateVisitorTest() {
        final var builder = UriBuilder.fromUri(URI_BASE + "{path: .*}");
        identifierConverter = new HttpIdentifierConverter(builder);
        propsConfig.setServerManagedPropsMode(ServerManagedPropsMode.STRICT);
    }

    @BeforeEach
    public void setUp() {
        makeVisitor(UUID.randomUUID().toString());
    }

    private void makeVisitor(final String id) {
        final var internalhttpUri = URI_BASE + id;
        resourceId = FedoraId.create(identifierConverter.toInternalId(internalhttpUri));
        httpUri = identifierConverter.toExternalId(resourceId.getFullDescribedId());
        visitor = new SparqlTranslateVisitor(identifierConverter, propsConfig);
    }

    private String runVisits(final String input, final SparqlTranslateVisitor visitor) {
        final UpdateRequest request = UpdateFactory.create(input, httpUri);
        final List<Update> updates = request.getOperations();
        for (final Update update : updates) {
            update.visit(visitor);
        }
        return visitor.getTranslatedRequest().toString();
    }

    private String sparqlEqualityCleanup(final String input) {
        return input.replaceAll("\\n", " ").replaceAll("\\s+", " ")
                .replaceAll("\\. }", ".}").replaceAll("\\{ }", "{}").trim();
    }

    /**
     * Assert equality while accounting for changes in the Sparql text when run through the visitor.
     * @param expected
     *   The expected string.
     * @param actual
     *   The actual string.
     */
    private void assertEqualsSparqlStrings(final String expected, final String actual) {
        final var expect1 = sparqlEqualityCleanup(expected);
        final var actual1 = sparqlEqualityCleanup(actual);
        assertEquals(
                expect1,
                actual1
        );
    }

    private void runMultipleTests(final String expected, final String inputTemplate) {
        final var originalUris = List.of(
                "",
                identifierConverter.toExternalId(resourceId.getFullId()),
                identifierConverter.toExternalId(resourceId.getFullDescribedId()),
                resourceId.getFullId(),
                resourceId.getFullDescribedId()
        );
        // Clean up duplicates for containers to reduce redundant assertations.
        final var uris = new ArrayList<>(new HashSet<>(originalUris));
        runMultipleTests(uris, expected, inputTemplate);
    }

    private void runMultipleTests(final List<String> uris, final String expected, final String inputTemplate) {
        visitor = new SparqlTranslateVisitor(identifierConverter, propsConfig);
        for (final var uri : uris) {
            final var input = String.format(inputTemplate, uri);
            final var output = runVisits(input, visitor);
            assertEqualsSparqlStrings(expected, output);
            // Need to clear the request queue
            visitor = new SparqlTranslateVisitor(identifierConverter, propsConfig);
        }
    }

    @Test
    public void testInsertDataContainer() {
        final var originalString = "INSERT DATA { <> <" + DC_11.title.getURI() + "> \"Some title\" . }";
        final var expected = "INSERT DATA { <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() +
                "> \"Some title\" .}";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }

    @Test
    public void testDeleteDataContainer() {
        final var originalString = "DELETE DATA { <> <" + DC_11.title.getURI() + "> \"Some title\" . }";
        final var expected = "DELETE DATA { <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() +
                "> \"Some title\" .}";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }

    @Test
    public void testInsertDataBinary() {
        makeVisitor(UUID.randomUUID() + "/fcr:metadata");
        final var originalStringTemplate = "INSERT DATA { <%s> <" + DC_11.title.getURI() + "> \"Some title\" . }";
        final var expected = "INSERT DATA { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() +
                "> \"Some title\" .}";
        runMultipleTests(expected, originalStringTemplate);
    }

    @Test
    public void testDeleteDataBinary() {
        makeVisitor(UUID.randomUUID() + "/fcr:metadata");
        final var originalStringTemplate = "DELETE DATA { <%s> <" + DC_11.title.getURI() +
                "> \"Some title\" . }";
        final var expected = "DELETE DATA { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() + "> " +
                "\"Some title\" . }";
        runMultipleTests(expected, originalStringTemplate);
    }

    @Test
    public void testInsertWhereContainer() {
        final var originalString = "INSERT { <> <" + DC_11.title.getURI() + "> \"Some title\" . } WHERE {}";
        final var expected = "INSERT { <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> \"Some " +
                "title\" .} WHERE {}";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }

    @Test
    public void testDeleteWhereContainer() {
        final var originalString = "DELETE { <> <" + DC_11.title.getURI() + "> \"Some title\" . } WHERE {}";
        final var expected = "DELETE { <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> \"Some " +
                "title\" .} WHERE {}";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }

    @Test
    public void testInsertWhereBinary() {
        makeVisitor(UUID.randomUUID() + "/fcr:metadata");
        final var originalStringTemplate = "INSERT { <%s> <" + DC_11.title.getURI() + "> \"Some title\" . } WHERE {}";
        final var expected = "INSERT { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() + "> " +
                "\"Some title\" .} WHERE {}";
        runMultipleTests(expected, originalStringTemplate);
    }

    @Test
    public void testDeleteWhereBinary() {
        makeVisitor(UUID.randomUUID() + "/fcr:metadata");
        final var originalStringTemplate = "DELETE { <%s> <" + DC_11.title.getURI() + "> \"Some title\" . } WHERE {}";
        final var expected = "DELETE { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() + "> " +
        "\"Some title\" . } WHERE { }";
        runMultipleTests(expected, originalStringTemplate);
    }

    @Test
    public void testDeleteAndInsertWhereContainer() {
        final var originalString =
                "DELETE { <> <" + DC_11.title.getURI() + "> ?o . } INSERT { <> <" + DC_11.title.getURI() + "> " +
                        "\"Some title\" . } WHERE { <> <" + DC_11.title.getURI() + "> ?o }";
        final var expected = "DELETE { <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> ?o . } INSERT " +
                "{ <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> \"Some title\" . } WHERE { <" +
                resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> ?o }";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }

    @Test
    public void testDeleteAndInsertWhereBinary() {
        makeVisitor(UUID.randomUUID() + "/fcr:metadata");
        final var originalStringTemplate =
                "DELETE { <%1$s> <" + DC_11.title.getURI() + "> ?o . } INSERT { <> <" + DC_11.title.getURI() + "> " +
                "\"Some title\" . } WHERE { <%1$s> <" + DC_11.title.getURI() + "> ?o }";
        final var expected = "DELETE { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() + "> ?o . " +
                "} INSERT { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() +
                "> \"Some title\" . } WHERE { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() +
                "> ?o }";
        runMultipleTests(expected, originalStringTemplate);
    }

    @Test
    public void testInsertContainerWithBinaryDescriptionAsObject() {
        final var descriptionId = FedoraId.create(UUID.randomUUID() + "/fcr:metadata");
        final var originalStringTemplate = "INSERT DATA { <> <" + DCTerms.isPartOf.getURI() + "> <%s> . }";
        final var uris = List.of(
                identifierConverter.toExternalId(descriptionId.getFullId()),
                identifierConverter.toExternalId(descriptionId.getFullDescribedId()),
                descriptionId.getFullId(),
                descriptionId.getFullDescribedId()
        );
        final var expected = "INSERT DATA { <" + resourceId.getFullId() + "> <" + DCTerms.isPartOf.getURI() + "> <" +
                descriptionId.getFullDescribedId() + "> . }";
        runMultipleTests(uris, expected, originalStringTemplate);
    }
}
