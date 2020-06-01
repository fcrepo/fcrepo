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

import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.InvalidQueryException;
import org.fcrepo.search.api.PaginationInfo;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.search.api.SearchResult;
import org.fcrepo.search.api.SearchService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the {@link org.fcrepo.search.api.SearchService}
 *
 * @author dbernstein
 */
@Component
public class SearchServiceImpl implements SearchService {
    private static final String FEDORA_ID_DB_COLUMN = "fedora_id";

    @Inject
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Inject
    private PlatformTransactionManager platformTransactionManager;

    /**
     * Connect to the database
     */
    @PostConstruct
    private void setup() {
        jdbcTemplate = getNamedParameterJdbcTemplate();
    }

    private NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(this.dataSource);
    }

    @Override
    public SearchResult doSearch(final SearchParameters parameters) throws InvalidQueryException {
        //translate parameters into a SQL query
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        final var whereClauses = new ArrayList<String>();
        for (Condition condition : parameters.getConditions()) {
            if (condition.getField().equals(Condition.Field.fedora_id) &&
                    condition.getOperator().equals(Condition.Operator.EQ)) {
                var object = condition.getObject();
                if (!object.equals("*")) {
                    if (object.contains("*")) {
                        object = object.replace("*", "%");
                        whereClauses.add(FEDORA_ID_DB_COLUMN + " like '" + object + "'");
                    } else {
                        whereClauses.add(FEDORA_ID_DB_COLUMN + " = '" + object + "'");
                    }
                }
            } else {
                throw new InvalidQueryException("Condition not supported: " +
                        condition.getField() +
                        condition.getOperator().getStringValue() +
                        condition.getObject());
            }
        }

        final var sql = new StringBuffer("select " + FEDORA_ID_DB_COLUMN + " from resources");
        if (!whereClauses.isEmpty()) {
            sql.append(" where ");
            for (int x = 0; x < whereClauses.size(); x++) {
                if (x > 0) {
                    sql.append(" and ");
                }
                sql.append(whereClauses.get(x));
            }
        }
        sql.append(" order by " + FEDORA_ID_DB_COLUMN);
        sql.append(" limit :limit offset :offset");
        parameterSource.addValue("limit", parameters.getMaxResults());
        parameterSource.addValue("offset", parameters.getOffset());
        final List<Map<String, Object>> items = jdbcTemplate.queryForList(sql.toString(), parameterSource);
        final var pagination = new PaginationInfo(parameters.getMaxResults(), parameters.getOffset());
        return new SearchResult(items, pagination);
    }
}
