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
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.InvalidQueryException;
import org.fcrepo.search.api.PaginationInfo;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.search.api.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
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
import java.sql.Date;
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
        LOGGER.info("Applying ddl: {}", DDL);
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
            if (field.equals(Condition.Field.FEDORA_ID) &&
                    condition.getOperator().equals(Condition.Operator.EQ)) {
                if (!object.equals("*")) {
                    if (object.contains("*")) {
                        object = object.replace("*", "%");
                        whereClauses.add(Condition.Field.FEDORA_ID + " like '" + object + "'");
                    } else {
                        whereClauses.add(Condition.Field.FEDORA_ID + " = '" + object + "'");
                    }
                }
            } else if (field.equals(Condition.Field.CREATED) || field.equals(Condition.Field.MODIFIED)) {
                //parse date
                try {
                    final var instant = InstantParser.parse(object);
                    final var paramName = "param" + paramCount++;
                    whereClauses.add(field + " " + operation.getStringValue() +
                            " :" + paramName);
                    parameterSource.addValue(paramName, new Date(instant.toEpochMilli()), Types.TIMESTAMP);
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
        sql.append(" ORDER BY " + Condition.Field.FEDORA_ID);
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
    public void addUpdateIndex(final FedoraId fedoraId, final Instant created, final Instant modified,
                               final Long size, final String mimetype) {
        final var fullId = fedoraId.getFullId();
        final var selectParams = new MapSqlParameterSource();
        selectParams.addValue("fedora_id", fullId);
        final var result = jdbcTemplate.queryForList("SELECT fedora_id FROM " + SIMPLE_SEARCH_TABLE +
                        " WHERE fedora_id = :fedora_id",
                selectParams);

        final var txId = UUID.randomUUID().toString();
        executeInDbTransaction(txId, status -> {
            try {

                final var params = new MapSqlParameterSource();
                params.addValue("fedora_id", fullId);
                params.addValue("modified", modified);

                if (result.size() > 0) {
                    //update
                    final var sql = "UPDATE " + SIMPLE_SEARCH_TABLE +
                            " SET modified=:modified  WHERE fedora_id = :fedora_id;";
                    jdbcTemplate.update(sql, params);
                } else {
                    params.addValue("created", created);

                    final var sql = "INSERT INTO " + SIMPLE_SEARCH_TABLE + " (fedora_id, modified, created) " +
                            "VALUES(:fedora_id, :modified, :created)";
                    jdbcTemplate.update(sql, params);
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
        final var txId = UUID.randomUUID().toString();
        executeInDbTransaction(txId, status -> {
            try {
                final var params = new MapSqlParameterSource();
                params.addValue("fedora_id", fedoraId.getFullId());
                final var sql = "DELETE FROM " + SIMPLE_SEARCH_TABLE + " WHERE fedora_id = :fedora_id;";
                jdbcTemplate.update(sql, params);
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RepositoryRuntimeException("Failed to delete search index entry for " +
                        fedoraId.getFullId(), e);
            }
        });
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
