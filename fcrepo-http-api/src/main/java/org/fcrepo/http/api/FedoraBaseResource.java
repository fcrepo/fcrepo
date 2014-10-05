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

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.UriAwareIdentifierConverter;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.DatastreamImpl;
import org.fcrepo.kernel.impl.FedoraBinaryImpl;
import org.fcrepo.kernel.impl.FedoraObjectImpl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.PathSegment;

import java.util.List;

import static org.fcrepo.jcr.FedoraJcrTypes.FCR_METADATA;

/**
 * @author cabeer
 * @since 10/5/14
 */
abstract public class FedoraBaseResource extends AbstractResource {

    protected IdentifierConverter<Resource,Node> identifierTranslator;

    protected abstract Session session();

    protected IdentifierConverter<Resource,Node> translator() {
        if (identifierTranslator == null) {
            identifierTranslator = new UriAwareIdentifierConverter(session(),
                    uriInfo.getBaseUriBuilder().clone().path(FedoraLdp.class));
        }

        return identifierTranslator;
    }

    protected FedoraResource getResourceFromPath(final List<PathSegment> pathList, final String path) {
        final FedoraResource resource;
        try {
            final boolean metadata = pathList != null
                    && pathList.get(pathList.size() - 1).getPath().equals(FCR_METADATA);

            final Node node = session().getNode(path);

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
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

}
