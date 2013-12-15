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

package org.fcrepo.transform.transformations;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.io.IOUtils;
import org.fcrepo.transform.Transformation;

import java.io.IOException;
import java.io.InputStream;

import static org.fcrepo.kernel.rdf.SerializationUtils.unifyDatasetModel;

/**
 * SPARQL Query-based transforms
 */
public class SparqlQueryTransform implements Transformation {

    private final InputStream query;

    /**
     * Construct a new SparqlQueryTransform from the data from
     * the InputStream
     * @param query
     */
    public SparqlQueryTransform(final InputStream query) {
        this.query = query;
    }

    @Override
    public QueryExecution apply(final Dataset dataset) {

        try {
            final Model model = unifyDatasetModel(dataset);
            final Query sparqlQuery =
                QueryFactory.create(IOUtils.toString(query));

            return QueryExecutionFactory.create(sparqlQuery, model);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public InputStream getQuery() {
        return query;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SparqlQueryTransform &&
                   query.equals(((SparqlQueryTransform)other).getQuery());
    }

    @Override
    public int hashCode() {
        return 5 + 7 * query.hashCode();
    }

}
