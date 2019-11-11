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
package org.fcrepo.persistence.ocfl.impl;

import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.ocfl.api.Persister;

import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;

import static java.lang.String.format;
import static org.apache.jena.riot.system.StreamRDFWriter.getWriterStream;

/**
 * A base abstract persister class
 *
 * @author dbernstein
 * @since 6.0.0
 */
public abstract class AbstractPersister<T extends ResourceOperation> implements Persister<T> {

    private ResourceOperationType resourceOperationType;

    AbstractPersister(final ResourceOperationType resourceOperationType) {
        this.resourceOperationType = resourceOperationType;
    }

    @Override
    public boolean handle(final ResourceOperation operation) {
        //get the class of T
        final var clazz = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

        //retrieve the interfaces of the operation class
        final var interfaces = operation.getClass().getInterfaces();

        //ensure that at least one of them match.
        for (var i : interfaces) {
            if (clazz.equals(i)) {
                //return true if the operation types match.
                return this.resourceOperationType.equals(operation.getType());
            }
        }
        return false;
    }

    protected String getSubpath(final String parentResourceId, final String resourceId){
        if(resourceId.startsWith(parentResourceId)){
            return resourceId.substring(parentResourceId.length() + 1);
        }

        throw new RuntimeException(format("resource (%s) is not prefixed by parent resource (%s)", resourceId,
                parentResourceId));
    }

    protected void writeTriples(final RdfStream rdfStream, final RDFFormat format,
                              final OutputStream output) {
        final StreamRDF stream = getWriterStream(output, format);
        stream.start();
        rdfStream.forEach(stream::triple);
        stream.finish();
    }

}
