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
package org.fcrepo.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.fcrepo.RdfLexicon;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

public class NamespaceChangedStatementListenerTest {

    private NamespaceChangedStatementListener testObj;
    private NamespaceRegistry mockNamespaceRegistry;

    @Before
    public void setUp() throws RepositoryException {
        Session mockSession = mock(Session.class);
        Workspace mockWorkspace = mock(Workspace.class);
        mockNamespaceRegistry = mock(NamespaceRegistry.class);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        testObj = new NamespaceChangedStatementListener(mockSession);
        
    }
    
    @Test
    public void shouldAddNamespaceStatement() throws RepositoryException {
        Statement mockStatement = mock(Statement.class);
        when(mockStatement.getSubject()).thenReturn(ResourceFactory.createResource("uri"));
        when(mockStatement.getPredicate()).thenReturn(RdfLexicon.HAS_NAMESPACE_PREFIX);
        when(mockStatement.getObject()).thenReturn(ResourceFactory.createPlainLiteral("123"));

        testObj.addedStatement(mockStatement);
        verify(mockNamespaceRegistry).registerNamespace("123", "uri");
    }

    @Test
    public void shouldIgnoreNonNamespaceStatements() throws RepositoryException {
        Statement mockStatement = mock(Statement.class);
        when(mockStatement.getSubject()).thenReturn(ResourceFactory.createResource("uri"));
        when(mockStatement.getPredicate()).thenReturn(ResourceFactory.createProperty("some-random-predicate"));
        when(mockStatement.getObject()).thenReturn(ResourceFactory.createPlainLiteral("abc"));

        testObj.addedStatement(mockStatement);

        verify(mockNamespaceRegistry, never()).registerNamespace("abc", "uri");
    }


    @Test
    public void shouldRemoveNamespaceStatement() throws RepositoryException {
        Statement mockStatement = mock(Statement.class);
        when(mockStatement.getSubject()).thenReturn(ResourceFactory.createResource("uri"));
        when(mockStatement.getPredicate()).thenReturn(RdfLexicon.HAS_NAMESPACE_PREFIX);
        when(mockStatement.getObject()).thenReturn(ResourceFactory.createPlainLiteral("123"));

        when(mockNamespaceRegistry.getPrefix("uri")).thenReturn("123");
        testObj.removedStatement(mockStatement);
        verify(mockNamespaceRegistry).unregisterNamespace("123");
    }


    @Test
    public void shouldIgnoreNonMatchingNamespacesOnRemoveNamespaceStatement() throws RepositoryException {
        Statement mockStatement = mock(Statement.class);
        when(mockStatement.getSubject()).thenReturn(ResourceFactory.createResource("uri"));
        when(mockStatement.getPredicate()).thenReturn(RdfLexicon.HAS_NAMESPACE_PREFIX);
        when(mockStatement.getObject()).thenReturn(ResourceFactory.createPlainLiteral("456"));

        when(mockNamespaceRegistry.getPrefix("uri")).thenReturn("123");
        testObj.removedStatement(mockStatement);
        verify(mockNamespaceRegistry, never()).unregisterNamespace("456");
    }

    @Test
    public void shouldIgnoreNonNamespaceStatementsOnRemove() throws RepositoryException {
        Statement mockStatement = mock(Statement.class);
        when(mockStatement.getSubject()).thenReturn(ResourceFactory.createResource("uri"));
        when(mockStatement.getPredicate()).thenReturn(ResourceFactory.createProperty("some-random-predicate"));
        when(mockStatement.getObject()).thenReturn(ResourceFactory.createPlainLiteral("abc"));

        testObj.removedStatement(mockStatement);

        verify(mockNamespaceRegistry, never()).unregisterNamespace("abc");

    }
}
