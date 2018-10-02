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
package org.fcrepo.kernel.api.rdf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.fcrepo.kernel.api.utils.AutoReloadingConfiguration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Registry of RDF namespaces
 *
 * @author bbpennel
 */
public class RdfNamespaceRegistry extends AutoReloadingConfiguration {

    private Map<String, String> namespaces;

    /**
     * Load the namespace prefix to URI configuration file
     */
    @Override
    protected synchronized void loadConfiguration() throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {};
        namespaces = mapper.readValue(new File(configPath), typeRef);
    }

    /**
     * Get the mapping of namespace prefixes to URIs
     *
     * @return map of namespace prefixes to URIs, or an empty map if no mapping was provided.
     */
    public Map<String, String> getNamespaces() {
        if (namespaces == null) {
            namespaces = new HashMap<>();
        }
        return namespaces;
    }

    /**
     * Set the mapping of namespace prefixes to URIs
     *
     * @param namespaces mapping of namespaces
     */
    public void setNamespaces(final Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }
}
