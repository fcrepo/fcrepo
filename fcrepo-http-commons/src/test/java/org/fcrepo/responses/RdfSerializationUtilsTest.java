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

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.responses.RdfSerializationUtils.getFirstValueForPredicate;
import static org.fcrepo.responses.RdfSerializationUtils.setCachingHeaders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class RdfSerializationUtilsTest {

    Dataset testData = new DatasetImpl(createDefaultModel());

    {
        testData.asDatasetGraph().getDefaultGraph().add(
                new Triple(createURI("test:subject"),
                        createURI("test:predicate"),
                        createLiteral("test:object")));

    }

    @Test
    public void testGetFirstValueForPredicate() {
        final String foundValue =
                getFirstValueForPredicate(testData, ANY,
                        createURI("test:predicate"));
        assertEquals("Didn't find correct value for predicate!", foundValue,
                "test:object");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetCachingHeaders() {
        final MultivaluedMap<?, ?> headers = new MultivaluedMapImpl();
        setCachingHeaders((MultivaluedMap<String, Object>) headers, testData);
        assertTrue(headers.get("Cache-Control").size() > 0);
    }

}
