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
import static org.fcrepo.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.test.util.TestHelpers.mockSession;
import static org.fcrepo.test.util.TestHelpers.setField;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class FedoraFieldSearchTest {

    private FedoraFieldSearch testObj;

    private Session mockSession;

    @Mock
    private Request mockRequest;

    @Mock
    private NodeService mockNodeService;

    private UriInfo uriInfo;

    @Mock
    private UriBuilder mockUriBuilder;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraFieldSearch();
        mockSession = mockSession(testObj);
        this.uriInfo = getUriInfoImpl();
        setField(testObj, "uriInfo", uriInfo);
        setField(testObj, "nodeService", mockNodeService);
        setField(testObj, "session", mockSession);
    }

    @Test
    public void testFieldSearch() throws RepositoryException {
        when(uriInfo.getRequestUri()).thenReturn(
                URI.create("http://localhost/fcrepo/path/to/query/endpoint"));
        when(mockRequest.selectVariant(anyListOf(Variant.class))).thenReturn(
                new Variant(MediaType.valueOf("application/n-triples"), null,
                        null));
        when(
                mockNodeService
                        .searchRepository(
                                any(GraphSubjects.class),
                                eq(createResource("http://localhost/fcrepo/path/to/query/endpoint")),
                                eq(mockSession), eq("ZZZ"), eq(0), eq(0L)))
                .thenReturn(createMem());
        when(mockUriBuilder.path(FedoraFieldSearch.class)).thenReturn(
                mockUriBuilder);
        when(mockUriBuilder.buildFromMap(Matchers.<Map<String, Object>> any()))
                .thenReturn(URI.create("path/to/object"));

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
