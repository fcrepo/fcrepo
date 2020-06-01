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
import org.apache.http.client.methods.HttpGet;
import org.fcrepo.search.api.SearchResult;
import org.junit.Test;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.fcrepo.search.api.Condition.Field.fedora_id;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author dbernstein
 * @since 05/06/20
 */
public class FedoraSearchIT extends AbstractResourceIT {


    private List<String> createResources(final int count) throws IOException {
        return createResources(getRandomUniqueId(), count);
    }

    private List<String> createResources(final String prefix, final int count) throws IOException {
        final var resources = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            final var response = createObject(prefix + "-" + String.format("%05d", i));
            resources.add(getLocation(response));
            response.close();
        }
        return resources;
    }

    @Test
    public void testSearchAllResources() throws Exception {
        final var resources = createResources(3);
        final var condition = fedora_id + "=*";
        final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertNotNull(result);
            assertNotNull(result.getPagination());
            assertTrue(result.getItems().size() >= 3);
            final var resultingFedoraIds =
                    result.getItems().stream().map(x -> x.get("fedora_id")).collect(Collectors.toList());
            assertTrue("results must contain all newly created resources",
                    resultingFedoraIds.containsAll(resources));
        }
    }

    private String getSearchEndpoint() {
        return serverAddress + "fcr:search?";
    }

    @Test
    public void testWildcardMatchingOnPartialFedoraId() throws Exception {
        final var id = getRandomUniqueId();
        final int count = 3;
        createResources(id, count);
        final var urlPrefix = serverAddress + id;
        // try all valid prefix formulations
        final var prefixes = new String[]{id, "/" + id, urlPrefix, "info:fedora/" + id};
        for (String prefix : prefixes) {
            final var condition = fedora_id + "=" + prefix + "*";
            final String searchUrl = getSearchEndpoint() + "condition=" + encode(condition);
            try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
                assertEquals(OK.getStatusCode(), getStatus(response));
                final ObjectMapper objectMapper = new ObjectMapper();
                final SearchResult result = objectMapper.readValue(response.getEntity().getContent(),
                        SearchResult.class);
                assertEquals("expected " + count + " items where condition = " + condition, count,
                        result.getItems().size());

                result.getItems().stream().map(x -> x.get("fedora_id"))
                        .forEach(x -> assertTrue(x.toString().startsWith(urlPrefix)));
            }
        }

    }

    @Test
    public void testMaxResultsAndOffset() throws Exception {
        final var prefix = getRandomUniqueId();
        final int count = 3;
        final var resources = createResources(prefix, count);
        final var condition = fedora_id + "=" + prefix + "*";
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
    public void testSearchNoMatchingFedoraIds() throws Exception {
        final var condition = fedora_id + "=" + serverAddress + "this-should-not-match-any-fedora-id";
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

    private String encode(final String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
