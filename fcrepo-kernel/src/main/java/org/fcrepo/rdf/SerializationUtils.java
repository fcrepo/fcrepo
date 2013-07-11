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

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.Symbol;
import org.slf4j.Logger;

import java.util.Iterator;

import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static org.slf4j.LoggerFactory.getLogger;

public class SerializationUtils {

    private static final Logger logger = getLogger(SerializationUtils.class);

    public static final Symbol subjectKey = Symbol.create("uri");

    /**
     * Set the subject of the dataset by injecting a context "uri"
     *
     * @param rdf
     * @return
     */
    public static void setDatasetSubject(final Dataset rdf, final String uri) {
        Context context = rdf.getContext();
        context.set(subjectKey, uri);
    }

    /**
     * Get the subject of the dataset, given by the context's "uri"
     *
     * @param rdf
     * @return
     */
    public static Node getDatasetSubject(final Dataset rdf) {
        Context context = rdf.getContext();
        String uri = context.getAsString(subjectKey);
        logger.debug("uri from context: {}", uri);
        if (uri != null) {
            return createURI(uri);
        } else {
            return null;
        }
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
        Model model = ModelFactory.createDefaultModel();

        model = model.union(dataset.getDefaultModel());

        while (iterator.hasNext()) {
            final String modelName = iterator.next();
            logger.debug("Serializing model {}", modelName);
            model = model.union(dataset.getNamedModel(modelName));
        }

        model.setNsPrefixes(dataset.getDefaultModel().getNsPrefixMap());
        return model;
    }
}
