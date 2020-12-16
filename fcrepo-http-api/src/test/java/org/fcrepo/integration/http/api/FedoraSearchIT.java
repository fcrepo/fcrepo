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
package org.fcrepo.integration.http.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.SearchResult;
import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.search.api.Condition.Field.CONTENT_SIZE;
import static org.fcrepo.search.api.Condition.Field.CREATED;
import static org.fcrepo.search.api.Condition.Field.FEDORA_ID;
import static org.fcrepo.search.api.Condition.Field.MIME_TYPE;
import static org.fcrepo.search.api.Condition.Field.MODIFIED;
import static org.fcrepo.search.api.Condition.Field.RDF_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author dbernstein
 * @since 05/06/20
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FedoraSearchIT extends AbstractResourceIT {

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
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertNotNull(result);
            assertNotNull(result.getPagination());
            assertTrue("Items not found! " + resources, result.getItems().size() > 0);
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
                final ObjectMapper objectMapper = new ObjectMapper();
                final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                        SearchResult.class);
                assertEquals("expected " + count + " items where condition = " + condition, count,
                        result.getItems().size());

                result.getItems().stream().map(x -> x.get(FEDORA_ID.toString()))
                        .forEach(x -> assertTrue(x.toString().startsWith(urlPrefix)));
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
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals("expected 1 item where condition = " + condition, 1,
                    result.getItems().size());
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
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals("expected 1 item where condition = " + condition, 1,
                    result.getItems().size());
            final var newModified = result.getItems().get(0).get(MODIFIED.toString()).toString();
            assertNotEquals("Modified date should have changed  but it did not.", modified, newModified);
        }
    }

    @Test
    public void testUpdateSearchIndexOnResourceDelete() throws Exception {
        final var resourceId = createResources(1).get(0);
        final var condition = FEDORA_ID + "=" + resourceId;
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals("expected 1 item where condition = " + condition, 1,
                    result.getItems().size());
        }

        final var httpDelete = new HttpDelete(resourceId);
        try (final CloseableHttpResponse response = execute(httpDelete)) {
            assertEquals(NO_CONTENT.getStatusCode(), getStatus(response));
        }

        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals("expected 0 items where condition = " + condition, 0,
                    result.getItems().size());
        }
    }

    public void testModifiedGreaterAndLessThan() throws Exception {
        final var id = getRandomUniqueId();
        final int count = 1;
        final var instant = Instant.now();
        final var now = instant.toString();
        final var tomorrow = instant.plus(Duration.ofDays(1)).toString();
        createResources(id, count);
        final var fedoraId = FEDORA_ID_PREFIX + "/" + id;
        final var fedoraIdCondition = FEDORA_ID + "=" + fedoraId;
        final var lessThanNow = MODIFIED + "<" + now;
        final var greaterThanNow = MODIFIED + ">" + now;
        final var lessThanTomorrow = MODIFIED + "<" + tomorrow;
        final var greaterThanTomorrow = MODIFIED + ">" + tomorrow;

        //no results for resources modified before now
        String searchUrl =
                getSearchEndpoint() + "condition=" + encode(fedoraIdCondition) + "&" + lessThanNow;
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals("expected no results", 0,
                    result.getItems().size());
        }
        //no results for resources modified after tomorrow
        searchUrl =
                getSearchEndpoint() + "condition=" + encode(fedoraIdCondition) + "&" + greaterThanTomorrow;
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals("expected no results", 0,
                    result.getItems().size());
        }

        // ensure that greater than now returns a result
        searchUrl = getSearchEndpoint() + "condition=" + encode(fedoraIdCondition) + "&" + greaterThanNow;
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals("expected " + count + " results", count,
                    result.getItems().size());

            result.getItems().stream().map(x -> x.get("fedora_id"))
                    .forEach(x -> assertTrue(x.toString().equals(fedoraId)));
        }

        // ensure that less than tomorrow returns a result
        searchUrl = getSearchEndpoint() + "condition=" + encode(fedoraIdCondition) + "&" + lessThanTomorrow;
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals("expected " + count + " results", count,
                    result.getItems().size());

            result.getItems().stream().map(x -> x.get("fedora_id"))
                    .forEach(x -> assertTrue(x.toString().equals(fedoraId)));
        }

        // ensure that between now and tomorrow returns a result.
        searchUrl = getSearchEndpoint() + "condition=" + encode(fedoraIdCondition) + "&" + greaterThanNow +
                "&" + lessThanTomorrow;
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                    SearchResult.class);
            assertEquals("expected " + count + " results ", count,
                    result.getItems().size());
            result.getItems().stream().map(x -> x.get("fedora_id"))
                    .forEach(x -> assertTrue(x.toString().equals(fedoraId)));
        }
    }

    @Test
    public void testFieldsReturnsSpecifiedFields() throws Exception {
        testFieldsReturnsOnlySpecifiedFields(Arrays.asList(FEDORA_ID, CREATED));
    }

    @Test
    public void testFieldsReturnsMimeType() throws Exception {
        testFieldsReturnsOnlySpecifiedFields(Arrays.asList(MIME_TYPE));
    }

    @Test
    public void testFieldsReturnsContentSize() throws Exception {
        testFieldsReturnsOnlySpecifiedFields(Arrays.asList(CONTENT_SIZE));
    }

    @Test
    public void testFieldsReturnsRDFType() throws Exception {
        testFieldsReturnsOnlySpecifiedFields(Arrays.asList(RDF_TYPE));
    }


    private void testFieldsReturnsOnlySpecifiedFields(final List<Condition.Field> fieldList) throws Exception {
        final var id = getRandomUniqueId();
        final var count = 1;
        createResources(id, count);
        final var fields = fieldList.stream()
                .map(x -> x.toString()).collect(Collectors.toList());
        final String searchUrl =
                getSearchEndpoint() + "condition=" + encode("fedora_id=" + id + "*") + "&fields=" + String.join(",",
                        fields);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            final var items = result.getItems();
            assertEquals("expected " + count + " items", count, items.size());
            for (final Map<String, Object> item : items) {
                assertEquals("expected " + fields.size() + " fields returned", fields.size(), item.size());
                for (final String field : fields) {
                    assertTrue("Result does not contain expected field: " + field, item.containsKey(field));
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
                getSearchEndpoint() + "condition=" + encode(condition) + "&max_results=" + maxResults + "&offset=2";
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(maxResults, result.getItems().size());
            final var returnedIds =
                    result.getItems().stream().map(x -> x.get("fedora_id")).collect(Collectors.toList());
            assertEquals(resources.get(2), returnedIds.get(0));
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
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(2, result.getItems().size());
        }

        final String searchUrl2 =
                getSearchEndpoint() + "condition=" + encode(condition) +
                        "&condition=" + encode(contentSizeConditionLongerLength);

        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl2))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
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
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);

            assertEquals(1, result.getItems().size());
        }

        final String searchUrl2 =
                getSearchEndpoint() + "condition=" + encode(condition) +
                        "&condition=" + encode(nonMatchingMimetypeCondition);

        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl2))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
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
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertNotNull(result);
            assertNotNull(result.getPagination());
            assertEquals("No results expected.", 0, result.getItems().size());
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
    public void testDefaultOrdering() throws Exception {
        final var prefix = getRandomUniqueId();
        final var resources = createResources(prefix, 3);
        final var condition = FEDORA_ID + "=" + prefix + "*";
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
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
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition) + "&order=desc";
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
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

        assertEquals(201, getStatus(putObjMethod(resourceA, "video/mp4",
                "video")));
        assertEquals(201, getStatus(putObjMethod(resourceB, "image/jpg",
                "image")));
        assertEquals(201, getStatus(putObjMethod(resourceC, "text/plain",
                "text")));

        final var resources =
                Arrays.asList(resourceA, resourceC, resourceB).stream().map(x -> serverAddress + x)
                        .collect(Collectors.toList());
        final var condition = FEDORA_ID + "=" + resourceId + "/*";
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition) +
                "&order_by=mime_type&order=desc";
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
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
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(1, result.getItems().size());
        }

        final String searchUrl2 =
                getSearchEndpoint() + "condition=" + encode(condition) +
                        "&condition=" + encode(rdfTypeCondition + "with-non-matching-suffix");

        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl2))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertEquals(0, result.getItems().size());
        }
    }

    private String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
