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

package org.fcrepo.transform.sparql;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class JQLResultSetTest {
    JQLResultSet testObj;

    @Mock
    Session mockSession;

    @Mock
    QueryResult mockQueryResult;

    @Mock
    GraphSubjects mockGraphSubjects;

    @Mock
    private RowIterator mockRows;

    @Mock
    private Row mockRow;

    private String[] columnNames;

    @Mock
    private Value mockValue;

    @Mock
    private javax.jcr.Node mockNode;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        when(mockQueryResult.getRows()).thenReturn(mockRows);
        testObj = new JQLResultSet(mockSession, mockGraphSubjects, mockQueryResult);
        columnNames = new String[]{"a", "b"};
        when(mockQueryResult.getColumnNames()).thenReturn(columnNames);
        when(mockRows.nextRow()).thenReturn(mockRow);

        when(mockRow.getValue("a")).thenReturn(mockValue);

    }

    @Test( expected = java.lang.UnsupportedOperationException.class)
    public void testRemove() throws Exception {
        testObj.remove();
    }

    @Test
    public void testHasNext() throws Exception {
        when(mockRows.hasNext()).thenReturn(true);
        assertTrue(testObj.hasNext());
        verify(mockRows).hasNext();
    }

    @Test
    public void testNextWithLiteral() throws Exception {
        when(mockValue.getString()).thenReturn("x");
        final QuerySolution solution = testObj.next();

        assertTrue(solution.contains("a"));
        assertEquals("x", solution.get("a").asLiteral().getLexicalForm());
        assertEquals(solution.get("a"), solution.getLiteral("a"));
    }

    @Test
    public void testNextWithLiteralBoolean() throws Exception {
        when(mockValue.getType()).thenReturn(PropertyType.BOOLEAN);
        when(mockValue.getString()).thenReturn("true");
        final QuerySolution solution = testObj.next();

        assertEquals("true", solution.get("a").asLiteral().getLexicalForm());
    }

    @Test
    public void testNextWithLiteralDate() throws Exception {
        when(mockValue.getType()).thenReturn(PropertyType.DATE);
        final Calendar date = Calendar.getInstance();
        when(mockValue.getDate()).thenReturn(date);
        final QuerySolution solution = testObj.next();

        assertNotNull(solution.get("a").asLiteral());
        assertEquals(XSDDatatype.XSDdateTime.getURI(), solution.get("a").asLiteral().getDatatypeURI());
    }

    @Test
    public void testNextWithLiteralDecimal() throws Exception {
        when(mockValue.getType()).thenReturn(PropertyType.DECIMAL);
        when(mockValue.getDecimal()).thenReturn(BigDecimal.valueOf(1.0));
        final QuerySolution solution = testObj.next();

        assertEquals(1.0, solution.get("a").asLiteral().getDouble(), 0.1);
    }

    @Test
    public void testNextWithLiteralDouble() throws Exception {
        when(mockValue.getType()).thenReturn(PropertyType.DOUBLE);
        when(mockValue.getDouble()).thenReturn(1.0);
        final QuerySolution solution = testObj.next();

        assertEquals(1.0, solution.get("a").asLiteral().getDouble(), 0.1);
    }

    @Test
    public void testNextWithLiteralLong() throws Exception {
        when(mockValue.getType()).thenReturn(PropertyType.LONG);
        when(mockValue.getLong()).thenReturn(1L);
        final QuerySolution solution = testObj.next();

        assertEquals(1L, solution.get("a").asLiteral().getLong());
    }

    @Test
    public void testNextWithResource() throws Exception {
        when(mockValue.getType()).thenReturn(PropertyType.PATH);
        when(mockValue.getString()).thenReturn("/x");
        when(mockGraphSubjects.getGraphSubject("/x")).thenReturn(ResourceFactory.createResource("info:x"));
        final QuerySolution solution = testObj.next();

        assertTrue(solution.contains("a"));
        assertEquals("info:x", solution.get("a").asResource().getURI());
        assertEquals(solution.get("a"), solution.getResource("a"));
    }

    @Test
    public void testNextWithReference() throws Exception {
        when(mockValue.getType()).thenReturn(PropertyType.REFERENCE);
        when(mockValue.getString()).thenReturn("uuid");
        when(mockSession.getNodeByIdentifier("uuid")).thenReturn(mockNode);
        when(mockGraphSubjects.getGraphSubject(mockNode.getPath())).thenReturn(ResourceFactory.createResource("http://localhost:8080/xyz"));
        final QuerySolution solution = testObj.next();

        assertEquals("http://localhost:8080/xyz", solution.get("a").asResource().getURI());
    }

    @Test
    public void testNextWithURI() throws Exception {
        when(mockValue.getType()).thenReturn(PropertyType.URI);
        when(mockValue.getString()).thenReturn("info:xyz");
        final QuerySolution solution = testObj.next();

        assertEquals("info:xyz", solution.get("a").asResource().getURI());
    }

    @Test
    public void testSolutionVars() throws Exception {
        final QuerySolution solution = testObj.nextSolution();
        final List<String> vars = ImmutableList.copyOf(solution.varNames());

        assertArrayEquals(columnNames, vars.toArray());
    }

    @Test
    public void testNextBinding() throws Exception {
        when(mockValue.getString()).thenReturn("x");
        final Binding binding = testObj.nextBinding();

        assertTrue(binding.contains(Var.alloc("a")));
        final Node a = binding.get(Var.alloc("a"));
        assertEquals("x", a.getLiteralLexicalForm());

    }

    @Test
    public void testBindingSize() throws Exception {
        final Binding binding = testObj.nextBinding();
        assertEquals(2, binding.size());
    }

    @Test
    public void testBindingEmpty() throws Exception {
        final Binding binding = testObj.nextBinding();
        assertFalse(binding.isEmpty());
    }

    @Test
    public void testBindingVars() throws Exception {
        final Binding binding = testObj.nextBinding();
        final List<String> vars = ImmutableList.copyOf(Iterators.transform(binding.vars(), new Function<Var, String>() {
            @Override
            public String apply(final Var var) {
                return var.getVarName();
            }

        }));

        assertArrayEquals(columnNames, vars.toArray());
        assertFalse(binding.isEmpty());
    }

    @Test
    public void testEmptyBinding() throws Exception {

        when(mockQueryResult.getColumnNames()).thenReturn(new String[] { });
        final Binding binding = testObj.nextBinding();
        assertTrue(binding.isEmpty());
    }

    @Test
    public void testGetRowNumber() throws Exception {
        assertEquals(0, testObj.getRowNumber());
    }

    @Test
    public void testGetRowNumberAfterGettingNext() throws Exception {
        testObj.next();
        testObj.next();
        testObj.next();
        testObj.next();
        testObj.next();
        assertEquals(5, testObj.getRowNumber());
    }

    @Test
    public void testGetResultVars() throws Exception {
        assertArrayEquals(columnNames, testObj.getResultVars().toArray());
    }

    @Test
    public void testGetResourceModel() throws Exception {
        assertNull(testObj.getResourceModel());
    }
}
