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
package org.fcrepo.http.api;

import org.fcrepo.kernel.api.models.Tombstone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Session;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author cabeer
 */
public class FedoraTombstonesTest {

    @Mock
    private Tombstone mockResource;

    private String path = "/test/object";

    private FedoraTombstones testObj;

    @Mock
    private Session mockSession;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = spy(new FedoraTombstones(path));
        setField(testObj, "session", mockSession);
    }

    @Test
    public void testDelete() throws Exception {
        final Tombstone mockResource = mock(Tombstone.class);

        doReturn(mockResource).when(testObj).resource();

        final Response actual = testObj.delete();
        assertEquals(NO_CONTENT.getStatusCode(), actual.getStatus());
        verify(mockResource).delete();
        verify(mockSession).save();
    }
}
