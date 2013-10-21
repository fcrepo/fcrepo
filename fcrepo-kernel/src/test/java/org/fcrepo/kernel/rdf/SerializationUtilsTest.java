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

package org.fcrepo.kernel.rdf;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.rdf.SerializationUtils.getDatasetSubject;
import static org.fcrepo.kernel.rdf.SerializationUtils.setDatasetSubject;
import static org.fcrepo.kernel.rdf.SerializationUtils.subjectKey;
import static org.fcrepo.kernel.rdf.SerializationUtils.unifyDatasetModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;

public class SerializationUtilsTest {


    @Test
    public void testSetDatasetSubject() {
        final Dataset dataset = DatasetFactory.create(createDefaultModel());

        setDatasetSubject(dataset, "some-uri");

        assertEquals("some-uri", dataset.getContext().getAsString(subjectKey));
    }

    @Test
    public void testGetDatasetSubject() {
        final Dataset dataset = DatasetFactory.create(createDefaultModel());

        setDatasetSubject(dataset, "some-uri");

        assertEquals(createURI("some-uri"), getDatasetSubject(dataset));
    }

    @Test
    public void testGetDatasetSubjectWithoutContext() {
        final Dataset dataset = DatasetFactory.create(createDefaultModel());

        assertEquals(null, getDatasetSubject(dataset));
    }

    @Test
    public void testUnifyDatasetModels() {
        final Model model = createDefaultModel();
        model.setNsPrefix("a", "b");
        model.add(model.createResource(), model.createProperty("xyz"), "abc");

        final Dataset dataset = DatasetFactory.create(model);

        final Model model2 = createDefaultModel();
        model.add(model.createResource(), model.createProperty("abc"), "xyz");

        dataset.addNamedModel("xyz", model2);


        final Model mergedModel = unifyDatasetModel(dataset);

        assertEquals(model.getNsPrefixMap(), mergedModel.getNsPrefixMap());
        assertTrue(mergedModel.containsAll(model));
        assertTrue(mergedModel.containsAll(model2));
    }
}
