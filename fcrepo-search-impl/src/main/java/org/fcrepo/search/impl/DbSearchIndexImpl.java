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

import org.fcrepo.common.db.DbPlatform;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.fcrepo.common.db.DbPlatform.H2;
import static org.fcrepo.common.db.DbPlatform.MARIADB;
import static org.fcrepo.common.db.DbPlatform.MYSQL;
import static org.fcrepo.common.db.DbPlatform.POSTGRESQL;
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
    public static final String SELECT_RDF_TYPE_ID = "select id from search_rdf_type where rdf_type_uri = :rdf_type_uri";
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
    private static final String RDF_TYPE_TABLE = ", (SELECT rrt.resource_id,  group_concat_function as rdf_type " +
            "from search_resource_rdf_type rrt, " +
            "search_rdf_type rt  WHERE rrt.rdf_type_id = rt.id group by rrt.resource_id) r, " +
            "(SELECT rrt.resource_id from search_resource_rdf_type rrt, search_rdf_type rt " +
            "WHERE rt.rdf_type_uri like :rdf_type_uri and rrt.rdf_type_id = rt.id group by rrt.resource_id) r_filter";
    private static final String DEFAULT_DDL = "sql/default-search-index.sql";

    private static final Map<DbPlatform, String> DDL_MAP = Map.of(
            MYSQL, DEFAULT_DDL,
            H2, DEFAULT_DDL,
            POSTGRESQL, "sql/postgresql-search-index.sql",
            MARIADB, DEFAULT_DDL
    );
    public static final String SEARCH_RESOURCE_RDF_TYPE_TABLE = "search_resource_rdf_type";
    public static final String RESOURCE_ID_PARAM = "resource_id";
    public static final String RDF_TYPE_ID_PARAM = "rdf_type_id";
    public static final String RDF_TYPE_URI_PARAM = "rdf_type_uri";
    public static final String SEARCH_RDF_TYPE_TABLE = "search_rdf_type";
    public static final String ID_COLUMN = "id";
    private static final String GROUP_CONCAT_FUNCTION = "group_concat_function";
    private static final String POSTGRES_GROUP_CONCAT_FUNCTION = "STRING_AGG(rt.rdf_type_uri, ',')";
    private static final String DEFAULT_GROUP_CONCAT_FUNCTION = "GROUP_CONCAT(distinct rt.rdf_type_uri " +
            "ORDER BY rt.rdf_type_uri ASC SEPARATOR ',')";

    @Inject
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Inject
    private ResourceFactory resourceFactory;

    private DbPlatform dbPlatForm;

    private String rdfTables;

    /**
     * Setup database table and connection
     */
    @PostConstruct
    public void setup() {
        this.dbPlatForm = DbPlatform.fromDataSource(this.dataSource);
        final var ddl = lookupDdl();
        LOGGER.debug("Applying ddl: {}", ddl);
        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new DefaultResourceLoader().getResource("classpath:" + ddl)),
                this.dataSource);
        this.jdbcTemplate = getNamedParameterJdbcTemplate();

        this.rdfTables = RDF_TYPE_TABLE.replace(GROUP_CONCAT_FUNCTION,
                isPostgres() ? POSTGRES_GROUP_CONCAT_FUNCTION : DEFAULT_GROUP_CONCAT_FUNCTION);
    }

    private String lookupDdl() {
        return DDL_MAP.get(dbPlatForm);
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
            whereClauses.add("r.resource_id = r_filter.resource_id");
        }

        final var sql =
                new StringBuilder("SELECT " + String.join(",", fields) + " FROM " + SIMPLE_SEARCH_TABLE + " s");

        if (containsRDFTypeField) {
            sql.append(rdfTables);
            var rdfTypeUriParamValue = "*";
            for (Condition condition: conditions) {
                if (condition.getField().equals(RDF_TYPE)) {
                    rdfTypeUriParamValue = condition.getObject();
                    break;
                }
            }
            parameterSource.addValue(RDF_TYPE_URI_PARAM, convertToSqlLikeWildcard(rdfTypeUriParamValue));
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
                                final List<String> whereClauses,
                                final Condition condition) throws InvalidQueryException {
        final var field = condition.getField();
        final var operation = condition.getOperator();
        var object = condition.getObject();
        final var paramName = "param" + paramCount;
        if ((field.equals(FEDORA_ID) || field.equals(MIME_TYPE)) &&
                condition.getOperator().equals(Condition.Operator.EQ)) {
            if (!object.equals("*")) {
                final String whereClause;
                if (object.contains("*")) {
                    object = convertToSqlLikeWildcard(object);
                    whereClause = field + " like :" + paramName;
                } else {
                    whereClause = field + " = :" + paramName;
                }

                whereClauses.add("s." +  whereClause);
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
        } else if (field.equals(RDF_TYPE) && condition.getOperator().equals(Condition.Operator.EQ) ) {
           //allowed but no where clause added here.
        } else {
            throw new InvalidQueryException("Condition not supported: \"" + condition + "\"");
        }
    }

    private String convertToSqlLikeWildcard(final String value) {
        return value.replace("*", "%");
    }

    @Override
    public void addUpdateIndex(final ResourceHeaders resourceHeaders) {
        addUpdateIndex(null, resourceHeaders);
    }

    @Transactional
    @Override
    public void addUpdateIndex(final String txId, final ResourceHeaders resourceHeaders) {
        final var fullId = resourceHeaders.getId().getFullId();
        final var fedoraId = FedoraId.create(fullId);
        final var selectParams = new MapSqlParameterSource();
        selectParams.addValue(FEDORA_ID_PARAM, fullId);
        final var result =
                jdbcTemplate.queryForList(SELECT_BY_FEDORA_ID,
                        selectParams);

        if (fedoraId.isAcl() || fedoraId.isMemento()) {
            LOGGER.debug("The search index does not include acls or mementos. Ignoring resource {}", fullId);
            return;
        }

        try {
            final var fedoraResource = resourceFactory.getResource(txId, fedoraId);
            final var rdfTypes = fedoraResource.getTypes();
            final var rdfTypeIds = findOrCreateRdfTypesInDb(rdfTypes);
            final var params = new MapSqlParameterSource();
            params.addValue(FEDORA_ID_PARAM, fullId);
            params.addValue(MODIFIED_PARAM, new Timestamp(resourceHeaders.getLastModifiedDate().toEpochMilli()));
            params.addValue(MIME_TYPE_PARAM, resourceHeaders.getMimeType());
            params.addValue(CONTENT_SIZE_PARAM, resourceHeaders.getContentSize());
            final var exists = result.size() > 0;
            final Long resourcePrimaryKey;
            if (exists) {
                resourcePrimaryKey = (Long) result.get(0).get(ID_COLUMN);
                jdbcTemplate.update(UPDATE_INDEX_SQL, params);
                //delete rdf_type associations
                deleteRdfTypeAssociations(resourcePrimaryKey);
            } else {
                params.addValue(CREATED_PARAM, new Timestamp(resourceHeaders.getCreatedDate().toEpochMilli()));
                final var jdbcInsertResource =
                        new SimpleJdbcInsert(this.jdbcTemplate.getJdbcTemplate()).usingGeneratedKeyColumns(
                                ID_COLUMN);
                resourcePrimaryKey =
                        jdbcInsertResource.withTableName(SIMPLE_SEARCH_TABLE).executeAndReturnKey(params)
                                .longValue();
            }
            insertRdfTypeAssociations(rdfTypeIds, resourcePrimaryKey);
        } catch (Exception e) {
            throw new RepositoryRuntimeException("Failed add/updated the search index for : " + fullId, e);
        }
    }

    private void insertRdfTypeAssociations(final List<Long> rdfTypeIds, final Long resourceId) {
        //add rdf type associations
        final var jdbcInsertRdfTypeAssociations = new SimpleJdbcInsert(this.jdbcTemplate.getJdbcTemplate());
        jdbcInsertRdfTypeAssociations.withTableName(SEARCH_RESOURCE_RDF_TYPE_TABLE).usingGeneratedKeyColumns(
                ID_COLUMN);
        for (var rdfTypeId : rdfTypeIds) {
            final var assocParams = new MapSqlParameterSource();
            assocParams.addValue(RESOURCE_ID_PARAM, resourceId);
            assocParams.addValue(RDF_TYPE_ID_PARAM, rdfTypeId);
            jdbcInsertRdfTypeAssociations.execute(assocParams);
        }
    }

    private void deleteRdfTypeAssociations(final Long resourceId) {
        final var deleteParams = new MapSqlParameterSource();
        deleteParams.addValue(RESOURCE_ID_PARAM, resourceId);
        jdbcTemplate.update(DELETE_RDF_TYPE_ASSOCIATIONS,
                deleteParams);
    }

    private ArrayList<Long> findOrCreateRdfTypesInDb(final List<URI> rdfTypes) {
        final var jdbcInsertRdfTypes = new SimpleJdbcInsert(this.jdbcTemplate.getJdbcTemplate());
        jdbcInsertRdfTypes.withTableName(SEARCH_RDF_TYPE_TABLE).usingGeneratedKeyColumns(
                ID_COLUMN);
        final var rdfTypeIds = new ArrayList<Long>();
        for (var rdfTypeUri : rdfTypes) {
            final var typeParams = new MapSqlParameterSource();
            typeParams.addValue(RDF_TYPE_URI_PARAM, rdfTypeUri.toString());
            final var results = jdbcTemplate.queryForList(SELECT_RDF_TYPE_ID,
                    typeParams);
            if (CollectionUtils.isEmpty(results)) {
                final Number key = jdbcInsertRdfTypes.executeAndReturnKey(typeParams);
                rdfTypeIds.add(key.longValue());
            } else {
                rdfTypeIds.add((long) results.get(0).get(ID_COLUMN));
            }
        }
        return rdfTypeIds;
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

    @Override
    public void reset() {
        try (final var conn = this.dataSource.getConnection()) {
            final var statement = conn.createStatement();
            for (var sql : toggleForeignKeyChecks(false)) {
                statement.addBatch(sql);
            }
            statement.addBatch(truncateTable(SEARCH_RESOURCE_RDF_TYPE_TABLE));
            statement.addBatch(truncateTable(SIMPLE_SEARCH_TABLE));
            statement.addBatch(truncateTable(SEARCH_RDF_TYPE_TABLE));
            for (var sql : toggleForeignKeyChecks(true)) {
                statement.addBatch(sql);
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RepositoryRuntimeException("Failed to truncate search index tables", e);
        }
    }

    private List<String> toggleForeignKeyChecks(final boolean enable) {

        if (isPostgres()) {
            return List.of(
                    togglePostgresTriggers(SEARCH_RESOURCE_RDF_TYPE_TABLE, enable),
                    togglePostgresTriggers(SEARCH_RDF_TYPE_TABLE, enable),
                    togglePostgresTriggers(SIMPLE_SEARCH_TABLE, enable)
            );
        } else {
            return List.of("SET FOREIGN_KEY_CHECKS = " + (enable ? 1 : 0) + ";");
        }
    }

    private boolean isPostgres() {
        return dbPlatForm.equals(POSTGRESQL);
    }

    private String togglePostgresTriggers(final String tableName, final boolean enable) {
        return "ALTER TABLE " + tableName + " " +
                (enable ? "ENABLE" : "DISABLE") + " TRIGGER ALL;";
    }

    private String truncateTable(final String tableName) {
        final var addCascade = isPostgres();
        return "TRUNCATE TABLE " + tableName + (addCascade ? " CASCADE" : "") + ";";
    }

}
