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

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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

import com.google.common.io.Files;

@Component
@Scope("prototype")
@Path("/fcr:backup")
public class FedoraRepositoryBackup extends AbstractResource {

    private final Logger LOGGER = getLogger(FedoraRepositoryBackup.class);

    @InjectedSession
    protected Session session;

    /**
     * This method runs a repository backup.
     *
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    @POST
    public String runBackup(InputStream bodyStream) throws RepositoryException, IOException {

        File backupDirectory = null;
        if (null != bodyStream) {
            String body = IOUtils.toString(bodyStream).trim();

            backupDirectory = new File(body.trim());
            if (body.isEmpty()) {
                // Backup to a temp directory
                backupDirectory = Files.createTempDir();

            } else if (!backupDirectory.exists() || !backupDirectory.canWrite()) {
                throw new WebApplicationException(
                        Response.serverError().entity(
                                "Backup directory does not exist or is not writable: " +
                                        backupDirectory.getAbsolutePath())
                                .build());
            }

        } else {
            // Backup to a temp directory
            backupDirectory = Files.createTempDir();
        }

        LOGGER.debug("Backing up to: {}", backupDirectory.getAbsolutePath());
        try {
            Problems problems = nodeService.backupRepository(session, backupDirectory);

            if ( problems.hasProblems() ) {
                LOGGER.error("Problems backing up the repository:");

                StringBuilder problemsOutput = new StringBuilder();

                // Report the problems (we'll just print them out) ...
                for ( Problem problem : problems ) {
                    LOGGER.error("{}", problem.getMessage());
                    problemsOutput.append(problem.getMessage());
                    problemsOutput.append("\n");
                }

                throw new WebApplicationException(Response.serverError().entity(problemsOutput.toString()).build());

            } else {
                return backupDirectory.getCanonicalPath();
            }
        } finally {
            session.logout();
        }
    }
}
