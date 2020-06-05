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

package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.persistence.ocfl.api.FedoraOCFLMappingNotFoundException;
import org.fcrepo.persistence.ocfl.api.FedoraToOcflObjectIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

/**
 * Maps Fedora IDs to the OCFL IDs of the OCFL objects the Fedora resource is stored in. This implementation is backed
 * by a relational database.
 */
@Component
public class DbFedoraToOcflObjectIndex implements FedoraToOcflObjectIndex {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbFedoraToOcflObjectIndex.class);

    private static final String DDL = "sql/default-ocfl-index.sql";

    private static final String MAPPING_TABLE = "ocfl_id_map";

    private static final String FEDORA_ID_COLUMN = "fedora_id";

    private static final String FEDORA_ROOT_ID_COLUMN = "fedora_root_id";

    private static final String OCFL_ID_COLUMN = "ocfl_id";

    private static final String LOOKUP_MAPPING = "SELECT * FROM " + MAPPING_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :fedoraId";

    private static final String INSERT_MAPPING = "INSERT INTO " + MAPPING_TABLE +
            " (" + FEDORA_ID_COLUMN + ", " + FEDORA_ROOT_ID_COLUMN + ", " + OCFL_ID_COLUMN + ")" +
            " VALUES (:fedoraId, :fedoraRootId, :ocflId)";

    private static final String REMOVE_MAPPING = "DELETE FROM " + MAPPING_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :fedoraId";

    private static final String TRUNCATE_MAPPINGS = "TRUNCATE TABLE " + MAPPING_TABLE;

    private final DataSource dataSource;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DbFedoraToOcflObjectIndex(@Autowired final DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @PostConstruct
    public void setup() {
        LOGGER.info("Applying ddl: {}", DDL);
        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new DefaultResourceLoader().getResource("classpath:" + DDL)),
                dataSource);
    }

    @Override
    public FedoraOCFLMapping getMapping(final String fedoraId) throws FedoraOCFLMappingNotFoundException {
        try {
            return jdbcTemplate.queryForObject(LOOKUP_MAPPING,
                    Map.of("fedoraId", fedoraId), (rs, rowNum) -> {
                        return new FedoraOCFLMapping(rs.getString(2), rs.getString(3));
                    });
        } catch (EmptyResultDataAccessException e) {
            throw new FedoraOCFLMappingNotFoundException("No OCFL mapping found for " + fedoraId);
        }
    }

    @Override
    public FedoraOCFLMapping addMapping(final String fedoraId, final String fedoraRootId, final String ocflId) {
        jdbcTemplate.update(INSERT_MAPPING, Map.of(
                "fedoraId", fedoraId,
                "fedoraRootId", fedoraRootId,
                "ocflId", ocflId));
        return new FedoraOCFLMapping(fedoraRootId, ocflId);
    }

    @Override
    public void removeMapping(final String fedoraId) {
        jdbcTemplate.update(REMOVE_MAPPING, Map.of("fedoraId", fedoraId));
    }

    @Override
    public void reset() {
        jdbcTemplate.update(TRUNCATE_MAPPINGS, Collections.EMPTY_MAP);
    }

}
