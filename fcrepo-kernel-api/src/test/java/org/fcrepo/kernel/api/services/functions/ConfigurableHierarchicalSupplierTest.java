/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services.functions;

import static org.junit.Assert.assertEquals;
import org.fcrepo.config.FedoraPropsConfig;
import org.junit.Before;;

import org.junit.Test;

/**
 * <p>
 * ConfigurableHierarchicalSupplierTest class.
 * </p>
 *
 * @author rdfloyd
 */
public class ConfigurableHierarchicalSupplierTest {

    private FedoraPropsConfig fedoraPropsConfig;

    @Before
    public void setUp() {
        fedoraPropsConfig = new FedoraPropsConfig();
    }

    @Test
    public void testGet() {
        final UniqueValueSupplier defaultPidMinter = new ConfigurableHierarchicalSupplier();
        final String id = defaultPidMinter.get();
        // No pairtrees is default; with no args check to see that id contains just the 1 pid part
        final int parts = (id.split("/").length);
        assertEquals(1, parts);
    }

    @Test
    public void testGetIdNoPairtree() {
        final UniqueValueSupplier defaultPidMinter = new ConfigurableHierarchicalSupplier();
        final String id = defaultPidMinter.get();
        // With (desiredLength,desiredCount=0), check to see that id contains 1 part and no slashes
        final int parts = (id.split("/").length);
        assertEquals(1, parts);
    }

    @Test
    public void testGetIdPairtreeParams() {
        fedoraPropsConfig.setFcrepoPidMinterLength(2);
        fedoraPropsConfig.setFcrepoPidMinterCount(4);
        final UniqueValueSupplier defaultPidMinter = new ConfigurableHierarchicalSupplier();
        final String id = defaultPidMinter.get();
        // With (desiredLength > 0 && desiredCount > 0) check to see that id contains (count + 1) parts
        final int parts = (id.split("/").length);
        assertEquals(5, parts);
    }

}
