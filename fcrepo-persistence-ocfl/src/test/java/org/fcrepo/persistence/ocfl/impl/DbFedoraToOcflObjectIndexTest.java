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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/**
 * @author dbernstein
 * @since 6.0.0
 */
public class DbFedoraToOcflObjectIndexTest {

    private static final String RESOURCE_ID_1 = "info:fedora/parent/child1";
    private static final String RESOURCE_ID_2 = "info:fedora/parent/child2";
    private static final String RESOURCE_ID_3 = "info:fedora/resource3";
    private static final String ROOT_RESOURCE_ID = "info:fedora/parent";
    private static final String OCFL_ID = "ocfl-id";
    private static final String OCFL_ID_RESOURCE_3 = "ocfl-id-resource-3";

    private static DriverManagerDataSource dataSource;
    private static DbFedoraToOcflObjectIndex index;

    @BeforeClass
    public static void beforeClass() {
        dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.jdbcx.JdbcDataSource");
        dataSource.setUrl("jdbc:h2:mem:index;DB_CLOSE_DELAY=-1");
        index = new DbFedoraToOcflObjectIndex(dataSource);
        index.setup();
    }

    @Before
    public void setup() {
        index.reset();
    }

    @Test
    public void test() throws Exception {
        index.addMapping(ROOT_RESOURCE_ID, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(RESOURCE_ID_1, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(RESOURCE_ID_2, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(RESOURCE_ID_3, RESOURCE_ID_3, OCFL_ID_RESOURCE_3);

        final FedoraOCFLMapping mapping1 = index.getMapping(RESOURCE_ID_1);
        final FedoraOCFLMapping mapping2 = index.getMapping(RESOURCE_ID_2);
        final FedoraOCFLMapping mapping3 = index.getMapping(ROOT_RESOURCE_ID);

        assertEquals(mapping1, mapping2);
        assertEquals(mapping2, mapping3);

        verifyMapping(mapping1, ROOT_RESOURCE_ID, OCFL_ID);

        final FedoraOCFLMapping mapping4 = index.getMapping(RESOURCE_ID_3);
        assertNotEquals(mapping4, mapping3);

        verifyMapping(mapping4, RESOURCE_ID_3, OCFL_ID_RESOURCE_3);

        assertEquals(ROOT_RESOURCE_ID, mapping1.getRootObjectIdentifier());
        assertEquals(OCFL_ID, mapping1.getOcflObjectId());
    }

    @Test(expected = FedoraOCFLMappingNotFoundException.class)
    public void testNotExists() throws Exception {
        index.getMapping(RESOURCE_ID_1);
    }

    @Test
    public void removeIndexWhenExists() throws FedoraOCFLMappingNotFoundException {
        final var mapping = index.addMapping(RESOURCE_ID_1, RESOURCE_ID_1, OCFL_ID);

        assertEquals(mapping, index.getMapping(RESOURCE_ID_1));

        index.removeMapping(RESOURCE_ID_1);

        try {
            index.getMapping(RESOURCE_ID_1);
            fail("expected exception");
        } catch (final FedoraOCFLMappingNotFoundException e) {
            // expected mapping to not exist
        }
    }

    @Test
    public void removeIndexWhenNotExists() throws FedoraOCFLMappingNotFoundException {
        index.removeMapping(RESOURCE_ID_1);

        try {
            index.getMapping(RESOURCE_ID_1);
            fail("expected exception");
        } catch (final FedoraOCFLMappingNotFoundException e) {
            // expected mapping to not exist
        }
    }

    private void verifyMapping(final FedoraOCFLMapping mapping1, final String rootResourceId, final String ocflId) {
        assertEquals(rootResourceId, mapping1.getRootObjectIdentifier());
        assertEquals(ocflId, mapping1.getOcflObjectId());
    }

}