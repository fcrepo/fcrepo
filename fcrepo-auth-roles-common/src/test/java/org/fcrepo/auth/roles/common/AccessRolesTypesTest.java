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
package org.fcrepo.auth.roles.common;

import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.net.URL;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

import org.fcrepo.http.commons.session.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author bbpennel
 * @since Feb 17, 2014
 */
public class AccessRolesTypesTest {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AccessRolesTypesTest.class);

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Workspace workspace;

    @Mock
    private Session session;

    private AccessRolesTypes accessRolesTypes;

    @Mock
    private NodeTypeManager nodeTypeManager;

    @Mock
    private NodeType mockNodeType;

    @Before
    public void setUp() throws RepositoryException, IOException {
        initMocks(this);

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getNodeTypeManager()).thenReturn(nodeTypeManager);

        final NodeTypeIterator mockNTI = mock(NodeTypeIterator.class);
        when(mockNTI.hasNext()).thenReturn(true, false);
        when(mockNTI.nextNodeType()).thenReturn(mockNodeType).thenThrow(
                new ArrayIndexOutOfBoundsException());

        when(nodeTypeManager.registerNodeTypes(any(URL.class), anyBoolean()))
                .thenReturn(mockNTI);

        accessRolesTypes = new AccessRolesTypes();
        setField(accessRolesTypes, "sessionFactory", sessionFactory);

        when(sessionFactory.getInternalSession()).thenReturn(session);
    }

    @Test(expected = RepositoryException.class)
    public void testSetupRepoConfigGetSessionException()
            throws RepositoryException,
            IOException {
        when(sessionFactory.getInternalSession()).thenThrow(
                new RepositoryException());

        try {
            accessRolesTypes.setUpRepositoryConfiguration();
        } finally {
            verify(nodeTypeManager, never()).registerNodeTypes(any(URL.class),
                    anyBoolean());
            verify(session, never()).save();
            verify(session, never()).logout();
        }
    }

    @Test(expected = RepositoryException.class)
    public void testSetupRepoConfigGetNodeTypeManagerException()
            throws RepositoryException, IOException {
        when(workspace.getNodeTypeManager()).thenThrow(
                new RepositoryException());

        try {
            accessRolesTypes.setUpRepositoryConfiguration();
        } finally {
            verify(nodeTypeManager, never()).registerNodeTypes(any(URL.class),
                    anyBoolean());
            verify(session, never()).save();
            verify(session).logout();
        }
    }

    @Test
    public void testSetupRepoConfig() throws RepositoryException, IOException {
        accessRolesTypes.setUpRepositoryConfiguration();

        verify(nodeTypeManager).registerNodeTypes(any(URL.class), anyBoolean());
        verify(session).save();
        verify(session).logout();
    }
}
