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

package org.fcrepo.rdf;

import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SerializationUtilsTest {


    @Test
    public void testSetDatasetSubject() {
        final Dataset dataset = DatasetFactory.create(ModelFactory.createDefaultModel());

        SerializationUtils.setDatasetSubject(dataset, "some-uri");

        assertEquals("some-uri", dataset.getContext().getAsString(SerializationUtils.subjectKey));
    }

    @Test
    public void testGetDatasetSubject() {
        final Dataset dataset = DatasetFactory.create(ModelFactory.createDefaultModel());

        SerializationUtils.setDatasetSubject(dataset, "some-uri");

        assertEquals(NodeFactory.createURI("some-uri"), SerializationUtils.getDatasetSubject(dataset));
    }

    @Test
    public void testGetDatasetSubjectWithoutContext() {
        final Dataset dataset = DatasetFactory.create(ModelFactory.createDefaultModel());

        assertEquals(null, SerializationUtils.getDatasetSubject(dataset));
    }

    @Test
    public void testUnifyDatasetModels() {
        final Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("a", "b");
        model.add(model.createResource(), model.createProperty("xyz"), "abc");

        final Dataset dataset = DatasetFactory.create(model);

        final Model model2 = ModelFactory.createDefaultModel();
        model.add(model.createResource(), model.createProperty("abc"), "xyz");

        dataset.addNamedModel("xyz", model2);


        final Model mergedModel = SerializationUtils.unifyDatasetModel(dataset);

        assertEquals(model.getNsPrefixMap(), mergedModel.getNsPrefixMap());
        assertTrue(mergedModel.containsAll(model));
        assertTrue(mergedModel.containsAll(model2));
    }
}
