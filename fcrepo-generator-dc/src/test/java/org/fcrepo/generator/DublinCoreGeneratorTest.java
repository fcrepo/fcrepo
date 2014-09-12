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
package org.fcrepo.generator;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.fcrepo.http.commons.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.generator.dublincore.DCGenerator;
import org.fcrepo.kernel.impl.FedoraResourceImpl;
import org.fcrepo.kernel.services.NodeService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>DublinCoreGeneratorTest class.</p>
 *
 * @author cbeer
 */
public class DublinCoreGeneratorTest {

    private DublinCoreGenerator testObj;

    @Mock
    private NodeService mockNodeService;

    @Mock
    private DCGenerator mockGenerator;

    private Session mockSession;

    @Mock
    private InputStream mockIS;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new DublinCoreGenerator();
        setField(testObj, "nodeService", mockNodeService);
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
        testObj.dcgenerators = asList(mockGenerator);
    }

    @Test
    public void testGetObjectAsDublinCore() throws RepositoryException {
        final FedoraResourceImpl mockResource = mock(FedoraResourceImpl.class);
        when(mockResource.getNode()).thenReturn(mock(Node.class));
        when(mockNodeService.getObject(mockSession, "/objects/foo"))
                .thenReturn(mockResource);
        when(mockGenerator.getStream(any(Node.class))).thenReturn(mockIS);
        testObj.getObjectAsDublinCore(createPathList("objects", "foo"));

    }

    @Test
    public void testNoGenerators() {
        testObj.dcgenerators = emptyList();
        try {
            testObj.getObjectAsDublinCore(createPathList("objects", "foo"));
            fail("Should have failed without a generator configured!");
        } catch (final PathNotFoundException ex) {
            // this is what we expect
        } catch (final RepositoryException e) {
            fail("unexpected RepositoryException: " + e.getMessage());
        }
    }
}
