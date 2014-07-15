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
package org.fcrepo.transform.sparql;

import com.hp.hpl.jena.rdf.model.Model;
import org.fcrepo.http.commons.test.util.TestHelpers;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.MockitoAnnotations.initMocks;

import javax.jcr.Session;
import javax.ws.rs.core.UriInfo;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.kernel.RdfLexicon.HAS_SPARQL_ENDPOINT;
import static org.fcrepo.kernel.RdfLexicon.SPARQL_SD_NAMESPACE;
import static org.junit.Assert.assertTrue;

/**
 * <p>SparqlServiceDescriptionTest class.</p>
 *
 * @author lsitu
 */
public class SparqlServiceDescriptionTest {
    private SparqlServiceDescription testObj;

    private UriInfo uriInfo;

    @Mock
    Session mockSession;

    @Before
    public void setUp() {
        initMocks(this);

        uriInfo = TestHelpers.getUriInfoImpl();
        testObj = new SparqlServiceDescription(mockSession, uriInfo);
    }

    @Test
    public void shouldGenerateServiceDescriptionRdf() {
        final RdfStream rdfStream = testObj.createServiceDescription();
        final Model model = rdfStream.asModel();
        assertTrue(model.listStatements().toList().size() > 0);
        assertTrue(model.listResourcesWithProperty(
                createProperty(RdfLexicon.RDF_NAMESPACE + "type"),
                createProperty(SPARQL_SD_NAMESPACE + "Service")).hasNext());
        assertTrue(model.listResourcesWithProperty(HAS_SPARQL_ENDPOINT).hasNext());
        assertTrue(model.listResourcesWithProperty(
                createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "supportedLanguage")).hasNext());
        assertTrue(model.listResourcesWithProperty(
                createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "resultFormat")).hasNext());
        assertTrue(model.listResourcesWithProperty(
                createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "feature")).hasNext());
    }
}
