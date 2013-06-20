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

package org.fcrepo;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.identifiers.PidMinter;
import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.PathSegmentImpl;
import org.fcrepo.test.util.TestHelpers;
import org.fcrepo.utils.NamespaceTools;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({NamespaceTools.class})
public class AbstractResourceTest {

    AbstractResource testObj;

    @Before
    public void setUp() {
        testObj = new AbstractResource() {

        };
    }

    @Test
    public void testInitialize() throws RepositoryException {
        NamespaceRegistry mockNames = mock(NamespaceRegistry.class);
        mockStatic(NamespaceTools.class);
        when(NamespaceTools.getNamespaceRegistry(any(Session.class)))
                .thenReturn(mockNames);
        testObj.initialize();
    }

    @Test
    public void testSetPidMinter()throws Exception {
        PidMinter mockPids = mock(PidMinter.class);
        TestHelpers.setField(testObj, "pidMinter", mockPids);
        assertEquals(mockPids, testObj.pidMinter);
    }

    @Test
    public void testSetNodeService() throws Exception{
        NodeService mockNodes = mock(NodeService.class);
        TestHelpers.setField(testObj, "nodeService", mockNodes);
        assertEquals(mockNodes, testObj.nodeService);
    }

    @Test
    public void testSetUriInfo() throws Exception{
        UriInfo mockUris = mock(UriInfo.class);
        TestHelpers.setField(testObj, "uriInfo", mockUris);
        assertEquals(mockUris, testObj.uriInfo);
    }

    @Test
    public void testToPath() {
        List<PathSegment> pathList =
                PathSegmentImpl.createPathList("foo", "", "bar", "baz");
        // empty path segments ('//') should be suppressed
        String expected = "/foo/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathWorkspace() {
        List<PathSegment> pathList =
                PathSegmentImpl.createPathList("workspace:abc", "bar", "baz");
        // workspaces should be ignored
        String expected = "/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathWorkspaceInSomeOtherSegment() {
        List<PathSegment> pathList =
                PathSegmentImpl.createPathList("asdf", "workspace:abc", "bar",
                        "baz");
        // workspaces should be ignored
        String expected = "/asdf/workspace:abc/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathWorkspaceWithEmptyPrefix() {
        List<PathSegment> pathList =
                PathSegmentImpl.createPathList("", "workspace:abc", "bar",
                        "baz");
        // workspaces should be ignored
        String expected = "/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathTransaction() {
        List<PathSegment> pathList =
                PathSegmentImpl.createPathList("tx:abc", "bar", "baz");
        // workspaces should be ignored
        String expected = "/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathTxInSomeOtherSegment() {
        List<PathSegment> pathList =
                PathSegmentImpl.createPathList("asdf", "tx:abc", "bar", "baz");
        // workspaces should be ignored
        String expected = "/asdf/tx:abc/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathTxWithEmptyPrefix() {
        List<PathSegment> pathList =
                PathSegmentImpl.createPathList("", "tx:abc", "bar", "baz");
        // workspaces should be ignored
        String expected = "/bar/baz";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathUuid() {
        List<PathSegment> pathList = PathSegmentImpl.createPathList("[foo]");
        String expected = "[foo]";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathEmpty() {
        List<PathSegment> pathList = PathSegmentImpl.createPathList();
        // empty path segments ('//') should be suppressed
        String expected = "/";
        String actual = AbstractResource.toPath(pathList);
        assertEquals(expected, actual);
    }
}
