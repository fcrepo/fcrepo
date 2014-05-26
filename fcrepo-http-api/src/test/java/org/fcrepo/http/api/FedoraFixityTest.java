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
package org.fcrepo.http.api;

import static javax.jcr.PropertyType.PATH;
import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockDatastream;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>FedoraFixityTest class.</p>
 *
 * @author awoods
 */
public class FedoraFixityTest {

    FedoraFixity testObj;

    @Mock
    private DatastreamService mockDatastreams;

    Session mockSession;

    private UriInfo uriInfo;

    @Mock
    private Request mockRequest;

    @Mock
    private Node mockNode;

    @Mock
    private Value mockValue;

    @Mock
    private ValueFactory mockValueFactory;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraFixity();
        setField(testObj, "datastreamService", mockDatastreams);
        this.uriInfo = getUriInfoImpl();
        setField(testObj, "uriInfo", uriInfo);
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
    }

    @Test
    public void testGetDatastreamFixity() throws RepositoryException {
        final String pid = "FedoraDatastreamsTest1";
        final String path = "/objects/" + pid + "/testDS";
        final String dsId = "testDS";
        final Datastream mockDs = mockDatastream(pid, dsId, null);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockDs.getNode()).thenReturn(mockNode);
        when(mockDatastreams.getDatastream(mockSession, path)).thenReturn(
                mockDs);
        when(mockDatastreams.getFixityResultsModel(any(IdentifierTranslator.class),
                eq(mockDs))).thenReturn(new RdfStream());
        testObj.getDatastreamFixity(createPathList("objects", pid, "testDS"),
                mockRequest, uriInfo);
        verify(mockDatastreams).getFixityResultsModel(any(IdentifierTranslator.class),
                eq(mockDs));
    }
}
