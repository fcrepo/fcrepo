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

package org.fcrepo.http.commons.api.rdf;

import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.uri.UriBuilderImpl;
import org.fcrepo.kernel.TxSession;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.junit.Before;
import org.mockito.Mock;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public abstract class GraphSubjectsTest {

    protected String testPath = "/foo/bar";

    protected GraphSubjects testObj;

    @Mock
    protected Session mockSession;

    @Mock
    protected TxSession mockSessionTx;

    @Mock
    protected Repository mockRepository;

    @Mock
    protected Workspace mockWorkspace;

    @Mock protected  Resource mockSubject;

    @Mock
    protected Node mockNode;

    @Mock
    protected NodeType mockNodeType;

    @Mock
    protected ValueFactory mockValueFactory;

    protected UriInfo uriInfo;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        uriInfo = getUriInfoImpl(testPath);
        when(mockSession.getValueFactory()).thenReturn(mockValueFactory);
        when(mockSession.getRepository()).thenReturn(mockRepository);
        testObj = getTestObj();

    }

    /**
     * Provides the test object instance which must be an instance of
     * GraphSubjects.
     */
    protected abstract GraphSubjects getTestObj();

    protected static UriInfo getUriInfoImpl(final String path) {
        // UriInfo ui = mock(UriInfo.class,withSettings().verboseLogging());
        final UriInfo ui = mock(UriInfo.class);
        final UriBuilder ub = new UriBuilderImpl();
        ub.scheme("http");
        ub.host("localhost");
        ub.port(8080);
        ub.path("/fcrepo");

        final UriBuilder rb = new UriBuilderImpl();
        rb.scheme("http");
        rb.host("localhost");
        rb.port(8080);
        rb.path("/fcrepo/rest" + path);

        when(ui.getRequestUri()).thenReturn(
                URI.create("http://localhost:8080/fcrepo/rest" + path));
        when(ui.getBaseUri()).thenReturn(
                URI.create("http://localhost:8080/fcrepo"));
        when(ui.getBaseUriBuilder()).thenReturn(ub);
        when(ui.getAbsolutePathBuilder()).thenReturn(rb);

        return ui;
    }

    @Path("/rest/{path}")
    protected class MockNodeController {

    }
}
