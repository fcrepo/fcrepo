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

import com.google.common.annotations.VisibleForTesting;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.models.Tombstone;
import org.fcrepo.kernel.exception.TombstoneException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.slf4j.Logger;

import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
import javax.ws.rs.core.UriInfo;

import java.net.URI;

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
     * @param externalPath
     * @return
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
     **/
    protected void setUpJMSBaseURIs(final UriInfo uriInfo) {
        try {
            final URI baseURL = uriInfo.getBaseUri();
            LOGGER.debug("setting baseURL = " + baseURL.toString());
            final ObservationManager obs = session().getWorkspace().getObservationManager();
            final String json = "{\"baseURL\":\"" + baseURL.toString() + "\"}";
            obs.setUserData(json);
        } catch ( Exception ex ) {
            LOGGER.warn("Error setting baseURL", ex);
        }
    }

}
