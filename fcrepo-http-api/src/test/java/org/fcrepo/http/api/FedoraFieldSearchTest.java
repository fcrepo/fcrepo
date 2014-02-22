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

package org.fcrepo.http.api;

import static com.hp.hpl.jena.query.DatasetFactory.createMem;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.services.RepositoryService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

@Ignore("Destined for death.")
public class FedoraFieldSearchTest {

    private FedoraFieldSearch testObj;

    private Session mockSession;

    @Mock
    private Request mockRequest;

    @Mock
    private RepositoryService mockService;

    private UriInfo uriInfo;

    @Mock
    private UriBuilder mockUriBuilder;

    @Mock
    private HttpServletResponse mockResponse;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraFieldSearch();
        mockSession = mockSession(testObj);
        this.uriInfo = getUriInfoImpl();
        setField(testObj, "uriInfo", uriInfo);
        setField(testObj, "repositoryService", mockService);
        setField(testObj, "session", mockSession);
    }

    @Test
    public void testFieldSearch() throws RepositoryException {
        when(mockRequest.selectVariant(anyListOf(Variant.class))).thenReturn(
                new Variant(MediaType.valueOf("application/n-triples"), null,
                        null));
        when(
                mockService
                        .searchRepository(
                                any(GraphSubjects.class),
                                eq(createResource("http://localhost/fcrepo/fcr:search?q=ZZZ")),
                                eq(mockSession), eq("ZZZ"), eq(0), eq(0L)))
                .thenReturn(createMem());
        when(uriInfo.getRequestUriBuilder()).thenReturn(mockUriBuilder);

        testObj.searchSubmitRdf("ZZZ", 0, 0, mockRequest, mockResponse, uriInfo);

        verify(mockService)
                .searchRepository(
                        any(GraphSubjects.class),
                eq(createResource("http://localhost/fcrepo/fcr:search?q=ZZZ")),
                        eq(mockSession), eq("ZZZ"), eq(0), eq(0L));
    }

}
