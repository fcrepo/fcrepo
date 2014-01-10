/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.transform.http;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.jcr.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.RdfLexicon.HAS_SPARQL_ENDPOINT;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.http.commons.api.rdf.UriAwareResourceModelFactory;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.springframework.stereotype.Component;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

/**
 * Expose SPARQL endpoint relation on the root node
 *
 * @author cbeer
 */
@Component
public class TransformResources implements UriAwareResourceModelFactory {
    @Override
    public Model createModelForResource(final FedoraResource resource,
         final UriInfo uriInfo, final GraphSubjects graphSubjects) throws RepositoryException {
        final Model model = createDefaultModel();
        final Resource s = graphSubjects.getGraphSubject(resource.getNode());

        if (resource.getNode().getPrimaryNodeType().isNodeType(ROOT)) {
            model.add(s, HAS_SPARQL_ENDPOINT, model.createResource(uriInfo
                    .getBaseUriBuilder().path(FedoraSparql.class).build()
                    .toASCIIString()));
        }

        return model;
    }
}
