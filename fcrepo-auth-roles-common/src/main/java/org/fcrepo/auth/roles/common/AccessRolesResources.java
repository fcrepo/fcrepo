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
package org.fcrepo.auth.roles.common;

import static java.util.Collections.singletonMap;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.commons.api.rdf.UriAwareResourceModelFactory;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Exposes access roles endpoint for any resource.
 *
 * @author Gregory Jansen
 */
@Component
public class AccessRolesResources implements UriAwareResourceModelFactory {

    /*
     * (non-Javadoc)
     * @see org.fcrepo.http.commons.api.rdf.UriAwareResourceModelFactory#
     * createModelForResource( org.fcrepo.kernel.FedoraResourceImpl,
     * javax.ws.rs.core.UriInfo, org.fcrepo.kernel.rdf.IdentifierTranslator)
     */
    @Override
    public Model createModelForResource(final FedoraResource resource,
            final UriInfo uriInfo, final IdentifierTranslator graphSubjects)
        throws RepositoryException {
        final Model model = ModelFactory.createDefaultModel();
        final Resource s = graphSubjects.getSubject(resource.getNode().getPath());

        if (resource.getNode().isNodeType(
                FedoraJcrTypes.FEDORA_RESOURCE)) {
            if (resource.getPath(graphSubjects) == null) {
                throw new RepositoryException("resource.getPath(graphSubjects) is Null: " + resource.getPath());
            }
            final Map<String, String> pathMap =
                    singletonMap("path", resource.getPath(graphSubjects).substring(1));
            final Resource acl = model.createResource(uriInfo.getBaseUriBuilder().path(
                    AccessRoles.class).buildFromMap(pathMap).toASCIIString());
            model.add(s, RdfLexicon.HAS_ACCESS_ROLES_SERVICE, acl);
        }
        return model;
    }
}
