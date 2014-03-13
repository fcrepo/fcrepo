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
package org.fcrepo.transform;

import org.apache.jena.riot.WebContent;
import org.fcrepo.transform.transformations.LDPathTransform;
import org.fcrepo.transform.transformations.SparqlQueryTransform;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.MediaType;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

public class TransformationFactoryTest {

    @Mock
    InputStream mockInputStream;

    TransformationFactory transformationFactory;

    @Before
    public void setUp() throws NoSuchMethodException, SecurityException {
        initMocks(this);
        transformationFactory = new TransformationFactory();
    }

    @Test
    public void testLDPathCreation() {

        final Transformation transform = transformationFactory.getTransform(MediaType.valueOf(LDPathTransform.APPLICATION_RDF_LDPATH), mockInputStream);

        assertEquals(new LDPathTransform(mockInputStream), transform);

    }

    @Test
    public void testSparqlCreation() {

        final Transformation transform = transformationFactory.getTransform(MediaType.valueOf(WebContent.contentTypeSPARQLQuery), mockInputStream);
        assertEquals(new SparqlQueryTransform(mockInputStream), transform);

    }


    @Test(expected = UnsupportedOperationException.class)
    public void testOtherCreation() {

        transformationFactory.getTransform(MediaType.valueOf("some/mime-type"), mockInputStream);

    }
}
