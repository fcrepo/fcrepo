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
package org.fcrepo.http.commons.domain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.net.URI;

import org.fcrepo.kernel.api.services.ExternalContentService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.MultivaluedHashMap;

/**
 * @author cabeer
 */
public class ContentLocationMessageBodyReaderTest {


    private ContentLocationMessageBodyReader testObj;

    @Mock
    private InputStream mockInputStream;

    @Mock
    private ExternalContentService mockContentService;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new ContentLocationMessageBodyReader();
        testObj.setContentService(mockContentService);
    }

    @Test
    public void testReadFromURI() throws Exception {
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-Location", "http://localhost:8080/xyz");
        when(mockContentService.retrieveExternalContent(new URI("http://localhost:8080/xyz")))
            .thenReturn(mockInputStream);
        try (final InputStream actual = testObj.readFrom(InputStream.class, null, null, null, headers, null)) {
            assertEquals(mockInputStream, actual);
        }
    }

    @Test
    public void testReadFromRequestBody() throws Exception {
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        try (final InputStream actual =
                testObj.readFrom(InputStream.class, null, null, null, headers, mockInputStream)) {
            assertEquals(mockInputStream, actual);
        }

    }
}
