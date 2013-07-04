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

import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.Datastream;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;
import org.junit.Test;

public class FedoraFixityTest {

    FedoraFixity testObj;

    DatastreamService mockDatastreams;

    Session mockSession;

    private UriInfo uriInfo;

    private Request mockRequest;

    @Before
    public void setUp() throws Exception {

        mockRequest = mock(Request.class);
        mockDatastreams = mock(DatastreamService.class);

        testObj = new FedoraFixity();
        TestHelpers.setField(testObj, "datastreamService", mockDatastreams);
        this.uriInfo = TestHelpers.getUriInfoImpl();
        TestHelpers.setField(testObj, "uriInfo", uriInfo);
        mockSession = TestHelpers.mockSession(testObj);
        TestHelpers.setField(testObj, "session", mockSession);
    }

    @Test
    public void testGetDatastreamFixity() throws RepositoryException,
            IOException {
        final String pid = "FedoraDatastreamsTest1";
        final String path = "/objects/" + pid + "/testDS";
        final String dsId = "testDS";
        final Datastream mockDs = TestHelpers.mockDatastream(pid, dsId, null);
        final Node mockNode = mock(Node.class);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockDs.getNode()).thenReturn(mockNode);
        when(mockDatastreams.getDatastream(mockSession, path)).thenReturn(
                mockDs);
        testObj.getDatastreamFixity(createPathList("objects", pid, "testDS"),
                mockRequest, uriInfo);
        verify(mockDatastreams).getFixityResultsModel(any(GraphSubjects.class),
                eq(mockDs));
    }
}
