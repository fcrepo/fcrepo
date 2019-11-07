/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.services.functions;

import static org.fcrepo.kernel.api.RdfLexicon.BASIC_CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINER;
import static org.fcrepo.kernel.api.RdfLexicon.DEFAULT_INTERACTION_MODEL;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RESOURCE;
import static org.fcrepo.kernel.impl.services.functions.resourceServiceFunctions.determineInteractionModel;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.Silent.class)
public class resourceServiceFunctionsTest {

    private final List<String> stringTypesNotValid = Stream.of(MEMENTO_TYPE, RESOURCE.toString(),
            FEDORA_RESOURCE.toString()).collect(Collectors.toList());

    private final List<String> stringTypesValid = Stream.of(MEMENTO_TYPE, CONTAINER.toString(),
            BASIC_CONTAINER.toString()).collect(Collectors.toList());

    private final String defaultInteractionModel = DEFAULT_INTERACTION_MODEL.toString();

    @Test
    public void testSendingValidInteractionModel() {
        // If you provide a valid interaction model, you should always get it back.
        final String expected = BASIC_CONTAINER.toString();
        final String model1 = determineInteractionModel(stringTypesValid, false, false,
                false);
        assertEquals(expected, model1);
        final String model2 = determineInteractionModel(stringTypesValid, false, false,
                true);
        assertEquals(expected, model2);
        final String model3 = determineInteractionModel(stringTypesValid, false, true,
                false);
        assertEquals(expected, model3);
        final String model4 = determineInteractionModel(stringTypesValid, false, true,
                true);
        assertEquals(expected, model4);
        final String model5 = determineInteractionModel(stringTypesValid, true, false,
                false);
        assertEquals(expected, model5);
        final String model6 = determineInteractionModel(stringTypesValid, true, false,
                true);
        assertEquals(expected, model6);
        final String model7 = determineInteractionModel(stringTypesValid, true, true,
                false);
        assertEquals(expected, model7);
        final String model8 = determineInteractionModel(stringTypesValid, true, true,
                true);
        assertEquals(expected, model8);
    }

    @Test
    public void testSendingInvalidInteractionModelIsNotRdf() {
        final String model = determineInteractionModel(stringTypesNotValid, false, false,
                false);
        assertEquals(defaultInteractionModel, model);
    }

    @Test
    public void testNotRdfNoContentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = determineInteractionModel(stringTypesNotValid, false, false,
                true);
        assertEquals(expected, model);
    }

    @Test
    public void testNotRdfContentPresentNotExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = determineInteractionModel(stringTypesNotValid, false, true,
                false);
        assertEquals(expected, model);
    }

    @Test
    public void testNotRdfContentPresentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = determineInteractionModel(stringTypesNotValid, false, true,
                true);
        assertEquals(expected, model);
    }

    @Test
    public void testIsRdfNoContentNotExternal() {
        final String model = determineInteractionModel(stringTypesNotValid, true, false,
                false);
        assertEquals(defaultInteractionModel, model);
    }

    @Test
    public void testIsRdfNoContentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = determineInteractionModel(stringTypesNotValid, true, false,
                true);
        assertEquals(expected, model);
    }

    @Test
    public void testIsRdfHasContentNotExternal() {
        final String model = determineInteractionModel(stringTypesNotValid, true, true,
                false);
        assertEquals(defaultInteractionModel, model);
    }

    @Test
    public void testIsRdfHasContentIsExternal() {
        final String expected = NON_RDF_SOURCE.toString();
        final String model = determineInteractionModel(stringTypesNotValid, true, true,
                true);
        assertEquals(expected, model);
    }
}
