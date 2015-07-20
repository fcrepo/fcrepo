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
package org.fcrepo.transform.http.responses;

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.valueOf;
import static org.apache.jena.riot.WebContent.contentTypeResultsXML;
import static org.fcrepo.kernel.api.RdfLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.core.DatasetImpl;

/**
 * <p>QueryExecutionProviderTest class.</p>
 *
 * @author cbeer
 */
@RunWith(MockitoJUnitRunner.class)
public class QueryExecutionProviderTest {

    @Mock
    private MultivaluedMap<String, Object> mockMultivaluedMap;

    final QueryExecutionProvider testObj = new QueryExecutionProvider();

    Dataset testData = new DatasetImpl(createDefaultModel());

    {
        testData.asDatasetGraph().getDefaultGraph().add(
                new Triple(createURI("test:subject"),
                        createURI("test:predicate"),
                        createLiteral("test:object")));
        testData.asDatasetGraph().getDefaultGraph().add(
                new Triple(createURI("test:subject"),
                        createURI(getRDFNamespaceForJcrNamespace(JCR_NAMESPACE) + "primaryType"),
                        createLiteral("nt:file")));

    }

    @Test
    public void testWriteTo() throws WebApplicationException,
            IllegalArgumentException {

        final Query sparqlQuery =
            QueryFactory.create("SELECT ?x WHERE { ?x ?y ?z }");

        try (final QueryExecution testResult =
                QueryExecutionFactory.create(sparqlQuery, testData)) {

            final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            testObj.writeTo(testResult, QueryExecution.class, mock(Type.class),
                    null, valueOf(contentTypeResultsXML),
                    mockMultivaluedMap, outStream);
            final byte[] results = outStream.toByteArray();
            assertTrue("Got no output from serialization!", results.length > 0);
            assertTrue("Couldn't find test RDF-object mentioned!", new String(
                    results).contains("test:subject"));
        }
    }

    @Test
    public void testGetSize() {
        assertEquals("Returned wrong size from QueryExecutionProvider!",
                testObj.getSize(null, null, null, null, null), -1);

    }

    @Test
    public void testIsWritable() {
        assertTrue(
                "Gave false response to QueryExecutionProvider.isWriteable() that contained a legitimate combination " +
                        "of parameters!",
                testObj.isWriteable(QueryExecution.class, QueryExecution.class,
                                    null, valueOf(contentTypeResultsXML)));
        assertFalse(
                "RdfProvider.isWriteable() should return false if asked to serialize anything other than " +
                        "QueryExecution!",
                testObj.isWriteable(QueryExecutionProvider.class,
                                    QueryExecutionProvider.class, null,
                                    valueOf(contentTypeResultsXML)));
        assertFalse(
                "RdfProvider.isWriteable() should return false to text/html!",
                testObj.isWriteable(QueryExecution.class, QueryExecution.class,
                        null, TEXT_HTML_TYPE));

    }
}
