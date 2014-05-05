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

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.RFC2822DATEFORMAT;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.getFirstValueForPredicate;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.lastModifiedPredicate;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.setCachingHeaders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import javax.ws.rs.core.MultivaluedMap;

import org.joda.time.DateTime;
import org.junit.Test;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.Symbol;
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

    @SuppressWarnings("unchecked")
    @Test
    public void testSetCachingHeadersWithLastModified() {
        final MultivaluedMap<?, ?> headers = new MultivaluedMapImpl();

        final Model m = createDefaultModel();

        final Calendar c = Calendar.getInstance();
        m.add(m.createResource("test:subject"), m
                .createProperty(lastModifiedPredicate.getURI()), m
                .createTypedLiteral(c));
        final Dataset testDatasetWithLastModified = DatasetFactory.create(m);
        final Context context = testDatasetWithLastModified.getContext();
        context.set(Symbol.create("uri"), "test:subject");

        setCachingHeaders((MultivaluedMap<String, Object>) headers,
                testDatasetWithLastModified);
        assertTrue(new DateTime(c).withMillisOfSecond(0).isEqual(
                RFC2822DATEFORMAT.parseDateTime((String) headers.get(
                        "Last-Modified").get(0))));
    }

}
