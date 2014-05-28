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
package org.fcrepo.kernel.impl.rdf.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * <p>WorkspaceRdfContextTest class.</p>
 *
 * @author cbeer
 */
public class WorkspaceRdfContextTest {


    @Mock
    private IdentifierTranslator subjects;

    @Mock
    private Session session;

    @Mock
    private Repository repository;

    @Mock
    private Workspace mockWorkspace;


    @Before
    public void setUp() {
        initMocks(this);
        subjects = new DefaultIdentifierTranslator();
        when(session.getRepository()).thenReturn(repository);
        when(session.getWorkspace()).thenReturn(mockWorkspace);
    }

    @Test
    public void testWorkspaceTriples() throws RepositoryException {
        when(mockWorkspace.getAccessibleWorkspaceNames()).thenReturn(new String[] { "default", "a", "b" } );
        final WorkspaceRdfContext testObj = new WorkspaceRdfContext(session, subjects);

        final Model model = testObj.asModel();

        assertTrue(model.contains(subjects.getSubject("/"),
                                      RdfLexicon.HAS_DEFAULT_WORKSPACE,
                                     subjects.getSubject("/workspace:default" )));

        assertTrue(model.contains(subjects.getSubject("/workspace:default" ),
                                     RdfLexicon.DC_TITLE,
                                     "default"));

        assertTrue(model.contains(subjects.getSubject("/"),
                                     RdfLexicon.HAS_WORKSPACE,
                                     subjects.getSubject("/workspace:a")));

        assertTrue(model.contains(subjects.getSubject("/"),
                                     RdfLexicon.HAS_WORKSPACE,
                                     subjects.getSubject("/workspace:b" )));

    }

}
