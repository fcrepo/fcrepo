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


import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.noContent;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.fcrepo.http.commons.domain.PATCH;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.RdfStream;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import com.google.common.annotations.VisibleForTesting;

/**
 * Endpoint for managing versions of nodes
 *
 * @author awoods
 * @author ajs6f
 */
@Scope("request")
@Path("/{path: .*}/fcr:versions/{labelAndOptionalPathIntoVersion: .*}")
public class FedoraVersions extends ContentExposingResource {

    private static final Logger LOGGER = getLogger(FedoraVersions.class);

    @PathParam("path") protected String externalPath;

    @PathParam("labelAndOptionalPathIntoVersion") protected String pathListIntoVersion;

    protected FedoraResource baseResource;

    /**
     * Default JAX-RS entry point
     */
    public FedoraVersions() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param externalPath the externalPath
     * @param pathListIntoVersion the string value of pathListIntoVersion
     */
    @VisibleForTesting
    public FedoraVersions(final String externalPath, final String pathListIntoVersion) {
        this.externalPath = externalPath;
        this.pathListIntoVersion = pathListIntoVersion;
    }

    /**
     * Reverts the resource at the given path to the version specified by
     * the label.
     * @return response
     */
    @PATCH
    public Response revertToVersion() {
        LOGGER.info("Reverting {} to version {}.", versionedPath(), versionLabel());
        versionService.revertToVersion(session.getFedoraSession(), unversionedResourcePath(), versionLabel());
        return noContent().build();
    }

    /**
     * Removes the version specified by the label.
     * @return 204 No Content
    **/
    @DELETE
    public Response removeVersion() {
        LOGGER.info("Removing {} version {}.", versionedPath(), versionLabel());
        versionService.removeVersion(session.getFedoraSession(), unversionedResourcePath(), versionLabel());
        return noContent().build();
    }

    /**
     * Retrieve a version of an object.  The path structure is as follows
     * (though these URLs are returned from getVersionList and need not be
     * constructed manually):
     * /versionable-node/fcr:versions/label/path/to/any/copied/unversionable/nodes
     * @param rangeValue the range value
     * @throws IOException if IO exception occurred
     * @return the version of the object as RDF in the requested format
     */
    @SuppressWarnings("resource")
    @GET
    @Produces({TURTLE + ";qs=1.0", JSON_LD + ";qs=0.8",
            N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
            TEXT_HTML, "*/*"})
    public Response getVersion(@HeaderParam("Range") final String rangeValue) throws IOException {
        LOGGER.trace("Getting version profile for: {} at version: {}", versionedPath(), versionLabel());
        checkCacheControlHeaders(request, servletResponse, resource(), session);
        final RdfStream rdfStream = new DefaultRdfStream(asNode(resource()));
        addResourceHttpHeaders(resource());

        return getContent(rangeValue, rdfStream);
    }

    protected String unversionedResourcePath() {

        if (baseResource == null) {
            baseResource = getResourceFromPath(externalPath);
            if ( baseResource instanceof FedoraBinary ) {
                baseResource = ((FedoraBinary)baseResource).getDescription();
            }
        }

        return baseResource.getPath();
    }

    @Override
    protected FedoraResource resource() {

        if (resource == null) {
            resource = getResourceFromPath(versionedPath());
        }

        return resource;
    }

    @Override
    protected void addResourceHttpHeaders(final FedoraResource resource) {
        super.addResourceHttpHeaders(resource);
        super.addResourceLinkHeaders(resource);
    }

    @Override
    protected String externalPath() {
        return externalPath;
    }

    private String versionedPath() {
        return externalPath + "/" + FCR_VERSIONS + "/" + pathListIntoVersion;
    }

    private String versionLabel() {
        return pathListIntoVersion.split("/", 2)[0];
    }
}
