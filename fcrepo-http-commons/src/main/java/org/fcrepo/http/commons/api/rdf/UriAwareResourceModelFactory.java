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
package org.fcrepo.http.commons.api.rdf;

import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Helper to generate an RDF model for a FedoraResourceImpl that (likely) creates
 * relations from our resource to other HTTP components
 *
 * @author awoods
 */
public interface UriAwareResourceModelFactory {

    /**
     * Given a resource, the UriInfo and a way to generate graph subjects,
     * create a model with triples to inject into an RDF response for the
     * resource (e.g. to add HATEOAS links)
     *
     * @param resource the resource
     * @param uriInfo the uri info
     * @param idTranslator the id translator
     * @return model containing triples for the given resource
     */
    Model createModelForResource(final FedoraResource resource,
            final UriInfo uriInfo, final IdentifierConverter<Resource,FedoraResource> idTranslator);
}
