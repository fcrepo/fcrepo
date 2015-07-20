/**
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
package org.fcrepo.kernel.modeshape.utils.iterators;

import static com.google.common.collect.Sets.newHashSet;
import static com.hp.hpl.jena.graph.NodeFactory.createAnon;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.PAGE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.Session;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import com.google.common.collect.ObjectArrays;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * <p>PersistingRdfStreamConsumerTest class.</p>
 *
 * @author ajs6f
 */
@Ignore
public class PersistingRdfStreamConsumerTest {

    @Test
    public void testConsumeAsync() {

        final RdfStream testStream = new RdfStream(profferedStatements);

        final Set<Statement> rejectedStatements =
            newHashSet(profferedStatements);
        final Set<Statement> acceptedStatements = newHashSet();

        final Set<Resource> rejectedMixins = newHashSet(profferedMixins);
        final Set<Resource> acceptedMixins = newHashSet();

        testPersister =
            new PersistingRdfStreamConsumer(idTranslator, mockSession, testStream) {

                @Override
                protected void operateOnProperty(final Statement s,
                    final FedoraResource resource) {
                    rejectedStatements.remove(s);
                    acceptedStatements.add(s);
                }

                @Override
                protected void operateOnMixin(final Resource mixinResource,
                        final FedoraResource resource) {
                    rejectedMixins.remove(mixinResource);
                    acceptedMixins.add(mixinResource);
                }
            };

        testPersister.consumeAsync();

        assertTrue("Failed to operate on ordinary property!",
                acceptedStatements.contains(propertyStatement)
                        && !rejectedStatements.contains(propertyStatement));

        assertTrue("Wrongly operated on foreign property!",
                !acceptedStatements.contains(foreignStatement)
                        && rejectedStatements.contains(foreignStatement));

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
            new PersistingRdfStreamConsumer(idTranslator, mockSession,
                    new RdfStream(mockTriples)) {

                    @Override
                    protected void operateOnProperty(final Statement s,
                        final FedoraResource resource) {
                    }

                    @Override
                    protected void operateOnMixin(final Resource mixinResource,
                            final FedoraResource resource) {
                    }
                };
        // this should blow out when we try to retrieve the result
        testPersister.consumeAsync().get();
    }

    @Before
    public void setUp() {
        initMocks(this);
        idTranslator = new DefaultIdentifierTranslator(mockSession);
    }

    private static final Model m = createDefaultModel();

    private static final com.hp.hpl.jena.graph.Node subject = m.createResource("x").asNode();
    private static final com.hp.hpl.jena.graph.Node object = m.createResource("y").asNode();
    private static final com.hp.hpl.jena.graph.Node foreignSubject = m.createResource("z").asNode();

    private static final Triple propertyTriple = create(subject,
            createAnon(), object);

    private static final Statement propertyStatement = m
            .asStatement(propertyTriple);

    private static final Triple ldpManagedPropertyTriple = create(subject,
            PAGE.asNode(), object);

    private static final Statement ldpManagedPropertyStatement = m
            .asStatement(ldpManagedPropertyTriple);

    private static final Triple fedoraManagedPropertyTriple = create(subject,
            createURI(REPOSITORY_NAMESPACE + "thing"), object);

    private static final Statement fedoraManagedPropertyStatement = m
            .asStatement(fedoraManagedPropertyTriple);

    private static final Statement jcrManagedPropertyStatement =
        ResourceFactory.createStatement(ResourceFactory.createResource(),
                ResourceFactory.createProperty(JCR_NAMESPACE, "thing"),
                ResourceFactory.createResource());

    private static final Triple managedMixinTriple = create(subject, type
            .asNode(), createURI(REPOSITORY_NAMESPACE + "mixin"));

    private static final Statement managedMixinStatement = m.asStatement(managedMixinTriple);

    private static final Triple mixinTriple = create(subject,
            type.asNode(), createURI("myNS:mymixin"));

    private static final Statement mixinStatement = m.asStatement(mixinTriple);


    private static final Triple foreignTriple = create(foreignSubject,
            createAnon(), object);

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

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    @Mock
    private Iterator<Triple> mockTriples;

    private PersistingRdfStreamConsumer testPersister;

}
