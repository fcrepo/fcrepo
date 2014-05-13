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
package org.fcrepo.http.commons;

import static org.fcrepo.http.commons.AbstractResource.getSimpleContentType;
import static org.fcrepo.http.commons.AbstractResource.getJCRPath;
import static org.fcrepo.http.commons.AbstractResource.toPath;
import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.identifiers.PidMinter;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.utils.NamespaceTools;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.slf4j.*", "javax.xml.parsers.*", "org.apache.xerces.*"})
@PrepareForTest({NamespaceTools.class})
public class AbstractResourceTest {

    private AbstractResource testObj;

    @Mock
    private NodeService mockNodes;

    @Mock
    private PidMinter mockPids;

    @Mock
    private UriInfo mockUris;

    @Mock
    private NamespaceRegistry mockNames;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new AbstractResource() {};
    }

    @Test
    public void testSetPidMinter() throws Exception {
        setField(testObj, "pidMinter", mockPids);
        assertEquals(mockPids, testObj.pidMinter);
    }

    @Test
    public void testSetNodeService() throws Exception {
        setField(testObj, "nodeService", mockNodes);
        assertEquals(mockNodes, testObj.nodeService);
    }

    @Test
    public void testSetUriInfo() throws Exception {
        setField(testObj, "uriInfo", mockUris);
        assertEquals(mockUris, testObj.uriInfo);
    }

    @Test
    public void testToPath() {
        final List<PathSegment> pathList =
                createPathList("foo", "", "bar", "baz");
        // empty path segments ('//') should be suppressed
        final String expected = "/foo/bar/baz";
        final String actual = toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathWorkspace() {
        final List<PathSegment> pathList =
                createPathList("workspace:abc", "bar", "baz");
        // workspaces should be ignored
        final String expected = "/bar/baz";
        final String actual = toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathWorkspaceInSomeOtherSegment() {
        final List<PathSegment> pathList =
                createPathList("asdf", "workspace:abc", "bar", "baz");
        // workspaces should be ignored
        final String expected = "/asdf/workspace:abc/bar/baz";
        final String actual = toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathWorkspaceWithEmptyPrefix() {
        final List<PathSegment> pathList =
                createPathList("", "workspace:abc", "bar", "baz");
        // workspaces should be ignored
        final String expected = "/bar/baz";
        final String actual = toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathTransaction() {
        final List<PathSegment> pathList =
                createPathList("tx:abc", "bar", "baz");
        // workspaces should be ignored
        final String expected = "/bar/baz";
        final String actual = toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathTxInSomeOtherSegment() {
        final List<PathSegment> pathList =
                createPathList("asdf", "tx:abc", "bar", "baz");
        // workspaces should be ignored
        final String expected = "/asdf/tx:abc/bar/baz";
        final String actual = toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathTxWithEmptyPrefix() {
        final List<PathSegment> pathList =
                createPathList("", "tx:abc", "bar", "baz");
        // workspaces should be ignored
        final String expected = "/bar/baz";
        final String actual = toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathUuid() {
        final List<PathSegment> pathList = createPathList("[foo]");
        final String expected = "[foo]";
        final String actual = toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testToPathEmpty() {
        final List<PathSegment> pathList = createPathList();
        // empty path segments ('//') should be suppressed
        final String expected = "/";
        final String actual = toPath(pathList);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetSimpleContentType() {
        final MediaType mediaType = new MediaType("text", "plain", ImmutableMap.of("charset", "UTF-8"));
        final MediaType sanitizedMediaType = getSimpleContentType(mediaType);

        assertEquals("text/plain", sanitizedMediaType.toString());
    }

    @Test
    public void testGetJCRPath() throws RepositoryException, URISyntaxException {
        Resource mockResource = mock(Resource.class);
        IdentifierTranslator idTranslator = mock(IdentifierTranslator.class);
        when(idTranslator.getPathFromSubject(mockResource)).thenReturn("some-path");

        final String result = getJCRPath(mockResource, idTranslator);
        assertEquals("some-path", result);
    }
}
