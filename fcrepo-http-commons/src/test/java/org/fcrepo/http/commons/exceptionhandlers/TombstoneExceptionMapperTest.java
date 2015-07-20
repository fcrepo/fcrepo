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

import org.fcrepo.kernel.api.exception.TombstoneException;
import org.fcrepo.kernel.api.models.Tombstone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.GONE;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 */
public class TombstoneExceptionMapperTest {

    private ExceptionMapper<TombstoneException> testObj;

    @Mock
    public Tombstone mockTombstone;

    @Before
    public void setUp() {
        initMocks(this);

        testObj = new TombstoneExceptionMapper();
    }

    @Test
    public void testUrilessException() {
        final Response response = testObj.toResponse(new TombstoneException(mockTombstone));
        assertEquals(GONE.getStatusCode(), response.getStatus());
    }

    @Test
    public void testExceptionWithUri() {
        final Response response = testObj.toResponse(new TombstoneException(mockTombstone, "some:uri"));
        assertEquals(GONE.getStatusCode(), response.getStatus());
        final Link link = Link.valueOf(response.getHeaderString("Link"));
        assertEquals("some:uri", link.getUri().toString());
        assertEquals("hasTombstone", link.getRel());
    }

}
