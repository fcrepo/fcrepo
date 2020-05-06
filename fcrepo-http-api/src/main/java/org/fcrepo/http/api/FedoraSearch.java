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
package org.fcrepo.http.api;

import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.search.api.SearchService;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ok;
import static org.fcrepo.http.commons.domain.RDFMediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author dbernstein
 * @since 05/06/20
 */

@Scope("request")
@Path("/fcr:search")
public class FedoraSearch extends FedoraBaseResource {

    private static final Logger LOGGER = getLogger(FedoraSearch.class);

    private static final String WANT_DIGEST = "Want-Digest";

    private static final String DIGEST = "Digest";

    private static final MediaType DEFAULT_RDF_CONTENT_TYPE = TURTLE_TYPE;
    private static final MediaType DEFAULT_NON_RDF_CONTENT_TYPE = APPLICATION_OCTET_STREAM_TYPE;

    @Inject
    private SearchService service;

    /**
     * Default JAX-RS entry point
     */
    public FedoraSearch() {
        super();
    }

    /**
     * Perform simple search on the repository
     *
     * @return A response object with the search results
     * @throws IOException                   if IO exception occurred
     * @throws UnsupportedAlgorithmException if unsupported digest algorithm occurred
     */
    @GET
    @Produces({APPLICATION_JSON + ";qs=1.0",
            TEXT_PLAIN_WITH_CHARSET})
    public Response doSearch(@QueryParam(value="query") final String query)
            throws IOException, UnsupportedAlgorithmException {
        final var params = new SearchParameters(query);
        final Response.ResponseBuilder builder = ok();
        builder.entity(this.service.doSearch(params));
        return builder.build();
    }

}

