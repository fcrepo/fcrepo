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

import java.net.URLEncoder;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author dbernstein
 * @since 05/06/20
 */
public class FedoraSearchIT extends AbstractResourceIT {


    @Test
    public void testSearchAllResources() throws Exception {

        final var condition = "fedoraId=*";
        final String searchUrl = serverAddress + "fcr:search" + "?query=" + doubleEncode(condition);
        try (final CloseableHttpResponse response = execute(new HttpGet(searchUrl))) {
            assertEquals(OK.getStatusCode(), getStatus(response));
            final ObjectMapper objectMapper = new ObjectMapper();
            final SearchResult result = objectMapper.readValue(response.getEntity().getContent(), SearchResult.class);
            assertNotNull(result);
            assertNotNull(result.getPagination());
            assertEquals(0, result.getResults().size());
        }
    }

    private String encode(final String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    private String doubleEncode(final String value) throws Exception {
        return encode(encode(value));
    }
}
