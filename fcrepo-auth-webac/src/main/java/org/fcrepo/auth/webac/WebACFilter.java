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

package org.fcrepo.auth.webac;

import com.fasterxml.jackson.core.JsonParseException;
import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFReader;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.UpdateData;
import org.apache.jena.sparql.modify.request.UpdateDataDelete;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.http.commons.domain.MultiPrefer;
import org.fcrepo.http.commons.domain.SinglePrefer;
import org.fcrepo.http.commons.domain.ldp.LdpPreferTag;
import org.fcrepo.http.commons.session.TransactionProvider;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.TransactionManager;
import org.fcrepo.kernel.api.TransactionUtils;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.slf4j.Logger;
import org.springframework.web.filter.RequestContextFilter;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.WebContent.contentTypeJSONLD;
import static org.apache.jena.riot.WebContent.contentTypeN3;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.apache.jena.riot.WebContent.contentTypeTurtle;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_USER_ROLE;
import static org.fcrepo.auth.webac.URIConstants.FOAF_AGENT_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_APPEND;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_CONTROL;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE;
import static org.fcrepo.auth.webac.WebACAuthorizingRealm.URIS_TO_AUTHORIZE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.fcrepo.http.commons.session.TransactionConstants.ATOMIC_ID_HEADER;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_TX;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author peichman
 */
public class WebACFilter extends RequestContextFilter {

    private static final Logger log = getLogger(WebACFilter.class);

    private static final MediaType sparqlUpdate = MediaType.valueOf(contentTypeSPARQLUpdate);

    private static final Principal FOAF_AGENT_PRINCIPAL = new Principal() {

        @Override
        public String getName() {
            return FOAF_AGENT_VALUE;
        }

        @Override
        public String toString() {
            return getName();
        }

    };

    private static final PrincipalCollection FOAF_AGENT_PRINCIPAL_COLLECTION =
            new SimplePrincipalCollection(FOAF_AGENT_PRINCIPAL, WebACAuthorizingRealm.class.getCanonicalName());

    private static Subject FOAF_AGENT_SUBJECT;

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private TransactionManager transactionManager;

    private static Set<URI> directOrIndirect = Set.of(INDIRECT_CONTAINER, DIRECT_CONTAINER).stream()
            .map(Resource::toString).map(URI::create).collect(Collectors.toSet());

    private static Set<String> rdfContentTypes = Set.of(contentTypeTurtle, contentTypeJSONLD, contentTypeN3,
            contentTypeRDFXML, contentTypeNTriples);

    /**
     * Generate a HttpIdentifierConverter from the request URL.
     * @param request the servlet request.
     * @return a converter.
     */
    public static HttpIdentifierConverter identifierConverter(final HttpServletRequest request) {
        final var uriBuild = UriBuilder.fromUri(getBaseUri(request)).path("/{path: .*}");
        return new HttpIdentifierConverter(uriBuild);
    }

    /**
     * Calculate a base Uri for this request.
     * @param request the incoming request
     * @return the URI
     */
    public static URI getBaseUri(final HttpServletRequest request) {
        final String host = request.getScheme() + "://" + request.getServerName() +
                (request.getServerPort() != 80 ? ":" + request.getServerPort() : "") + "/";
        final String requestUrl = request.getRequestURL().toString();
        final String contextPath = request.getContextPath() + request.getServletPath();
        final String baseUri;
        if (contextPath.length() == 0) {
            baseUri = host;
        } else {
            baseUri = requestUrl.split(contextPath)[0] + contextPath + "/";
        }
        return URI.create(baseUri);
    }

    /**
     * Add URIs to collect permissions information for.
     *
     * @param httpRequest the request.
     * @param uri the uri to check.
     */
    private void addURIToAuthorize(final HttpServletRequest httpRequest, final URI uri) {
        @SuppressWarnings("unchecked")
        Set<URI> targetURIs = (Set<URI>) httpRequest.getAttribute(URIS_TO_AUTHORIZE);
        if (targetURIs == null) {
            targetURIs = new HashSet<>();
            httpRequest.setAttribute(URIS_TO_AUTHORIZE, targetURIs);
        }
        targetURIs.add(uri);
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
                                    final FilterChain chain) throws ServletException, IOException {
        final Subject currentUser = SecurityUtils.getSubject();
        HttpServletRequest httpRequest = request;
        if (isSparqlUpdate(httpRequest) || isRdfRequest(httpRequest)) {
            // If this is a sparql request or contains RDF.
            httpRequest = new CachedHttpRequest(httpRequest);
        }

        final String requestUrl = httpRequest.getRequestURL().toString();
        try {
            FedoraId.create(identifierConverter(httpRequest).toInternalId(requestUrl));
        } catch (final InvalidResourceIdentifierException e) {
            printException(response, SC_BAD_REQUEST, e);
            return;
        } catch (final IllegalArgumentException e) {
            // No Fedora request path provided, so just continue along.
        }

        // add the request URI to the list of URIs to retrieve the ACLs for
        addURIToAuthorize(httpRequest, URI.create(requestUrl));

        if (currentUser.isAuthenticated()) {
            log.debug("User is authenticated");
            if (currentUser.hasRole(FEDORA_ADMIN_ROLE)) {
                log.debug("User has fedoraAdmin role");
            } else if (currentUser.hasRole(FEDORA_USER_ROLE)) {
                log.debug("User has fedoraUser role");
                // non-admins are subject to permission checks
                if (!isAuthorized(currentUser, httpRequest)) {
                    // if the user is not authorized, set response to forbidden
                    response.sendError(SC_FORBIDDEN);
                    return;
                }
            } else {
                log.debug("User has no recognized servlet container role");
                // missing a container role, return forbidden
                response.sendError(SC_FORBIDDEN);
                return;
            }
        } else {
            log.debug("User is NOT authenticated");
            // anonymous users are subject to permission checks
            if (!isAuthorized(getFoafAgentSubject(), httpRequest)) {
                // if anonymous user is not authorized, set response to forbidden
                response.sendError(SC_FORBIDDEN);
                return;
            }
        }

        // proceed to the next filter
        chain.doFilter(httpRequest, response);
    }

    /**
     * Displays the message from the exception to the screen.
     * @param response the servlet response
     * @param e the exception being handled
     * @throws IOException if problems opening the output writer.
     */
    private void printException(final HttpServletResponse response, final int responseCode, final Throwable e)
            throws IOException {
        final var message = e.getMessage();
        response.resetBuffer();
        response.setStatus(responseCode);
        response.setContentType(TEXT_PLAIN_WITH_CHARSET);
        response.setContentLength(message.length());
        response.setCharacterEncoding("UTF-8");
        final var write = response.getWriter();
        write.write(message);
        write.flush();
    }

    private Subject getFoafAgentSubject() {
        if (FOAF_AGENT_SUBJECT == null) {
            FOAF_AGENT_SUBJECT = new Subject.Builder().principals(FOAF_AGENT_PRINCIPAL_COLLECTION).buildSubject();
        }
        return FOAF_AGENT_SUBJECT;
    }

    private Transaction transaction(final HttpServletRequest request) {
        final String txId = request.getHeader(ATOMIC_ID_HEADER);
        if (txId == null) {
            return null;
        }
        final var txProvider = new TransactionProvider(transactionManager, request, getBaseUri(request));
        return txProvider.provide();
    }

    private String getContainerUrl(final HttpServletRequest servletRequest) {
        final String pathInfo = servletRequest.getPathInfo();
        final String baseUrl = servletRequest.getRequestURL().toString().replace(pathInfo, "");
        final String[] paths = pathInfo.split("/");
        final String[] parentPaths = java.util.Arrays.copyOfRange(paths, 0, paths.length - 1);
        return baseUrl + String.join("/", parentPaths);
    }

    private FedoraResource getContainer(final HttpServletRequest servletRequest) {
        final FedoraResource resource = resource(servletRequest);
        if (resource != null) {
            return resource(servletRequest).getContainer();
        }
        final String parentURI = getContainerUrl(servletRequest);
        return resource(servletRequest, getIdFromRequest(servletRequest, parentURI));
    }

    private FedoraResource resource(final HttpServletRequest servletRequest) {
        return resource(servletRequest, getIdFromRequest(servletRequest));
    }

    private FedoraResource resource(final HttpServletRequest servletRequest, final FedoraId resourceId) {
        try {
            return this.resourceFactory.getResource(transaction(servletRequest), resourceId);
        } catch (final PathNotFoundException e) {
            return null;
        }
    }

    private FedoraId getIdFromRequest(final HttpServletRequest servletRequest) {
        final String httpURI = servletRequest.getRequestURL().toString();
        return getIdFromRequest(servletRequest, httpURI);
    }

    private FedoraId getIdFromRequest(final HttpServletRequest request, final String httpURI) {
        return FedoraId.create(identifierConverter(request).toInternalId(httpURI));
    }

    private boolean isAuthorized(final Subject currentUser, final HttpServletRequest httpRequest) throws IOException {
        final String requestURL = httpRequest.getRequestURL().toString();
        final boolean isAcl = requestURL.endsWith(FCR_ACL);
        final boolean isTxEndpoint = requestURL.endsWith(FCR_TX) || requestURL.endsWith(FCR_TX + "/");
        final URI requestURI = URI.create(requestURL);
        log.debug("Request URI is {}", requestURI);
        final FedoraResource resource = resource(httpRequest);
        final FedoraResource container = getContainer(httpRequest);

        // WebAC permissions
        final WebACPermission toRead = new WebACPermission(WEBAC_MODE_READ, requestURI);
        final WebACPermission toWrite = new WebACPermission(WEBAC_MODE_WRITE, requestURI);
        final WebACPermission toAppend = new WebACPermission(WEBAC_MODE_APPEND, requestURI);
        final WebACPermission toControl = new WebACPermission(WEBAC_MODE_CONTROL, requestURI);

        switch (httpRequest.getMethod()) {
        case "OPTIONS":
        case "HEAD":
        case "GET":
            if (isAcl) {
                if (currentUser.isPermitted(toControl)) {
                    log.debug("GET allowed by {} permission", toControl);
                    return true;
                } else {
                    log.debug("GET prohibited without {} permission", toControl);
                    return false;
                }
            } else {
                if (currentUser.isPermitted(toRead)) {
                    if (!isAuthorizedForEmbeddedRequest(httpRequest, currentUser, resource)) {
                        log.debug("GET/HEAD/OPTIONS request to {} denied, user {} not authorized for an embedded " +
                                "resource", requestURL, currentUser.toString());
                        return false;
                    }
                    return true;
                }
                return false;
            }
        case "PUT":
            if (isAcl) {
                if (currentUser.isPermitted(toControl)) {
                    log.debug("PUT allowed by {} permission", toControl);
                    return true;
                } else {
                    log.debug("PUT prohibited without {} permission", toControl);
                    return false;
                }
            } else if (currentUser.isPermitted(toWrite)) {
                if (!isAuthorizedForMembershipResource(httpRequest, currentUser, resource, container)) {
                    log.debug("PUT denied, not authorized to write to membershipRelation");
                    return false;
                }
                log.debug("PUT allowed by {} permission", toWrite);
                return true;
            } else {
                if (resource != null) {
                    // can't PUT to an existing resource without acl:Write permission
                    log.debug("PUT prohibited to existing resource without {} permission", toWrite);
                    return false;
                } else {
                    // find nearest parent resource and verify that user has acl:Append on it
                    // this works because when the authorizations are inherited, it is the target request URI that is
                    // added as the resource, not the accessTo or other URI in the original authorization
                    log.debug("Resource doesn't exist; checking parent resources for acl:Append permission");
                    if (currentUser.isPermitted(toAppend)) {
                        if (!isAuthorizedForMembershipResource(httpRequest, currentUser, resource, container)) {
                            log.debug("PUT denied, not authorized to write to membershipRelation");
                            return false;
                        }
                        log.debug("PUT allowed for new resource by inherited {} permission", toAppend);
                        return true;
                    } else {
                        log.debug("PUT prohibited for new resource without inherited {} permission", toAppend);
                        return false;
                    }
                }
            }
        case "POST":
            if (isTxEndpoint && currentUser.isAuthenticated()) {
                final String currentUsername = ((Principal) currentUser.getPrincipal()).getName();
                log.debug("POST allowed to transaction endpoint for authenticated user {}", currentUsername);
                return true;
            }
            if (currentUser.isPermitted(toWrite)) {
                if (!isAuthorizedForMembershipResource(httpRequest, currentUser, resource, container)) {
                    log.debug("POST denied, not authorized to write to membershipRelation");
                    return false;
                }
                log.debug("POST allowed by {} permission", toWrite);
                return true;
            }
            if (resource != null) {
                if (isBinaryOrDescription(resource)) {
                    // LDP-NR
                    // user without the acl:Write permission cannot POST to binaries
                    log.debug("POST prohibited to binary resource without {} permission", toWrite);
                    return false;
                } else {
                    // LDP-RS
                    // user with the acl:Append permission may POST to containers
                    if (currentUser.isPermitted(toAppend)) {
                        if (!isAuthorizedForMembershipResource(httpRequest, currentUser, resource, container)) {
                            log.debug("POST denied, not authorized to write to membershipRelation");
                            return false;
                        }
                        log.debug("POST allowed to container by {} permission", toAppend);
                        return true;
                    } else {
                        log.debug("POST prohibited to container without {} permission", toAppend);
                        return false;
                    }
                }
            } else {
                // prohibit POST to non-existent resources without the acl:Write permission
                log.debug("POST prohibited to non-existent resource without {} permission", toWrite);
                return false;
            }
        case "DELETE":
            if (isAcl) {
                if (currentUser.isPermitted(toControl)) {
                    log.debug("DELETE allowed by {} permission", toControl);
                    return true;
                } else {
                    log.debug("DELETE prohibited without {} permission", toControl);
                    return false;
                }
            } else {
                if (!isAuthorizedForMembershipResource(httpRequest, currentUser, resource, container)) {
                    log.debug("DELETE denied, not authorized to write to membershipRelation");
                    return false;
                } else if (currentUser.isPermitted(toWrite)) {
                    if (!isAuthorizedForContainedResources(resource, WEBAC_MODE_WRITE, httpRequest, currentUser,
                            true)) {
                        log.debug("DELETE denied, not authorized to write to a descendant of {}", resource);
                        return false;
                    }
                    return true;
                }
                return false;
            }
        case "PATCH":

            if (isAcl) {
                if (currentUser.isPermitted(toControl)) {
                    log.debug("PATCH allowed by {} permission", toControl);
                    return true;
                } else {
                    log.debug("PATCH prohibited without {} permission", toControl);
                    return false;
                }
            } else if (currentUser.isPermitted(toWrite)) {
                if (!isAuthorizedForMembershipResource(httpRequest, currentUser, resource, container)) {
                    log.debug("PATCH denied, not authorized to write to membershipRelation");
                    return false;
                }
                return true;
            } else {
                if (currentUser.isPermitted(toAppend)) {
                    if (!isAuthorizedForMembershipResource(httpRequest, currentUser, resource, container)) {
                        log.debug("PATCH denied, not authorized to write to membershipRelation");
                        return false;
                    }
                    return isPatchContentPermitted(httpRequest);
                }
            }
            return false;
        default:
            return false;
        }
    }

    private boolean isPatchContentPermitted(final HttpServletRequest httpRequest) throws IOException {
        if (!isSparqlUpdate(httpRequest)) {
            log.debug("Cannot verify authorization on NON-SPARQL Patch request.");
            return false;
        }
        if (httpRequest.getInputStream() != null) {
            boolean noDeletes = false;
            try {
                noDeletes = !hasDeleteClause(IOUtils.toString(httpRequest.getInputStream(), UTF_8));
            } catch (final QueryParseException ex) {
                log.error("Cannot verify authorization! Exception while inspecting SPARQL query!", ex);
            }
            return noDeletes;
        } else {
            log.debug("Authorizing SPARQL request with no content.");
            return true;
        }
    }

    private boolean hasDeleteClause(final String sparqlString) {
        final UpdateRequest sparqlUpdate = UpdateFactory.create(sparqlString);
        return sparqlUpdate.getOperations().stream()
                .filter(update -> update instanceof UpdateDataDelete)
                .map(update -> (UpdateDataDelete) update)
                .anyMatch(update -> update.getQuads().size() > 0) ||
                sparqlUpdate.getOperations().stream().filter(update -> (update instanceof UpdateModify))
                .peek(update -> log.debug("Inspecting update statement for DELETE clause: {}", update.toString()))
                .map(update -> (UpdateModify)update)
                .filter(UpdateModify::hasDeleteClause)
                .anyMatch(update -> update.getDeleteQuads().size() > 0);
    }

    private boolean isSparqlUpdate(final HttpServletRequest request) {
        try {
            return request.getMethod().equals("PATCH") &&
                    sparqlUpdate.isCompatible(MediaType.valueOf(request
                            .getContentType()));
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Does the request's content-type match one of the RDF types.
     *
     * @param request the http servlet request
     * @return whether the content-type matches.
     */
    private boolean isRdfRequest(final HttpServletRequest request) {
        return request.getContentType() != null && rdfContentTypes.contains(request.getContentType());
    }

    /**
     * Is the request to create an indirect or direct container.
     *
     * @param request The current request
     * @return whether we are acting on/creating an indirect/direct container.
     */
    private boolean isPayloadIndirectOrDirect(final HttpServletRequest request) {
        return Collections.list(request.getHeaders("Link")).stream().map(Link::valueOf).map(Link::getUri)
                .anyMatch(l -> directOrIndirect.contains(l));
    }

    /**
     * Is the current resource a direct or indirect container
     *
     * @param resource the resource to check
     * @return whether it is a direct or indirect container.
     */
    private boolean isResourceIndirectOrDirect(final FedoraResource resource) {
        return resource != null && resource.getTypes().stream().anyMatch(l -> directOrIndirect.contains(l));
    }

    /**
     * Check if we are authorized to access the target of membershipRelation if required. Really this is a test for
     * failure. The default is true because we might not be looking at an indirect or direct container.
     *
     * @param request The current request
     * @param currentUser The current principal
     * @param resource The resource
     * @param container The container
     * @return Whether we are creating an indirect/direct container and can write the membershipRelation
     * @throws IOException when getting request's inputstream
     */
    private boolean isAuthorizedForMembershipResource(final HttpServletRequest request, final Subject currentUser,
                                                      final FedoraResource resource, final FedoraResource container)
            throws IOException {
        if (resource != null && request.getMethod().equalsIgnoreCase("POST")) {
            // Check resource if it exists and we are POSTing to it.
            if (isResourceIndirectOrDirect(resource)) {
                final URI membershipResource = getHasMemberFromResource(request);
                addURIToAuthorize(request, membershipResource);
                if (!currentUser.isPermitted(new WebACPermission(WEBAC_MODE_WRITE, membershipResource))) {
                    return false;
                }
            }
        } else if (request.getMethod().equalsIgnoreCase("PUT")) {
            // PUT to a URI check that the immediate container is not direct or indirect.
            if (isResourceIndirectOrDirect(container)) {
                final URI membershipResource = getHasMemberFromResource(request, container);
                addURIToAuthorize(request, membershipResource);
                if (!currentUser.isPermitted(new WebACPermission(WEBAC_MODE_WRITE, membershipResource))) {
                    return false;
                }
            }
        } else if (isSparqlUpdate(request) && isResourceIndirectOrDirect(resource)) {
            // PATCH to a direct/indirect might change the ldp:membershipResource
            final URI membershipResource = getHasMemberFromPatch(request);
            if (membershipResource != null) {
                log.debug("Found membership resource: {}", membershipResource);
                // add the membership URI to the list URIs to retrieve ACLs for
                addURIToAuthorize(request, membershipResource);
                if (!currentUser.isPermitted(new WebACPermission(WEBAC_MODE_WRITE, membershipResource))) {
                    return false;
                }
            }
        } else if (request.getMethod().equalsIgnoreCase("DELETE")) {
            if (isResourceIndirectOrDirect(resource)) {
                // If we delete a direct/indirect container we have to have access to the ldp:membershipResource
                final URI membershipResource = getHasMemberFromResource(request);
                addURIToAuthorize(request, membershipResource);
                if (!currentUser.isPermitted(new WebACPermission(WEBAC_MODE_WRITE, membershipResource))) {
                    return false;
                }
            } else if (isResourceIndirectOrDirect(container)) {
                // or if we delete a child of a direct/indirect container we have to have access to the
                // ldp:membershipResource
                final URI membershipResource = getHasMemberFromResource(request, container);
                addURIToAuthorize(request, membershipResource);
                if (!currentUser.isPermitted(new WebACPermission(WEBAC_MODE_WRITE, membershipResource))) {
                    return false;
                }
            }
        }

        if (isPayloadIndirectOrDirect(request)) {
            // Check if we are creating a direct/indirect container.
            final URI membershipResource = getHasMemberFromRequest(request);
            if (membershipResource != null) {
                log.debug("Found membership resource: {}", membershipResource);
                // add the membership URI to the list URIs to retrieve ACLs for
                addURIToAuthorize(request, membershipResource);
                if (!currentUser.isPermitted(new WebACPermission(WEBAC_MODE_WRITE, membershipResource))) {
                    return false;
                }
            }
        }
        // Not indirect/directs or we are authorized.
        return true;
    }

    /**
     * Get the memberRelation object from the contents.
     *
     * @param request The request.
     * @return The URI of the memberRelation object
     * @throws IOException when getting request's inputstream
     */
    private URI getHasMemberFromRequest(final HttpServletRequest request) throws IOException {
        final String baseUri = request.getRequestURL().toString();
        final RDFReader reader;
        final String contentType = request.getContentType();
        final Lang format = contentTypeToLang(contentType);
        final Model inputModel;
        try {
            inputModel = createDefaultModel();
            reader = inputModel.getReader(format.getName().toUpperCase());
            reader.read(inputModel, request.getInputStream(), baseUri);
            final Statement st = inputModel.getProperty(null, MEMBERSHIP_RESOURCE);
            return (st != null ? URI.create(st.getObject().toString()) : null);
        } catch (final RiotException e) {
            throw new BadRequestException("RDF was not parsable: " + e.getMessage(), e);
        } catch (final RuntimeIOException e) {
            if (e.getCause() instanceof JsonParseException) {
                final var cause = e.getCause();
                throw new MalformedRdfException(cause.getMessage(), cause);
            }
            throw new RepositoryRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Get the membershipRelation from a PATCH request
     *
     * @param request the http request
     * @return URI of the first ldp:membershipRelation object.
     * @throws IOException converting the request body to a string.
     */
    private URI getHasMemberFromPatch(final HttpServletRequest request) throws IOException {
        final String sparqlString = IOUtils.toString(request.getInputStream(), UTF_8);
        final String baseURI = request.getRequestURL().toString().replace(request.getContextPath(), "").replaceAll(
                request.getPathInfo(), "").replaceAll("rest$", "");
        final UpdateRequest sparqlUpdate = UpdateFactory.create(sparqlString);
        // The INSERT|DELETE DATA quads
        final Stream<Quad> insertDeleteData = sparqlUpdate.getOperations().stream()
                .filter(update -> update instanceof UpdateData)
                .map(update -> (UpdateData) update)
                .flatMap(update -> update.getQuads().stream());
        // Get the UpdateModify instance to re-use below.
        final List<UpdateModify> updateModifyStream = sparqlUpdate.getOperations().stream()
                .filter(update -> (update instanceof UpdateModify))
                .peek(update -> log.debug("Inspecting update statement for DELETE clause: {}", update.toString()))
                .map(update -> (UpdateModify) update)
                .collect(toList());
        // The INSERT {} WHERE {} quads
        final Stream<Quad> insertQuadData = updateModifyStream.stream()
                .flatMap(update -> update.getInsertQuads().stream());
        // The DELETE {} WHERE {} quads
        final Stream<Quad> deleteQuadData = updateModifyStream.stream()
                .flatMap(update -> update.getDeleteQuads().stream());
        // The ldp:membershipResource triples.
        return Stream.concat(Stream.concat(insertDeleteData, insertQuadData), deleteQuadData)
                .filter(update -> update.getPredicate().equals(MEMBERSHIP_RESOURCE.asNode()) && update.getObject()
                        .isURI())
                .map(update -> update.getObject().getURI())
                .map(update -> update.replace("file:///", baseURI))
                .findFirst().map(URI::create).orElse(null);
    }

    /**
     * Get ldp:membershipResource from an existing resource
     *
     * @param request the request
     * @return URI of the ldp:membershipResource triple or null if not found.
     */
    private URI getHasMemberFromResource(final HttpServletRequest request) {
        final FedoraResource resource = resource(request);
        return getHasMemberFromResource(request, resource);
    }

    /**
     * Get ldp:membershipResource from an existing resource
     *
     * @param request the request
     * @param resource the FedoraResource
     * @return URI of the ldp:membershipResource triple or null if not found.
     */
    private URI getHasMemberFromResource(final HttpServletRequest request, final FedoraResource resource) {
        return resource.getTriples()
                .filter(triple -> triple.getPredicate().equals(MEMBERSHIP_RESOURCE.asNode()) && triple.getObject()
                        .isURI())
                .map(Triple::getObject).map(Node::getURI)
                .findFirst().map(URI::create).orElse(null);
    }

    /**
     * Determine if the resource is a binary or a binary description.
     * @param resource the fedora resource to check
     * @return true if a binary or binary description.
     */
    private static boolean isBinaryOrDescription(final FedoraResource resource) {
        return resource.getTypes().stream().map(URI::toString)
                .anyMatch(t -> t.equals(NON_RDF_SOURCE.toString()) || t.equals(FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI));
    }

    /**
     * Determine if the request is for embedding container resource descriptions.
     * @param request the request
     * @return true if include the Prefer tag for http://www.w3.org/ns/oa#PreferContainedDescriptions
     */
    private static boolean isEmbeddedRequest(final HttpServletRequest request) {
        final var preferTags = request.getHeaders("Prefer");
        final Set<SinglePrefer> preferTagSet = new HashSet<>();
        while (preferTags.hasMoreElements()) {
            preferTagSet.add(new SinglePrefer(preferTags.nextElement()));
        }
        final MultiPrefer multiPrefer = new MultiPrefer(preferTagSet);
        if (multiPrefer.hasReturn()) {
            final LdpPreferTag ldpPreferences = new LdpPreferTag(multiPrefer.getReturn());
            return ldpPreferences.prefersEmbed();
        }
        return false;
    }

    /**
     * Is the user authorized to access the immediately contained resources of the requested resource.
     * @param request the request
     * @param currentUser the current user
     * @param resource the resource being requested.
     * @return true if authorized or not an embedded resource request on a container.
     */
    private boolean isAuthorizedForEmbeddedRequest(final HttpServletRequest request, final Subject currentUser,
                                                      final FedoraResource resource) {
        if (isEmbeddedRequest(request)) {
            return isAuthorizedForContainedResources(resource, WEBAC_MODE_READ, request, currentUser, false);
        }
        // Is not an embedded resource request
        return true;
    }

    /**
     * Utility to check for a permission on the contained resources of a parent resource.
     * @param resource the parent resource
     * @param permission the permission required
     * @param request the current request
     * @param currentUser the current user
     * @param deepTraversal whether to check children of children.
     * @return true if we are allowed access to all descendants, false otherwise.
     */
    private boolean isAuthorizedForContainedResources(final FedoraResource resource, final URI permission,
                                                      final HttpServletRequest request, final Subject currentUser,
                                                      final boolean deepTraversal) {
        if (!isBinaryOrDescription(resource)) {
            final Transaction transaction = transaction(request);
            final Stream<FedoraResource> children = resourceFactory.getChildren(TransactionUtils.openTxId(transaction),
                    resource.getFedoraId());
            return children.noneMatch(resc -> {
                final URI childURI = URI.create(resc.getFedoraId().getFullId());
                log.debug("Found embedded resource: {}", resc);
                // add the contained URI to the list URIs to retrieve ACLs for
                addURIToAuthorize(request, childURI);
                if (!currentUser.isPermitted(new WebACPermission(permission, childURI))) {
                    log.debug("Failed to access embedded resource: {}", childURI);
                    return true;
                }
                if (deepTraversal) {
                    // We invert this because the recursive noneMatch reports opposite what we want in here.
                    // Here we want the true (no children failed) to become a false (no children matched a failure).
                    return !isAuthorizedForContainedResources(resc, permission, request, currentUser, deepTraversal);
                }
                return false;
            });
        }
        // Is a binary or description.
        return true;
    }

}
