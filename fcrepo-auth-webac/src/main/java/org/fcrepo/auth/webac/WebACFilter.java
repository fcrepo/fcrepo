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

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_ADMIN_ROLE;
import static org.fcrepo.auth.common.ServletContainerAuthFilter.FEDORA_USER_ROLE;
import static org.fcrepo.auth.webac.URIConstants.FOAF_AGENT_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_APPEND;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_CONTROL;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_READ;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_WRITE;
import static org.fcrepo.auth.webac.WebACAuthorizingRealm.URIS_TO_AUTHORIZE;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_BINARY;
import static org.fcrepo.kernel.api.RdfLexicon.INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.modify.request.UpdateDataDelete;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.fcrepo.http.api.FedoraLdp;
import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.NodeService;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;

/**
 * @author peichman
 */
public class WebACFilter implements Filter {

    private static final Logger log = getLogger(WebACFilter.class);

    private static final MediaType sparqlUpdate = MediaType.valueOf(contentTypeSPARQLUpdate);

    private FedoraSession session;

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
    private NodeService nodeService;

    @Inject
    private SessionFactory sessionFactory;

    private static Set<URI> directOrIndirect = new HashSet<>();

    static {
        directOrIndirect.add(URI.create(INDIRECT_CONTAINER.toString()));
        directOrIndirect.add(URI.create(DIRECT_CONTAINER.toString()));
    }

    @Override
    public void init(final FilterConfig filterConfig) {
        // this method intentionally left empty
    }

    public void addURIToAuthorize(final HttpServletRequest httpRequest, final URI uri) {
        Set<URI> targetURIs = (Set<URI>) httpRequest.getAttribute(URIS_TO_AUTHORIZE);
        if (targetURIs == null) {
            targetURIs = new HashSet<URI>();
            httpRequest.setAttribute(URIS_TO_AUTHORIZE, targetURIs);
        }
        targetURIs.add(uri);
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final Subject currentUser = SecurityUtils.getSubject();
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (isSparqlUpdate(httpRequest)) {
            httpRequest = new CachedSparqlRequest(httpRequest);
        }

        // add the request URI to the list of URIs to retrieve the ACLs for
        addURIToAuthorize(httpRequest, URI.create(httpRequest.getRequestURL().toString()));

        if (currentUser.isAuthenticated()) {
            log.debug("User is authenticated");
            if (currentUser.hasRole(FEDORA_ADMIN_ROLE)) {
                log.debug("User has fedoraAdmin role");
            } else if (currentUser.hasRole(FEDORA_USER_ROLE)) {
                log.debug("User has fedoraUser role");
                // non-admins are subject to permission checks
                if (!isAuthorized(currentUser, httpRequest)) {
                    // if the user is not authorized, set response to forbidden
                    ((HttpServletResponse) response).sendError(SC_FORBIDDEN);
                    return;
                }
            } else {
                log.debug("User has no recognized servlet container role");
                // missing a container role, return forbidden
                ((HttpServletResponse) response).sendError(SC_FORBIDDEN);
                return;
            }
        } else {
            log.debug("User is NOT authenticated");
            // anonymous users are subject to permission checks
            if (!isAuthorized(getFoafAgentSubject(), httpRequest)) {
                // if anonymous user is not authorized, set response to forbidden
                ((HttpServletResponse) response).sendError(SC_FORBIDDEN);
                return;
            }
        }

        // proceed to the next filter
        chain.doFilter(httpRequest, response);
    }

    private Subject getFoafAgentSubject() {
        if (FOAF_AGENT_SUBJECT == null) {
            FOAF_AGENT_SUBJECT = new Subject.Builder().principals(FOAF_AGENT_PRINCIPAL_COLLECTION).buildSubject();
        }
        return FOAF_AGENT_SUBJECT;
    }

    @Override
    public void destroy() {
        // this method intentionally left empty
    }

    private FedoraSession session() {
        if (session == null) {
            session = sessionFactory.getInternalSession();
        }
        return session;
    }

    private String getBaseURL(final HttpServletRequest servletRequest) {
        final String url = servletRequest.getRequestURL().toString();
        // the base URL will be the request URL if there is no path info
        String baseURL = url;

        // strip out the path info, if it exists
        final String pathInfo = servletRequest.getPathInfo();
        if (pathInfo != null) {
            final int loc = url.lastIndexOf(pathInfo);
            baseURL = url.substring(0, loc);
        }

        log.debug("Base URL determined from servlet request is {}", baseURL);
        return baseURL;
    }

    private FedoraResource resource(final HttpServletRequest servletRequest) {
        return nodeService.find(session(), getRepoPath(servletRequest));
    }

    private boolean resourceExists(final HttpServletRequest servletRequest) {
        return nodeService.exists(session(), getRepoPath(servletRequest));
    }

    private String getRepoPath(final HttpServletRequest servletRequest) {
        final String httpURI = servletRequest.getRequestURL().toString();
        final HttpSession httpSession = new HttpSession(session());

        final UriBuilder uriBuilder = UriBuilder.fromUri(getBaseURL(servletRequest)).path(FedoraLdp.class);
        final HttpResourceConverter conv = new HttpResourceConverter(httpSession, uriBuilder);
        final Resource resource = ModelFactory.createDefaultModel().createResource(httpURI);

        final String repoPath = conv.asString(resource);
        log.debug("Converted request URI {} to repo path {}", httpURI, repoPath);
        return repoPath;
    }

    private boolean isAuthorized(final Subject currentUser, final HttpServletRequest httpRequest) throws IOException {
        final String requestURL = httpRequest.getRequestURL().toString();
        final boolean isAcl = requestURL.endsWith(FCR_ACL);
        final URI requestURI = URI.create(requestURL);
        log.debug("Request URI is {}", requestURI);

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
                return currentUser.isPermitted(toRead);
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
                log.debug("PUT allowed by {} permission", toWrite);
                if (!isAuthorizedForMembershipResource(httpRequest, currentUser)) {
                    log.debug("Not authorized to write to membershipRelation");
                    return false;
                }
                return true;
            } else {
                if (resourceExists(httpRequest)) {
                    // can't PUT to an existing resource without acl:Write permission
                    log.debug("PUT prohibited to existing resource without {} permission", toWrite);
                    return false;
                } else {
                    // find nearest parent resource and verify that user has acl:Append on it
                    // this works because when the authorizations are inherited, it is the target request URI that is
                    // added as the resource, not the accessTo or other URI in the original authorization
                    log.debug("Resource doesn't exist; checking parent resources for acl:Append permission");
                    if (currentUser.isPermitted(toAppend)) {
                        log.debug("PUT allowed for new resource by inherited {} permission", toAppend);
                        if (!isAuthorizedForMembershipResource(httpRequest, currentUser)) {
                            log.debug("Not authorized to write to membershipRelation");
                            return false;
                        }
                        return true;
                    } else {
                        log.debug("PUT prohibited for new resource without inherited {} permission", toAppend);
                        return false;
                    }
                }
            }
        case "POST":
            if (currentUser.isPermitted(toWrite)) {
                if (!isAuthorizedForMembershipResource(httpRequest, currentUser)) {
                    log.debug("Not authorized to write to membershipRelation");
                    return false;
                }
                log.debug("POST allowed by {} permission", toWrite);
                return true;
            }
            if (resourceExists(httpRequest)) {
                if (resource(httpRequest).hasType(FEDORA_BINARY)) {
                    // LDP-NR
                    // user without the acl:Write permission cannot POST to binaries
                    log.debug("POST prohibited to binary resource without {} permission", toWrite);
                    return false;
                } else {
                    // LDP-RS
                    // user with the acl:Append permission may POST to containers
                    if (currentUser.isPermitted(toAppend)) {
                        if (!isAuthorizedForMembershipResource(httpRequest, currentUser)) {
                            log.debug("Not authorized to write to membershipRelation");
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
                return currentUser.isPermitted(toWrite);
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
                if (!isAuthorizedForMembershipResource(httpRequest, currentUser)) {
                    log.debug("Not authorized to write to membershipRelation");
                    return false;
                }
                return true;
            } else {
                if (currentUser.isPermitted(toAppend)) {
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
     * Is the request on or to create an indirect or direct container.
     * 
     * @param request The current request
     * @return whether we are acting on/creating an indirect/direct container.
     */
    private boolean isIndirectOrDirect(final HttpServletRequest request) {
        if (resourceExists(request) && !request.getMethod().equalsIgnoreCase("POST")) {
            final FedoraResource resource = resource(request);
            return resource.getTypes().stream().anyMatch(l -> directOrIndirect.contains(l));
        } else {
            return Collections.list(request.getHeaders("Link")).stream().map(Link::valueOf).map(Link::getUri)
                    .anyMatch(l -> directOrIndirect
                .contains(l));
        }
    }

    /**
     * Check if we are authorized to access the target of membershipRelation if required. Really this is a test for
     * failure. The default is true because we might not be looking at an indirect or direct container.
     *
     * @param request The current request
     * @param currentUser The current principal
     * @return Whether we are creating an indirect/direct container and can write the membershipRelation
     * @throws IOException when getting request's inputstream
     */
    private boolean isAuthorizedForMembershipResource(final HttpServletRequest request, final Subject currentUser)
            throws IOException {
        if (isIndirectOrDirect(request)) {
            final URI membershipResource = getHasMember(request.getRequestURL().toString(),
                    request.getInputStream(),
                    request.getContentType());
            if (membershipResource != null) {
                log.debug("Found membership resource: {}", membershipResource);
                // add the membership URI to the list URIs to retrieve ACLs for
                addURIToAuthorize(request, membershipResource);
                final WebACPermission toWrite = new WebACPermission(WEBAC_MODE_WRITE, membershipResource);
                return currentUser.isPermitted(toWrite);
            }
        }
        return true;
    }

    /**
     * Get the memberRelation object from the contents.
     *
     * @param baseUri The current request URL
     * @param body The request body
     * @param contentType The content type.
     * @return The URI of the memberRelation object
     */
    private URI getHasMember(final String baseUri, final InputStream body, final String contentType) {
        final Lang format = contentTypeToLang(contentType.toString());
        final Model inputModel;
        try {
            inputModel = createDefaultModel();
            inputModel.read(body, baseUri, format.getName().toUpperCase());
            final Statement st = inputModel.getProperty(null, MEMBERSHIP_RESOURCE);
            return (st != null ? URI.create(st.getObject().toString()) : null);
        } catch (final RiotException e) {
            throw new BadRequestException("RDF was not parsable: " + e.getMessage(), e);
        } catch (final RuntimeIOException e) {
            if (e.getCause() instanceof JsonParseException) {
                throw new MalformedRdfException(e.getCause());
            }
            throw new RepositoryRuntimeException(e);
        }
    }

}
