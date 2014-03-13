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

import org.fcrepo.transform.transformations.LDPathTransform;
import org.fcrepo.transform.transformations.SparqlQueryTransform;

import javax.ws.rs.core.MediaType;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.apache.jena.riot.WebContent.contentTypeSPARQLQuery;
import static org.fcrepo.transform.transformations.LDPathTransform.APPLICATION_RDF_LDPATH;

/**
 * Get a Transformation from a MediaType
 */
public class TransformationFactory {

    private Map<String, Transformation<?>> mimeToTransform = new HashMap<>();

    /**
     * Get a new TransformationFactory with the default classes
     * @throws SecurityException
     */
    public TransformationFactory() {
        mimeToTransform.put(contentTypeSPARQLQuery, new SparqlQueryTransform(null));
        mimeToTransform.put(APPLICATION_RDF_LDPATH, new LDPathTransform(null));
    }

    /**
     * Get a Transformation from a MediaType and an InputStream with
     * the transform program
     * @param contentType
     * @param inputStream
     * @return
     */
    @SuppressWarnings("unchecked")
    // this suppression is in place representing the condition that the generator
    // map actually maps the mimetypes proffered to legitimate Transformations for those mimetype
    public <T> Transformation<T> getTransform(final MediaType contentType, final InputStream inputStream) {
        if (mimeToTransform.containsKey(contentType.toString())) {
            return (Transformation<T>) mimeToTransform.get(contentType.toString()).newTransform(inputStream);
        }
        throw new UnsupportedOperationException("No transform type exists for that media type!");
    }
}
