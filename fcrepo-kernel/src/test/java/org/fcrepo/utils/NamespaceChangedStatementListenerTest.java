package org.fcrepo.utils;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(mockStatement.getPredicate()).thenReturn(ResourceFactory.createProperty(JcrRdfTools.HAS_NAMESPACE_PREDICATE));
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
        when(mockStatement.getPredicate()).thenReturn(ResourceFactory.createProperty(JcrRdfTools.HAS_NAMESPACE_PREDICATE));
        when(mockStatement.getObject()).thenReturn(ResourceFactory.createPlainLiteral("123"));

        when(mockNamespaceRegistry.getPrefix("uri")).thenReturn("123");
        testObj.removedStatement(mockStatement);
        verify(mockNamespaceRegistry).unregisterNamespace("123");
    }


    @Test
    public void shouldIgnoreNonMatchingNamespacesOnRemoveNamespaceStatement() throws RepositoryException {
        Statement mockStatement = mock(Statement.class);
        when(mockStatement.getSubject()).thenReturn(ResourceFactory.createResource("uri"));
        when(mockStatement.getPredicate()).thenReturn(ResourceFactory.createProperty(JcrRdfTools.HAS_NAMESPACE_PREDICATE));
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
