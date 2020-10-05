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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Providers;

import org.fcrepo.kernel.api.exception.RepositoryException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * @author cabeer
 */
public class RepositoryRuntimeExceptionMapperTest {

    @Mock
    private Providers mockProviders;

    private RepositoryRuntimeExceptionMapper testObj;

    @Mock
    private ExceptionMapper<RepositoryException> mockProvider;

    @Before
    public void setUp() {
        initMocks(this);

        testObj = new RepositoryRuntimeExceptionMapper(mockProviders);
    }

    @Test
    public void testToResponseWithHandledRepositoryException() {
        when(mockProviders.getExceptionMapper(RepositoryException.class)).thenReturn(mockProvider);
        final RepositoryException cause = new RepositoryException("xyz");
        final RepositoryRuntimeException ex = new RepositoryRuntimeException(cause.getMessage(), cause);
        testObj.toResponse(ex);
        verify(mockProvider).toResponse(cause);
    }

    @Test
    public void testToResponseWithUnhandledRepositoryException() {
        when(mockProviders.getExceptionMapper(Exception.class)).thenReturn(null);
        final Exception cause = new Exception("xyz");
        final RepositoryRuntimeException ex = new RepositoryRuntimeException(cause.getMessage(), cause);
        final Response response = testObj.toResponse(ex);
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testToResponseWithNoWrappedException() {
        when(mockProviders.getExceptionMapper(Exception.class)).thenReturn(null);
        final RepositoryRuntimeException ex = new RepositoryRuntimeException("!");
        final Response response = testObj.toResponse(ex);
        assertEquals(500, response.getStatus());
    }
}
