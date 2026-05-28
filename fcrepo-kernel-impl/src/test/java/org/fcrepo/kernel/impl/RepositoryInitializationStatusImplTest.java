/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author bbpennel
 */
public class RepositoryInitializationStatusImplTest {

    private RepositoryInitializationStatusImpl status;

    @BeforeEach
    void setUp() {
        status = new RepositoryInitializationStatusImpl();
    }

    @Test
    void testInitialStateIsFalse() {
        assertFalse(status.isInitializationComplete());
    }

    @Test
    void testSetInitializationCompleteToTrue() {
        status.setInitializationComplete(true);
        assertTrue(status.isInitializationComplete());
    }

    @Test
    void testMultipleToggles() {
        status.setInitializationComplete(true);
        assertTrue(status.isInitializationComplete());

        status.setInitializationComplete(false);
        assertFalse(status.isInitializationComplete());

        status.setInitializationComplete(true);
        assertTrue(status.isInitializationComplete());
    }
}
