/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.identifiers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.fcrepo.kernel.utils.TestHelpers.setField;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.client.methods.HttpUriRequest;

public class HttpPidMinterTest {

    private HttpPidMinter testMinter;

    @Before
    public void setUp() {
        testMinter = new HttpPidMinter();
        testMinter.setMinterURL("http://localhost/minter");
        testMinter.setMinterMethod("POST");
        testMinter.setTrimExpression(".*/");
    }

    @Test
    public void testMintPid() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse mockResponse = mock(HttpResponse.class);
        ByteArrayEntity entity = new ByteArrayEntity("/foo/bar/baz".getBytes());

        setField( testMinter, "client", mockClient );
        when(mockClient.execute(isA(HttpUriRequest.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(entity);

        final String pid = testMinter.mintPid();
        verify(mockClient).execute(isA(HttpUriRequest.class));
        assertEquals( pid, "baz" );
    }
}
