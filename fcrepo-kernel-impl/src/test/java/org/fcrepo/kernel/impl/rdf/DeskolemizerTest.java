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

package org.fcrepo.kernel.impl.rdf;

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.models.FedoraResource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class DeskolemizerTest {

    @Mock
    private IdentifierConverter<Resource, FedoraResource> mockIdTranslator;

    @Mock
    private FedoraResource mockSkolemFedoraResource;

    @Mock
    private FedoraResource mockNonSkolemFedoraResource;

    private static final Model testModel = createDefaultModel();

    private static final Node skolemNode = createURI("info:skolem");

    @Before
    public void setUp() {
        when(mockSkolemFedoraResource.hasType("fedora:Skolem")).thenReturn(true);
        when(mockNonSkolemFedoraResource.hasType("fedora:Skolem")).thenReturn(false);
    }

    @Test
    public void tripleWithNoBNodeOperations() {
        when(mockIdTranslator.inDomain(any(Resource.class))).thenReturn(true);
        when(mockIdTranslator.asString(any(Resource.class))).thenReturn("non-fcr-URI");
        when(mockIdTranslator.convert(any(Resource.class))).thenReturn(mockSkolemFedoraResource);
        when(mockSkolemFedoraResource.hasType("fedora:Skolem")).thenReturn(false);
        final Deskolemizer testDeskolemizer = new Deskolemizer(mockIdTranslator, null);

        final Triple testTriple = create(randomURIResource(), randomURIResource(), randomURIResource());
        assertEquals(testTriple, testDeskolemizer.apply(testTriple));

        final Triple testTripleWithLiteralObject =
                create(randomURIResource(), randomURIResource(), createLiteral("some literal value"));
        assertEquals(testTripleWithLiteralObject, testDeskolemizer.apply(testTripleWithLiteralObject));

        final Triple testTripleWithObjectWithURIWithQueryString =
                create(randomURIResource(), randomURIResource(), createURI("info:test?with=query-string"));
        assertEquals(testTripleWithObjectWithURIWithQueryString, testDeskolemizer
                .apply(testTripleWithObjectWithURIWithQueryString));

        final Node outOfDomainNode = createURI("info:out-of-domain");
        final Triple testTripleWithObjectWithOutOfDomainURI =
                create(randomURIResource(), randomURIResource(), outOfDomainNode);
        final Resource outOfDomainObject = testModel.asRDFNode(outOfDomainNode).asResource();
        when(mockIdTranslator.inDomain(outOfDomainObject)).thenReturn(false);
        assertEquals(testTripleWithObjectWithOutOfDomainURI, testDeskolemizer
                .apply(testTripleWithObjectWithOutOfDomainURI));

        final Node fcrContainingNode = createURI("info:test/fcr:metadata-containing-URI");
        final Triple testTripleWithObjectWithfcrContainingURI =
                create(randomURIResource(), randomURIResource(), fcrContainingNode);
        final Resource fcrContainingObject = testModel.asRDFNode(fcrContainingNode).asResource();
        when(mockIdTranslator.inDomain(any(Resource.class))).thenReturn(true);
        when(mockIdTranslator.asString(fcrContainingObject)).thenReturn("foo/fcr:metadata");
        assertEquals(testTripleWithObjectWithfcrContainingURI, testDeskolemizer
                .apply(testTripleWithObjectWithfcrContainingURI));
    }

    @Test(expected = RuntimeException.class)
    public void tripleWithBadSubject() {
        when(mockIdTranslator.inDomain(any(Resource.class))).thenThrow(new RuntimeException("Expected."));
        final Deskolemizer testDeskolemizer = new Deskolemizer(mockIdTranslator, null);
        final Triple testTriple = create(randomURIResource(), randomURIResource(), randomURIResource());
        testDeskolemizer.apply(testTriple);
    }

    @Test
    public void tripleWithSkolemSubjectShouldBeChanged() {
        final Triple testTriple = create(skolemNode, randomURIResource(), randomURIResource());

        when(mockIdTranslator.inDomain(any(Resource.class))).thenReturn(true);
        when(mockIdTranslator.asString(any(Resource.class))).thenReturn("non-fcr-URI");
        final Resource testSubject = testModel.asRDFNode(testTriple.getSubject()).asResource();
        when(mockIdTranslator.convert(testSubject)).thenReturn(mockSkolemFedoraResource);
        final Resource testObject = testModel.asRDFNode(testTriple.getObject()).asResource();
        when(mockIdTranslator.convert(testObject)).thenReturn(mockNonSkolemFedoraResource);

        final Deskolemizer testDeskolemizer = new Deskolemizer(mockIdTranslator, testModel);
        final Triple result = testDeskolemizer.apply(testTriple);
        assertNotEquals(testTriple, result);
        final Node deskolem = result.getSubject();
        assertTrue(deskolem.isBlank());
    }

    @Test
    public void statementWithSkolemObjectShouldBeChanged() {
        final Triple testTriple = create(randomURIResource(), randomURIResource(), skolemNode);

        when(mockIdTranslator.inDomain(any(Resource.class))).thenReturn(true);
        when(mockIdTranslator.asString(any(Resource.class))).thenReturn("non-fcr-URI");
        final Resource testSubject = testModel.asRDFNode(testTriple.getSubject()).asResource();
        when(mockIdTranslator.convert(testSubject)).thenReturn(mockNonSkolemFedoraResource);
        final Resource testObject = testModel.asRDFNode(testTriple.getObject()).asResource();
        when(mockIdTranslator.convert(testObject)).thenReturn(mockSkolemFedoraResource);

        final Deskolemizer testDeskolemizer = new Deskolemizer(mockIdTranslator, testModel);
        final Triple result = testDeskolemizer.apply(testTriple);
        assertNotEquals(testTriple, result);
        final Node deskolem = result.getObject();
        assertTrue(deskolem.isBlank());
    }

    @Test
    public void statementWithSameSkolemSubjectAndObjectShouldBeChanged() {

        final Triple testTriple = create(skolemNode, randomURIResource(), skolemNode);

        when(mockIdTranslator.inDomain(any(Resource.class))).thenReturn(true);
        when(mockIdTranslator.asString(any(Resource.class))).thenReturn("non-fcr-URI");
        final Resource testSubject = testModel.asRDFNode(testTriple.getSubject()).asResource();
        when(mockIdTranslator.convert(testSubject)).thenReturn(mockSkolemFedoraResource);
        final Resource testObject = testModel.asRDFNode(testTriple.getObject()).asResource();
        when(mockIdTranslator.convert(testObject)).thenReturn(mockSkolemFedoraResource);

        final Deskolemizer testDeskolemizer = new Deskolemizer(mockIdTranslator, testModel);
        final Triple result = testDeskolemizer.apply(testTriple);
        assertNotEquals(testTriple, result);
        final Node subject = result.getSubject();
        assertTrue(subject.isBlank());
        final Node object = result.getObject();
        assertTrue(object.isBlank());
        assertEquals(subject, object);
    }

    private static Node randomURIResource() {
        return createURI("test:/" + randomUUID());
    }
}
