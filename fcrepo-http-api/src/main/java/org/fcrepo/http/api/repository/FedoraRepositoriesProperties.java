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
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLUpdate;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.fcrepo.http.api.FedoraLdp;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.UriAwareIdentifierConverter;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import com.codahale.metrics.annotation.Timed;

/**
 * Utility endpoint for running SPARQL Update queries on any object in the
 * repository
 *
 * @author awoods
 */
@Scope("prototype")
@Path("/fcr:properties")
public class FedoraRepositoriesProperties extends AbstractResource {

    @Inject
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

        if (requestBodyStream != null) {

            final FedoraResource result =
                    nodeService.getObject(session, "/");

            final UriAwareIdentifierConverter translator
                    = new UriAwareIdentifierConverter(session, UriBuilder.fromResource(FedoraLdp.class));

            result.updatePropertiesDataset(translator, IOUtils
                            .toString(requestBodyStream));

            try {
                session.save();
            } catch (final RepositoryException e) {
                throw new RepositoryRuntimeException(e);
            }

            return status(SC_NO_CONTENT).build();
        }
        return status(SC_BAD_REQUEST).entity(
                "SPARQL-UPDATE requests must have content ").build();
    }
}
