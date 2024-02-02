/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons;

import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.function.Supplier;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>AbstractResourceTest class.</p>
 *
 * @author awoods
 */
public class AbstractResourceTest {

    private AbstractResource testObj;

    @Mock
    private Supplier<String> mockPids;

    @Mock
    private UriInfo mockUris;

    @Mock
    private HttpHeaders mockHeaders;

    @Before
    public void setUp() {
        initMocks(this);
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
