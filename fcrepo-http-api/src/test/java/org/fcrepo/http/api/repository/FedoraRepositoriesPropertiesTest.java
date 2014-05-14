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
package org.fcrepo.http.api.repository;

import static org.fcrepo.http.commons.test.util.TestHelpers.getUriInfoImpl;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.NodeService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.query.Dataset;

/**
 * <p>FedoraRepositoriesPropertiesTest class.</p>
 *
 * @author awoods
 */
public class FedoraRepositoriesPropertiesTest {

    private FedoraRepositoriesProperties testObj;

    @Mock
    private NodeService mockNodes;

    @Mock
    private FedoraObject mockObject;

    @Mock
    private Dataset mockDataset;

    @Mock
    private Session mockSession;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testObj = new FedoraRepositoriesProperties();
        setField(testObj, "session", mockSession);
        setField(testObj, "nodeService", mockNodes);
        setField(testObj, "uriInfo", getUriInfoImpl());
    }

    @Test
    public void testSparqlUpdate() throws RepositoryException, IOException {
        final InputStream mockStream =
            new ByteArrayInputStream("my-sparql-statement".getBytes());
        when(mockNodes.getObject(mockSession, "/")).thenReturn(mockObject);
        when(
                mockObject.updatePropertiesDataset(any(IdentifierTranslator.class),
                        any(String.class))).thenReturn(mockDataset);
        testObj.updateSparql(mockStream);

        verify(mockObject).updatePropertiesDataset(any(IdentifierTranslator.class),
                eq("my-sparql-statement"));
        verify(mockSession).save();
        verify(mockSession).logout();
    }
}
