package org.fcrepo.api.repository;

import com.google.common.io.Files;
import org.fcrepo.AbstractResource;
import org.modeshape.jcr.api.Problem;
import org.modeshape.jcr.api.Problems;
import org.slf4j.Logger;
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
@Path("/rest/fcr:restore")
public class FedoraRepositoryRestore extends AbstractResource {

    private final Logger LOGGER = getLogger(FedoraRepositoryRestore.class);

    @POST
    public Response runBackup() throws RepositoryException, IOException {

        final Session session = getAuthenticatedSession();

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
