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
package org.fcrepo.kernel.api.utils;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;

/**
 * Abstract configuration class which monitors a file path in order to reload the configuration when it changes.
 *
 * @author bbpennel
 */
public abstract class AutoReloadingConfiguration {
    private static final Logger LOGGER = getLogger(AutoReloadingConfiguration.class);

    protected String configPath;

    private boolean monitorForChanges;

    private Thread monitorThread;

    private boolean monitorRunning;

    /**
     * Initialize the configuration and set up monitoring
     *
     * @throws IOException thrown if the configuration cannot be loaded.
     *
     */
    public void init() throws IOException {
        if (isEmpty(configPath)) {
            return;
        }

        loadConfiguration();

        if (monitorForChanges) {
            monitorForChanges();
        }
    }

    /**
     * Shut down the change monitoring thread
     */
    public void shutdown() {
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
    }

    /**
     * Load the configuration file.
     *
     * @throws IOException thrown if the configuration cannot be loaded.
     */
    protected abstract void loadConfiguration() throws IOException;

    /**
     * Starts up monitoring of the configuration for changes.
     */
    private void monitorForChanges() {
        if (monitorRunning) {
            return;
        }

        final Path path;
        try {
            path = Paths.get(configPath);
        } catch (final Exception e) {
            LOGGER.warn("Cannot monitor configuration {}, disabling monitoring; {}", configPath, e.getMessage());
            return;
        }

        if (!path.toFile().exists()) {
            LOGGER.debug("Configuration {} does not exist, disabling monitoring", configPath);
            return;
        }
        final Path directoryPath = path.getParent();

        try {
            final WatchService watchService = FileSystems.getDefault().newWatchService();
            directoryPath.register(watchService, ENTRY_MODIFY);

            monitorThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        for (;;) {
                            final WatchKey key;
                            try {
                                key = watchService.take();
                            } catch (final InterruptedException e) {
                                LOGGER.debug("Interrupted the configuration monitor thread.");
                                break;
                            }

                            for (final WatchEvent<?> event : key.pollEvents()) {
                                final WatchEvent.Kind<?> kind = event.kind();
                                if (kind == OVERFLOW) {
                                    continue;
                                }

                                // If the configuration file triggered this event, reload it
                                final Path changed = (Path) event.context();
                                if (changed.equals(path.getFileName())) {
                                    LOGGER.info(
                                            "Configuration {} has been updated, reloading.",
                                            path);
                                    try {
                                        loadConfiguration();
                                    } catch (final IOException e) {
                                        LOGGER.error("Failed to reload configuration {}", configPath, e);
                                    }
                                }

                                // reset the key
                                final boolean valid = key.reset();
                                if (!valid) {
                                    LOGGER.debug("Monitor of {} is no longer valid", path);
                                    break;
                                }
                            }
                        }
                    } finally {
                        try {
                            watchService.close();
                        } catch (final IOException e) {
                            LOGGER.error("Failed to stop configuration monitor", e);
                        }
                    }
                    monitorRunning = false;
                }
            });
        } catch (final IOException e) {
            LOGGER.error("Failed to start configuration monitor", e);
        }

        monitorThread.start();
        monitorRunning = true;
    }

    /**
     * Set the file path for the configuration
     *
     * @param configPath file path for configuration
     */
    public void setConfigPath(final String configPath) {
        // Resolve classpath references without spring's help
        if (configPath != null && configPath.startsWith("classpath:")) {
            final String relativePath = configPath.substring(10);
            this.configPath = this.getClass().getResource(relativePath).getPath();
        } else {
            this.configPath = configPath;
        }
    }

    /**
     * Set whether to monitor the configuration file for changes
     *
     * @param monitorForChanges flag controlling if to enable configuration monitoring
     */
    public void setMonitorForChanges(final boolean monitorForChanges) {
        this.monitorForChanges = monitorForChanges;
    }
}
