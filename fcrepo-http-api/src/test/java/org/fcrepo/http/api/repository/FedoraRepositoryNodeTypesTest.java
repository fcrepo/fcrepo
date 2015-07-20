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
package org.fcrepo.http.api.repository;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.InputStream;
import java.net.URI;

import javax.jcr.Session;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 *
 * @author cbeer
 */
public class FedoraRepositoryNodeTypesTest {

    private FedoraRepositoryNodeTypes testObj;

    @Mock
    private NodeService mockNodes;

    @Mock
    private InputStream mockInputStream;

    private final RdfStream mockRdfStream = new RdfStream();

    private Session mockSession;

    @Mock
    private UriInfo mockUriInfo;

    @Mock
    private UriBuilder mockUriBuilder;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraRepositoryNodeTypes();
        setField(testObj, "nodeService", mockNodes);
        setField(testObj, "uriInfo", getUriInfoImpl());
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
        when(mockUriInfo.getBaseUriBuilder()).thenReturn(mockUriBuilder);
        when(mockUriBuilder.path(any(Class.class))).thenReturn(mockUriBuilder);
        when(mockUriBuilder.build(any(String.class))).thenReturn(
                URI.create("mock:uri"));
    }

    @Test
    public void itShouldRetrieveNodeTypes() {
        when(mockNodes.getNodeTypes(mockSession)).thenReturn(mockRdfStream);

        final RdfStream nodeTypes = testObj.getNodeTypes();

        assertEquals("Got wrong triples!", mockRdfStream, nodeTypes);

    }

}
