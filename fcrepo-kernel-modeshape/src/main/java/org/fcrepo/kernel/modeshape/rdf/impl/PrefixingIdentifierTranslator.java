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
package org.fcrepo.kernel.modeshape.rdf.impl;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.functions.InjectiveConverter;

/**
 * A very simple {@link org.fcrepo.kernel.api.functions.Converter} which translates JCR paths into Fedora subjects with
 * a configurable resource namespace (e.g., a baseURL).  When a REST API context is available for
 * constructing URIs, org.fcrepo.http.commons.api.rdf.HttpResourceConverter should be used instead.
 *
 * @author barmintor
 * @author ajs6f
 * @author escowles
 * @since 2015-04-24
 */
public class PrefixingIdentifierTranslator implements InjectiveConverter<String, String> {

    private final String resourceNamespace;

    /**
     * Construct the graph with the provided resource namespace, which will translate JCR paths into URIs prefixed
     * with that namespace. Should only be used when a REST API context is not available for constructing URIs.
     *
     * @param resourceNamespace Resource namespace (i.e., base URL)
     **/
    public PrefixingIdentifierTranslator(final String resourceNamespace) {
        this.resourceNamespace = resourceNamespace;
    }

    @Override
    public String apply(final String subject) {
        if (!inDomain(subject)) {
            throw new RepositoryRuntimeException("Subject " + subject + " is not in this repository");
        }
        final String path = subject.substring(resourceNamespace.length() - 1);
        return path.isEmpty() ? "/" : path;
    }

    @Override
    public boolean inDomain(final String subject) {
        return subject.startsWith(resourceNamespace);
    }

    @Override
    public String toDomain(final String absPath) {
        final String relativePath = absPath.startsWith("/") ? absPath.substring(1) : absPath;
        return resourceNamespace + relativePath;
    }
}
