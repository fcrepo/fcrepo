/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.config.FlywayFactory;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.ReadOnlyTransaction;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.InvalidResourceIdentifierException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.persistence.ocfl.api.FedoraOcflMappingNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

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

    @BeforeAll
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

    @BeforeEach
    public void setup() {
        index.reset();
        session = Mockito.mock(Transaction.class);
        when(session.getId()).thenReturn(UUID.randomUUID().toString());
        when(session.isShortLived()).thenReturn(false);
        when(session.isCommitted()).thenReturn(false);
        when(session.isOpenLongRunning()).thenReturn(true);
        doAnswer(invocationOnMock -> {
            invocationOnMock.getArgument(0, Runnable.class).run();
            return null;
        }).when(session).doInTx(any(Runnable.class));
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

    @Test
    public void testNotExists() throws Exception {
        assertThrows(FedoraOcflMappingNotFoundException.class, () -> index.getMapping(readOnlyTx, RESOURCE_ID_1));
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

        // No longer accessible to anyone, expect mapping to not exist
        assertThrows(FedoraOcflMappingNotFoundException.class, () -> index.getMapping(readOnlyTx, RESOURCE_ID_1));
    }

    @Test
    public void removeIndexWhenNotExists() {
        index.removeMapping(session, RESOURCE_ID_1);
        index.commit(session);

        // expect mapping to not exist
        assertThrows(FedoraOcflMappingNotFoundException.class, () -> index.getMapping(readOnlyTx, RESOURCE_ID_1));
    }

    @Test
    public void testRollback() throws Exception {
        index.addMapping(session, RESOURCE_ID_1, ROOT_RESOURCE_ID, OCFL_ID);

        final FedoraOcflMapping expected = new FedoraOcflMapping(ROOT_RESOURCE_ID, OCFL_ID);
        assertEquals(expected, index.getMapping(session, RESOURCE_ID_1));

        // Not yet committed, exception expected
        assertThrows(FedoraOcflMappingNotFoundException.class, () -> index.getMapping(readOnlyTx, RESOURCE_ID_1));

        index.rollback(session);

        assertThrows(FedoraOcflMappingNotFoundException.class, () -> index.getMapping(session, RESOURCE_ID_1));
    }

    @Test
    public void identifierTooLong() throws InvalidResourceIdentifierException {
        int cnt;
        String root = "info:fedora/longtest";
        for (cnt = 0; cnt < 149; cnt++) {
            root = root + "/" + String.valueOf(cnt);
        }
        final var finalRoot = root;
        assertThrows(InvalidResourceIdentifierException.class, () -> {
            final FedoraId tempid = FedoraId.create(finalRoot);
            index.addMapping(session, tempid, ROOT_RESOURCE_ID, OCFL_ID);
        });
    }

    @Test
    public void testClearAllTransactions() throws Exception {
        final var mapping = index.addMapping(session, RESOURCE_ID_1, RESOURCE_ID_1, OCFL_ID);
        index.commit(session);

        final var mapping2 = index.addMapping(session, RESOURCE_ID_2, RESOURCE_ID_2, OCFL_ID);

        assertEquals(mapping, index.getMapping(session, RESOURCE_ID_1));
        index.getMapping(readOnlyTx, RESOURCE_ID_1);
        assertEquals(mapping2, index.getMapping(session, RESOURCE_ID_2));
        assertThrows(FedoraOcflMappingNotFoundException.class, () -> index.getMapping(readOnlyTx, RESOURCE_ID_2));

        index.clearAllTransactions();

        index.getMapping(readOnlyTx, RESOURCE_ID_1);
        // resource 2 only existed in a TX, so it shouldn't be accessible anymore in a session or out of it
        assertThrows(FedoraOcflMappingNotFoundException.class, () -> index.getMapping(session, RESOURCE_ID_2));
        assertThrows(FedoraOcflMappingNotFoundException.class, () -> index.getMapping(readOnlyTx, RESOURCE_ID_2));

    }

    private void verifyMapping(final FedoraOcflMapping mapping1, final FedoraId rootResourceId, final String ocflId) {
        assertEquals(rootResourceId, mapping1.getRootObjectIdentifier());
        assertEquals(ocflId, mapping1.getOcflObjectId());
    }

}