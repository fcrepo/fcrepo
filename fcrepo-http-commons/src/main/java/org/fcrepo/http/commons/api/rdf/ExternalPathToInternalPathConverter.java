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

import java.util.List;
import org.fcrepo.kernel.api.functions.InjectiveConverter;
import org.fcrepo.kernel.api.identifiers.ChainWrappingConverter;
import org.fcrepo.kernel.modeshape.identifiers.HashConverter;
import org.fcrepo.kernel.modeshape.identifiers.NamespaceConverter;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

/**
 * Converts between External Paths and Internal Paths.
 * External paths are URI components.
 * Internal paths have hash path components, and resolved namespaces.
 * They are distinguished from JCR paths by their use of signal suffixes such as
 * fcr:metadata and fcr:tombstone, and fcr:versions.
 * @author barmintor
 *
 */
public class ExternalPathToInternalPathConverter extends ChainWrappingConverter<String, String> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = getLogger(ExternalPathToInternalPathConverter.class);

    /**
     * Build a converter with the default transforms
     */
    public ExternalPathToInternalPathConverter() {
        setTranslationChain(defaultList);
    }

    @SuppressWarnings("rawtypes")
    static List<InjectiveConverter> defaultList = ImmutableList.of(new HashConverter(), new NamespaceConverter());

    @Override
    public boolean inDomain(final String a) {
        return a != null && super.inDomain(a);
    }
}
