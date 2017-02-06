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

import com.google.common.annotations.VisibleForTesting;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.commons.lang3.StringUtils;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.kernel.api.exception.SessionMissingException;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.Tombstone;
import org.slf4j.Logger;

import java.net.URI;
import java.security.Principal;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import static org.fcrepo.kernel.api.observer.OptionalValues.BASE_URL;
import static org.fcrepo.kernel.api.observer.OptionalValues.USER_AGENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/5/14
 */
abstract public class FedoraBaseResource extends AbstractResource {

    private static final Logger LOGGER = getLogger(FedoraBaseResource.class);

    static final String JMS_BASEURL_PROP = "fcrepo.jms.baseUrl";

    @Inject
    protected HttpSession session;

    @Context
    protected SecurityContext securityContext;

    protected IdentifierConverter<Resource, FedoraResource> idTranslator;

    protected IdentifierConverter<Resource, FedoraResource> translator() {
        if (idTranslator == null) {
            idTranslator = new HttpResourceConverter(session(),
                    uriInfo.getBaseUriBuilder().clone().path(FedoraLdp.class));
        }

        return idTranslator;
    }

    /**
     * This is a helper method for using the idTranslator to convert this resource into an associated Jena Node.
     *
     * @param resource to be converted into a Jena Node
     * @return the Jena node
     */
    protected Node asNode(final FedoraResource resource) {
        return translator().reverse().convert(resource).asNode();
    }

    /**
     * Get the FedoraResource for the resource at the external path
     * @param externalPath the external path
     * @return the fedora resource at the external path
     */
    @VisibleForTesting
    public FedoraResource getResourceFromPath(final String externalPath) {
        final Resource resource = translator().toDomain(externalPath);
        final FedoraResource fedoraResource = translator().convert(resource);

        if (fedoraResource instanceof Tombstone) {
            throw new TombstoneException(fedoraResource, resource.getURI() + "/fcr:tombstone");
        }

        return fedoraResource;
    }

    /**
     * Set the baseURL for JMS events.
     * @param uriInfo the uri info
     * @param headers HTTP headers
     **/
    protected void setUpJMSInfo(final UriInfo uriInfo, final HttpHeaders headers) {
        try {
            String baseURL = getBaseUrlProperty(uriInfo);
            if (baseURL.length() == 0) {
                baseURL = uriInfo.getBaseUri().toString();
            }
            LOGGER.debug("setting baseURL = " + baseURL);
            session.getFedoraSession().addSessionData(BASE_URL, baseURL);
            if (!StringUtils.isBlank(headers.getHeaderString("user-agent"))) {
                session.getFedoraSession().addSessionData(USER_AGENT, headers.getHeaderString("user-agent"));
            }
        } catch (final Exception ex) {
            LOGGER.warn("Error setting baseURL", ex.getMessage());
        }
    }

    /**
     * Produce a baseURL for JMS events using the system property fcrepo.jms.baseUrl of the form http[s]://host[:port],
     * if it exists.
     *
     * @param uriInfo used to build the base url
     * @return String the base Url
     */
    private String getBaseUrlProperty(final UriInfo uriInfo) {
        final String propBaseURL = System.getProperty(JMS_BASEURL_PROP, "");
        if (propBaseURL.length() > 0 && propBaseURL.startsWith("http")) {
            final URI propBaseUri = URI.create(propBaseURL);
            if (propBaseUri.getPort() < 0) {
                return uriInfo.getBaseUriBuilder().port(-1).uri(propBaseUri).toString();
            }
            return uriInfo.getBaseUriBuilder().uri(propBaseUri).toString();
        }
        return "";
    }

    protected HttpSession session() {
        if (session == null) {
            throw new SessionMissingException("Invalid session");
        }
        return session;
    }

    protected String getUserPrincipal() {
        final Principal p = securityContext.getUserPrincipal();
        return p == null ? null : p.getName();
    }
}
