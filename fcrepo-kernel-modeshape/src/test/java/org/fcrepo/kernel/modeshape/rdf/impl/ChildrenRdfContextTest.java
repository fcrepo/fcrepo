/*
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.modeshape.rdf.impl;

import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.Iterator;

import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * ChildrenRdfContextTest class.
 *
 * @author awoods
 * @since 2015-11-28
 */
public class ChildrenRdfContextTest {

    @Mock
    private FedoraResource mockResource;

    @Mock
    private Node mockResourceNode;

    @Mock
    private Node mockBinaryNode;

    @Mock
    private NonRdfSourceDescription mockNonRdfSourceDescription;

    @Mock
    private Session mockSession;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    private static final String RDF_PATH = "/resource/path";

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        // Mock RDF Source
        when(mockResource.getNode()).thenReturn(mockResourceNode);
        when(mockResourceNode.getSession()).thenReturn(mockSession);
        when(mockResource.getPath()).thenReturn(RDF_PATH);

        idTranslator = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void testNoChildren() throws RepositoryException {
        when(mockResourceNode.hasNodes()).thenReturn(false);

        final Model results = new ChildrenRdfContext(mockResource, idTranslator).collect(toModel());
        final Resource subject = idTranslator.reverse().convert(mockResource);

        final StmtIterator stmts = results.listStatements(subject, RdfLexicon.HAS_CHILD_COUNT, (RDFNode) null);

        assertTrue("There should have been a statement!", stmts.hasNext());
        final Statement stmt = stmts.nextStatement();
        assertTrue("Object should be a literal! " + stmt.getObject(), stmt.getObject().isLiteral());
        assertEquals(0, stmt.getInt());

        assertFalse("There should not have been a second statement!", stmts.hasNext());
    }

    @Test
    public void testChildren() throws RepositoryException {
        when(mockResourceNode.hasNodes()).thenReturn(true);
        when(mockResource.getChildren()).thenReturn(childrenIterator());

        final Model results = new ChildrenRdfContext(mockResource, idTranslator).collect(toModel());
        final Resource subject = idTranslator.reverse().convert(mockResource);

        final StmtIterator stmts = results.listStatements(subject, RdfLexicon.HAS_CHILD_COUNT, (RDFNode) null);

        assertTrue("There should have been a statement!", stmts.hasNext());
        final Statement stmt = stmts.nextStatement();
        assertTrue("Object should be a literal! " + stmt.getObject(), stmt.getObject().isLiteral());
        assertEquals(3, stmt.getInt());

        assertFalse("There should not have been a second statement!", stmts.hasNext());
    }

    private Iterator<FedoraResource> childrenIterator() {
        return Iterators.forArray(mock(FedoraResource.class), mock(FedoraResource.class), mock(FedoraResource.class));
    }

}
