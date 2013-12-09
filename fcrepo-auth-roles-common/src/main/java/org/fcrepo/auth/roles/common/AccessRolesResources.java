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
package org.fcrepo.auth.roles.common;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.api.rdf.UriAwareResourceModelFactory;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.rdf.GraphSubjects;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;


/**
 * @author Gregory Jansen
 *
 */
public class AccessRolesResources implements UriAwareResourceModelFactory {

    /* (non-Javadoc)
     * @see org.fcrepo.http.commons.api.rdf.UriAwareResourceModelFactory#createModelForResource(
     * org.fcrepo.kernel.FedoraResource, javax.ws.rs.core.UriInfo, org.fcrepo.kernel.rdf.GraphSubjects)
     */
    @Override
    public Model createModelForResource(final FedoraResource resource,
            final UriInfo uriInfo, final GraphSubjects graphSubjects)
        throws RepositoryException {
        final Model model = ModelFactory.createDefaultModel();
        final Resource s = graphSubjects.getGraphSubject(resource.getNode());

        if (resource.getNode().getPrimaryNodeType().isNodeType(
                FedoraJcrTypes.ROOT)) {
            model.add(s, RdfLexicon.HAS_ACCESS_ROLES_SERVICE, model
                    .createResource(uriInfo.getBaseUriBuilder().path(
                            AccessRoles.class).build().toASCIIString()));
        }

        return model;
    }

}
