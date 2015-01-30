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
package org.fcrepo.transform;

import org.fcrepo.transform.transformations.LDPathTransform;
import org.fcrepo.transform.transformations.SparqlQueryTransform;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.MediaType;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import static org.apache.jena.riot.WebContent.contentTypeSPARQLQuery;
import static org.fcrepo.transform.transformations.LDPathTransform.APPLICATION_RDF_LDPATH;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * <p>TransformationFactoryTest class.</p>
 *
 * @author cbeer
 */
public class TransformationFactoryTest {

    @Mock
    InputStream mockInputStream;

    TransformationFactory transformationFactory;

    @Before
    public void setUp() {
        initMocks(this);
        transformationFactory = new TransformationFactory();
    }

    @Test
    public void testLDPathCreation() {

        final Transformation<Map<String, Collection<Object>>> transform =
            transformationFactory.getTransform(MediaType.valueOf(APPLICATION_RDF_LDPATH), mockInputStream);

        assertEquals(new LDPathTransform(mockInputStream), transform);

    }

    @Test
    public void testSparqlCreation() {

        final Transformation<Map<String, Collection<Object>>> transform =
            transformationFactory.getTransform(MediaType.valueOf(contentTypeSPARQLQuery), mockInputStream);
        assertEquals(new SparqlQueryTransform(mockInputStream), transform);

    }


    @Test(expected = UnsupportedOperationException.class)
    public void testOtherCreation() {

        transformationFactory.getTransform(MediaType.valueOf("some/mime-type"), mockInputStream);

    }
}
