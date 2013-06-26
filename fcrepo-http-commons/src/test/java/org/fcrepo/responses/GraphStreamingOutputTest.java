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

package org.fcrepo.responses;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.http.RDFMediaType.NTRIPLES_TYPE;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.ws.rs.WebApplicationException;

import org.junit.Test;
import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.sparql.modify.GraphStoreBasic;
import com.hp.hpl.jena.update.GraphStore;

public class GraphStreamingOutputTest {

    private static final Logger logger =
            getLogger(GraphStreamingOutputTest.class);

    @Test
    public void testStuff() throws WebApplicationException, IOException,
        RepositoryException {
        final GraphStore graphStore =
                new GraphStoreBasic(DatasetFactory.create(createDefaultModel()));
        final Graph g = new GraphMem();
        g.add(new Triple(createURI("test:subject"),
                createURI("test:predicate"), createURI("test:object")));
        graphStore.setDefaultGraph(g);
        final GraphStoreStreamingOutput test =
                new GraphStoreStreamingOutput(graphStore, NTRIPLES_TYPE);
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            test.write(out);
            final String serialized = out.toString();
            logger.debug("Created serialized RDF: \n {}", serialized);
            assertTrue("Couldn't find test subject!", serialized
                    .contains("test:subject"));
            assertTrue("Couldn't find test predicate!", serialized
                    .contains("test:predicate"));
            assertTrue("Couldn't find test object!", serialized
                    .contains("test:object"));
        }
    }
}
