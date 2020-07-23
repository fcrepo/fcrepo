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
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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


/**
 * An implementation of the {@link SearchIndex}
 *
 * @author dbernstein
 */
@Component
public class DbSearchIndexImpl implements SearchIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(DbSearchIndexImpl.class);
    private static final String SIMPLE_SEARCH_TABLE = "simple_search";
    private static final String DDL = "sql/default-search-index.sql";
    private static final String DELETE_FROM_INDEX_SQL = "DELETE FROM simple_search WHERE fedora_id = :fedora_id;";
    private static final String UPDATE_INDEX_SQL =
            "UPDATE simple_search SET modified = :modified, content_size = :content_size, mime_type =:mime_type " +
                    "WHERE fedora_id = :fedora_id;";
    private static final String INSERT_INTO_INDEX_SQL =
            "INSERT INTO simple_search (fedora_id, modified, created, content_size, mime_type) VALUES" +
            "(:fedora_id, :modified, :modified, :content_size, :mime_type)";
    private static final String SELECT_BY_FEDORA_ID =
            "SELECT fedora_id FROM simple_search WHERE fedora_id = :fedora_id";
    private static final String FEDORA_ID_PARAM = "fedora_id";
    private static final String MODIFIED_PARAM = "modified";
    private static final String CONTENT_SIZE_PARAM = "content_size";
    private static final String MIME_TYPE_PARAM = "mime_type";
    private static final String CREATED_PARAM = "created";

    @Inject
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Inject
    private PlatformTransactionManager platformTransactionManager;

    /**
     * Setup database table and connection
     */
    @PostConstruct
    public void setup() {
        LOGGER.debug("Applying ddl: {}", DDL);
        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new DefaultResourceLoader().getResource("classpath:" + DDL)),
                this.dataSource);
        this.jdbcTemplate = getNamedParameterJdbcTemplate();
    }

    private NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(this.dataSource);
    }

    @Override
    public SearchResult doSearch(final SearchParameters parameters) throws InvalidQueryException {
        //translate parameters into a SQL query
        var paramCount = 1;

        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        final var whereClauses = new ArrayList<String>();
        for (Condition condition : parameters.getConditions()) {
            final var field = condition.getField();
            final var operation = condition.getOperator();
            var object = condition.getObject();
            if ((field.equals(FEDORA_ID) || field.equals(MIME_TYPE)) &&
                    condition.getOperator().equals(Condition.Operator.EQ)) {
                if (!object.equals("*")) {
                    final var paramName = "param" + paramCount++;
                    if (object.contains("*")) {
                        object = object.replace("*", "%");
                        whereClauses.add(field + " like :" + paramName);
                    } else {
                        whereClauses.add(field + " = :" + paramName);
                    }
                    parameterSource.addValue(paramName, object);
                }
            } else if (field.equals(Condition.Field.CREATED) || field.equals(Condition.Field.MODIFIED)) {
                //parse date
                try {
                    final var instant = InstantParser.parse(object);
                    final var paramName = "param" + paramCount++;
                    whereClauses.add(field + " " + operation.getStringValue() + " :" + paramName);
                    parameterSource.addValue(paramName, new Timestamp(instant.toEpochMilli()), Types.TIMESTAMP);
                } catch (Exception ex) {
                    throw new InvalidQueryException(ex.getMessage());
                }
            } else if (field.equals(CONTENT_SIZE)) {
                try {
                    final var paramName = "param" + paramCount++;
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

        final var fields = parameters.getFields().stream().map(x -> x.toString()).collect(Collectors.toList());
        final var sql =
                new StringBuilder("SELECT " + String.join(",", fields) + " FROM " + SIMPLE_SEARCH_TABLE);

        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ");
            for (var it = whereClauses.iterator(); it.hasNext(); ) {
                sql.append(it.next());
                if (it.hasNext()) {
                    sql.append(" AND ");
                }
            }
        }
        sql.append(" ORDER BY " +  parameters.getOrderBy() + " " + parameters.getOrder());
        sql.append(" LIMIT :limit OFFSET :offset");
        parameterSource.addValue("limit", parameters.getMaxResults());
        parameterSource.addValue("offset", parameters.getOffset());

        final var rowMapper = new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final Map<String, Object> map = new HashMap<>();
                for (String f : fields) {
                    var value = rs.getObject(f);
                    if (value instanceof Timestamp) {
                        value = ISO_INSTANT.format(Instant.ofEpochMilli(((Timestamp) value).getTime()));
                    }
                    map.put(f, value);
                }
                return map;
            }
        };

        final List<Map<String, Object>> items = jdbcTemplate.query(sql.toString(), parameterSource, rowMapper);
        final var pagination = new PaginationInfo(parameters.getMaxResults(), parameters.getOffset());
        LOGGER.debug("Search query with parameters: {} - {}", sql, parameters);
        return new SearchResult(items, pagination);
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

                final var params = new MapSqlParameterSource();
                params.addValue(FEDORA_ID_PARAM, fullId);
                params.addValue(MODIFIED_PARAM, new Timestamp(resourceHeaders.getLastModifiedDate().toEpochMilli()));
                params.addValue(MIME_TYPE_PARAM, resourceHeaders.getMimeType());
                params.addValue(CONTENT_SIZE_PARAM, resourceHeaders.getContentSize());
                final String sql;
                if (result.size() > 0) {
                    //update
                    jdbcTemplate.update(UPDATE_INDEX_SQL, params);
                } else {
                    params.addValue(CREATED_PARAM, new Timestamp(resourceHeaders.getCreatedDate().toEpochMilli()));
                    jdbcTemplate.update(INSERT_INTO_INDEX_SQL, params);
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
        jdbcTemplate.update("TRUNCATE TABLE " + SIMPLE_SEARCH_TABLE, Collections.EMPTY_MAP);
    }

}
