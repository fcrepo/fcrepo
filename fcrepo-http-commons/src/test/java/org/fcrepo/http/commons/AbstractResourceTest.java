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
package org.fcrepo.http.commons;

import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.function.Supplier;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

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
