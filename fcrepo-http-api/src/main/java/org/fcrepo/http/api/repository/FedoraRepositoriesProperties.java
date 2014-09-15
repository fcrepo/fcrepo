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
package org.fcrepo.http.api.repository;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.fcrepo.kernel.rdf.GraphProperties.PROBLEMS_MODEL_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.fcrepo.http.api.FedoraNodes;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * Utility endpoint for running SPARQL Update queries on any object in the
 * repository
 *
 * @author awoods
 */
@Component
@Scope("prototype")
@Path("/fcr:properties")
public class FedoraRepositoriesProperties extends AbstractResource {

    @InjectedSession
    protected Session session;

    private static final Logger LOGGER =
        getLogger(FedoraRepositoriesProperties.class);

    /**
     * Update an object using SPARQL-UPDATE
     *
     * @return 201
     * @throws javax.jcr.RepositoryException
     * @throws java.io.IOException
     */
    @POST
    @Consumes({contentTypeSPARQLUpdate})
    @Timed
    public Response updateSparql(final InputStream requestBodyStream)
        throws IOException {

        try {
            if (requestBodyStream != null) {

                final FedoraResource result =
                    nodeService.getObject(session, "/");

                final Dataset dataset =
                    result.updatePropertiesDataset(new HttpIdentifierTranslator(
                            session, FedoraNodes.class, uriInfo), IOUtils
                            .toString(requestBodyStream));
                if (dataset.containsNamedModel(PROBLEMS_MODEL_NAME)) {
                    final Model problems = dataset.getNamedModel(PROBLEMS_MODEL_NAME);

                    LOGGER.info("Found these problems updating the properties for {}: {}",
                                   "/", problems.toString());
                    return status(FORBIDDEN).entity(problems.toString())
                            .build();
                }

                try {
                    session.save();
                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }

                return status(SC_NO_CONTENT).build();
            }
            return status(SC_BAD_REQUEST).entity(
                    "SPARQL-UPDATE requests must have content ").build();
        } finally {
            session.logout();
        }
    }
}
