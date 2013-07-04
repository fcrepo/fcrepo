/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.api;

import static com.hp.hpl.jena.query.DatasetFactory.createMem;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class FedoraFieldSearchTest {

    FedoraFieldSearch testObj;

    Session mockSession;

    private NodeService mockNodeService;

    private UriInfo uriInfo;

    @Before
    public void setUp() throws Exception {
        testObj = new FedoraFieldSearch();
        mockSession = TestHelpers.mockSession(testObj);
        mockNodeService = mock(NodeService.class);
        this.uriInfo = TestHelpers.getUriInfoImpl();
        TestHelpers.setField(testObj, "uriInfo", uriInfo);
        TestHelpers.setField(testObj, "nodeService", mockNodeService);
        TestHelpers.setField(testObj, "session", mockSession);
    }

    @After
    public void tearDown() {

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFieldSearch() throws RepositoryException,
            URISyntaxException {

        final Request mockRequest = mock(Request.class);

        when(uriInfo.getRequestUri()).thenReturn(
                new URI("http://localhost/fcrepo/path/to/query/endpoint"));
        when(mockRequest.selectVariant(any(List.class))).thenReturn(
                new Variant(MediaType.valueOf("application/n-triples"), null,
                        null));

        when(
                mockNodeService
                        .searchRepository(
                                any(GraphSubjects.class),
                                eq(createResource("http://localhost/fcrepo/path/to/query/endpoint")),
                                eq(mockSession), eq("ZZZ"), eq(0), eq(0L)))
                .thenReturn(createMem());
        final UriBuilder mockUriBuilder = mock(UriBuilder.class);

        when(mockUriBuilder.path(FedoraFieldSearch.class)).thenReturn(
                mockUriBuilder);
        when(mockUriBuilder.buildFromMap(any(Map.class))).thenReturn(
                new URI("path/to/object"));

        when(uriInfo.getRequestUriBuilder()).thenReturn(mockUriBuilder);

        testObj.searchSubmitRdf("ZZZ", 0, 0, mockRequest, uriInfo);

        verify(mockNodeService)
                .searchRepository(
                        any(GraphSubjects.class),
                        eq(ResourceFactory
                                .createResource("http://localhost/fcrepo/path/to/query/endpoint")),
                        eq(mockSession), eq("ZZZ"), eq(0), eq(0L));
    }

}
