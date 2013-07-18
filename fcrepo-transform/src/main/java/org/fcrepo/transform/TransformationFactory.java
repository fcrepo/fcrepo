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

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;

/**
 * Get a Transformation from a MediaType
 */
public class TransformationFactory {

    private Map<String, Class> mimeToTransform;

    /**
     * Get a new TransformationFactory with the default classes
     */
    public TransformationFactory() {
        mimeToTransform = new HashMap<String, Class>();
        mimeToTransform.put(WebContent.contentTypeSPARQLQuery, SparqlQueryTransform.class);
        mimeToTransform.put(LDPathTransform.APPLICATION_RDF_LDPATH, LDPathTransform.class);

    }

    /**
     * Get a new TransformationFactory using the provided mapping
     * @param mimeToTransform
     */
    public TransformationFactory(Map<String, Class> mimeToTransform) {
        mimeToTransform = mimeToTransform;
    }

    /**
     * Get a Transformation from a MediaType and an InputStream with
     * the transform program
     * @param contentType
     * @param inputStream
     * @return
     */
    public Transformation getTransform(final MediaType contentType,
                                              final InputStream inputStream) {

        if (mimeToTransform.containsKey(contentType.toString())) {
            Class transform = mimeToTransform.get(contentType.toString());

            if (Transformation.class.isAssignableFrom(transform)) {
                try {
                    return (Transformation)(transform.getConstructor(InputStream.class).newInstance(inputStream));
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    propagate(e);
                }
            }

        }

        return null;

    }
}
