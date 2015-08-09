/**
 * Copyright 2015 DuraSpace, Inc.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.commons.lang3.StringUtils;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.Tombstone;
import org.slf4j.Logger;

import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/5/14
 */
abstract public class FedoraBaseResource extends AbstractResource {

    private static final Logger LOGGER = getLogger(FedoraBaseResource.class);

    protected IdentifierConverter<Resource, FedoraResource> idTranslator;

    protected abstract Session session();

    protected IdentifierConverter<Resource, FedoraResource> translator() {
        if (idTranslator == null) {
            idTranslator = new HttpResourceConverter(session(),
                    uriInfo.getBaseUriBuilder().clone().path(FedoraLdp.class));
        }

        return idTranslator;
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
            String baseURL = getBaseUrlProperty();
            if (baseURL.length() == 0) {
                baseURL = uriInfo.getBaseUri().toString();
            }
            LOGGER.debug("setting baseURL = " + baseURL);
            final ObservationManager obs = session().getWorkspace().getObservationManager();
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode json = mapper.createObjectNode();
            json.put("baseURL", baseURL);
            if (!StringUtils.isBlank(headers.getHeaderString("user-agent"))) {
                json.put("userAgent", headers.getHeaderString("user-agent"));
            }
            obs.setUserData(mapper.writeValueAsString(json));
        } catch ( final Exception ex ) {
            LOGGER.warn("Error setting baseURL", ex);
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
}
