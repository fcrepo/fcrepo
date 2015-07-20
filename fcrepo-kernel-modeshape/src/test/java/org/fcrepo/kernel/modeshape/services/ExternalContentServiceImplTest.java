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
package org.fcrepo.kernel.modeshape.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpClientConnectionManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


/**
 * @author cabeer
 */
public class ExternalContentServiceImplTest {

    private ExternalContentServiceImpl testObj;

    @Mock
    private HttpClientConnectionManager mockClientPool;

    @Mock
    private org.apache.http.impl.client.CloseableHttpClient mockClient;

    private URI sourceUri;

    @Mock
    private CloseableHttpResponse mockResponse;

    @Mock
    private HttpEntity mockEntity;

    @Mock
    private InputStream mockInputStream;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        initMocks(this);
        sourceUri = new URI("http://localhost:8080/xyz");
        testObj = spy(new ExternalContentServiceImpl());
        testObj.setConnManager(mockClientPool);

        when(testObj.getCloseableHttpClient()).thenReturn(mockClient);
        when(mockClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(mockInputStream);
    }

    @Test
    public void testRetrieveExternalContent() throws Exception {
        try (final InputStream xyz = testObj.retrieveExternalContent(sourceUri)) {
            assertEquals(mockInputStream, xyz);
        }
    }
}
