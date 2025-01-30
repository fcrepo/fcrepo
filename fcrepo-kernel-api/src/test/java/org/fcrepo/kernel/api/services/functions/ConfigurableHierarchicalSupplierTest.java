/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.fcrepo.config.FedoraPropsConfig;;

/**
 * <p>
 * ConfigurableHierarchicalSupplierTest class.
 * </p>
 *
 * @author rdfloyd
 */
public class ConfigurableHierarchicalSupplierTest {

    private final FedoraPropsConfig propsConfig = new FedoraPropsConfig();

    private final UniqueValueSupplier defaultPidMinter = new ConfigurableHierarchicalSupplier();

    @BeforeEach
    public void setUp() {
        // Need to set the defaults
        propsConfig.setFcrepoPidMinterLength(0);
        propsConfig.setFcrepoPidMinterCount(0);
        setField(defaultPidMinter, "fedoraPropsConfig", propsConfig);
    }

    @Test
    public void testGetIdNoPairtree() {
        final String id = defaultPidMinter.get();
        // With (desiredLength,desiredCount=0), check to see that id contains 1 part and no slashes
        final int parts = (id.split("/").length);
        assertEquals(1, parts);
    }

    @Test
    public void testGetIdPairtreeParams() {
        // Alter the settings for this test.
        propsConfig.setFcrepoPidMinterLength(2);
        propsConfig.setFcrepoPidMinterCount(4);
        final String id = defaultPidMinter.get();
        // With (desiredLength > 0 && desiredCount > 0) check to see that id contains (count + 1) parts
        final int parts = (id.split("/").length);
        assertEquals(5, parts);
    }

}
