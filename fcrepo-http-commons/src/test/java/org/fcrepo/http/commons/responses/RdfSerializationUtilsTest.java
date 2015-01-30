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
package org.fcrepo.http.commons.responses;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.of;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.getFirstValueForPredicate;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.getAllValuesForPredicate;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.Model;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * <p>RdfSerializationUtilsTest class.</p>
 *
 * @author awoods
 */
public class RdfSerializationUtilsTest {

    private final UriInfo info = Mockito.mock(UriInfo.class);

    private final Model testData = createDefaultModel();

    private PathSegment segment;

    @Before
    public void setup() {
        testData.add(createResource("test:subject"),
                createProperty("test:predicate"),
                createTypedLiteral("test:object"));
        testData.add(createResource("test:subject"),
                createProperty("test:anotherPredicate"),
                createTypedLiteral("test:object1"));
        testData.add(createResource("test:subject"),
                 createProperty("test:anotherPredicate"),
                 createTypedLiteral("test:object2"));
        final List<PathSegment> segments = new ArrayList<>();
        segment = Mockito.mock(PathSegment.class);
        segments.add(segment);
        Mockito.when(info.getPathSegments()).thenReturn(segments);
    }

    @Test
    public void testGetFirstValueForPredicate() {
        final String foundValue =
            getFirstValueForPredicate(testData, createURI("test:subject"), createURI("test:predicate"));
        assertEquals("Didn't find correct value for predicate!", "test:object", foundValue);
    }

    @Test
    public void testGetAllValuesForPredicate() {
        final Iterator<String> foundValues =
            getAllValuesForPredicate(testData, createURI("test:subject"),
                createURI("test:anotherPredicate"));
        assertEquals("Didn't find correct values for predicate!", copyOf(foundValues),
            of("test:object1", "test:object2"));
    }

}
