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

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author bbpennel
 */
public class RdfNamespaceRegistryTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private RdfNamespaceRegistry registry;

    private File registryFile;

    @Before
    public void init() throws Exception {
        registryFile = tmpDir.newFile();

        registry = new RdfNamespaceRegistry();

    }

    @Test
    public void testGetNamespaces() {
        final Map<String, String> namespaces = new HashMap<>();
        namespaces.put("ldp", "http://www.w3.org/ns/ldp#");

        registry.setNamespaces(namespaces);
        assertEquals("http://www.w3.org/ns/ldp#", registry.getNamespaces().get("ldp"));
    }

    @Test
    public void testGetNamespacesNoFile() throws Exception {
        registry.init();

        assertEquals(0, registry.getNamespaces().size());
    }

    @Test(expected = IOException.class)
    public void testLoadFileDoesNotExist() throws Exception {
        final String configPath = registryFile.getAbsolutePath();
        registryFile.delete();

        registry.setConfigPath(configPath);
        registry.init();
    }

    @Test
    public void testGetNamespacesFromFile() throws Exception {
        final String yaml = "ldp: http://www.w3.org/ns/ldp#\n" +
                            "memento: http://mementoweb.org/ns#\n";
        FileUtils.write(registryFile, yaml, "UTF-8");

        registry.setConfigPath(registryFile.getAbsolutePath());
        registry.init();

        final Map<String, String> namespaces = registry.getNamespaces();
        assertEquals("Incorrect number of namespace mappings", 2, namespaces.size());
        assertEquals("http://www.w3.org/ns/ldp#", namespaces.get("ldp"));
        assertEquals("http://mementoweb.org/ns#", namespaces.get("memento"));
    }

    @Test(expected = IOException.class)
    public void testLoadBadFile() throws Exception {
        final String yaml = "uri_array:\n" +
                            "  - http://www.w3.org/ns/ldp#\n";
        FileUtils.write(registryFile, yaml, "UTF-8");

        registry.setConfigPath(registryFile.getAbsolutePath());
        registry.init();
    }
}
