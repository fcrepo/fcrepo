/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import com.google.common.annotations.VisibleForTesting;
import org.fcrepo.http.api.versioning.VersionAwareHttpIdentifierTranslator;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.exception.RepositoryVersionRuntimeException;
import org.fcrepo.kernel.impl.DatastreamImpl;
import org.fcrepo.kernel.impl.FedoraBinaryImpl;
import org.fcrepo.kernel.impl.FedoraObjectImpl;
import org.fcrepo.kernel.impl.rdf.impl.VersionsRdfContext;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singleton;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_METADATA;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/25/14
 */
@Scope("request")
@Path("/{path: .*}/fcr:versions")
public class FedoraVersioning extends AbstractResource {

    @Inject
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraVersioning.class);


    @Context protected Request request;
    @Context protected HttpServletResponse servletResponse;
    @Context protected UriInfo uriInfo;

    @PathParam("path") protected List<PathSegment> pathList;

    protected String path;

    /**
     * Default JAX-RS entry point
     */
    public FedoraVersioning() {
        super();
    }

    /**
     * Create a new FedoraNodes instance for a given path
     * @param path
     */
    @VisibleForTesting
    public FedoraVersioning(final String path) {
        this.path = path;
    }

    @PostConstruct
    private void postConstruct() {
        this.path = toPath(pathList);
    }


    /**
     * Enable versioning
     * @return
     * @throws java.net.URISyntaxException
     */
    @PUT
    public Response enableVersioning() throws URISyntaxException {
        try {

            final FedoraResource resource = nodeService.getObject(session, path);
            resource.enableVersioning();

            try {
                session.save();
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }
            return created(new URI(translator().getSubject(
                    path) + "/fcr:versions")).build();
        } finally {
            session.logout();
        }
    }

    /**
     * Disable versioning
     * @return
     */
    @DELETE
    public Response disableVersioning() {
        try {
            final FedoraResource resource = nodeService.getObject(session, path);
            resource.disableVersioning();

            try {
                session.save();
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            return noContent().build();
        } finally {
            session.logout();
        }
    }

    /**
     * Create a new version checkpoint and tag it with the given label.  If
     * that label already describes another version it will silently be
     * reassigned to describe this version.
     *
     * @return response
     * @throws RepositoryException
     */
    @POST
    public Response addVersion(@HeaderParam("Slug") final String slug) throws RepositoryException {
        try {
            final Collection<String> versions = versionService.createVersion(session.getWorkspace(),
                    singleton(path));

            if (slug != null) {
                unversionedResource().addVersionLabel(slug);
            }

            final String version = (slug != null) ? slug : versions.iterator().next();

            return noContent().header("Location", translator().getSubject(
                    path) + "/fcr:versions/" + version).build();
        } finally {
            session.logout();
        }
    }


    /**
     * Get the list of versions for the object
     *
     * @return List of versions for the object as RDF
     * @throws RepositoryException
     */
    @GET
    @HtmlTemplate(value = "fcr:versions")
    @Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
            TEXT_HTML, APPLICATION_XHTML_XML, JSON_LD})
    public RdfStream getVersionList() {
        final FedoraResource resource = nodeService.getObject(session, path);

        if (!resource.hasType("mix:versionable")) {
            throw new RepositoryVersionRuntimeException("This operation requires that the node be versionable");
        }

        return resource.getTriples(translator(), VersionsRdfContext.class)
                .session(session)
                .topic(translator().getSubject(resource.getPath()).asNode());
    }

    /**
     * A translator suitable for subjects that represent nodes.
     */
    protected VersionAwareHttpIdentifierTranslator translator() {
        return new VersionAwareHttpIdentifierTranslator(session,  FedoraNodes.class,
                uriInfo);
    }


    @VisibleForTesting
    protected FedoraResource unversionedResource() {
        final FedoraResource resource;

        try {
            final boolean metadata = pathList.get(pathList.size() - 1).getPath().equals(FCR_METADATA);

            final Node node = session.getNode(path);

            if (DatastreamImpl.hasMixin(node)) {
                final DatastreamImpl datastream = new DatastreamImpl(node);

                if (metadata) {
                    resource = datastream;
                } else {
                    resource = datastream.getBinary();
                }
            } else if (FedoraBinaryImpl.hasMixin(node)) {
                resource = new FedoraBinaryImpl(node);
            } else {
                resource = new FedoraObjectImpl(node);
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return resource;
    }

}
