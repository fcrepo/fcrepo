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
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RDF_XML;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_TSV;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_XML;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_UNKNOWN;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RDF_NT;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RDF_TTL;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_BIO;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_CSV;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_JSON;
import static javax.ws.rs.core.MediaType.valueOf;
import static org.apache.jena.riot.WebContent.contentTypeNTriples;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeResultsBIO;
import static org.apache.jena.riot.WebContent.contentTypeResultsJSON;
import static org.apache.jena.riot.WebContent.contentTypeResultsXML;
import static org.apache.jena.riot.WebContent.contentTypeTextCSV;
import static org.apache.jena.riot.WebContent.contentTypeTextTSV;
import static org.apache.jena.riot.WebContent.contentTypeTurtleAlt2;
import static org.fcrepo.kernel.api.RdfLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.fcrepo.transform.http.responses.ResultSetStreamingOutput.getResultsFormat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.core.DatasetImpl;

import javax.ws.rs.core.MediaType;

/**
 * <p>ResultSetStreamingOutputTest class.</p>
 *
 * @author cbeer
 */
public class ResultSetStreamingOutputTest {

    @Mock
    ResultSet mockResultSet;

    ResultSetStreamingOutput testObj;

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

    @Before
    public void setUp() {
        initMocks(this);
        testObj = new ResultSetStreamingOutput();
    }

    @Test
    public void testWrite() throws Exception {

        final Query sparqlQuery =
            QueryFactory.create("SELECT ?x WHERE { ?x ?y ?z }");

        try (final QueryExecution testResult =
                QueryExecutionFactory.create(sparqlQuery, testData)) {
            final ResultSet resultSet = testResult.execSelect();


            try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                testObj.writeTo(resultSet, null, null, null,
                        valueOf(contentTypeTextTSV), null, out);

                final String serialized = out.toString();
                assertTrue(serialized.contains("test:subject"));
            }
        }
    }

    @Test
    public void testWriteWithRDFFormat() throws Exception {

        final Query sparqlQuery =
            QueryFactory.create("SELECT ?x WHERE { ?x ?y ?z }");

        try (final QueryExecution testResult =
                QueryExecutionFactory.create(sparqlQuery, testData)) {
            final ResultSet resultSet = testResult.execSelect();

            try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                testObj.writeTo(resultSet, null, null, null,
                        valueOf(contentTypeRDFXML), null, out);
                final String serialized = out.toString();
                assertTrue(serialized.contains("rs:ResultSet"));
            }
        }
    }

    @Test
    public void testGetResultsFormat() {
        assertEquals(FMT_RS_TSV, getResultsFormat(valueOf(contentTypeTextTSV)));
        assertEquals(FMT_UNKNOWN, getResultsFormat(valueOf("some/type")));
        assertEquals(FMT_RS_CSV, getResultsFormat(valueOf(contentTypeTextCSV)));
        assertEquals(FMT_RS_JSON, getResultsFormat(valueOf(contentTypeResultsJSON)));
        assertEquals(FMT_RS_BIO, getResultsFormat(valueOf(contentTypeResultsBIO)));
        assertEquals(FMT_RDF_TTL, getResultsFormat(valueOf(contentTypeTurtleAlt2)));
        assertEquals(FMT_RDF_NT, getResultsFormat(valueOf(contentTypeNTriples)));
        assertEquals(FMT_RDF_XML, getResultsFormat(valueOf(contentTypeRDFXML)));
        assertEquals(FMT_RS_XML, getResultsFormat(valueOf(contentTypeResultsXML)));
    }

    @Test
    public void testNonWriteable() {
        assertFalse(testObj.isWriteable(null, null, null,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    @Test
    public void testNegativeSize() {
        assertTrue(testObj.getSize(null, null, null, null,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE) == -1);
    }
}
