/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static com.google.common.io.Files.createTempDir;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.Response.serverError;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.inject.Inject;
import javax.jcr.Session;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.io.IOUtils;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.kernel.api.functions.InjectiveConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.RepositoryService;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;

import com.hp.hpl.jena.rdf.model.Resource;
/**
 * Repository-wide backup endpoint
 *
 * @author cbeer
 */
@Scope("prototype")
@Path("/fcr:backup")
public class FedoraRepositoryBackup extends AbstractResource {

    private static final Logger LOGGER = getLogger(FedoraRepositoryBackup.class);

    @Inject
    protected Session session;

    /**
     * The fcrepo repository service
     */
    @Inject
    protected RepositoryService repositoryService;

    /**
     * This method runs a repository backup.
     *
     * @param bodyStream the input body stream
     * @return path to the backup
     * @throws IOException if IO exception occurred
     */
    @POST
    public String runBackup(final InputStream bodyStream) throws IOException {

        File backupDirectory;
        if (null != bodyStream) {
            final String body = IOUtils.toString(bodyStream).trim();

            backupDirectory = new File(body.trim());
            if (body.isEmpty()) {
                // Backup to a temp directory
                backupDirectory = createTempDir();

            } else if (!backupDirectory.exists() || !backupDirectory.canWrite()) {
                throw new WebApplicationException(
                        serverError().entity(
                                "Backup directory does not exist or is not writable: " +
                                        backupDirectory.getAbsolutePath())
                                .build());
            }

        } else {
            // Backup to a temp directory
            backupDirectory = createTempDir();
        }

        LOGGER.debug("Backing up to: {}", backupDirectory.getAbsolutePath());
        final Collection<Throwable> problems = repositoryService.backupRepository(session, backupDirectory);

        if (!problems.isEmpty()) {
            LOGGER.error("Problems backing up the repository:");

            // Report the problems (we'll just print them out) ...
            final String output = problems.stream().map(Throwable::getMessage).peek(LOGGER::error)
                    .collect(joining("\n"));

            throw new WebApplicationException(serverError().entity(output).build());

        }
        return backupDirectory.getCanonicalPath();
    }

    @Override
    protected InjectiveConverter<Resource, String> translator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected InjectiveConverter<Resource, FedoraResource> uriToResource() {
        // TODO Auto-generated method stub
        return null;
    }
}
