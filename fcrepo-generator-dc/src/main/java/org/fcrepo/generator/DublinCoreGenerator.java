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
package org.fcrepo.generator;

import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.ok;

import java.io.InputStream;

import javax.inject.Inject;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.fcrepo.generator.dublincore.DublinCoreGenerators;
import org.fcrepo.http.api.FedoraBaseResource;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.generator.dublincore.DCGenerator;
import org.springframework.context.annotation.Scope;

/**
 * Dublin Core output controller
 *
 * @author cbeer
 * @author barmintor
 */
@Scope("prototype")
@Path("/{path: .*}/oai:dc")
public class DublinCoreGenerator extends FedoraBaseResource {

    @Inject
    DublinCoreGenerators dcgenerators;

    @Inject
    protected Session session;

    /**
     * Get Dublin Core XML for a node
     * @param externalPath
     * @return response
     * @throws RepositoryException
     */
    @GET
    @Produces(TEXT_XML)
    public Response getObjectAsDublinCore(@PathParam("path") final String externalPath) throws RepositoryException {

        try {
            final FedoraResource obj = getResourceFromPath(externalPath);

            for (final DCGenerator indexer : dcgenerators) {
                final InputStream inputStream =
                        indexer.getStream(obj.getNode());

                if (inputStream != null) {
                    return ok(inputStream).build();
                }
            }
            // no indexers = no path for DC
            throw new PathNotFoundException();
        } finally {
            session.logout();
        }

    }

    @Override
    protected Session session() {
        return session;
    }
}
