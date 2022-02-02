/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.impl;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.fcrepo.common.db.DbPlatform;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.stats.api.BinaryStatsResult;
import org.fcrepo.stats.api.MimetypeStatsResult;
import org.fcrepo.stats.api.Stats;
import org.fcrepo.stats.api.StatsParameters;
import org.fcrepo.stats.api.StatsResult;
import org.fcrepo.stats.api.StatsResults;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author dbernstein
 */
@Component("stats")
public class DbStatsImpl implements Stats {

    @Inject
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    private DbPlatform dbPlatForm;

    /**
     * Setup database table and connection
     */
    @PostConstruct
    public void setup() {
        this.dbPlatForm = DbPlatform.fromDataSource(this.dataSource);
        this.jdbcTemplate = new NamedParameterJdbcTemplate(this.dataSource);
    }

    @Override
    public StatsResults getStatistics(final StatsParameters statsParams) {

        final var results = new StatsResults();

        //count all resources
        final var resourceCountQuery = "select count(*) from simple_search";
        final var parameterSource = new MapSqlParameterSource();
        final var resourceCount = jdbcTemplate.queryForObject(resourceCountQuery, parameterSource,
                Long.class);

        final var all = new StatsResult();
        all.setResourceCount(resourceCount);
        results.setAll(all);


        //count binaries by mimetype
        final var mimetypeQuery = "select a.mime_type, count(a.*), sum(a.content_size) " +
                "from simple_search a, " +
                "search_resource_rdf_type b,  search_rdf_type c  where a.id = b.resource_id and b.rdf_type_id = c.id " +
                "and c.rdf_type_uri = '" + RdfLexicon.NON_RDF_SOURCE.getURI() + "' group by a.mime_type";

        final var mimetypeResults = jdbcTemplate.queryForRowSet(mimetypeQuery, parameterSource);

        if (mimetypeResults.first()) {
            final var mimeTypes = new ArrayList<MimetypeStatsResult>();
            do {
                final var mimetypeResult = new MimetypeStatsResult();
                mimetypeResult.setMimetype(mimetypeResults.getString(1));
                mimetypeResult.setResourceCount(mimetypeResults.getLong(2));
                mimetypeResult.setByteCount(mimetypeResults.getLong(3));
                mimeTypes.add(mimetypeResult);
            } while (mimetypeResults.next());
            results.setMimetypes(mimeTypes);

            //sum the mimetype counts for all binaries
            final var binary = new BinaryStatsResult();
            final AtomicLong byteCount = new AtomicLong(0);
            final AtomicLong binaryResourceCount = new AtomicLong(0);
            mimeTypes.forEach(x -> {
                byteCount.addAndGet(x.getByteCount());
                binaryResourceCount.addAndGet(x.getResourceCount());
            });
            binary.setResourceCount(binaryResourceCount.get());
            binary.setByteCount(byteCount.get());
            results.setBinaries(binary);
        }

        return results;
    }
}
