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

import static javax.ws.rs.core.HttpHeaders.LINK;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_HTML_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Link;

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.annotation.Timed;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.services.FixityService;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

/**
 * Run a fixity check on a path
 *
 * @author ajs6f
 * @since Jun 12, 2013
 */
@Timed
@Scope("request")
@Path("/{path: .*}/fcr:fixity")
public class FedoraFixity extends ContentExposingResource {

    private static final Logger LOGGER = getLogger(FedoraFixity.class);

    @PathParam("path") protected String externalPath;

    @Inject private FixityService fixityService;

    /**
     * Default JAX-RS entry point
     */
    public FedoraFixity() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param externalPath the external path
     */
    @VisibleForTesting
    public FedoraFixity(final String externalPath) {
        this.externalPath = externalPath;
    }

    /**
     * Get the results of a fixity check for a path
     *
     * GET /path/to/some/datastream/fcr:fixity
     *
     * @return datastream fixity in the given format
     */
    @GET
    @HtmlTemplate(value = "fcr:fixity")
    @Produces({TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8", N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET,
            RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET, TURTLE_X, TEXT_HTML_WITH_CHARSET, "*/*"})
    public RdfNamespacedStream getDatastreamFixity() {

        if (!(resource() instanceof Binary)) {
            throw new NotFoundException(resource() + " is not a binary");
        }

        final Link.Builder resourceLink = Link.fromUri(RESOURCE.getURI()).rel("type");
        servletResponse.addHeader(LINK, resourceLink.build().toString());
        final Link.Builder rdfSourceLink = Link.fromUri(RDF_SOURCE.getURI()).rel("type");
        servletResponse.addHeader(LINK, rdfSourceLink.build().toString());

        final Binary binaryResource = (Binary) resource();
        LOGGER.info("Get fixity for '{}'", externalPath);

        final RdfStream rdfStream = httpRdfService.bodyToExternalStream(getUri(binaryResource).toString(),
                fixityService.checkFixity(binaryResource), identifierConverter());
        return new RdfNamespacedStream(rdfStream, namespaceRegistry.getNamespaces());
    }

    @Override
    protected String externalPath() {
        return externalPath;
    }
}
