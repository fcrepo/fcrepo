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

import com.hp.hpl.jena.graph.Triple;
import org.fcrepo.kernel.Lock;
import org.fcrepo.kernel.services.LockService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URISyntaxException;
import java.util.UUID;

import static javax.jcr.PropertyType.PATH;
import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.fcrepo.kernel.RdfLexicon.HAS_LOCK_TOKEN;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Mike Durbin
 */
public class FedoraLocksTest {

    FedoraLocks testObj;

    @Mock
    private LockService mockLockService;

    @Mock
    private Lock mockLock;

    @Mock
    private Node mockNode;

    @Mock
    private NodeType mockNodeType;

    Session mockSession;

    private UriInfo mockUriInfo;

    @Mock
    private Value mockValue;

    @Mock
    private ValueFactory mockValueFactory;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraLocks();
        setField(testObj, "lockService", mockLockService);
        this.mockUriInfo = getUriInfoImpl();
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
        when(mockLock.getLockToken()).thenReturn("token");
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockValueFactory.createValue("a", PATH)).thenReturn(mockValue);
    }

    @Test
    public void testGetLock() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final String path = "/" + pid;
        initializeMockNode(path);
        when(mockLockService.getLock(mockSession, path)).thenReturn(mockLock);

        testObj.getLock(createPathList(pid));

        verify(mockLockService).getLock(mockSession, path);
    }

    @Test
    public void testCreateLock() throws RepositoryException, URISyntaxException {
        final String pid = UUID.randomUUID().toString();
        final String path = "/" + pid;
        initializeMockNode(path);
        when(mockLockService.acquireLock(mockSession, path, false)).thenReturn(mockLock);

        final Response response = testObj.createLock(createPathList(pid), false);

        verify(mockLockService).acquireLock(mockSession, path, false);
        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeleteLock() throws RepositoryException, URISyntaxException {
        final String pid = UUID.randomUUID().toString();
        final String path = "/" + pid;
        initializeMockNode(path);

        final Response response = testObj.deleteLock(createPathList(pid));

        verify(mockLockService).releaseLock(mockSession, path);
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRDFGenerationForLockToken() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final String path = "/" + pid;
        initializeMockNode(path);
        when(mockLockService.getLock(mockSession, path)).thenReturn(mockLock);

        final RdfStream stream = testObj.getLock(createPathList(pid));
        while (stream.hasNext()) {
            final Triple t = stream.next();
            if (t.getPredicate().getURI().equals(HAS_LOCK_TOKEN.getURI())
                    && t.getObject().getLiteralValue().equals(mockLock.getLockToken())) {
                return;
            }
        }
        fail("Unable to find the lock token in the returned RDF!");
    }

    private void initializeMockNode(final String path) throws RepositoryException {
        when(mockNode.getPath()).thenReturn(path);
        when(mockSession.getNode(path)).thenReturn(mockNode);
    }

}
