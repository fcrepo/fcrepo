/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ok;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_HTML_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.micrometer.core.annotation.Timed;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.fcrepo.stats.api.Stats;
import org.fcrepo.stats.api.StatsParameters;
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
public class FedoraStats extends FedoraBaseResource {

    private static final Logger LOGGER = getLogger(FedoraStats.class);

    private static final FastDateFormat DATE_FORMAT = DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT;
    @Autowired
    @Qualifier("stats")
    private Stats stats;

    /**
     * Default JAX-RS entry point
     */
    public FedoraStats() {
        super();
    }

    /**
     * @param mimeTypes A comma-separated list of mimetypes
     * @param rdfTypes  A comma-separateed list of rdf types
     * @return
     */
    @GET
    @Produces({APPLICATION_JSON + ";qs=1.0",
            TEXT_PLAIN_WITH_CHARSET,
            TEXT_HTML_WITH_CHARSET})
    public Response getStats(@DefaultValue ("all") @QueryParam(value = "mime_types") final List<String> mimeTypes,
                             @QueryParam(value = "rdf_types") final List<String> rdfTypes
    ) {
        final var builder = ok();
        final var statsParams = new StatsParameters(mimeTypes, rdfTypes, null,
                null);
        final var results = stats.getStatistics(statsParams);

        return builder.entity(results).build();
    }

}
