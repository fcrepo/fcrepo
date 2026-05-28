/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.ok;
import static jakarta.ws.rs.core.Response.status;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import io.micrometer.core.annotation.Timed;
import org.fcrepo.stats.api.AggregatedRepositoryStatsResults;
import org.fcrepo.stats.api.RepositoryStats;
import org.fcrepo.stats.api.RepositoryStatsParameters;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;

/**
 * An HTTP endpoint for retrieving statistics related to the repository.
 *
 * @author dbernstein
 * @since 2022-01-27
 */
@Timed
@Scope("request")
@Path("/fcr:stats")
public class FedoraRepositoryStats extends FedoraBaseResource {

    private static final Logger LOGGER = getLogger(FedoraRepositoryStats.class);

    @Autowired
    @Qualifier("stats")
    private RepositoryStats repositoryStats;

    /**
     * Default JAX-RS entry point
     */
    public FedoraRepositoryStats() {
        super();
    }


    /**
     * Query summary info
     *
     * @return
     */
    @GET
    @Produces({APPLICATION_JSON + ";qs=1.0",
            APPLICATION_JSON})
    public Response getStats() {
        final var builder = ok();
        final var statsParams = new RepositoryStatsParameters();
        final var totalResourceCount = repositoryStats.getResourceCount(statsParams);
        final var binaryResources = repositoryStats.getByMimeTypes(statsParams);
        final var rdfTypes = repositoryStats.getByRdfType(statsParams);
        final var aggregatedStats = new AggregatedRepositoryStatsResults();
        aggregatedStats.setResourceCount(totalResourceCount.getResourceCount());
        aggregatedStats.setBinaries(binaryResources);
        aggregatedStats.setAllResources(rdfTypes);
        return builder.entity(aggregatedStats).build();

    }

    /**
     * Method for querying rdf type stats
     *
     * @param rdfTypes A list of rdf types by which to limit the results. By default show all rdf types.
     * @return
     */
    @GET
    @Path("/rdf-types")
    @Produces({APPLICATION_JSON + ";qs=1.0",
            APPLICATION_JSON})
    public Response getStatsByRdfType(@QueryParam(value = "rdf_type") final List<String> rdfTypes) {
        if (listHasSingleBlankEntry(rdfTypes)) {
            return status(BAD_REQUEST).build();
        }
        final var builder = ok();
        final var statsParams = new RepositoryStatsParameters();
        statsParams.setRdfTypes(rdfTypes);
        final var results = repositoryStats.getByRdfType(statsParams);
        return builder.entity(results).build();
    }

    private boolean listHasSingleBlankEntry(final List<String> list) {
        return !isEmpty(list) && list.size() == 1 && list.get(0).trim().equals("");
    }

    /**
     * Method for querying binary stats
     *
     * @param mimeTypes A list of mime types by which to limit the results. By default show all binary mime types.
     * @return
     */
    @GET
    @Path("/binaries")
    @Produces({APPLICATION_JSON + ";qs=1.0",
            APPLICATION_JSON})
    public Response getBinaryStats(@QueryParam(value = "mime_type") final List<String> mimeTypes ) {
        if (listHasSingleBlankEntry(mimeTypes)) {
            return status(BAD_REQUEST).build();
        }

        final var builder = ok();
        final var statsParams = new RepositoryStatsParameters();
        statsParams.setMimeTypes(mimeTypes);
        final var results = repositoryStats.getByMimeTypes(statsParams);
        return builder.entity(results).build();
    }
}
