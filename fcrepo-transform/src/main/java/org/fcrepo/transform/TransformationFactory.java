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

import javax.ws.rs.core.MediaType;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.apache.jena.riot.WebContent.contentTypeSPARQLQuery;
import static org.fcrepo.transform.transformations.LDPathTransform.APPLICATION_RDF_LDPATH;

/**
 * Get a Transformation from a MediaType
 *
 * @author cbeer
 * @author ajs6f
 */
public class TransformationFactory {

    @SuppressWarnings("rawtypes")
    private final Map<String, Function<InputStream, Transformation>> mimeToTransform = new HashMap<>();

    /**
     * Get a new TransformationFactory with the default classes
     * @throws SecurityException if security exception occurred
     */
    public TransformationFactory() {
        mimeToTransform.put(contentTypeSPARQLQuery, SparqlQueryTransform::new);
        mimeToTransform.put(APPLICATION_RDF_LDPATH, LDPathTransform::new);
    }

    /**
     * Get a Transformation from a MediaType and an InputStream with
     * the transform program
     * @param contentType the content type
     * @param inputStream the input stream
     * @return T a Transformation
     */

    public <T> Transformation<T> getTransform(final MediaType contentType, final InputStream inputStream) {
        final String mimeType = contentType.toString();
        if (mimeToTransform.containsKey(mimeType)) {
            return mimeToTransform.get(contentType.toString()).apply(inputStream);
        }
        throw new UnsupportedOperationException(
                "No transform type exists for media type " + mimeType + "!");
    }
}
