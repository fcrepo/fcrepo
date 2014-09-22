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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.Symbol;

/**
 * <p>RdfSerializationUtilsTest class.</p>
 *
 * @author awoods
 */
public class RdfSerializationUtilsTest {

    private final UriInfo info = Mockito.mock(UriInfo.class);

    private final Dataset testData = new DatasetImpl(createDefaultModel());

    private PathSegment segment;

    @Before
    public void setup() {
        testData.asDatasetGraph().getDefaultGraph().add(
                new Triple(createURI("test:subject"),
                        createURI("test:predicate"),
                        createLiteral("test:object")));

        final List<PathSegment> segments = new ArrayList<>();
        segment = Mockito.mock(PathSegment.class);
        segments.add(segment);
        Mockito.when(info.getPathSegments()).thenReturn(segments);
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
        final MultivaluedMap<?, ?> headers = new MultivaluedHashMap<>();
        Mockito.when(segment.getPath()).thenReturn("/fedora");
        setCachingHeaders((MultivaluedMap<String, Object>) headers, testData, info);
        final List<?> cacheControlHeaders = headers.get("Cache-Control");
        assertEquals("Two cache control headers expected: ", 2, cacheControlHeaders.size());
        assertEquals("max-age=0 expected", "max-age=0", cacheControlHeaders.get(0));
        assertEquals("must-revalidate expected", "must-revalidate", cacheControlHeaders.get(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetNoLastModifiedHeaderWithinTransaction() {
        final MultivaluedMap<?, ?> headers = new MultivaluedHashMap<>();

        final Model m = createDefaultModel();
        final Calendar c = Calendar.getInstance();
        m.add(m.createResource("test:subject"),
              m.createProperty(lastModifiedPredicate.getURI()),
              m.createTypedLiteral(c));
        final Dataset testDatasetWithLastModified = DatasetFactory.create(m);
        final Context context = testDatasetWithLastModified.getContext();
        context.set(Symbol.create("uri"), "test:subject");
        Mockito.when(segment.getPath()).thenReturn("tx:abc");

        setCachingHeaders((MultivaluedMap<String, Object>) headers, testDatasetWithLastModified, info);
        assertNull("No Last-Modified header expected during transaction", headers.get("Last-Modified"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetCachingHeadersWithLastModified() {
        final MultivaluedMap<?, ?> headers = new MultivaluedHashMap<>();

        final Model m = createDefaultModel();

        final Calendar c = Calendar.getInstance();
        m.add(m.createResource("test:subject"), m
                .createProperty(lastModifiedPredicate.getURI()), m
                .createTypedLiteral(c));
        final Dataset testDatasetWithLastModified = DatasetFactory.create(m);
        final Context context = testDatasetWithLastModified.getContext();
        context.set(Symbol.create("uri"), "test:subject");
        Mockito.when(segment.getPath()).thenReturn("/fedora");

        setCachingHeaders((MultivaluedMap<String, Object>) headers,
                testDatasetWithLastModified, info);
        assertTrue(new DateTime(c).withMillisOfSecond(0).isEqual(
                RFC2822DATEFORMAT.parseDateTime((String) headers.get(
                        "Last-Modified").get(0))));
    }

}
