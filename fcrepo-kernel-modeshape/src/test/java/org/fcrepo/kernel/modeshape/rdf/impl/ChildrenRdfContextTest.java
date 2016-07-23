/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static java.util.stream.Stream.of;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * ChildrenRdfContextTest class.
 *
 * @author awoods
 * @since 2015-11-28
 */
@RunWith(MockitoJUnitRunner.class)
public class ChildrenRdfContextTest {

    @Mock
    private Node mockResourceNode, mockBinaryNode;

    @Mock
    private NonRdfSourceDescription mockNonRdfSourceDescription;

    @Mock
    private Session mockSession;

    @Mock
    private FedoraResourceImpl mockResource, mockRes1, mockRes2, mockRes3;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    private static final String RDF_PATH = "/resource/path";

    @Before
    public void setUp() throws RepositoryException {
        // Mock RDF Source
        when(mockResource.getNode()).thenReturn(mockResourceNode);
        when(mockResourceNode.getSession()).thenReturn(mockSession);
        when(mockResource.getPath()).thenReturn(RDF_PATH);

        idTranslator = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void testNoChildren() throws RepositoryException {
        when(mockResourceNode.hasNodes()).thenReturn(false);

        try (final ChildrenRdfContext childrenRdfContext = new ChildrenRdfContext(mockResource, idTranslator)) {
            final Model results = childrenRdfContext.collect(toModel());
            final Resource subject = idTranslator.reverse().convert(mockResource);

            final StmtIterator stmts = results.listStatements(subject, RdfLexicon.CONTAINS, (RDFNode) null);
            assertFalse("There should NOT have been a statement!", stmts.hasNext());
        }
    }

    @Test
    public void testChildren() throws RepositoryException {
        when(mockRes1.getPath()).thenReturn(RDF_PATH + "/res1");
        when(mockRes2.getPath()).thenReturn(RDF_PATH + "/res2");
        when(mockRes3.getPath()).thenReturn(RDF_PATH + "/res3");
        when(mockRes1.getDescribedResource()).thenReturn(mockRes1);
        when(mockRes2.getDescribedResource()).thenReturn(mockRes2);
        when(mockRes3.getDescribedResource()).thenReturn(mockRes3);
        when(mockResourceNode.hasNodes()).thenReturn(true);
        final Stream<FedoraResource> first = of(mockRes1, mockRes2, mockRes3);
        final Stream<FedoraResource> second = of(mockRes1, mockRes2, mockRes3);
        when(mockResource.getChildren()).thenReturn(first).thenReturn(second);

        try (final ChildrenRdfContext context = new ChildrenRdfContext(mockResource, idTranslator)) {
            final Model results = context.collect(toModel());
            final Resource subject = idTranslator.reverse().convert(mockResource);

            final StmtIterator stmts = results.listStatements(subject, RdfLexicon.CONTAINS, (RDFNode) null);

            final AtomicInteger count = new AtomicInteger(0);
            assertTrue("There should have been a statement!", stmts.hasNext());
            stmts.forEachRemaining(stmt -> {
                        assertTrue("Object should be a URI! " + stmt.getObject(), stmt.getObject().isURIResource());
                        count.incrementAndGet();
                    }
            );

            assertEquals(3, count.get());
            assertFalse("There should not have been a second statement!", stmts.hasNext());
        }
    }
}
