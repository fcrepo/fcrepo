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

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createStatement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.models.FedoraResource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class HashURIDetectorTest {

    @Mock
    private IdentifierConverter<Resource, FedoraResource> mockIdTranslator;

    private HashURIDetector testHashURIDetector;

    @Before
    public void setUp() {
        testHashURIDetector = new HashURIDetector(mockIdTranslator);
    }

    @Test
    public void operationWithoutHashURIs() {
        Resource testSubject = createResource();
        Resource testObject = createResource();
        final Property testPredicate = createProperty("info:test");
        Statement testStatement =
                createStatement(testSubject, testPredicate, testObject);
        final HashURIDetector testHashURIDetector = new HashURIDetector(mockIdTranslator);
        assertEquals(testStatement, testHashURIDetector.apply(testStatement));
        assertTrue(testHashURIDetector.get().isEmpty());

        testSubject = createResource("info:/test");
        testStatement =
                createStatement(testSubject, testPredicate, testObject);
        when(mockIdTranslator.inDomain(testSubject)).thenReturn(false);
        assertEquals(testStatement, testHashURIDetector.apply(testStatement));
        assertTrue(testHashURIDetector.get().isEmpty());

        testObject = createResource("info:/test");
        testStatement =
                createStatement(testSubject, testPredicate, testObject);
        when(mockIdTranslator.inDomain(testObject)).thenReturn(false);
        assertEquals(testStatement, testHashURIDetector.apply(testStatement));
        assertTrue(testHashURIDetector.get().isEmpty());

        when(mockIdTranslator.inDomain(testSubject)).thenReturn(true);
        assertEquals(testStatement, testHashURIDetector.apply(testStatement));
        assertTrue(testHashURIDetector.get().isEmpty());

        when(mockIdTranslator.inDomain(testObject)).thenReturn(true);
        assertEquals(testStatement, testHashURIDetector.apply(testStatement));
        assertTrue(testHashURIDetector.get().isEmpty());

        when(mockIdTranslator.inDomain(testPredicate)).thenReturn(true);
        assertEquals(testStatement, testHashURIDetector.apply(testStatement));
        assertTrue(testHashURIDetector.get().isEmpty());
    }

    @Test
    public void operationWithHashURISubject() {
        final Resource testSubject = createResource("info:test#hash");
        when(mockIdTranslator.inDomain(testSubject)).thenReturn(true);
        final Statement testStatement =
                createStatement(testSubject, createProperty("info:test"), createResource());

        assertEquals(testStatement, testHashURIDetector.apply(testStatement));
        assertEquals(1, testHashURIDetector.get().size());
        assertEquals(testSubject, testHashURIDetector.get().iterator().next());
    }

    @Test
    public void operationWithHashURIPredicate() {
        final Property testPredicate = createProperty("info:test#hash");
        when(mockIdTranslator.inDomain(testPredicate)).thenReturn(true);
        final Statement testStatement =
                createStatement(createResource(), testPredicate, createResource());

        assertEquals(testStatement, testHashURIDetector.apply(testStatement));
        assertEquals(1, testHashURIDetector.get().size());
        assertEquals(testPredicate, testHashURIDetector.get().iterator().next());
    }

    @Test
    public void operationWithHashURIObject() {
        final Resource testObject = createResource("info:test#hash");
        when(mockIdTranslator.inDomain(testObject)).thenReturn(true);
        final Statement testStatement =
                createStatement(createResource(), createProperty("info:test"), testObject);

        assertEquals(testStatement, testHashURIDetector.apply(testStatement));
        assertEquals(1, testHashURIDetector.get().size());
        assertEquals(testObject, testHashURIDetector.get().iterator().next());
    }
}
