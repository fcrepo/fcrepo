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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;

import org.apache.commons.lang3.StringUtils;

import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.kernel.api.exception.SessionMissingException;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.api.functions.InjectiveConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.Tombstone;
import org.fcrepo.kernel.modeshape.identifiers.InternalPathToNodeConverter;

import org.slf4j.Logger;

import javax.inject.Inject;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
import javax.ws.rs.core.HttpHeaders;
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

    @Inject
    protected Session session;

    protected InjectiveConverter<Resource, FedoraResource> uriToResource;


    @Override
    protected InjectiveConverter<Resource, String> translator() {
        if (idTranslator == null) {
            idTranslator =
                    new HttpResourceConverter(session(), uriInfo.getBaseUriBuilder().clone().path(FedoraLdp.class));
        }

        return idTranslator;
    }

    @Override
    protected InjectiveConverter<Resource, FedoraResource> uriToResource() {
        if (uriToResource == null) {
            uriToResource = translator()
                    .andThen(new InternalPathToNodeConverter(session()))
                    .andThen(org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter);
        }

        return uriToResource;
    }

    /**
     * This is a helper method for using the idTranslator to convert this resource into an associated Jena Node.
     *
     * @param resource to be converted into a Jena Node
     * @return the Jena node
     */
    protected Node asNode(final FedoraResource resource) {
        return resource.asUri(translator()).asNode();
    }

    /**
     * Get the FedoraResource for the resource at the external path
     * @param externalPath the external path
     * @return the fedora resource at the external path
     */
    @VisibleForTesting
    public FedoraResource getResourceFromPath(final String externalPath) {
        final Resource resource = translator().toDomain(externalPath);
        final FedoraResource fedoraResource = uriToResource().apply(resource);
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
            String baseURL = getBaseUrlProperty();
            if (baseURL.length() == 0) {
                baseURL = uriInfo.getBaseUri().toString();
            }
            LOGGER.debug("setting baseURL = " + baseURL);
            final ObservationManager obs = session().getWorkspace().getObservationManager();
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode json = mapper.createObjectNode();
            json.put(BASE_URL, baseURL);
            if (!StringUtils.isBlank(headers.getHeaderString("user-agent"))) {
                json.put(USER_AGENT, headers.getHeaderString("user-agent"));
            }
            obs.setUserData(mapper.writeValueAsString(json));
        } catch (final Exception ex) {
            LOGGER.warn("Error setting baseURL", ex.getMessage());
        }
    }

    /**
     * Produce a baseURL for JMS events using the system property fcrepo.jms.baseUrl of the form http[s]://host[:port],
     * if it exists.
     *
     * @return String the base Url
     */
    protected String getBaseUrlProperty() {
        final String propBaseURL = System.getProperty("fcrepo.jms.baseUrl", "");
        if (propBaseURL.length() > 0 && propBaseURL.startsWith("http")) {
            return uriInfo.getBaseUriBuilder().uri(propBaseURL).toString();
        }
        return "";
    }

    protected Session session() {
        if (session == null) {
            throw new SessionMissingException("Invalid session");
        }
        return session;
    }
}
