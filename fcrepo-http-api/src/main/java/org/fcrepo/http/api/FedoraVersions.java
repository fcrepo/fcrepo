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

import static javax.ws.rs.core.Response.noContent;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_HTML_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.fcrepo.http.api.PathLockManager.AcquiredLock;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

/**
 * Endpoint for managing versions of nodes
 *
 * @author awoods
 * @author ajs6f
 */
@Scope("request")
@Path("/{path: .*}/fcr:versions/{labelAndOptionalPathIntoVersion: .*(?<!/fcr:fixity)$}")
public class FedoraVersions extends ContentExposingResource {

    private static final Logger LOGGER = getLogger(FedoraVersions.class);

    @PathParam("path") protected String externalPath;

    @PathParam("labelAndOptionalPathIntoVersion") protected String pathListIntoVersion;

    protected String path;
    protected String label;
    protected String pathIntoVersion;

    protected FedoraResource baseResource;

    /**
     * Default JAX-RS entry point
     */
    public FedoraVersions() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param path the path
     * @param label the label
     * @param pathIntoVersion the string value of pathIntoVersion
     */
    @VisibleForTesting
    public FedoraVersions(final String path, final String label, final String pathIntoVersion) {
        this.path = path;
        this.label = label;
        this.pathIntoVersion = pathIntoVersion;
    }

    @PostConstruct
    private void postConstruct() {
        this.path = externalPath + "/" + FCR_VERSIONS + "/" + pathListIntoVersion;
        this.label = pathListIntoVersion.split("/", 2)[0];
    }

    /**
     * Removes the version specified by the label.
     * @return 204 No Content
    **/
    @DELETE
    public Response removeVersion() {
        LOGGER.info("Removing {} version {}.", path, label);
        hasRestrictedPath(externalPath);
        if (resource() instanceof Container) {
            final String depth = headers.getHeaderString("Depth");
            LOGGER.debug("Depth header value is: {}", depth);
            if (depth != null && !depth.equalsIgnoreCase("infinity")) {
                throw new ClientErrorException("Depth header, if present, must be set to 'infinity' for containers",
                    SC_BAD_REQUEST);
            }
        }

        evaluateRequestPreconditions(request, servletResponse, resource(), session);

        LOGGER.info("Delete resource '{}'", externalPath);

        final AcquiredLock lock = lockManager.lockForDelete(resource().getPath());
        try {
            resource().delete();
            session.commit();
            return noContent().build();
        } finally {
            lock.release();
        }
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
    @Produces({TURTLE_WITH_CHARSET + ";qs=1.0", JSON_LD + ";qs=0.8", N3_WITH_CHARSET, N3_ALT2_WITH_CHARSET,
            RDF_XML, NTRIPLES, TEXT_PLAIN_WITH_CHARSET, TURTLE_X,
            TEXT_HTML_WITH_CHARSET, "*/*"})
    public Response getVersion(@HeaderParam("Range") final String rangeValue) throws IOException {
        LOGGER.trace("Getting version profile for: {} at version: {}", path,
                label);
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
            resource = getResourceFromPath(path);
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
}
