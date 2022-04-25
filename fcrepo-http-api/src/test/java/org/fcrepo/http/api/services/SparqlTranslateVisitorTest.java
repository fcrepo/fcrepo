/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api.services;

import static org.junit.Assert.assertEquals;

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
        final var originalString = "DELETE DATA { <> <" + DC_11.title.getURI() + "> \"Some title\" . }";
        final var visitor = makeVisitor(UUID.randomUUID() + "/fcr:metadata");
        final var expected = "DELETE DATA { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() +
                "> \"Some title\" . " +
                "<" + resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> \"Some title\" . }";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
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
        final var originalString = "DELETE { <> <" + DC_11.title.getURI() + "> \"Some title\" . } WHERE {}";
        final var visitor = makeVisitor(UUID.randomUUID() + "/fcr:metadata");
        final var expected = "DELETE { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() +
                "> \"Some title\" . <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() +
                "> \"Some title\" . } WHERE {}";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
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
        final var originalString =
                "DELETE { <> <" + DC_11.title.getURI() + "> ?o . } INSERT { <> <" + DC_11.title.getURI() + "> " +
                        "\"Some title\" . } WHERE { <> <" + DC_11.title.getURI() + "> ?o }";
        final var visitor = makeVisitor(UUID.randomUUID() + "/fcr:metadata");
        final var expected = "DELETE { <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() +
                "> ?o . <" + resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> ?fedoraBinaryFix .} INSERT " +
                "{ <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() + "> \"Some title\" . } WHERE " +
                "{ <" + resourceId.getFullDescribedId() + "> <" + DC_11.title.getURI() + "> ?o . <" +
                resourceId.getFullId() + "> <" + DC_11.title.getURI() + "> ?fedoraBinaryFix }";
        final var output = runVisits(originalString, visitor);
        assertEqualsSparqlStrings(expected, output);
    }
}
