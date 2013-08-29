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

import com.google.common.io.Files;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.session.InjectedSession;
import org.modeshape.jcr.api.Problem;
import org.modeshape.jcr.api.Problems;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.Response.noContent;
import static org.slf4j.LoggerFactory.getLogger;

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
    public Response runBackup() throws RepositoryException, IOException {
        try {
            File backupDirectory = Files.createTempDir();
            Problems problems = nodeService.restoreRepository(session, backupDirectory);

            if ( problems.hasProblems() ) {
                LOGGER.error("Problems restoring up the repository:");

                List<String> problemsOutput = new ArrayList<String>();

                // Report the problems (we'll just print them out) ...
                for ( Problem problem : problems ) {
                    LOGGER.error("{}", problem);
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
