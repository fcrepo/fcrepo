/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.utils;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static java.lang.Thread.sleep;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link AutoReloadingConfiguration}
 *
 * @author whikloj
 */
public class AutoReloadingConfigurationTest {

    private TestAutoReloadingConfiguration autoReloadingConfiguration;

    private Path configPath;

    @BeforeEach
    public void setUp() throws IOException {
        // Create a temporary configuration file
        configPath = Files.createTempFile("auto-reloading-config", ".properties");
        try (final var writer = Files.newBufferedWriter(configPath)) {
            writer.write("test.property=value\n");
        }
        autoReloadingConfiguration = new TestAutoReloadingConfiguration();
        autoReloadingConfiguration.setConfigPath(configPath.toAbsolutePath().toString());
        autoReloadingConfiguration.setMonitorForChanges(true);
        autoReloadingConfiguration.init();
    }

    @AfterEach
    public void tearDown() {
        autoReloadingConfiguration.shutdown();
        try {
            Files.deleteIfExists(configPath);
        } catch (IOException e) {
            // Ignore exception
        }
    }

    /**
     * If the path doesn't exist, there is a log message but nothing else
     */
     @Test
     public void testNonExistentPath() throws IOException, InterruptedException {
         autoReloadingConfiguration.shutdown();
         sleep(1000);
         Files.deleteIfExists(configPath);
         autoReloadingConfiguration.init();
         assertTrue(autoReloadingConfiguration.properties.isEmpty());
     }

    /**
     * Test the configuration with the monitoring turned on.
     */
    @Test
    public void testAutoReloadChanges() throws IOException {
        assertTrue(autoReloadingConfiguration.properties.containsKey("test.property"));
        assertEquals("value", autoReloadingConfiguration.properties.get("test.property"));
        // Simulate a change in the configuration file
        Files.deleteIfExists(configPath);
        try (final var writer = Files.newBufferedWriter(configPath)) {
            writer.write("test.property=newValue\n");
        }
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        assertNotEquals("value", autoReloadingConfiguration.properties.get("test.property")));
        assertTrue(autoReloadingConfiguration.properties.containsKey("test.property"));
        assertEquals("newValue", autoReloadingConfiguration.properties.get("test.property"));
    }

    /**
     * Test the configuration with monitoring turned off.
     */
    @Test
    public void testNoAutoReload() throws IOException {
        // Switch to no auto-reload
        autoReloadingConfiguration.setMonitorForChanges(false);
        autoReloadingConfiguration.shutdown();
        autoReloadingConfiguration.init();

        assertTrue(autoReloadingConfiguration.properties.containsKey("test.property"));
        assertEquals("value", autoReloadingConfiguration.properties.get("test.property"));
        // Simulate a change in the configuration file
        Files.deleteIfExists(configPath);
        try (final var writer = Files.newBufferedWriter(configPath)) {
            writer.write("test.property=newValue\n");
        }
        // Wait for a bit to ensure the change is not picked up
        assertThrows(
                ConditionTimeoutException.class,
                () -> await().atMost(Duration.ofSeconds(5))
                        .untilAsserted(() ->
                                assertNotEquals(
                                        "value",
                                        autoReloadingConfiguration.properties.get("test.property")
                                ))
        );
        // Changes to the file are not picked up.
        assertEquals("value", autoReloadingConfiguration.properties.get("test.property"));
        autoReloadingConfiguration.loadConfiguration();
        assertTrue(autoReloadingConfiguration.properties.containsKey("test.property"));
        assertEquals("newValue", autoReloadingConfiguration.properties.get("test.property"));
    }

    /**
     * Test implementation of AutoReloadingConfiguration for testing purposes.
     */
    static class TestAutoReloadingConfiguration extends AutoReloadingConfiguration {

        protected Map<String, String> properties = new HashMap<>();

        @Override
        public void loadConfiguration() {
            properties.clear();
            try (final var reader = Files.newBufferedReader(Paths.get(configPath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String[] parts = line.split("=");
                    if (parts.length == 2) {
                        properties.put(parts[0], parts[1]);
                    }
                }
            } catch (IOException e) {
                // no-op
            }
        }
    }

}
