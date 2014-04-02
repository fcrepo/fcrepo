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

import static com.google.common.collect.Sets.newHashSet;
import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.RdfLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.PAGE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.RESTAPI_NAMESPACE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import com.google.common.collect.ObjectArrays;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

public class PersistingRdfStreamConsumerTest {

    @Test
    public void testConsumeAsync() throws Exception {

        final RdfStream testStream = new RdfStream(profferedStatements);

        final Set<Statement> rejectedStatements =
            newHashSet(profferedStatements);
        final Set<Statement> acceptedStatements = newHashSet();

        final Set<Resource> rejectedMixins = newHashSet(profferedMixins);
        final Set<Resource> acceptedMixins = newHashSet();

        testPersister =
            new PersistingRdfStreamConsumer(mockGraphSubjects, mockSession, testStream) {

                @Override
                protected void operateOnProperty(final Statement s,
                    final Node subjectNode) throws RepositoryException {
                    rejectedStatements.remove(s);
                    acceptedStatements.add(s);
                }

                @Override
                protected void operateOnMixin(final Resource mixinResource,
                        final Node subjectNode) throws RepositoryException {
                    rejectedMixins.remove(mixinResource);
                    acceptedMixins.add(mixinResource);
                }
            };

        testPersister.consumeAsync();

        assertTrue("Failed to operate on ordinary property!",
                acceptedStatements.contains(propertyStatement)
                        && !rejectedStatements.contains(propertyStatement));

        assertTrue("Wrongly operated on LDP managed property!",
                !acceptedStatements.contains(ldpManagedPropertyStatement)
                        && rejectedStatements.contains(ldpManagedPropertyStatement));

        assertTrue("Wrongly operated on JCR managed property!",
                !acceptedStatements.contains(jcrManagedPropertyStatement)
                        && rejectedStatements.contains(jcrManagedPropertyStatement));

        assertTrue("Wrongly operated on Fedora managed property!",
                !acceptedStatements.contains(fedoraManagedPropertyStatement)
                        && rejectedStatements.contains(fedoraManagedPropertyStatement));

        assertTrue("Wrongly operated on foreign property!",
                !acceptedStatements.contains(foreignStatement)
                        && rejectedStatements.contains(foreignStatement));

        assertTrue("Wrongly operated on managed mixin!", !acceptedMixins
                .contains(managedMixinStatement.getObject().asResource())
                && rejectedMixins.contains(managedMixinStatement.getObject()
                        .asResource()));

        assertTrue("Failed to operate on ordinary mixin!", acceptedMixins
                .contains(mixinStatement.getObject().asResource())
                && !rejectedMixins.contains(mixinStatement.getObject()
                        .asResource()));

    }

    @Test(expected = ExecutionException.class)
    public void testBadStream() throws Exception {
        when(mockTriples.hasNext())
                .thenThrow(new RuntimeException("Expected."));
        testPersister =
            new PersistingRdfStreamConsumer(mockGraphSubjects, mockSession,
                    new RdfStream(mockTriples)) {

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

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        for (final Statement fedoraStatement : fedoraStatements) {
            when(mockSession.getNode(mockGraphSubjects.getPathFromGraphSubject(fedoraStatement.getSubject())))
                    .thenReturn(mockNode);
            when(mockGraphSubjects.isFedoraGraphSubject(fedoraStatement.getSubject())).thenReturn(true);
        }
        when(mockSession.getNode(mockGraphSubjects.getPathFromGraphSubject(foreignStatement.getSubject()))).thenReturn(
                mockNode);
        when(mockGraphSubjects.isFedoraGraphSubject(foreignStatement.getSubject())).thenReturn(false);
    }

    private static final Model m = createDefaultModel();

    private static final Triple propertyTriple = create(createAnon(),
            createAnon(), createAnon());

    private static final Statement propertyStatement = m
            .asStatement(propertyTriple);

    private static final Triple ldpManagedPropertyTriple = create(createAnon(),
            PAGE.asNode(), createAnon());

    private static final Statement ldpManagedPropertyStatement = m
            .asStatement(ldpManagedPropertyTriple);

    private static final Triple fedoraManagedPropertyTriple = create(createAnon(),
            createURI(REPOSITORY_NAMESPACE + "thing"), createAnon());

    private static final Statement fedoraManagedPropertyStatement = m
            .asStatement(fedoraManagedPropertyTriple);

    private static final Statement jcrManagedPropertyStatement =
        ResourceFactory.createStatement(ResourceFactory.createResource(),
                ResourceFactory.createProperty(JCR_NAMESPACE, "thing"),
                ResourceFactory.createResource());

    private static final Triple managedMixinTriple = create(createAnon(), type
            .asNode(), createURI(RESTAPI_NAMESPACE + "mixin"));

    private static final Statement managedMixinStatement = m.asStatement(managedMixinTriple);

    private static final Triple mixinTriple = create(createAnon(),
            type.asNode(), createURI("myNS:mymixin"));

    private static final Statement mixinStatement = m.asStatement(mixinTriple);


    private static final Triple foreignTriple = create(createAnon(),
            createAnon(), createAnon());

    private static final Statement foreignStatement = m.asStatement(foreignTriple);

    private static final Statement[] fedoraStatements = new Statement[] {
            propertyStatement, ldpManagedPropertyStatement, mixinStatement,
            managedMixinStatement, jcrManagedPropertyStatement,
            fedoraManagedPropertyStatement};

    private static final Statement[] profferedStatements = ObjectArrays
            .concat(fedoraStatements, foreignStatement);

    private final static Resource[] profferedMixins = new Resource[] {
            mixinStatement.getObject().asResource(),
            managedMixinStatement.getObject().asResource()};

    @Mock
    private Session mockSession;

    @Mock
    private Node mockNode;

    @Mock
    private IdentifierTranslator mockGraphSubjects;

    @Mock
    private Iterator<Triple> mockTriples;

    private PersistingRdfStreamConsumer testPersister;

}
