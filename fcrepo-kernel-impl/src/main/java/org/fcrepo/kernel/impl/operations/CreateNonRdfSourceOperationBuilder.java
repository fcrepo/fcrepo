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
import java.util.Collection;

import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;


/**
 * Builder for operations to create new non-rdf sources
 *
 * @author bbpennel
 */
public class CreateNonRdfSourceOperationBuilder implements NonRdfSourceOperationBuilder {

    protected CreateNonRdfSourceOperationBuilder(final String rescId, final InputStream contentStream) {

    }

    @Override
    public CreateNonRdfSourceOperationBuilder mimeType(final String mimetype) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CreateNonRdfSourceOperationBuilder filename(final String filename) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CreateNonRdfSourceOperationBuilder contentDigests(final Collection<URI> digests) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CreateNonRdfSourceOperationBuilder contentSize(final long size) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CreateNonRdfSourceOperation build() {
        // TODO Auto-generated method stub
        return null;
    }

}
