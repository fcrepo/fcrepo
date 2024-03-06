/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.rdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bbpennel
 */
public class RdfNamespaceRegistryTest {

    @TempDir
    public Path tmpDir;

    private RdfNamespaceRegistry registry;

    private File registryFile;

    @BeforeEach
    public void init() throws Exception {
        registryFile = Files.createFile(
            tmpDir.resolve("namespaceRegistry.yml")
        ).toFile();

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

    @Test
    public void testLoadFileDoesNotExist() throws Exception {
        final String configPath = registryFile.getAbsolutePath();
        registryFile.delete();

        registry.setConfigPath(configPath);
        assertThrows(IOException.class, () -> registry.init());
    }

    @Test
    public void testGetNamespacesFromFile() throws Exception {
        final String yaml = "ldp: http://www.w3.org/ns/ldp#\n" +
                            "memento: http://mementoweb.org/ns#\n";
        FileUtils.write(registryFile, yaml, "UTF-8");

        registry.setConfigPath(registryFile.getAbsolutePath());
        registry.init();

        final Map<String, String> namespaces = registry.getNamespaces();
        assertEquals(2, namespaces.size(), "Incorrect number of namespace mappings");
        assertEquals("http://www.w3.org/ns/ldp#", namespaces.get("ldp"));
        assertEquals("http://mementoweb.org/ns#", namespaces.get("memento"));
    }

    @Test
    public void testLoadBadFile() throws Exception {
        final String yaml = "uri_array:\n" +
                            "  - http://www.w3.org/ns/ldp#\n";
        FileUtils.write(registryFile, yaml, "UTF-8");

        registry.setConfigPath(registryFile.getAbsolutePath());
        assertThrows(IOException.class, () -> registry.init());
    }
}
