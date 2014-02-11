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
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.Symbol;

/**
 * Utilities for serializing RDF.
 *
 * @author ajs6f
 * @date Aug 22, 2013
 */
public final class SerializationUtils {

    private static final Logger LOGGER = getLogger(SerializationUtils.class);

    public static final Symbol subjectKey = Symbol.create("uri");

    private SerializationUtils() {
        throw new AssertionError(this.getClass().getName()
                + " is a helper class which should never be instantiated!");
    }

    /**
     * Set the subject of the dataset by injecting a context "uri"
     *
     * @param rdf
     * @return
     */
    public static void setDatasetSubject(final Dataset rdf, final String uri) {
        final Context context = rdf.getContext();
        context.set(subjectKey, uri);
    }

    /**
     * Get the subject of the dataset, given by the context's "uri"
     *
     * @param rdf
     * @return
     */
    public static Node getDatasetSubject(final Dataset rdf) {
        final Context context = rdf.getContext();
        final String uri = context.getAsString(subjectKey);
        LOGGER.debug("uri from context: {}", uri);
        if (uri != null) {
            return createURI(uri);
        }
        return null;
    }

    /**
     * Merge a dataset's named graphs into a single model, in order to provide a
     * cohesive serialization.
     *
     * @param dataset
     * @return
     */
    public static Model unifyDatasetModel(final Dataset dataset) {
        final Iterator<String> iterator = dataset.listNames();
        Model model = createDefaultModel();

        model = model.union(dataset.getDefaultModel());

        while (iterator.hasNext()) {
            final String modelName = iterator.next();
            LOGGER.debug("Serializing model {}", modelName);
            model = model.union(dataset.getNamedModel(modelName));
        }

        model.setNsPrefixes(dataset.getDefaultModel().getNsPrefixMap());
        return model;
    }
}
