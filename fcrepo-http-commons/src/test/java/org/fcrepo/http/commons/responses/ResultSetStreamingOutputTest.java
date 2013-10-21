/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.http.commons.responses;

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_RS_TSV;
import static com.hp.hpl.jena.sparql.resultset.ResultsFormat.FMT_UNKNOWN;
import static org.apache.jena.riot.WebContent.contentTypeRDFXML;
import static org.apache.jena.riot.WebContent.contentTypeTextTSV;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.primaryTypePredicate;
import static org.fcrepo.http.commons.responses.ResultSetStreamingOutput.getResultsFormat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayOutputStream;

import javax.ws.rs.core.MediaType;

import org.fcrepo.http.commons.responses.ResultSetStreamingOutput;
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
                new Triple(createURI("test:subject"), primaryTypePredicate,
                        createLiteral("nt:file")));

    }

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testWrite() throws Exception {

        final Query sparqlQuery =
            QueryFactory.create("SELECT ?x WHERE { ?x ?y ?z }");

        final QueryExecution testResult =
            QueryExecutionFactory.create(sparqlQuery, testData);

        try {
            final ResultSet resultSet = testResult.execSelect();

            testObj =
                new ResultSetStreamingOutput(resultSet, MediaType
                        .valueOf(contentTypeTextTSV));

            try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                testObj.write(out);
                final String serialized = out.toString();
                assertTrue(serialized.contains("test:subject"));
            }
        } finally {
            testResult.close();
        }
    }

    @Test
    public void testWriteWithRDFFormat() throws Exception {

        final Query sparqlQuery =
            QueryFactory.create("SELECT ?x WHERE { ?x ?y ?z }");

        final QueryExecution testResult =
            QueryExecutionFactory.create(sparqlQuery, testData);

        try {
            final ResultSet resultSet = testResult.execSelect();

            testObj =
                new ResultSetStreamingOutput(resultSet, MediaType
                        .valueOf(contentTypeRDFXML));

            try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                testObj.write(out);
                final String serialized = out.toString();
                assertTrue(serialized.contains("rs:ResultSet"));
            }
        } finally {
            testResult.close();
        }
    }

    @Test
    public void testGetResultsFormat() throws Exception {
        assertEquals(FMT_RS_TSV, getResultsFormat(MediaType
                        .valueOf(contentTypeTextTSV)));
        assertEquals(FMT_UNKNOWN, getResultsFormat(MediaType.valueOf("some/type")));
    }
}
