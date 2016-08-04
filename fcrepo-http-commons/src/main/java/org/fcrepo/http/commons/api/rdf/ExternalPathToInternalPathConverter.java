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
package org.fcrepo.http.commons.api.rdf;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.fcrepo.kernel.api.functions.Converter;
import org.fcrepo.kernel.modeshape.identifiers.HashConverter;
import org.fcrepo.kernel.modeshape.identifiers.IdentifierConverter;
import org.fcrepo.kernel.modeshape.identifiers.NamespaceConverter;

import org.slf4j.Logger;

/**
 * Converts between External Paths and Internal Paths.
 * External paths are URI components.
 * Internal paths have hash path components, and resolved namespaces.
 * They are distinguished from JCR paths by their use of signal suffixes such as
 * fcr:metadata and fcr:tombstone, and fcr:versions.
 * @author barmintor
 *
 */
public class ExternalPathToInternalPathConverter extends IdentifierConverter<String, String> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = getLogger(ExternalPathToInternalPathConverter.class);

    private final List<Converter<String,String>> processors;

    /**
     * Build a converter with the default transforms
     */
    public ExternalPathToInternalPathConverter() {
        this(defaultList());
    }

    /**
     * 
     * @param processors
     */
    public ExternalPathToInternalPathConverter(final List<Converter<String,String>> processors) {
        this.processors = (processors != null) ? processors : defaultList();
    }

    static List<Converter<String,String>> defaultList() {
        final ArrayList<Converter<String,String>> processors = new ArrayList<>();
        processors.add(new HashConverter());
        processors.add(new NamespaceConverter());
        return processors;
    }

    @Override
    public String toDomain(final String t) {
        String result = t;
        final ListIterator<Converter<String, String>> iter = processors.listIterator(processors.size());
        while (iter.hasPrevious()) {
            result = iter.previous().toDomain(result);
        }
        return result;
    }

    @Override
    public String apply(final String t) {
        String result = t;
        for (Converter<String,String> converter:processors) {
            result = converter.apply(result);
        }
        return result;
    }

    @Override
    public String asString(final String resource) {
        return apply(resource);
    }

}
