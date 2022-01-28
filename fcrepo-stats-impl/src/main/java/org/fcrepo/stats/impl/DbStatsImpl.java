/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.impl;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.fcrepo.common.db.DbPlatform;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.stats.api.Stats;
import org.fcrepo.stats.api.StatsParameters;
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
        final var resourceCountQuery = "select count(*) from simple_search";
        final var parameterSource = new MapSqlParameterSource();
        final var resourceCount = jdbcTemplate.queryForObject(resourceCountQuery, parameterSource,
                Long.class);

        final var binaryResourceCountQuery = "select count(*), sum(content_size) from simple_search a, " +
                "search_resource_rdf_type b,  search_rdf_type c  where a.id = b.resource_id and b.rdf_type_id = c.id " +
                "and c.rdf_type_uri = '" + RdfLexicon.NON_RDF_SOURCE.getURI() + "'";

        final var binaryResults = jdbcTemplate.queryForRowSet(binaryResourceCountQuery, parameterSource);
        binaryResults.first();
        final var results = new StatsResults();

        results.setResourceCount(resourceCount);
        results.setBinaryResourceCount(binaryResults.getLong(1));
        results.setBinaryResourceBytes(binaryResults.getLong(2));
        return results;
    }
}
