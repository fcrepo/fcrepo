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
package org.fcrepo.kernel.impl.rdf.impl;

import static org.fcrepo.kernel.RdfLexicon.DESCRIBED_BY;
import static org.fcrepo.kernel.RdfLexicon.DESCRIBES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.testutilities.TestPropertyIterator;
import org.fcrepo.kernel.models.FedoraBinary;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.models.NonRdfSourceDescription;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
/**
 * <p>
 * PropertiesRdfContextTest class.
 * </p>
 *
 * @author yinlinchen
 */
public class PropertiesRdfContextTest {

    @Mock
    private FedoraResource mockResource;

    @Mock
    private FedoraBinary mockBinary;

    @Mock
    private Node mockNode;

    @Mock
    private Node mockBinaryNode;

    @Mock
    private NonRdfSourceDescription mockNonRdfSourceDescription;

    @Mock
    private Session mockSession;

    private IdentifierConverter<Resource, FedoraResource> idTranslator;

    private Resource mockContentSubject;

    private Resource mockSubject;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockNode.getProperties()).thenReturn(new TestPropertyIterator());

        when(mockBinary.getNode()).thenReturn(mockBinaryNode);
        when(mockBinaryNode.getSession()).thenReturn(mockSession);
        when(mockBinary.getDescription()).thenReturn(mockNonRdfSourceDescription);
        when(mockBinaryNode.getProperties()).thenReturn(new TestPropertyIterator());

        idTranslator = new DefaultIdentifierTranslator(mockSession);
        mockSubject = idTranslator.reverse().convert(mockResource);
        mockContentSubject = idTranslator.reverse().convert(mockBinary);

    }


    /*
     * (non-Javadoc) test the nonRdfSource response are included both NonRdfSourceDescription and RdfSourceDescription
     */
    @Test
    public void testFedoraBinaryProperties() throws RepositoryException {
        final Model results = new PropertiesRdfContext(mockBinary, idTranslator).asModel();

        assertTrue("Response contains NonRdfSourceDescription", results
                .contains(mockSubject, DESCRIBES, mockContentSubject));

        assertTrue("Response contains RdfSourceDescription", results
                .contains(mockContentSubject, DESCRIBED_BY, mockSubject));

    }

    /*
     * (non-Javadoc) test the FedoraResource response are included only RdfSourceDescription
     */
    @Test
    public void testFedoraResourceProperties() throws RepositoryException {

        final Model results = new PropertiesRdfContext(mockResource, idTranslator).asModel();
        assertTrue("Response contains RdfSourceDescription", results
                .contains(mockSubject, DESCRIBED_BY, mockContentSubject));
        assertFalse("Response does not contains NonRdfSourceDescription", results
                .contains(mockSubject, DESCRIBES, mockContentSubject));

    }

}