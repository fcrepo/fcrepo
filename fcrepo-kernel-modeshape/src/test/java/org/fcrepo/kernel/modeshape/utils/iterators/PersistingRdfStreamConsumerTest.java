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
package org.fcrepo.kernel.modeshape.utils.iterators;

import static java.util.stream.Stream.of;
import static com.google.common.collect.Sets.newHashSet;
import static org.apache.jena.graph.NodeFactory.createBlankNode;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.PAGE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.JCR_NAMESPACE;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Set;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.Session;

import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import com.google.common.collect.ObjectArrays;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 * <p>PersistingRdfStreamConsumerTest class.</p>
 *
 * @author ajs6f
 */
@Ignore
public class PersistingRdfStreamConsumerTest {

    @Test
    public void testConsumeAsync() {

        try (final RdfStream testStream = new DefaultRdfStream(createURI("subject"), of(profferedStatements)
                .map(Statement::asTriple))) {

            final Set<Statement> rejectedStatements = newHashSet(profferedStatements);
            final Set<Statement> acceptedStatements = newHashSet();

            final Set<Resource> rejectedMixins = newHashSet(profferedMixins);
            final Set<Resource> acceptedMixins = newHashSet();

            testPersister = new PersistingRdfStreamConsumer(idTranslator, mockSession, testStream) {

                @Override
                protected void operateOnProperty(final Statement s, final FedoraResource resource) {
                    rejectedStatements.remove(s);
                    acceptedStatements.add(s);
                }

                @Override
                protected void operateOnMixin(final Resource mixinResource, final FedoraResource resource) {
                    rejectedMixins.remove(mixinResource);
                    acceptedMixins.add(mixinResource);
                }
            };

            testPersister.consumeAsync();

            assertTrue("Failed to operate on ordinary property!", acceptedStatements.contains(propertyStatement) &&
                    !rejectedStatements.contains(propertyStatement));

            assertTrue("Wrongly operated on foreign property!", !acceptedStatements.contains(foreignStatement) &&
                    rejectedStatements.contains(foreignStatement));

            assertTrue("Failed to operate on ordinary mixin!", acceptedMixins.contains(mixinStatement.getObject()
                    .asResource()) && !rejectedMixins.contains(mixinStatement.getObject().asResource()));
        }
    }

    @Before
    public void setUp() {
        initMocks(this);
        idTranslator = new DefaultIdentifierTranslator(mockSession);
    }

    private static final Model m = createDefaultModel();

    private static final org.apache.jena.graph.Node subject = m.createResource("x").asNode();
    private static final org.apache.jena.graph.Node object = m.createResource("y").asNode();
    private static final org.apache.jena.graph.Node foreignSubject = m.createResource("z").asNode();

    private static final Triple propertyTriple = create(subject,
            createBlankNode(), object);

    private static final Statement propertyStatement = m
            .asStatement(propertyTriple);

    private static final Triple ldpManagedPropertyTriple = create(subject, PAGE.asNode(), object);

    private static final Statement ldpManagedPropertyStatement = m.asStatement(ldpManagedPropertyTriple);

    private static final Triple fedoraManagedPropertyTriple =
            create(subject,createURI(REPOSITORY_NAMESPACE + "thing"), object);

    private static final Statement fedoraManagedPropertyStatement = m.asStatement(fedoraManagedPropertyTriple);

    private static final Statement jcrManagedPropertyStatement =
        createStatement(createResource(), createProperty(JCR_NAMESPACE, "thing"), createResource());

    private static final Triple managedMixinTriple = create(subject, type.asNode(),
            createURI(REPOSITORY_NAMESPACE + "mixin"));

    private static final Statement managedMixinStatement = m.asStatement(managedMixinTriple);

    private static final Triple mixinTriple = create(subject,
            type.asNode(), createURI("myNS:mymixin"));

    private static final Statement mixinStatement = m.asStatement(mixinTriple);


    private static final Triple foreignTriple = create(foreignSubject,
            createBlankNode(), object);

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
    private Stream<Triple> mockTriples;

    private PersistingRdfStreamConsumer testPersister;

}
