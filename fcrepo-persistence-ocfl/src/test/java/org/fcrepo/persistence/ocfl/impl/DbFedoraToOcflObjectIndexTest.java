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

import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.util.UUID;

/**
 * @author dbernstein
 * @since 6.0.0
 */
public class DbFedoraToOcflObjectIndexTest {

    private static final FedoraId RESOURCE_ID_1 = FedoraId.create("info:fedora/parent/child1");
    private static final FedoraId RESOURCE_ID_2 = FedoraId.create("info:fedora/parent/child2");
    private static final FedoraId RESOURCE_ID_3 = FedoraId.create("info:fedora/resource3");
    private static final FedoraId ROOT_RESOURCE_ID = FedoraId.create("info:fedora/parent");
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
        final String sessId = UUID.randomUUID().toString();
        index.addMapping(sessId, ROOT_RESOURCE_ID, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(sessId, RESOURCE_ID_1, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(sessId, RESOURCE_ID_2, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(sessId, RESOURCE_ID_3, RESOURCE_ID_3, OCFL_ID_RESOURCE_3);

        final FedoraOcflMapping mapping1 = index.getMapping(sessId, RESOURCE_ID_1);
        final FedoraOcflMapping mapping2 = index.getMapping(sessId, RESOURCE_ID_2);
        final FedoraOcflMapping mapping3 = index.getMapping(sessId, ROOT_RESOURCE_ID);

        assertEquals(mapping1, mapping2);
        assertEquals(mapping2, mapping3);

        verifyMapping(mapping1, ROOT_RESOURCE_ID, OCFL_ID);

        index.commit(sessId);

        final FedoraOcflMapping mapping1_1 = index.getMapping(null, RESOURCE_ID_1);
        final FedoraOcflMapping mapping1_2 = index.getMapping(null, RESOURCE_ID_2);
        final FedoraOcflMapping mapping1_3 = index.getMapping(null, ROOT_RESOURCE_ID);

        assertEquals(mapping1_1, mapping1_2);
        assertEquals(mapping1_2, mapping1_3);

        final FedoraOcflMapping mapping4 = index.getMapping(null, RESOURCE_ID_3);
        assertNotEquals(mapping4, mapping3);

        verifyMapping(mapping4, RESOURCE_ID_3, OCFL_ID_RESOURCE_3);

        assertEquals(ROOT_RESOURCE_ID, mapping1.getRootObjectIdentifier());
        assertEquals(OCFL_ID, mapping1.getOcflObjectId());
    }

    @Test(expected = FedoraOcflMappingNotFoundException.class)
    public void testNotExists() throws Exception {
        index.getMapping(null, RESOURCE_ID_1);
    }

    @Test
    public void removeIndexWhenExists() throws FedoraOcflMappingNotFoundException {
        final String sessId = UUID.randomUUID().toString();
        final var mapping = index.addMapping(sessId, RESOURCE_ID_1, RESOURCE_ID_1, OCFL_ID);

        assertEquals(mapping, index.getMapping(sessId, RESOURCE_ID_1));
        try {
            index.getMapping(null, RESOURCE_ID_1);
            fail("This mapping should not be accessible to everyone yet.");
        } catch (final FedoraOcflMappingNotFoundException e) {
            // This should happen and is okay.
        }
        index.commit(sessId);

        assertEquals(mapping, index.getMapping(null, RESOURCE_ID_1));

        index.removeMapping(sessId, RESOURCE_ID_1);

        // Should still appear to outside the transaction.
        assertEquals(mapping, index.getMapping(null, RESOURCE_ID_1));

        // Should also still appear inside the transaction or we can't access files on disk.
        assertEquals(mapping, index.getMapping(sessId, RESOURCE_ID_1));

        index.commit(sessId);

        try {
            // No longer accessible to anyone.
            index.getMapping(null, RESOURCE_ID_1);
            fail("expected exception");
        } catch (final FedoraOcflMappingNotFoundException e) {
            // expected mapping to not exist
        }
    }

    @Test
    public void removeIndexWhenNotExists() {
        final String sessId = UUID.randomUUID().toString();
        index.removeMapping(sessId, RESOURCE_ID_1);
        index.commit(sessId);

        try {
            index.getMapping(null, RESOURCE_ID_1);
            fail("expected exception");
        } catch (final FedoraOcflMappingNotFoundException e) {
            // expected mapping to not exist
        }
    }

    @Test
    public void testRollback() throws Exception {
        final String sessId = UUID.randomUUID().toString();
        index.addMapping(sessId, RESOURCE_ID_1, ROOT_RESOURCE_ID, OCFL_ID);

        final FedoraOcflMapping expected = new FedoraOcflMapping(ROOT_RESOURCE_ID, OCFL_ID);
        assertEquals(expected, index.getMapping(sessId, RESOURCE_ID_1));

        try {
            // Not yet committed
            index.getMapping(null, RESOURCE_ID_1);
            fail();
        } catch (final FedoraOcflMappingNotFoundException e) {
            // The exception is expected.
        }

        index.rollback(sessId);

        try {
            index.getMapping(sessId, RESOURCE_ID_1);
            fail();
        } catch (final FedoraOcflMappingNotFoundException e) {
            // The exception is expected.
        }
    }

    @Test
    public void identifierTooLong() throws InvalidResourceIdentifierException {
        int cnt;
        String root = "info:fedora/longtest";
        for (cnt = 0; cnt < 149; cnt++) {
            root = root + "/" + String.valueOf(cnt);
        }
        try {
            final FedoraId tempid = FedoraId.create(root);
            final String sessId = UUID.randomUUID().toString();
            index.addMapping(sessId, tempid, ROOT_RESOURCE_ID, OCFL_ID);
            fail();
        } catch (final InvalidResourceIdentifierException e) {
            //the exception is expected
        }
    }

    private void verifyMapping(final FedoraOcflMapping mapping1, final FedoraId rootResourceId, final String ocflId) {
        assertEquals(rootResourceId, mapping1.getRootObjectIdentifier());
        assertEquals(ocflId, mapping1.getOcflObjectId());
    }

}