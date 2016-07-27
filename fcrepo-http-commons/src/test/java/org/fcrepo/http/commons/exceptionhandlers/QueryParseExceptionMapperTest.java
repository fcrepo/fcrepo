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
package org.fcrepo.http.commons.exceptionhandlers;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import org.apache.jena.query.QueryParseException;

/**
 * @author whikloj
 * @since September 10, 2014
 */
public class QueryParseExceptionMapperTest {

    private QueryParseExceptionMapper testObj;

    @Before
    public void setUp() {
        testObj = new QueryParseExceptionMapper();
    }

    @Test
    public void testInvalidNamespace() {
        final QueryParseException input = new QueryParseException(
            "Unresolved prefixed name: invalidNS:title", 14, 10);
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
        assertEquals(actual.getEntity(), input.getMessage());
    }

    @Test
    public void testToResponse() {
        final QueryParseException input = new QueryParseException("An error occurred", 14, 10);
        final Response actual = testObj.toResponse(input);
        assertEquals(BAD_REQUEST.getStatusCode(), actual.getStatus());
        assertNotNull(actual.getEntity());
    }
}