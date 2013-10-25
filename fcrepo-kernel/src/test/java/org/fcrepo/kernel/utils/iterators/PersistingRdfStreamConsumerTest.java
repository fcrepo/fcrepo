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

package org.fcrepo.kernel.utils.iterators;

import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class PersistingRdfStreamConsumerTest {

    @Mock
    private Session mockSession;

    @Mock
    private Node mockNode;

    @Mock
    private GraphSubjects mockGraphSubjects;

    @Mock
    private RdfStream mockStream;

    private PersistingRdfStreamConsumer testPersister;

    private boolean successfullyOperatedOnAProperty = false;

    private boolean successfullyOperatedOnAMixin = false;

    private static final Model m = createDefaultModel();

    private static final Triple propertyTriple = create(createAnon(),
            createAnon(), createAnon());

    private static final Statement propertyStatement = m
            .asStatement(propertyTriple);

    private static final Triple badMixinTriple = create(createAnon(),
            type.asNode(), createLiteral("mixin:mixin"));

    private static final Statement badMixinStatement = m.asStatement(badMixinTriple);

    private static final Triple mixinTriple = create(createAnon(),
            type.asNode(), createAnon());

    private static final Statement mixinStatement = m.asStatement(mixinTriple);


    private static final Triple foreignTriple = create(createAnon(),
            createAnon(), createAnon());

    private static final Statement foreignStatement = m.asStatement(foreignTriple);


    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

    }

    @Test
    public void testConsumeAsync() throws Exception {
        when(mockStream.hasNext()).thenReturn(true, true, true, true, false);
        when(mockStream.next()).thenReturn(propertyTriple, mixinTriple,
                foreignTriple, badMixinTriple);
        when(
                mockGraphSubjects.getNodeFromGraphSubject(propertyStatement
                        .getSubject())).thenReturn(mockNode);
        when(
                mockGraphSubjects.getNodeFromGraphSubject(mixinStatement
                        .getSubject())).thenReturn(mockNode);
        when(
                mockGraphSubjects.getNodeFromGraphSubject(foreignStatement
                        .getSubject())).thenReturn(mockNode);
        when(
                mockGraphSubjects.getNodeFromGraphSubject(badMixinStatement
                        .getSubject())).thenReturn(mockNode);
        when(
                mockGraphSubjects.isFedoraGraphSubject(propertyStatement
                        .getSubject())).thenReturn(true);
        when(
                mockGraphSubjects.isFedoraGraphSubject(mixinStatement
                        .getSubject())).thenReturn(true);
        when(
                mockGraphSubjects.isFedoraGraphSubject(foreignStatement
                        .getSubject())).thenReturn(false);
        when(
                mockGraphSubjects.isFedoraGraphSubject(badMixinStatement
                        .getSubject())).thenReturn(true);

        testPersister =
            new PersistingRdfStreamConsumer(mockGraphSubjects, mockSession, mockStream) {

                @Override
                protected void operateOnProperty(final Statement s,
                    final Node subjectNode) throws RepositoryException {
                    successfullyOperatedOnAProperty = true;
                }

                @Override
                protected void operateOnMixin(final Resource mixinResource,
                        final Node subjectNode) throws RepositoryException {
                    successfullyOperatedOnAMixin = true;
                }
            };

        testPersister.consumeAsync();
        assertTrue("Didn't successfully operate on a property!",
                successfullyOperatedOnAProperty);

        assertTrue("Didn't successfully operate on a mixin!",
                successfullyOperatedOnAMixin);
    }

    @Test(expected = ExecutionException.class)
    public void testBadStream() throws Exception {
        when(mockStream.hasNext()).thenThrow(new RuntimeException("Expected."));
        testPersister =
                new PersistingRdfStreamConsumer(mockGraphSubjects, mockSession, mockStream) {

                    @Override
                    protected void operateOnProperty(final Statement s,
                        final Node subjectNode) throws RepositoryException {
                    }

                    @Override
                    protected void operateOnMixin(final Resource mixinResource,
                            final Node subjectNode) throws RepositoryException {
                    }
                };
        // this should blow out when we try to retrieve the result
        testPersister.consumeAsync().get();
    }


}
