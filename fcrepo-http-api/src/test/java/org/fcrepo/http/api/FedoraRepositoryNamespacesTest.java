/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.http.api;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.mockSession;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.http.api.repository.FedoraRepositoryNamespaces;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.RepositoryService;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * <p>FedoraRepositoryNamespacesTest class.</p>
 *
 * @author awoods
 */
public class FedoraRepositoryNamespacesTest {

    FedoraRepositoryNamespaces testObj;

    @Mock
    private RepositoryService mockService;

    @Mock
    private Dataset mockDataset;

    private final RdfStream testRdfStream = new RdfStream();

    private Session mockSession;

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new FedoraRepositoryNamespaces();
        setField(testObj, "repositoryService", mockService);
        setField(testObj, "uriInfo", getUriInfoImpl());
        mockSession = mockSession(testObj);
        setField(testObj, "session", mockSession);
    }

    @Test
    public void testGetNamespaces() throws RepositoryException {
        when(mockService.getNamespaceRegistryStream(any(Session.class), any(IdentifierTranslator.class)))
                .thenReturn(testRdfStream);
        assertEquals(testRdfStream, testObj.getNamespaces());
    }

    @Test
    public void testUpdateNamespaces() throws RepositoryException, IOException {

        final Model model = createDefaultModel();
        final Dataset mockDataset = DatasetFactory.create(model);
        when(mockService.getNamespaceRegistryDataset(any(Session.class), any(IdentifierTranslator.class))).thenReturn(
                mockDataset);

        testObj.updateNamespaces(new ByteArrayInputStream(
                "INSERT { <http://example.com/this> <http://example.com/is> \"abc\"} WHERE { }"
                        .getBytes()));

        assertEquals(1, model.size());
    }
}
