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
package org.fcrepo.search.impl;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.InvalidQueryException;
import org.fcrepo.search.api.PaginationInfo;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.search.api.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.fcrepo.search.api.Condition.Field.CONTENT_SIZE;
import static org.fcrepo.search.api.Condition.Field.FEDORA_ID;
import static org.fcrepo.search.api.Condition.Field.MIME_TYPE;
import static org.fcrepo.search.api.Condition.Field.RDF_TYPE;


/**
 * An implementation of the {@link SearchIndex}
 *
 * @author dbernstein
 */
@Component
public class DbSearchIndexImpl implements SearchIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(DbSearchIndexImpl.class);
    private static final String SIMPLE_SEARCH_TABLE = "simple_search";
    private static final String DELETE_FROM_INDEX_SQL = "DELETE FROM simple_search WHERE fedora_id = :fedora_id;";
    private static final String UPDATE_INDEX_SQL =
            "UPDATE simple_search SET modified = :modified, content_size = :content_size, mime_type =:mime_type " +
                    "WHERE fedora_id = :fedora_id;";
    private static final String SELECT_BY_FEDORA_ID =
            "SELECT id FROM simple_search WHERE fedora_id = :fedora_id";
    private static final String FEDORA_ID_PARAM = "fedora_id";
    private static final String MODIFIED_PARAM = "modified";
    private static final String CONTENT_SIZE_PARAM = "content_size";
    private static final String MIME_TYPE_PARAM = "mime_type";
    private static final String CREATED_PARAM = "created";
    private static final String DELETE_RDF_TYPE_ASSOCIATIONS =
            "DELETE FROM search_resource_rdf_type where resource_id = :resource_id";
    private static final String RDF_TYPE_TABLE = ", (SELECT rrt.resource_id, " +
            "GROUP_CONCAT(distinct rt.rdf_type_uri ORDER BY rt.rdf_type_uri ASC SEPARATOR ',') as rdf_type " +
            "from search_resource_rdf_type rrt, search_rdf_type rt " +
            "WHERE rrt.rdf_type_id = rt.id group by rrt.resource_id) r";
    private static final String DEFAULT_DDL = "sql/default-search-index.sql";
    private static final Map<String, String> DDL_MAP = Map.of(
            "MySQL", DEFAULT_DDL,
            "H2", DEFAULT_DDL,
            "PostgreSQL", "sql/postgresql-search-index.sql",
            "MariaDB", DEFAULT_DDL
    );

    @Inject
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Inject
    private PlatformTransactionManager platformTransactionManager;

    @Inject
    private ResourceFactory resourceFactory;

    /**
     * Setup database table and connection
     */
    @PostConstruct
    public void setup() {
        final var ddl = lookupDdl();
        LOGGER.debug("Applying ddl: {}", ddl);
        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new DefaultResourceLoader().getResource("classpath:" + ddl)),
                this.dataSource);
        this.jdbcTemplate = getNamedParameterJdbcTemplate();
    }

    private String lookupDdl() {
        try (final var connection = dataSource.getConnection()) {
            final var productName = connection.getMetaData().getDatabaseProductName();
            LOGGER.debug("Identified database as: {}", productName);
            final var ddl = DDL_MAP.get(productName);
            if (ddl == null) {
                throw new IllegalStateException("Unknown database platform: " + productName);
            }
            return ddl;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(this.dataSource);
    }

    @Override
    public SearchResult doSearch(final SearchParameters parameters) throws InvalidQueryException {
        //translate parameters into a SQL query
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        final var whereClauses = new ArrayList<String>();
        final var conditions = parameters.getConditions();
        for (int i = 0; i < conditions.size(); i++) {
            addWhereClause(i, parameterSource, whereClauses, conditions.get(i));
        }

        final var fields = parameters.getFields().stream().map(x -> x.toString()).collect(Collectors.toList());
        final boolean containsRDFTypeField = fields.contains(RDF_TYPE.toString());
        if (containsRDFTypeField) {
            whereClauses.add("s.id = r.resource_id");
        }

        final var sql =
                new StringBuilder("SELECT " + String.join(",", fields) + " FROM " + SIMPLE_SEARCH_TABLE + " s");

        if (containsRDFTypeField) {
            sql.append(RDF_TYPE_TABLE);
        }

        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ");
            for (var it = whereClauses.iterator(); it.hasNext(); ) {
                sql.append(it.next());
                if (it.hasNext()) {
                    sql.append(" AND ");
                }
            }
        }
        sql.append(" ORDER BY " + parameters.getOrderBy() + " " + parameters.getOrder());
        sql.append(" LIMIT :limit OFFSET :offset");

        parameterSource.addValue("limit", parameters.getMaxResults());
        parameterSource.addValue("offset", parameters.getOffset());

        final var rowMapper = new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final Map<String, Object> map = new HashMap<>();
                for (String f : fields) {
                    final var fieldStr = f.toString();
                    var value = rs.getObject(fieldStr);
                    if (value instanceof Timestamp) {
                        //format as iso instant if timestamp
                        value = ISO_INSTANT.format(Instant.ofEpochMilli(((Timestamp) value).getTime()));
                    } else if (f.equals(RDF_TYPE.toString())) {
                        //convert the comma-separate string to an array for rdf_type
                        value = value.toString().split(",");
                    }
                    map.put(fieldStr, value);
                }
                return map;
            }
        };

        final List<Map<String, Object>> items = jdbcTemplate.query(sql.toString(), parameterSource, rowMapper);
        final var pagination = new PaginationInfo(parameters.getMaxResults(), parameters.getOffset());
        LOGGER.debug("Search query with parameters: {} - {}", sql, parameters);
        return new SearchResult(items, pagination);
    }

    private void addWhereClause(final int paramCount, final MapSqlParameterSource parameterSource,
                                final ArrayList<String> whereClauses,
                                final Condition condition) throws InvalidQueryException {
        final var field = condition.getField();
        final var operation = condition.getOperator();
        var object = condition.getObject();
        final var paramName = "param" + paramCount;
        if ((field.equals(FEDORA_ID) || field.equals(MIME_TYPE) || field.equals(RDF_TYPE)) &&
                condition.getOperator().equals(Condition.Operator.EQ)) {
            if (!object.equals("*")) {
                final String whereClause;
                if (object.contains("*")) {
                    object = object.replace("*", "%");
                    whereClause = field + " like :" + paramName;
                } else {
                    whereClause = field + " = :" + paramName;
                }

                whereClauses.add((field.equals(RDF_TYPE) ? "r." : "s.") + whereClause);
                parameterSource.addValue(paramName, object);
            }
        } else if (field.equals(Condition.Field.CREATED) || field.equals(Condition.Field.MODIFIED)) {
            //parse date
            try {
                final var instant = InstantParser.parse(object);
                whereClauses.add("s." + field + " " + operation.getStringValue() + " :" + paramName);
                parameterSource.addValue(paramName, new Timestamp(instant.toEpochMilli()), Types.TIMESTAMP);
            } catch (Exception ex) {
                throw new InvalidQueryException(ex.getMessage());
            }
        } else if (field.equals(CONTENT_SIZE)) {
            try {
                whereClauses.add(field + " " + operation.getStringValue() +
                        " :" + paramName);
                parameterSource.addValue(paramName, Long.parseLong(object), Types.INTEGER);
            } catch (Exception ex) {
                throw new InvalidQueryException(ex.getMessage());
            }
        } else {
            throw new InvalidQueryException("Condition not supported: \"" + condition + "\"");
        }
    }

    @Override
    public void addUpdateIndex(final ResourceHeaders resourceHeaders) {
        final var fullId = resourceHeaders.getId().getFullId();
        final var selectParams = new MapSqlParameterSource();
        selectParams.addValue(FEDORA_ID_PARAM, fullId);
        final var result =
                jdbcTemplate.queryForList(SELECT_BY_FEDORA_ID,
                        selectParams);

        final var txId = UUID.randomUUID().toString();
        executeInDbTransaction(txId, status -> {
            try {
                final var rdfTypes = new ArrayList<String>();
                rdfTypes.add(resourceHeaders.getInteractionModel());
                final var fedoraResource = resourceFactory.getResource(FedoraId.create(fullId)).getDescription();
                fedoraResource.getTriples().forEach(triple -> {
                    if (triple.getPredicate().getURI().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                        final var rdfTypeUri = triple.getObject().getURI();
                        rdfTypes.add(rdfTypeUri);
                    }
                });

                final var jdbcInsertRdfTypes = new SimpleJdbcInsert(this.jdbcTemplate.getJdbcTemplate());
                jdbcInsertRdfTypes.withTableName("search_rdf_type").usingGeneratedKeyColumns(
                        "id");

                final var rdfTypeIds = new ArrayList<Long>();
                for (var rdfTypeUri : rdfTypes) {
                    final var typeParams = new MapSqlParameterSource();
                    typeParams.addValue("rdf_type_uri", rdfTypeUri);
                    final var results = jdbcTemplate.queryForList("select id from search_rdf_type where " +
                                    "rdf_type_uri = :rdf_type_uri",
                            typeParams);


                    if (CollectionUtils.isEmpty(results)) {
                        final Number key = jdbcInsertRdfTypes.executeAndReturnKey(typeParams);
                        rdfTypeIds.add(key.longValue());
                    } else {
                        rdfTypeIds.add((long) results.get(0).get("id"));
                    }
                }

                final var params = new MapSqlParameterSource();
                params.addValue(FEDORA_ID_PARAM, fullId);
                params.addValue(MODIFIED_PARAM, new Timestamp(resourceHeaders.getLastModifiedDate().toEpochMilli()));
                params.addValue(MIME_TYPE_PARAM, resourceHeaders.getMimeType());
                params.addValue(CONTENT_SIZE_PARAM, resourceHeaders.getContentSize());
                final var exists = result.size() > 0;
                final Long resourcePrimaryKey;
                if (exists) {
                    resourcePrimaryKey = (Long) result.get(0).get("id");
                    jdbcTemplate.update(UPDATE_INDEX_SQL, params);
                    //delete rdf_type associations
                    final var deleteParams = new MapSqlParameterSource();
                    deleteParams.addValue("resource_id", resourcePrimaryKey);
                    jdbcTemplate.update(DELETE_RDF_TYPE_ASSOCIATIONS,
                            deleteParams);
                } else {
                    params.addValue(CREATED_PARAM, new Timestamp(resourceHeaders.getCreatedDate().toEpochMilli()));
                    final var jdbcInsertResource =
                            new SimpleJdbcInsert(this.jdbcTemplate.getJdbcTemplate()).usingGeneratedKeyColumns(
                                    "id");
                    resourcePrimaryKey =
                            jdbcInsertResource.withTableName(SIMPLE_SEARCH_TABLE).executeAndReturnKey(params)
                                    .longValue();
                }

                //add rdf type associations
                final var jdbcInsertRdfTypeAssociations = new SimpleJdbcInsert(this.jdbcTemplate.getJdbcTemplate());
                jdbcInsertRdfTypeAssociations.withTableName("search_resource_rdf_type").usingGeneratedKeyColumns(
                        "id");
                for (var rdfTypeId : rdfTypeIds) {
                    final var assocParams = new MapSqlParameterSource();
                    assocParams.addValue("resource_id", resourcePrimaryKey);
                    assocParams.addValue("rdf_type_id", rdfTypeId);
                    jdbcInsertRdfTypeAssociations.execute(assocParams);
                }

                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RepositoryRuntimeException("Failed add/updated the search index for : " + fullId, e);
            }
        });
    }

    @Override
    public void removeFromIndex(final FedoraId fedoraId) {
        try {
            final var params = new MapSqlParameterSource();
            params.addValue(FEDORA_ID_PARAM, fedoraId.getFullId());
            jdbcTemplate.update(DELETE_FROM_INDEX_SQL, params);
        } catch (DataAccessException ex) {
            throw new RepositoryRuntimeException("Failed to delete search index entry for " + fedoraId.getFullId());
        }
    }

    private <T> T executeInDbTransaction(final String txId, final TransactionCallback<T> callback) {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        // Seemingly setting the name ensures that we don't re-use a transaction.
        transactionTemplate.setName("tx-" + txId);
        transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
        return transactionTemplate.execute(callback);
    }

    @Override
    public void reset() {
        jdbcTemplate.update("TRUNCATE TABLE search_resource_rdf_type", Collections.EMPTY_MAP);
        jdbcTemplate.update("TRUNCATE TABLE search_rdf_type", Collections.EMPTY_MAP);
        jdbcTemplate.update("TRUNCATE TABLE " + SIMPLE_SEARCH_TABLE, Collections.EMPTY_MAP);
    }

}
