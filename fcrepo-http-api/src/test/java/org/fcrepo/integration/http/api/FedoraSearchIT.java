/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.SearchResult;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestExecutionListeners;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_BINARY;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI;
import static org.fcrepo.search.api.Condition.Field.CONTENT_SIZE;
import static org.fcrepo.search.api.Condition.Field.CREATED;
import static org.fcrepo.search.api.Condition.Field.FEDORA_ID;
import static org.fcrepo.search.api.Condition.Field.MIME_TYPE;
import static org.fcrepo.search.api.Condition.Field.MODIFIED;
import static org.fcrepo.search.api.Condition.Field.RDF_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static java.lang.Thread.sleep;
import static java.time.ZoneOffset.UTC;

/**
 * @author dbernstein
 * @since 05/06/20
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraSearchIT extends AbstractResourceIT {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String getSearchEndpoint() {
        return serverAddress + "fcr:search?";
    }

    private List<String> createResources(final int count) throws IOException {
        return createResources(getRandomUniqueId(), count);
    }

    private List<String> createResources(final String prefix, final int count) throws IOException {
        final var resources = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            try (final CloseableHttpResponse response = createObject(prefix + "-" + String.format("%05d", i))) {
                resources.add(getLocation(response));
            }
        }
        return resources;
    }

    @Test
    public void testSearchAllResources() throws Exception {
        final var resources = createResources(3);
        final var condition = FEDORA_ID + "=*";
        List<String> notFound = resources;

        final int maxResults = 10;
        int offset = 0;

        // Loop until all resources are found
        // - Failure case: loop dies with assertion for non-zero results in 'doSearchAllResources()'
        while (!notFound.isEmpty()) {
            notFound = doSearchAllResources(notFound, condition, maxResults, offset);
            offset += maxResults;
        }
    }

    private List<String> doSearchAllResources(final List<String> resources,
                                              final String condition,
                                              final int maxResults,
                                              final int offset) throws IOException {
        final String searchUrl = getSearchEndpoint()
                + "condition=" + encode(condition) + "&max_results=" + maxResults + "&offset=" + offset;

        final List<String> notFound = new ArrayList<>();
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertNotNull(result);
            assertNotNull(result.getPagination());
            assertTrue(result.getItems().size() > 0, "Items not found! " + resources);
            final var resultingFedoraIds =
                    result.getItems().stream().map(x -> x.get("fedora_id")).collect(Collectors.toList());

            resources.forEach(r -> {
                if (!resultingFedoraIds.contains(r)) {
                    notFound.add(r);
                }
            });
        }
        return notFound;
    }

    @Test
    public void testWildcardMatchingOnPartialFedoraId() throws Exception {
        final var id = getRandomUniqueId();
        final int count = 3;
        createResources(id, count);
        final var urlPrefix = serverAddress + id;
        // try all valid prefix formulations
        final var prefixes = new String[]{id, "/" + id, urlPrefix, FEDORA_ID_PREFIX + "/" + id};
        for (final String prefix : prefixes) {
            final var condition = FEDORA_ID + "=" + prefix + "*";
            final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition);
            try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
                assertEquals(OK.getStatusCode(), getStatus(response));
                final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                        SearchResult.class);
                assertEquals(count, result.getItems().size(),
                        "expected " + count + " items where condition = " + condition);

                assertTrue(result.getItems().stream().map(x -> x.get(FEDORA_ID.toString()))
                        .allMatch(x -> x.toString().startsWith(urlPrefix)));
            }
        }
    }

    @Test
    public void testUpdateSearchIndexOnResourceUpdate() throws Exception {
        final var resourceId = createResources(1).get(0);
        final var condition = FEDORA_ID + "=" + resourceId;
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition);
        String modified = null;
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals(1, result.getItems().size(),
                    "expected 1 item where condition = " + condition);
            modified = result.getItems().get(0).get(MODIFIED.toString()).toString();
        }

        TimeUnit.SECONDS.sleep(1);

        final var patch = new HttpPatch(resourceId);
        patch.setHeader("Content-Type", "application/sparql-update");
        patch.setEntity(new StringEntity("insert data { <> <http://example/blah> \"Blah\". }"));
        try (final CloseableHttpResponse response = execute(patch)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals(1, result.getItems().size(),
                    "expected 1 item where condition = " + condition);
            final var newModified = result.getItems().get(0).get(MODIFIED.toString()).toString();
            assertNotEquals("Modified date should have changed  but it did not.", modified, newModified);
        }
    }
    private void assertReturnsNumberOfItems(final String condition, final int expectedCount) throws Exception {
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals(expectedCount, result.getItems().size(),
                    "expected " + expectedCount + " items where condition = " + condition);
        }
    }

    @Test
    public void testUpdateSearchIndexOnBinaryCreation() throws Exception {
        final var binCondition = RDF_TYPE + "=" + FEDORA_BINARY.getURI();
        assertReturnsNumberOfItems(binCondition, 0);
        final var nonRdfCondition = RDF_TYPE + "=" + FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI;
        assertReturnsNumberOfItems(nonRdfCondition, 0);

        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        createDatastream(id, "test_binary", "foo");

        assertReturnsNumberOfItems(binCondition, 1);
        assertReturnsNumberOfItems(nonRdfCondition, 1);
    }

    @Test
    public void testUpdateSearchIndexOnResourceDelete() throws Exception {
        final var resourceId = createResources(1).get(0);
        final var condition = FEDORA_ID + "=" + resourceId;
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals(1, result.getItems().size(),
                    "expected 1 item where condition = " + condition);
        }

        final var httpDelete = new HttpDelete(resourceId);
        try (final CloseableHttpResponse response = execute(httpDelete)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals(0, result.getItems().size(),
                    "expected 0 items where condition = " + condition);
        }
    }

    @Test
    public void testUpdateSearchIndexOnBinaryDeletion() throws Exception {
        final String id = getRandomUniqueId();
        createObjectAndClose(id);
        final var dsUri = createDatastream(id, "test_binary", "foo");

        final var binCondition = RDF_TYPE + "=" + FEDORA_BINARY.getURI();
        assertReturnsNumberOfItems(binCondition, 1);
        final var nonRdfCondition = RDF_TYPE + "=" + FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI;
        assertReturnsNumberOfItems(nonRdfCondition, 1);

        final var httpDelete = new HttpDelete(dsUri);
        try (final CloseableHttpResponse response = execute(httpDelete)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        assertReturnsNumberOfItems(binCondition, 0);
        assertReturnsNumberOfItems(nonRdfCondition, 0);
    }

    @Test
    public void testModifiedGreaterAndLessThan() throws Exception {
        final var id = getRandomUniqueId();
        final int count = 1;
        final var instant = Instant.now();
        final var formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(UTC);

        final var tomorrow = instant.plus(Duration.ofDays(1));
        // ensure there is a delay between instant and the creation/modification time of the resources.
        sleep(1000);
        final var resources = createResources(id, count);
        assertEquals(1, resources.size());
        final var externalFedoraId = resources.get(0);
        final var lessThanNow = MODIFIED + encode("<") + formatter.format(instant);
        final var greaterThanNow = MODIFIED + encode(">") + formatter.format(instant);
        final var lessThanTomorrow = MODIFIED + encode("<") + formatter.format(tomorrow);
        final var greaterThanTomorrow = MODIFIED + encode(">") + formatter.format(tomorrow);

        // no results for resources modified before now
        String searchUrl =
                getSearchEndpoint() + "condition=" + lessThanNow;
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals(0, result.getItems().size(), "expected no results");
        }
        // no results for resources modified after tomorrow
        searchUrl =
                getSearchEndpoint() + "condition=" + greaterThanTomorrow;
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals(0, result.getItems().size(), "expected no results");
        }

        // ensure that greater than now returns a result
        searchUrl = getSearchEndpoint() + "condition=" + greaterThanNow;
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals(count, result.getItems().size(), "expected " + count + " results");

            assertTrue(result.getItems().stream().map(x -> x.get("fedora_id"))
                    .allMatch(x -> x.toString().startsWith(externalFedoraId)));
        }

        // ensure that less than tomorrow returns a result
        searchUrl = getSearchEndpoint() + "condition=" + lessThanTomorrow;
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals(count, result.getItems().size(), "expected " + count + " results");

            assertTrue(result.getItems().stream().map(x -> x.get("fedora_id"))
                    .allMatch(x -> x.toString().startsWith(externalFedoraId)));
        }

        // ensure that between now and tomorrow returns a result.
        searchUrl = getSearchEndpoint() + "condition=" + greaterThanNow +
                "&condition=" + lessThanTomorrow;
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals(count, result.getItems().size(), "expected " + count + " results ");
            assertTrue(result.getItems().stream().map(x -> x.get("fedora_id"))
                    .allMatch(x -> x.toString().startsWith(externalFedoraId)));
        }
    }

    @Test
    public void testFieldsReturnsSpecifiedFields() throws Exception {
        testFieldsReturnsOnlySpecifiedFields(List.of(FEDORA_ID, CREATED));
    }

    @Test
    public void testFieldsReturnsMimeType() throws Exception {
        testFieldsReturnsOnlySpecifiedFields(List.of(MIME_TYPE));
    }

    @Test
    public void testFieldsReturnsContentSize() throws Exception {
        testFieldsReturnsOnlySpecifiedFields(List.of(CONTENT_SIZE));
    }

    @Test
    public void testFieldsReturnsRDFType() throws Exception {
        testFieldsReturnsOnlySpecifiedFields(List.of(RDF_TYPE));
    }


    private void testFieldsReturnsOnlySpecifiedFields(final List<Condition.Field> fieldList) throws Exception {
        final var id = getRandomUniqueId();
        final var count = 1;
        createResources(id, count);
        final var fields = fieldList.stream()
                .map(Condition.Field::toString).collect(Collectors.toList());
        final String searchUrl =
                getSearchEndpoint() + "condition=" + encode("fedora_id=" + id + "*") + "&fields=" + String.join(",",
                        fields);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            final var items = result.getItems();
            assertEquals(count, items.size(), "expected " + count + " items");
            for (final Map<String, Object> item : items) {
                assertEquals(fields.size(), item.size(), "expected " + fields.size() + " fields returned");
                for (final String field : fields) {
                    assertTrue(item.containsKey(field), "Result does not contain expected field: " + field);
                }
            }
        }
    }

    @Test
    public void testMaxResultsAndOffset() throws Exception {
        final var prefix = getRandomUniqueId();
        final int count = 3;
        final var resources = createResources(prefix, count);
        final var condition = FEDORA_ID + "=" + prefix + "*";
        final var maxResults = 1;
        final String searchUrl =
                getSearchEndpoint() + "condition=" + encode(condition) + "&max_results=" + maxResults + "&offset=2" +
                        "&order_by=fedora_id";
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(maxResults, result.getItems().size());
            final var returnedIds =
                    result.getItems().stream().map(x -> x.get("fedora_id")).collect(Collectors.toList());
            assertEquals(resources.get(2), returnedIds.get(0));
        }
    }

    @Test
    public void testDoNotIncludeTotalCount() throws Exception {
        final var prefix = getRandomUniqueId();
        final int count = 3;
        createResources(prefix, count);
        final var condition = FEDORA_ID + "=" + prefix + "*";
        final var maxResults = 1;
        final String searchUrl =
                getSearchEndpoint() + "condition=" + encode(condition) + "&max_results=" + maxResults +
                        "&include_total_result_count=false";
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(maxResults, result.getItems().size());
            assertEquals(-1, result.getPagination().getTotalResults());
        }
    }

    @Test
    public void testIncludeTotalCount() throws Exception {
        final var prefix = getRandomUniqueId();
        final int count = 3;
        createResources(prefix, count);
        final var condition = FEDORA_ID + "=" + prefix + "*";
        final var maxResults = 1;
        final String searchUrl =
                getSearchEndpoint() + "condition=" + encode(condition) + "&max_results=" + maxResults +
                        "&include_total_result_count=true";
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(maxResults, result.getItems().size());
            assertEquals(count, result.getPagination().getTotalResults());
        }
    }

    @Test
    public void testSearchByContentSize() throws Exception {
        final var resourceId = getRandomUniqueId();
        createObjectAndClose(resourceId);
        final var ds1 = "test";
        createDatastream(resourceId, ds1, ds1);
        final var ds2 = "test-longer-length";
        createDatastream(resourceId, ds2, ds2);
        final var condition = FEDORA_ID + "=" + resourceId + "*";
        final var contentSizeCondition = CONTENT_SIZE + ">=" + ds1.getBytes().length;
        final var contentSizeConditionLongerLength = CONTENT_SIZE + ">=" + ds2.getBytes().length;
        final String searchUrl =
                getSearchEndpoint() + "condition=" + encode(condition) + "&condition=" + encode(contentSizeCondition);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(2, result.getItems().size());
        }

        final String searchUrl2 =
                getSearchEndpoint() + "condition=" + encode(condition) +
                        "&condition=" + encode(contentSizeConditionLongerLength);

        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl2))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(1, result.getItems().size());
        }
    }

    @Test
    public void testSearchByMimeType() throws Exception {
        final var resourceId = getRandomUniqueId();
        createObjectAndClose(resourceId);
        final var ds1 = "test";
        createDatastream(resourceId, ds1, ds1);
        final var condition = FEDORA_ID + "=" + resourceId + "*";
        final var matchingMimetypeCondition = MIME_TYPE + "=text/plain";
        final var nonMatchingMimetypeCondition = MIME_TYPE + "=image/jpg";
        final String searchUrl =
                getSearchEndpoint() + "condition=" + encode(condition) +
                        "&condition=" + encode(matchingMimetypeCondition);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);

            assertEquals(1, result.getItems().size());
        }

        final String searchUrl2 =
                getSearchEndpoint() + "condition=" + encode(condition) +
                        "&condition=" + encode(nonMatchingMimetypeCondition);

        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl2))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(0, result.getItems().size());
        }
    }

    @Test
    public void testSearchNoMatchingFedoraIds() throws Exception {
        final var condition = FEDORA_ID + "=" + serverAddress + "this-should-not-match-any-fedora-id";
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertNotNull(result);
            assertNotNull(result.getPagination());
            assertEquals(0, result.getItems().size(), "No results expected.");
        }
    }

    @Test
    public void testMalformedCondition() throws Exception {
        final var condition = "this_is_not_a_valid_condition";
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testInvalidField() throws Exception {
        final var invalidCondition = "customCondition=*";
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(invalidCondition);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(BAD_REQUEST.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testOrderingISAscendingByDefaultIfOrderByIsDefined() throws Exception {
        final var prefix = getRandomUniqueId();
        final var resources = createResources(prefix, 3);
        final var condition = FEDORA_ID + "=" + prefix + "*";
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition) + "&order_by=fedora_id";
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(resources, result.getItems().stream()
                    .map(x -> x.get("fedora_id")).collect(Collectors.toList()));
        }
    }

    @Test
    public void testDescOrdering() throws Exception {
        final var prefix = getRandomUniqueId();
        final var resources = createResources(prefix, 3);
        Collections.reverse(resources);
        final var condition = FEDORA_ID + "=" + prefix + "*";
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition) +
                "&order_by=fedora_id&order=desc";
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(resources, result.getItems().stream()
                    .map(x -> x.get("fedora_id")).collect(Collectors.toList()));
        }
    }

    @Test
    public void testOrderByMimetypeDesc() throws Exception {
        final var resourceId = getRandomUniqueId();
        createObjectAndClose(resourceId);
        final var resourceA = resourceId + "/a-video";
        final var resourceB = resourceId + "/b-image";
        final var resourceC = resourceId + "/c-text";
        final var resourceAMd = resourceId + "/a-video/fcr:metadata";
        final var resourceBMd = resourceId + "/b-image/fcr:metadata";
        final var resourceCMd = resourceId + "/c-text/fcr:metadata";

        assertEquals(201, getStatus(putObjMethod(resourceA, "video/mp4",
                "video")));
        assertEquals(201, getStatus(putObjMethod(resourceB, "image/jpg",
                "image")));
        assertEquals(201, getStatus(putObjMethod(resourceC, "text/plain",
                "text")));

        final var resources = Stream.of(resourceA, resourceC, resourceB, resourceAMd, resourceBMd, resourceCMd)
                        .map(x -> serverAddress + x)
                        .collect(Collectors.toList());
        final var condition = FEDORA_ID + "=" + resourceId + "/*";
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition) +
                "&order_by=mime_type&order=desc";
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(resources, result.getItems().stream()
                    .map(x -> x.get("fedora_id")).collect(Collectors.toList()));
        }
    }


    @Test
    public void testSearchByRdfSourceWildCards() throws Exception {
        testSearchByRdfTypeParam("*Basic*");
    }

    @Test
    public void testSearchByRdfSourceWildcard() throws Exception {
        testSearchByRdfTypeParam("*Container");
    }

    @Test
    public void testSearchByRdfSourceExactMatch() throws Exception {
        testSearchByRdfTypeParam("http://www.w3.org/ns/ldp#RDFSource");
    }

    private void testSearchByRdfTypeParam(final String rdfTypeString) throws Exception {
        final var resourceId = getRandomUniqueId();
        createObjectAndClose(resourceId);
        final var condition = FEDORA_ID + "=" + resourceId;
        final var rdfTypeCondition = RDF_TYPE + "=" + rdfTypeString;
        final String searchUrl =
                getSearchEndpoint() + "condition=" + encode(condition) + "&condition=" + encode(rdfTypeCondition);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(1, result.getItems().size());
        }

        final String searchUrl2 =
                getSearchEndpoint() + "condition=" + encode(condition) +
                        "&condition=" + encode(rdfTypeCondition + "with-non-matching-suffix");

        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl2))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(0, result.getItems().size());
        }
    }

    private String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Test
    public void testTotalResults() throws Exception {
        final String prefix = "test_total_";
        final var resources = createResources(prefix, 6);
        final var condition = FEDORA_ID + "=" + prefix + "*";
        final var maxResults = 2;
        final String searchUrl =
                getSearchEndpoint() + "include_total_result_count=true&condition=" + encode(condition) +
                        "&max_results=" + maxResults;
        try (final var response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(2, result.getItems().size());
            assertEquals(6, result.getPagination().getTotalResults());
        }
        // Try the next offset and see the total is the same
        final String searchUrl2 = searchUrl + "&offset=2";
        try (final var response = execute(new HttpGet(searchUrl2))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(2, result.getItems().size());
            assertEquals(6, result.getPagination().getTotalResults());
        }
    }

    @Test
    public void testTotalResultsMore() throws Exception {
        final String prefix = "test_total1_";
        final String prefix2 = "test_total2_";
        final var resources = createResources(prefix, 6);
        final var resources2 = createResources(prefix2, 6);
        final var condition = FEDORA_ID + "=" + prefix + "*";
        final var maxResults = 5;
        final String searchUrl =
                getSearchEndpoint() + "include_total_result_count=true&condition=" + encode(condition) +
                        "&max_results=" + maxResults;
        try (final var response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(5, result.getItems().size());
            assertEquals(6, result.getPagination().getTotalResults());
            assertTrue(result.getItems().stream().allMatch(r -> resources.contains(r.get("fedora_id").toString()) &&
                    !resources2.contains(r.get("fedora_id").toString())));
        }
        // Try the next offset and see the total is the same
        final String searchUrl2 = searchUrl + "&offset=5";
        try (final var response = execute(new HttpGet(searchUrl2))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(1, result.getItems().size());
            assertEquals(6, result.getPagination().getTotalResults());
            assertTrue(result.getItems().stream().allMatch(r -> resources.contains(r.get("fedora_id").toString()) &&
                    !resources2.contains(r.get("fedora_id").toString())));
        }
    }

    @Test
    public void testUnderScoreEscaping() throws Exception {
        final String prefix = "test_total_";
        final String prefix2 = "test_total2_";
        final String resource1;
        final String resource2;
        try (final var response = createObject(prefix + "-1")) {
            resource1 = getLocation(response);
        }
        try (final var response = createObject(prefix2 + "-1")) {
            resource2 = getLocation(response);
        }
        final var condition = FEDORA_ID + "=" + prefix + "*";

        final String searchUrl =
                getSearchEndpoint() + "include_total_result_count=true&condition=" + encode(condition) +
                        "&max_results=2";
        try (final var response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(1, result.getItems().size());
            assertEquals(1, result.getPagination().getTotalResults());
            assertEquals(resource1, result.getItems().get(0).get("fedora_id"));
        }
    }
}
