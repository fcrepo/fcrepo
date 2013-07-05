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

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.RdfLexicon.HAS_MEMBER_OF_RESULT;
import static org.fcrepo.test.util.PathSegmentImpl.createPathList;
import static org.fcrepo.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.test.util.TestHelpers.mockSession;
import static org.fcrepo.test.util.TestHelpers.setField;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.identifiers.PidMinter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.hp.hpl.jena.query.Dataset;

public class FedoraIdentifiersTest {

    private static final Logger LOGGER = getLogger(FedoraIdentifiersTest.class);

    @Mock
    private PidMinter mockPidMinter;

    private FedoraIdentifiers testObj;

    @Mock
    private Node mockNode;

    private UriInfo uriInfo;

    private Session mockSession;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraIdentifiers();
        setField(testObj, "pidMinter", mockPidMinter);
        this.uriInfo = getUriInfoImpl();
        setField(testObj, "uriInfo", uriInfo);
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);

    }

    @Test
    public void testGetNextPidAtRoot() throws NoSuchFieldException,
            RepositoryException, URISyntaxException {
        when(mockPidMinter.makePid()).thenReturn(
                new Function<Object, String>() {

                    @Override
                    public String apply(final Object input) {
                        return "asdf:123";
                    }
                });

        setField(testObj, "pidMinter", mockPidMinter);

        when(uriInfo.getAbsolutePath()).thenReturn(
                new URI("http://localhost/fcrepo/fcr:pid"));

        final Node mockNode = mock(Node.class);
        when(mockNode.getPath()).thenReturn("/asdf:123");
        when(mockSession.getNode("/asdf:123")).thenReturn(mockNode);

        final Dataset np = testObj.getNextPid(createPathList(""), 2, uriInfo);

        LOGGER.debug("Got dataset {}", np.getDefaultModel().toString());
        assertTrue(np.getDefaultModel().contains(
                createResource("http://localhost/fcrepo/fcr:pid"),
                HAS_MEMBER_OF_RESULT,
                createResource("http://localhost/fcrepo/asdf:123")));

    }

    @Test
    public void testGetNextPid() throws Exception {
        when(mockPidMinter.makePid()).thenReturn(
                new Function<Object, String>() {

                    @Override
                    public String apply(final Object input) {
                        return "asdf:123";
                    }
                });

        setField(testObj, "pidMinter", mockPidMinter);

        when(uriInfo.getAbsolutePath()).thenReturn(
                new URI("http://localhost/fcrepo/objects/fcr:pid"));

        when(mockNode.getPath()).thenReturn("/objects/asdf:123");
        when(mockSession.getNode("/objects/asdf:123")).thenReturn(mockNode);

        final Dataset np =
                testObj.getNextPid(createPathList("objects"), 2, uriInfo);

        LOGGER.debug("Got dataset {}", np.getDefaultModel().toString());
        assertTrue(np.getDefaultModel().contains(
                createResource("http://localhost/fcrepo/objects/fcr:pid"),
                HAS_MEMBER_OF_RESULT,
                createResource("http://localhost/fcrepo/objects/asdf:123")));

    }
}
