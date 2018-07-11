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

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.junit.Assert.assertEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_CREATED;

import javax.ws.rs.core.Link;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.junit.Test;

/**
 * @author whikloj
 * @since 2018-07-10
 */
public class ExternalContentHandlerIT extends AbstractResourceIT {

    private static final String NON_RDF_SOURCE_LINK_HEADER = "<" + NON_RDF_SOURCE.getURI() + ">;rel=\"type\"";

    @Test
    public void testRemoteUriContentType() throws Exception {
        final HttpPost method = postObjMethod();
        method.addHeader(CONTENT_TYPE, "audio/ogg");
        method.addHeader(LINK, NON_RDF_SOURCE_LINK_HEADER);
        method.setEntity(new StringEntity("xyz"));
        final String external_location;
        final String final_location = "proxy_resource";

        // Make an external remote URI.
        try (final CloseableHttpResponse response = execute(method)) {
            assertEquals(SC_CREATED, getStatus(response));
            external_location = getLocation(response);
        }
        // Make an external content resource proxying the above URI.
        final HttpPut put = putObjMethod(final_location);
        final String externalLink = Link.fromUri(external_location)
            .rel("http://fedora.info/definitions/fcrepo#ExternalContent").param("handling", "proxy").build().toString();
        put.addHeader("Link", externalLink);
        try (final CloseableHttpResponse response = execute(put)) {
            assertEquals(SC_CREATED, getStatus(response));
        }
        // Get the external content proxy resource.
        try (final CloseableHttpResponse response = execute(getObjMethod(final_location))) {
            assertEquals(SC_OK, getStatus(response));
            assertEquals("audio/ogg", response.getFirstHeader(CONTENT_TYPE).getValue());
            assertEquals(external_location, response.getFirstHeader(CONTENT_LOCATION).getValue());
        }

    }
}
