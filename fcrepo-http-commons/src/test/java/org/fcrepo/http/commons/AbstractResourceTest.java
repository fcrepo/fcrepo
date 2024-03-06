/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons;

import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import java.util.function.Supplier;

/**
 * <p>AbstractResourceTest class.</p>
 *
 * @author awoods
 */
@ExtendWith(MockitoExtension.class)
public class AbstractResourceTest {

    private AbstractResource testObj;

    @Mock
    private Supplier<String> mockPids;

    @Mock
    private UriInfo mockUris;

    @Mock
    private HttpHeaders mockHeaders;

    @BeforeEach
    public void setUp() {
        testObj = new AbstractResource() {/**/};
    }

    @Test
    public void testSetPidMinter() {
        setField(testObj, "pidMinter", mockPids);
        assertEquals(mockPids, testObj.pidMinter);
    }

    @Test
    public void testSetUriInfo() {
        setField(testObj, "uriInfo", mockUris);
        assertEquals(mockUris, testObj.uriInfo);
    }

    @Test
    public void testSetHeaders() {
        setField(testObj, "headers", mockHeaders);
        assertEquals(mockHeaders, testObj.headers);
    }

}
