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

import java.util.List;

import org.fcrepo.kernel.api.functions.InjectiveConverter;
import org.fcrepo.kernel.api.identifiers.ChainWrappingConverter;
import org.fcrepo.kernel.modeshape.identifiers.HashConverter;
import org.fcrepo.kernel.modeshape.identifiers.NamespaceConverter;

import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
  * A very simple {@link org.fcrepo.kernel.api.functions.Converter} which translates JCR paths into
  * un-dereference-able Fedora subjects (by replacing JCR-specific names with
  * Fedora names). Should not be used except in "embedded" deployments in which
  * no publication of translated identifiers is expected!
 *
 * @author barmintor
 * @author ajs6f
 * @author escowles
 * @since May 15, 2013
 */
public class DefaultIdentifierTranslator implements InjectiveConverter<Resource, String> {

    public static String DEFAULT_PREFIX = "info:fedora/";

    private final ChainWrappingConverter<String, String> internal = new ChainWrappingConverter<>();

    /**
     * Translation with a placeholder resource namespace
     */
    public DefaultIdentifierTranslator() {
        @SuppressWarnings("rawtypes")
        final List<InjectiveConverter> chain = ImmutableList.of(new NamespaceConverter(), new HashConverter(),
                new PrefixingIdentifierTranslator(DEFAULT_PREFIX));
        internal.setTranslationChain(chain);
    }

    @Override
    public String apply(final Resource a) {
        return internal.apply(a.getURI());
    }

    @Override
    public boolean inDomain(final Resource a) {
        return internal.inDomain(a.getURI());
    }

    @Override
    public Resource toDomain(final String b) {
        return ResourceFactory.createResource(internal.toDomain(b));
    }

    @Override
    public InjectiveConverter<String, Resource> reverse() {
        return new InverseConverterWrapper<>(this);
    }

}
