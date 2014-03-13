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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static org.apache.jena.riot.WebContent.contentTypeSPARQLQuery;
import static org.fcrepo.transform.transformations.LDPathTransform.APPLICATION_RDF_LDPATH;

/**
 * Get a Transformation from a MediaType
 */
public class TransformationFactory {

    private static Map<String, Constructor<?>> mimeToTransform = new HashMap<>();

    static {


    }

    /**
     * Get a new TransformationFactory with the default classes
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public TransformationFactory() throws NoSuchMethodException, SecurityException {
        mimeToTransform.put(contentTypeSPARQLQuery, SparqlQueryTransform.class.getConstructor(InputStream.class));
        mimeToTransform.put(APPLICATION_RDF_LDPATH, LDPathTransform.class.getConstructor(InputStream.class));
    }

    /**
     * Get a Transformation from a MediaType and an InputStream with
     * the transform program
     * @param contentType
     * @param inputStream
     * @return
     */
    public Transformation<?> getTransform(final MediaType contentType, final InputStream inputStream) {

        if (mimeToTransform.containsKey(contentType.toString())) {
            final Constructor<?> transform = mimeToTransform.get(contentType.toString());

            try {
                return (Transformation<?>) (transform.newInstance(inputStream));
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw propagate(e);
            }

        }
        throw new UnsupportedOperationException("No transform type exists for that media type!");
    }
}
