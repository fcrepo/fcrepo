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
package org.fcrepo.kernel.impl.operations;

import java.io.InputStream;
import java.net.URI;

import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;


/**
 * Factory for operations to update non-rdf sources
 *
 * @author bbpennel
 */
public class NonRdfSourceOperationFactoryImpl implements NonRdfSourceOperationFactory {

    @Override
    public NonRdfSourceOperationBuilder updateExternalBinaryBuilder(final String rescId, final String handling, final URI contentUri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NonRdfSourceOperationBuilder updateInternalBinaryBuilder(final String rescId, final InputStream contentStream) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NonRdfSourceOperationBuilder createExternalBinaryBuilder(final String rescId, final String handling, final URI contentUri) {
        return new CreateNonRdfSourceOperationBuilder(rescId).externalContentURI(contentUri)
                .externalContentHandling(handling);
    }

    @Override
    public NonRdfSourceOperationBuilder createInternalBinaryBuilder(final String rescId, final InputStream contentStream) {
        return new CreateNonRdfSourceOperationBuilder(rescId).content(contentStream);
    }

}
