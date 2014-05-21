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
package org.fcrepo.http.commons.responses;

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.MediaType.valueOf;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.primaryTypePredicate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.core.DatasetImpl;

/**
 * <p>RdfProviderTest class.</p>
 *
 * @author awoods
 */
public class RdfProviderTest {

    final RdfProvider rdfProvider = new RdfProvider();

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

    @Test
    public void testIsWriteable() {
        assertTrue(
                "Gave false response to RdfProvider.isWriteable() that contained legitimate combination of parameters!",
                rdfProvider.isWriteable(Dataset.class, Dataset.class, null,
                        valueOf("application/rdf+xml")));
        assertFalse(
                "RdfProvider.isWriteable() should return false if asked to serialize anything other than Dataset!",
                rdfProvider.isWriteable(RdfProvider.class, RdfProvider.class,
                        null, valueOf("application/rdf+xml")));
        assertFalse(
                "RdfProvider.isWriteable() should return false to text/html!",
                rdfProvider.isWriteable(Dataset.class, Dataset.class, null,
                        TEXT_HTML_TYPE));
    }

    @Test
    public void testGetSize() {
        assertEquals("Returned wrong size from RdfProvider!", rdfProvider
                .getSize(null, null, null, null, null), -1);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWriteTo() throws WebApplicationException,
                             IllegalArgumentException, IOException {
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();

        rdfProvider.writeTo(testData, Dataset.class, mock(Type.class), null,
                valueOf("application/rdf+xml"), mock(MultivaluedMap.class),
                outStream);
        final byte[] results = outStream.toByteArray();
        assertTrue("Got no output from serialization!", results.length > 0);
        assertTrue("Couldn't find test RDF-object mentioned!", new String(
                results).contains("test:object"));
    }
}
