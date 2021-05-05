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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.UUID;

import org.fcrepo.config.FlywayFactory;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.ReadOnlyTransaction;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @author dbernstein
 * @since 6.0.0
 */
public class DbFedoraToOcflObjectIndexTest {

    private FedoraId RESOURCE_ID_1;
    private FedoraId RESOURCE_ID_2;
    private FedoraId RESOURCE_ID_3;
    private FedoraId ROOT_RESOURCE_ID;
    private String OCFL_ID;
    private String OCFL_ID_RESOURCE_3;

    private static DriverManagerDataSource dataSource;
    private static DbFedoraToOcflObjectIndex index;

    private Transaction session;
    private Transaction readOnlyTx;

    private static OcflPropsConfig propsConfig;

    @BeforeClass
    public static void beforeClass() throws Exception {
        dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.jdbcx.JdbcDataSource");
        dataSource.setUrl("jdbc:h2:mem:index;DB_CLOSE_DELAY=-1");
        FlywayFactory.create().setDataSource(dataSource).setDatabaseType("h2").getObject();
        propsConfig = mock(OcflPropsConfig.class);
        when(propsConfig.getFedoraToOcflCacheSize()).thenReturn(2L);
        when(propsConfig.getFedoraToOcflCacheTimeout()).thenReturn(1L);
        index = new DbFedoraToOcflObjectIndex(dataSource);
        setField(index, "ocflPropsConfig", propsConfig);
        index.setup();
    }

    @Before
    public void setup() {
        index.reset();
        session = Mockito.mock(Transaction.class);
        when(session.getId()).thenReturn(UUID.randomUUID().toString());
        when(session.isShortLived()).thenReturn(false);
        when(session.isCommitted()).thenReturn(false);
        when(session.isOpenLongRunning()).thenReturn(true);
        readOnlyTx = ReadOnlyTransaction.INSTANCE;
        ROOT_RESOURCE_ID = FedoraId.create(getRandomUUID());
        RESOURCE_ID_1 = ROOT_RESOURCE_ID.resolve(getRandomUUID());
        RESOURCE_ID_2 = ROOT_RESOURCE_ID.resolve(getRandomUUID());
        RESOURCE_ID_3 = FedoraId.create(getRandomUUID());
        OCFL_ID = "ocfl-id-" + getRandomUUID();
        OCFL_ID_RESOURCE_3 = "ocfl-id-resource-3-" + getRandomUUID();
    }

    private static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    @Test
    public void test() throws Exception {
        index.addMapping(session, ROOT_RESOURCE_ID, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(session, RESOURCE_ID_1, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(session, RESOURCE_ID_2, ROOT_RESOURCE_ID, OCFL_ID);
        index.addMapping(session, RESOURCE_ID_3, RESOURCE_ID_3, OCFL_ID_RESOURCE_3);

        final FedoraOcflMapping mapping1 = index.getMapping(session, RESOURCE_ID_1);
        final FedoraOcflMapping mapping2 = index.getMapping(session, RESOURCE_ID_2);
        final FedoraOcflMapping mapping3 = index.getMapping(session, ROOT_RESOURCE_ID);

        assertEquals(mapping1, mapping2);
        assertEquals(mapping2, mapping3);

        verifyMapping(mapping1, ROOT_RESOURCE_ID, OCFL_ID);

        index.commit(session);

        final FedoraOcflMapping mapping1_1 = index.getMapping(readOnlyTx, RESOURCE_ID_1);
        final FedoraOcflMapping mapping1_2 = index.getMapping(readOnlyTx, RESOURCE_ID_2);
        final FedoraOcflMapping mapping1_3 = index.getMapping(readOnlyTx, ROOT_RESOURCE_ID);

        assertEquals(mapping1_1, mapping1_2);
        assertEquals(mapping1_2, mapping1_3);

        final FedoraOcflMapping mapping4 = index.getMapping(readOnlyTx, RESOURCE_ID_3);
        assertNotEquals(mapping4, mapping3);

        verifyMapping(mapping4, RESOURCE_ID_3, OCFL_ID_RESOURCE_3);

        assertEquals(ROOT_RESOURCE_ID, mapping1.getRootObjectIdentifier());
        assertEquals(OCFL_ID, mapping1.getOcflObjectId());
    }

    @Test(expected = FedoraOcflMappingNotFoundException.class)
    public void testNotExists() throws Exception {
        index.getMapping(readOnlyTx, RESOURCE_ID_1);
    }

    @Test
    public void removeIndexWhenExists() throws FedoraOcflMappingNotFoundException {
        final var mapping = index.addMapping(session, RESOURCE_ID_1, RESOURCE_ID_1, OCFL_ID);

        assertEquals(mapping, index.getMapping(session, RESOURCE_ID_1));
        try {
            index.getMapping(readOnlyTx, RESOURCE_ID_1);
            fail("This mapping should not be accessible to everyone yet.");
        } catch (final FedoraOcflMappingNotFoundException e) {
            // This should happen and is okay.
        }
        index.commit(session);

        assertEquals(mapping, index.getMapping(readOnlyTx, RESOURCE_ID_1));

        index.removeMapping(session, RESOURCE_ID_1);

        // Should still appear to outside the transaction.
        assertEquals(mapping, index.getMapping(readOnlyTx, RESOURCE_ID_1));

        // Should also still appear inside the transaction or we can't access files on disk.
        assertEquals(mapping, index.getMapping(session, RESOURCE_ID_1));

        index.commit(session);

        try {
            // No longer accessible to anyone.
            index.getMapping(readOnlyTx, RESOURCE_ID_1);
            fail("expected exception");
        } catch (final FedoraOcflMappingNotFoundException e) {
            // expected mapping to not exist
        }
    }

    @Test
    public void removeIndexWhenNotExists() {
        index.removeMapping(session, RESOURCE_ID_1);
        index.commit(session);

        try {
            index.getMapping(readOnlyTx, RESOURCE_ID_1);
            fail("expected exception");
        } catch (final FedoraOcflMappingNotFoundException e) {
            // expected mapping to not exist
        }
    }

    @Test
    public void testRollback() throws Exception {
        index.addMapping(session, RESOURCE_ID_1, ROOT_RESOURCE_ID, OCFL_ID);

        final FedoraOcflMapping expected = new FedoraOcflMapping(ROOT_RESOURCE_ID, OCFL_ID);
        assertEquals(expected, index.getMapping(session, RESOURCE_ID_1));

        try {
            // Not yet committed
            index.getMapping(readOnlyTx, RESOURCE_ID_1);
            fail();
        } catch (final FedoraOcflMappingNotFoundException e) {
            // The exception is expected.
        }

        index.rollback(session);

        try {
            index.getMapping(session, RESOURCE_ID_1);
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
            index.addMapping(session, tempid, ROOT_RESOURCE_ID, OCFL_ID);
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