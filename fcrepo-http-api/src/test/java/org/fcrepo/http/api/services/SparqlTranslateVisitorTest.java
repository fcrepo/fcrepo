/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.apache.jena.vocabulary.DC_11;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for SparqlTranslateVisitor
 * @author whikloj
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SparqlTranslateVisitorTest {

    private static final String URI_BASE = "http://localhost:8080/rest/";

    private HttpIdentifierConverter identifierConverter;

    private String idEnding;

    private String httpUri;

    private FedoraId resourceId;

    private final FedoraPropsConfig propsConfig = new FedoraPropsConfig();

    public SparqlTranslateVisitorTest() {
        final var builder = UriBuilder.fromUri(URI_BASE + "{path: .*}");
        identifierConverter = new HttpIdentifierConverter(builder);
        propsConfig.setServerManagedPropsMode(ServerManagedPropsMode.STRICT);
    }

    private SparqlTranslateVisitor makeVisitor() {
        return makeVisitor(UUID.randomUUID().toString());
    }

    private SparqlTranslateVisitor makeVisitor(final String id) {
        idEnding = id;
        final var internalhttpUri = URI_BASE + idEnding;
        resourceId = FedoraId.create(identifierConverter.toInternalId(internalhttpUri));
        httpUri = identifierConverter.toExternalId(resourceId.getFullDescribedId());
        return new SparqlTranslateVisitor(identifierConverter, propsConfig, resourceId);
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
     * Assert one string contains the other regardless of whitespace.
     * @param expected the expected string.
     * @param comparison the string to test.
     */
    private void assertStringContains(final String expected, final String comparison) {
        final String cleanedExpected = sparqlEqualityCleanup(expected);
        final String cleanedTest = sparqlEqualityCleanup(comparison);
        assertTrue(cleanedTest.contains(cleanedExpected));
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

    @Test
    public void testInsertDataContainer() {
        final var originalString = "INSERT DATA { <> <" + DC_11.title.getURI() + "> \"Some title\" . }";
        final var visitor = makeVisitor();
        final var expected = "INSERT DATA { <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() +
                "> \"Some title\" .}";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }

    @Test
    public void testDeleteDataContainer() {
        final var originalString = "DELETE DATA { <> <" + DC_11.title.getURI() + "> \"Some title\" . }";
        final var visitor = makeVisitor();
        final var expected = "DELETE DATA { <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() +
                "> \"Some title\" .}";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }

    @Test
    public void testInsertDataBinary() {
        final var originalString = "INSERT DATA { <> <" + DC_11.title.getURI() + "> \"Some title\" . }";
        final var visitor = makeVisitor(UUID.randomUUID() + "/fcr:metadata");
        final var expected = "INSERT DATA { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() +
                "> \"Some title\" .}";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }

    @Test
    public void testDeleteDataBinary() {
        final var id = UUID.randomUUID() + "/fcr:metadata";
        var visitor = makeVisitor(id);
        final var originalStringTemplate = "DELETE DATA { <%s> <" + DC_11.title.getURI() +
                "> \"Some title\" . }";
        final var uris = List.of(
                "",
                identifierConverter.toExternalId(resourceId.getFullId()),
                identifierConverter.toExternalId(resourceId.getFullDescribedId()),
                resourceId.getFullId(),
                resourceId.getFullDescribedId()
        );
        for (final var uri : uris) {
            final var input = String.format(originalStringTemplate, uri);
            final var output = runVisits(input, visitor);
            // Order of deletes flips so just check for the triples.
            assertStringContains(
                    String.format("<%s> <%s> \"Some title\" .", resourceId.getFullId(), DC_11.title.getURI()),
                    output
            );
            assertStringContains(
                    String.format("<%s> <%s> \"Some title\" .", resourceId.getFullDescribedId(), DC_11.title.getURI()),
                    output
            );
            // Need to clear the request queue
            visitor = new SparqlTranslateVisitor(identifierConverter, propsConfig, resourceId);
        }
    }

    @Test
    public void testInsertWhereContainer() {
        final var originalString = "INSERT { <> <" + DC_11.title.getURI() + "> \"Some title\" . } WHERE {}";
        final var visitor = makeVisitor();
        final var expected = "INSERT { <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> \"Some " +
                "title\" .} WHERE {}";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }

    @Test
    public void testDeleteWhereContainer() {
        final var originalString = "DELETE { <> <" + DC_11.title.getURI() + "> \"Some title\" . } WHERE {}";
        final var visitor = makeVisitor();
        final var expected = "DELETE { <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> \"Some " +
                "title\" .} WHERE {}";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }

    @Test
    public void testInsertWhereBinary() {
        final var originalString = "INSERT { <> <" + DC_11.title.getURI() + "> \"Some title\" . } WHERE {}";
        final var visitor = makeVisitor(UUID.randomUUID() + "/fcr:metadata");
        final var expected = "INSERT { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() + "> " +
                "\"Some title\" .} WHERE {}";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }

    @Test
    public void testDeleteWhereBinary() {
        final var id = UUID.randomUUID() + "/fcr:metadata";
        var visitor = makeVisitor(id);
        final var originalStringTemplate = "DELETE { <%s> <" + DC_11.title.getURI() + "> \"Some title\" . } WHERE {}";
        final var uris = List.of(
                "",
                identifierConverter.toExternalId(resourceId.getFullId()),
                identifierConverter.toExternalId(resourceId.getFullDescribedId()),
                resourceId.getFullId(),
                resourceId.getFullDescribedId()
        );
        final var expected = "DELETE { ?fedoraBinaryFix <" + DC_11.title.getURI() + "> \"Some title\" . } " +
                "WHERE { VALUES ?fedoraBinaryFix { <" + resourceId.getFullDescribedId() + "> <" +
                resourceId.getFullId() + "> } }";
        for (final var uri : uris) {
            final var input = String.format(originalStringTemplate, uri);
            final var output = runVisits(input, visitor);
            assertEqualsSparqlStrings(expected, output);
            visitor = new SparqlTranslateVisitor(identifierConverter, propsConfig, resourceId);
        }
    }

    @Test
    public void testDeleteAndInsertWhereContainer() {
        final var originalString =
                "DELETE { <> <" + DC_11.title.getURI() + "> ?o . } INSERT { <> <" + DC_11.title.getURI() + "> " +
                        "\"Some title\" . } WHERE { <> <" + DC_11.title.getURI() + "> ?o }";
        final var visitor = makeVisitor();
        final var expected = "DELETE { <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> ?o . } INSERT " +
                "{ <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> \"Some title\" . } WHERE { <" +
                resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> ?o }";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }

    @Test
    public void testDeleteAndInsertWhereBinary() {
        final var id = UUID.randomUUID() + "/fcr:metadata";
        var visitor = makeVisitor(id);
        final var originalStringTemplate =
                "DELETE { <%1$s> <" + DC_11.title.getURI() + "> ?o . } INSERT { <> <" + DC_11.title.getURI() + "> " +
                "\"Some title\" . } WHERE { <%1$s> <" + DC_11.title.getURI() + "> ?o }";
        final var uris = List.of(
                "",
                identifierConverter.toExternalId(resourceId.getFullId()),
                identifierConverter.toExternalId(resourceId.getFullDescribedId()),
                resourceId.getFullId(),
                resourceId.getFullDescribedId()
        );
        final var expected = "DELETE { ?fedoraBinaryFix <" + DC_11.title.getURI() + "> ?o . } " +
                "INSERT { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() + "> \"Some title\" . }" +
                " WHERE { ?fedoraBinaryFix <" + DC_11.title.getURI() + "> ?o VALUES ?fedoraBinaryFix { <" +
                resourceId.getFullDescribedId() + "> <" + resourceId.getFullId() + "> } }";
        for (final var uri : uris) {
            final var input = String.format(originalStringTemplate, uri);
            final var output = runVisits(input, visitor);
            assertEqualsSparqlStrings(expected, output);
            visitor = new SparqlTranslateVisitor(identifierConverter, propsConfig, resourceId);
        }
    }
}
