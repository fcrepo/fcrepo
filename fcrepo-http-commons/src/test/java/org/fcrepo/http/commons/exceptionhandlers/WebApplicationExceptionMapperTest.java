/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.junit.Assert.assertEquals;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

/**
 * <p>WebApplicationExceptionMapperTest class.</p>
 *
 * @author lsitu
 * @author awoods
 */
public class WebApplicationExceptionMapperTest {

    private WebApplicationExceptionMapper testObj;

    @Before
    public void setUp() {
        testObj = new WebApplicationExceptionMapper();
    }

    @Test
    public void testToResponse() {
        final WebApplicationException input = new WebApplicationException();
        final Response actual = testObj.toResponse(input);
        assertEquals(input.getResponse().getStatus(), actual.getStatus());
    }
}
