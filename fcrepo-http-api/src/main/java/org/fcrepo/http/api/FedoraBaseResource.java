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
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.UriAwareIdentifierConverter;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.DatastreamImpl;
import org.fcrepo.kernel.impl.FedoraBinaryImpl;
import org.fcrepo.kernel.impl.FedoraObjectImpl;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
import javax.ws.rs.core.UriInfo;

import java.net.URI;

import static org.fcrepo.jcr.FedoraJcrTypes.FCR_METADATA;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/5/14
 */
abstract public class FedoraBaseResource extends AbstractResource {

    private static final Logger LOGGER = getLogger(FedoraBaseResource.class);

    protected IdentifierConverter<Resource,Node> identifierTranslator;

    protected abstract Session session();

    protected IdentifierConverter<Resource,Node> translator() {
        if (identifierTranslator == null) {
            identifierTranslator = new UriAwareIdentifierConverter(session(),
                    uriInfo.getBaseUriBuilder().clone().path(FedoraLdp.class));
        }

        return identifierTranslator;
    }

    /**
     * Get the FedoraResource for the resource at the external path
     * @param externalPath
     * @return
     */
    @VisibleForTesting
    public FedoraResource getResourceFromPath(final String externalPath) {
        final FedoraResource resource;
        final boolean metadata = externalPath != null
                && externalPath.endsWith(FCR_METADATA);

        final Node node = translator().convert(translator().toDomain(externalPath));

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
        return resource;
    }



    private static boolean baseURLSet = false;
    /**
     * Set the baseURL for JMS events.
     **/
    protected void setUpJMSBaseURIs(final UriInfo uriInfo) {
        if ( !baseURLSet ) {
            // set to true the first time this is run.  if there is an exception the first time, there
            // will likely be an exception every time.  since this is run on each repository update,
            // we should fail fast rather than retrying over and over.
            baseURLSet = true;
            try {
                final URI baseURL = uriInfo.getBaseUri();
                final Class<? extends FedoraBaseResource> klass = this.getClass();
                LOGGER.debug(klass + ".init(): baseURL = " + baseURL.toString());
                final ObservationManager obs = session().getWorkspace().getObservationManager();
                final String json = "{\"baseURL\":\"" + baseURL.toString() + "\"}";
                obs.setUserData(json);
                LOGGER.trace(klass + ".init(): done");
            } catch ( Exception ex ) {
                LOGGER.warn("Error setting baseURL", ex);
            }
        }
    }

}
