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

import static javax.ws.rs.core.Response.noContent;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.session.InjectedSession;
import org.modeshape.jcr.api.Problem;
import org.modeshape.jcr.api.Problems;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Path("/fcr:restore")
public class FedoraRepositoryRestore extends AbstractResource {

    private final Logger LOGGER = getLogger(FedoraRepositoryRestore.class);

    @InjectedSession
    protected Session session;

    /**
     * This method runs a repository restore.
     *
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    @POST
    public Response runRestore(InputStream bodyStream) throws RepositoryException, IOException {

        if (null == bodyStream) {
            throw new WebApplicationException(
                    Response.serverError()
                            .entity("Request body must not be null")
                            .build());
        }

        String body = IOUtils.toString(bodyStream);
        File backupDirectory = new File(body.trim());
        if (!backupDirectory.exists()) {
            throw new WebApplicationException(
                    Response.serverError().entity(
                            "Backup directory does not exist: " +
                                    backupDirectory.getAbsolutePath()).build());
        }

        try {
            Problems problems = nodeService.restoreRepository(session, backupDirectory);
            if ( problems.hasProblems() ) {
                LOGGER.error("Problems restoring up the repository:");

                List<String> problemsOutput = new ArrayList<String>();

                // Report the problems (we'll just print them out) ...
                for ( Problem problem : problems ) {
                    LOGGER.error("{}", problem.getMessage());
                    problemsOutput.add(problem.getMessage());
                }

                throw new WebApplicationException(Response.serverError().entity(problemsOutput).build());

            } else {
                return noContent().build();
            }
        } finally {
            session.logout();
        }
    }
}
