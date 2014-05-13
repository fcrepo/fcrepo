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
package org.fcrepo.http.commons;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Date;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.api.rdf.HttpTripleUtil;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.identifiers.PidMinter;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.LockService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.services.RepositoryService;
import org.fcrepo.kernel.services.VersionService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.eventbus.EventBus;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Abstract superclass for Fedora JAX-RS Resources, providing convenience fields
 * and methods.
 *
 * @author ajs6f
 */
public abstract class AbstractResource {

    private static final Logger LOGGER = getLogger(AbstractResource.class);

    /**
     * Useful for constructing URLs
     */
    @Context
    protected UriInfo uriInfo;

    @Autowired
    protected SessionFactory sessions;

    /**
     * The fcrepo node service
     */
    @Autowired
    protected NodeService nodeService;

    /**
     * The fcrepo object service
     */
    @Autowired
    protected ObjectService objectService;

    /**
     * The fcrepo datastream service
     */
    @Autowired
    protected DatastreamService datastreamService;

    /**
     * The fcrepo version service
     */
    @Autowired
    protected VersionService versionService;

    /**
     * The fcrepo repository service
     */
    @Autowired
    protected RepositoryService repositoryService;

    /**
     * The fcrepo lock service
     */
    @Autowired
    protected LockService lockService;

    @Autowired(required = false)
    private HttpTripleUtil httpTripleUtil;

    @Autowired(required = false)
    protected EventBus eventBus;

    /**
     * A resource that can mint new Fedora PIDs.
     */
    @Autowired
    protected PidMinter pidMinter;

    /**
     * A convenience object provided by ModeShape for acting against the JCR
     * repository.
     */
    protected static final JcrTools jcrTools = new JcrTools(true);

    /**
     * Convert a JAX-RS list of PathSegments to a JCR path
     *
     * @param paths
     * @return
     */
    public static final String toPath(final List<PathSegment> paths) {
        final StringBuilder result = new StringBuilder();
        LOGGER.trace("converting URI path to JCR path: {}", paths);

        int i = 0;

        for (final PathSegment path : paths) {
            final String p = path.getPath();

            if (p.equals("")) {
                LOGGER.trace("Ignoring empty segment {}", p);
            } else if (i == 0 &&
                    (p.startsWith("tx:") || p.startsWith("workspace:"))) {
                LOGGER.trace("Ignoring internal segment {}", p);
                i++;
            } else {

                LOGGER.trace("Adding segment {}", p);

                if (!p.startsWith("[")) {
                    result.append('/');
                }
                result.append(p);
                i++;
            }
        }

        final String path = result.toString();

        if (path.isEmpty()) {
            return "/";
        }
        return path;
    }

    /**
     * Evaluate the cache control headers for the request to see if it can be served from
     * the cache.
     *
     * @param request
     * @param servletResponse
     * @param resource
     * @throws javax.jcr.RepositoryException
     */
    protected static void checkCacheControlHeaders(final Request request,
                                                   final HttpServletResponse servletResponse,
                                                   final FedoraResource resource) throws RepositoryException {
        evaluateRequestPreconditions(request, resource, true);
        addCacheControlHeaders(servletResponse, resource);
    }

    /**
     * Add ETag and Last-Modified cache control headers to the response
     * @param servletResponse
     * @param resource
     * @throws RepositoryException
     */
    protected static void addCacheControlHeaders(final HttpServletResponse servletResponse,
                                                 final FedoraResource resource) throws RepositoryException {

        final EntityTag etag = new EntityTag(resource.getEtagValue());
        final Date date = resource.getLastModifiedDate();

        if (!etag.getValue().isEmpty()) {
            servletResponse.addHeader("ETag", etag.toString());
        }

        if (date != null) {
            servletResponse.addDateHeader("Last-Modified", date.getTime());
        }
    }

    /**
     * Evaluate request preconditions to ensure the resource is the expected state
     * @param request
     * @param resource
     */
    protected static void evaluateRequestPreconditions(final Request request,
                                                       final FedoraResource resource) throws RepositoryException {
        evaluateRequestPreconditions(request, resource, false);
    }

    protected static MediaType getSimpleContentType(final MediaType requestContentType) {
        return requestContentType != null ? new MediaType(requestContentType.getType(), requestContentType.getSubtype())
            : APPLICATION_OCTET_STREAM_TYPE;
    }

    protected static boolean isRdfContentType(final String contentTypeString) {
        return !contentTypeString.equals("text/plain") && contentTypeToLang(contentTypeString) != null;
    }

    protected void addResponseInformationToStream(
            final FedoraResource resource, final RdfStream dataset,
            final UriInfo uriInfo, final IdentifierTranslator subjects)
        throws RepositoryException {
        if (httpTripleUtil != null) {
            httpTripleUtil.addHttpComponentModelsForResourceToStream(dataset, resource,
                    uriInfo, subjects);
        }
    }

    private static void evaluateRequestPreconditions( final Request request, final FedoraResource resource,
       boolean cacheControl ) throws RepositoryException {

        final EntityTag etag = new EntityTag(resource.getEtagValue());
        final Date date = resource.getLastModifiedDate();
        final Date roundedDate = new Date();

        if (date != null) {
            roundedDate.setTime(date.getTime() - date.getTime() % 1000);
        }

        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
        if ( builder != null ) {
            builder = builder.entity("ETag mismatch");
        } else {
            builder = request.evaluatePreconditions(roundedDate);
            if ( builder != null ) {
                builder = builder.entity("Date mismatch");
            }
        }

        if (builder != null && cacheControl ) {
            final CacheControl cc = new CacheControl();
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);
            // here we are implicitly emitting a 304
            // the exception is not an error, it's genuinely
            // an exceptional condition
            builder = builder.cacheControl(cc).lastModified(date).tag(etag);
        }
        if (builder != null) {
            throw new WebApplicationException(builder.build());
        }
    }
    /**
     * Convert the transparent path to hierarchy path
     * @param resource
     * @param session
     * @param uriInfo
     * @param clazz
     * @return
     * @throws RepositoryException
     */
    public static String getJCRPath(Resource resource, Session session, UriInfo uriInfo,
            Class<?> clazz) throws RepositoryException {
        final HttpIdentifierTranslator idTranslator =
                new HttpIdentifierTranslator(session, clazz, uriInfo);
        return idTranslator.getPathFromSubject(resource);
    }
}
