package org.fcrepo.api.repository;

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
import org.apache.http.HttpStatus;
import org.apache.jena.riot.WebContent;
import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraResource;
import org.modeshape.common.collection.Problems;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Path("/rest/fcr:properties")
public class FedoraRepositoriesProperties extends AbstractResource {

    private static final Logger logger = getLogger(FedoraRepositoriesProperties.class);


    /**
     * Update an object using SPARQL-UPDATE
     *
     * @return 201
     * @throws javax.jcr.RepositoryException
     * @throws java.io.IOException
     */
    @POST
    @Consumes({WebContent.contentTypeSPARQLUpdate})
    @Timed
    public Response updateSparql(final InputStream requestBodyStream) throws RepositoryException, IOException {

        final Session session = getAuthenticatedSession();

        try {

            if(requestBodyStream != null) {

                final FedoraResource result = nodeService.getObject(session, "/");

                result.updatePropertiesDataset(IOUtils.toString(requestBodyStream));
                Problems problems = result.getDatasetProblems();
                if (problems != null && problems.hasProblems()) {
                    logger.info("Found these problems updating the properties for {}: {}", "/", problems.toString());
                    return Response.status(Response.Status.FORBIDDEN).entity(problems.toString()).build();
                }

                session.save();

                return Response.status(HttpStatus.SC_NO_CONTENT).build();
            } else {
                return Response.status(HttpStatus.SC_BAD_REQUEST).entity("SPARQL-UPDATE requests must have content ").build();
            }
        } finally {
            session.logout();
        }
    }
}
