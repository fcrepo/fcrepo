/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.stats.impl;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javax.sql.DataSource;

import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.stats.api.MimeTypeStatsResult;
import org.fcrepo.stats.api.RdfTypeStatsResult;
import org.fcrepo.stats.api.RepositoryStats;
import org.fcrepo.stats.api.RepositoryStatsByMimeTypeResults;
import org.fcrepo.stats.api.RepositoryStatsByRdfTypeResults;
import org.fcrepo.stats.api.RepositoryStatsParameters;
import org.fcrepo.stats.api.RepositoryStatsResult;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * A database-backed implementation of the <code>RepositoryStats</code> interface.
 * It depends on the tables that drive the search index.
 *
 * @author dbernstein
 */
@Component("stats")
public class DbRepositoryStatsImpl implements RepositoryStats {

    private static final String SELECT_COUNT_FROM_SIMPLE_SEARCH = "select count(*) from simple_search";

    @Inject
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Setup template
     */
    @PostConstruct
    public void setup() {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(this.dataSource);
    }


    @Override
    public RepositoryStatsResult getResourceCount(final RepositoryStatsParameters statsParams) {
        final var parameterSource = new MapSqlParameterSource();
        final var results = jdbcTemplate.queryForRowSet(SELECT_COUNT_FROM_SIMPLE_SEARCH, parameterSource);
        results.first();
        final var result = new RepositoryStatsResult();
        result.setResourceCount(results.getLong(1));
        return result;
    }

    @Override
    public RepositoryStatsByMimeTypeResults getByMimeTypes(final RepositoryStatsParameters statsParams) {
        final var results = new RepositoryStatsByMimeTypeResults();
        final var mimeTypes = statsParams.getMimeTypes();
        final var parameterSource = new MapSqlParameterSource();
        final var mimeTypesQuery = formatMimetypeQuery(mimeTypes, parameterSource);
        final var mimetypeResults = jdbcTemplate.queryForRowSet(mimeTypesQuery, parameterSource);
        marshallMimeTypeResults(results, mimetypeResults);
        return results;
    }

    @Override
    public RepositoryStatsByRdfTypeResults getByRdfType(final RepositoryStatsParameters statsParams) {
        final var results = new RepositoryStatsByRdfTypeResults();
        final var parameterSource = new MapSqlParameterSource();
        final var query = formatRdfTypeQuery(statsParams.getRdfTypes(), parameterSource);
        final var rdfTypeResults = jdbcTemplate.queryForRowSet(query, parameterSource);
        marshallRdfTypeResults(results, rdfTypeResults);
        return results;
    }

    private void marshallMimeTypeResults(final RepositoryStatsByMimeTypeResults results,
                                         final SqlRowSet mimeTypeResults) {
        if (mimeTypeResults.first()) {
            final var mimeTypesResultList = new ArrayList<MimeTypeStatsResult>();
            do {
                final var mimeTypeResult = new MimeTypeStatsResult();
                mimeTypeResult.setMimeType(mimeTypeResults.getString(1));
                mimeTypeResult.setResourceCount(mimeTypeResults.getLong(2));
                mimeTypeResult.setByteCount(mimeTypeResults.getLong(3));
                mimeTypesResultList.add(mimeTypeResult);
            } while (mimeTypeResults.next());
            results.setMimeTypes(mimeTypesResultList);
        }
    }

    private String formatMimetypeQuery(final List<String> mimeTypes, final MapSqlParameterSource parameterSource) {
        final var mimeTypesQuery = new StringBuilder("select a.mime_type, count(a.id), sum(a.content_size) ");
        mimeTypesQuery.append("from simple_search a, search_resource_rdf_type b,  search_rdf_type c ");
        mimeTypesQuery.append("where a.id = b.resource_id and b.rdf_type_id = c.id and c.rdf_type_uri = '");
        mimeTypesQuery.append(RdfLexicon.NON_RDF_SOURCE.getURI());
        mimeTypesQuery.append("' ");
        if (!CollectionUtils.isEmpty(mimeTypes)) {
            mimeTypesQuery.append("and a.mime_type in (:mime_types) ");
            parameterSource.addValue("mime_types", mimeTypes);
        }
        mimeTypesQuery.append("group by a.mime_type order by a.mime_type");
        return mimeTypesQuery.toString();
    }

    private void marshallRdfTypeResults(final RepositoryStatsByRdfTypeResults results,
                                        final SqlRowSet rdfTypeResults) {
        if (rdfTypeResults.first()) {
            final var resourceTypeStatsResults = new ArrayList<RdfTypeStatsResult>();
            do {
                final var resourceTypeResult = new RdfTypeStatsResult();
                resourceTypeResult.setResourceType(rdfTypeResults.getString(1));
                resourceTypeResult.setResourceCount(rdfTypeResults.getLong(2));
                resourceTypeResult.setByteCount(rdfTypeResults.getLong(3));
                resourceTypeStatsResults.add(resourceTypeResult);
            } while (rdfTypeResults.next());
            results.setRdfTypes(resourceTypeStatsResults);
        }
    }


    private String formatRdfTypeQuery(final List<String> rdfTypes, final MapSqlParameterSource parameterSource) {
        final var rdfTypesQuery = new StringBuilder(
                "select c.rdf_type_uri, count(b.resource_id), sum(a.content_size) ");
        rdfTypesQuery.append("from simple_search a, search_resource_rdf_type b,  search_rdf_type c ");
        rdfTypesQuery.append("where a.id = b.resource_id and b.rdf_type_id = c.id ");
        if (!CollectionUtils.isEmpty(rdfTypes)) {
            rdfTypesQuery.append("and c.rdf_type_uri in (:rdf_types) ");
            parameterSource.addValue("rdf_types", rdfTypes);
        }
        rdfTypesQuery.append("group by c.rdf_type_uri");
        return rdfTypesQuery.toString();
    }
}
