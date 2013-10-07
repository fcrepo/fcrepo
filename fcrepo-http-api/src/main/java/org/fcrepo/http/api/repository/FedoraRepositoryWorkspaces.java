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

package org.fcrepo.http.api.repository;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.kernel.RdfLexicon.NOT_IMPLEMENTED;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.MalformedURLException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.http.api.FedoraNodes;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.utils.JcrRdfTools;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * This class exposes the JCR workspace functionality. It may be
 * too JCR-y in the long run, but this lets us exercise the functionality.
 */
@Component
@Scope("prototype")
@Path("/fcr:workspaces")
public class FedoraRepositoryWorkspaces extends AbstractResource {

    private static final Logger logger = getLogger(FedoraRepositoryWorkspaces.class);

    @InjectedSession
    protected Session session;

    /**
     * Get the list of accessible workspaces in this repository.
     *
     * @return
     * @throws RepositoryException
     */
    @GET
    @Produces({TURTLE, N3, N3_ALT1, N3_ALT2, RDF_XML, RDF_JSON, NTRIPLES,
               TEXT_HTML})
    @HtmlTemplate("jcr:workspaces")
    public Dataset getWorkspaces() throws RepositoryException {

        final Model workspaceModel =
                JcrRdfTools.withContext(null, session).getJcrPropertiesModel();

        final String[] workspaces =
                session.getWorkspace().getAccessibleWorkspaceNames();

        for (final String workspace : workspaces) {
            final Resource resource =
                    createResource(uriInfo.getBaseUriBuilder()
                                           .path("/workspace:" + workspace)
                                           .build()
                                           .toString());
            logger.debug("Discovered workspace: {}", resource);
            workspaceModel.add(resource, RDF.type, NOT_IMPLEMENTED);
        }

        try {
            return DatasetFactory.create(workspaceModel);
        } finally {
            session.logout();
        }
    }

    /**
     * Create a new workspace in the repository
     *
     * @param path
     * @param uriInfo
     * @return
     * @throws RepositoryException
     * @throws MalformedURLException
     */
    @POST
    @Path("{path}")
    public Response createWorkspace(@PathParam("path")
            final String path,
            @Context
            final UriInfo uriInfo) throws RepositoryException,
        MalformedURLException {
        final Workspace workspace = session.getWorkspace();
        workspace.createWorkspace(path);

        return Response.created(
                uriInfo.getAbsolutePathBuilder().path(FedoraNodes.class)
                        .buildFromMap(ImmutableMap.of("path", path))).build();

    }
}
