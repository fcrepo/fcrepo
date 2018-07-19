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
package org.fcrepo.http.api;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;
import org.slf4j.Logger;

/**
 * Validates external content paths to ensure that they are within a configured allowed list of paths.
 *
 * @author bbpennel
 */
public class ExternalContentPathValidator {

    private static final Logger LOGGER = getLogger(ExternalContentPathValidator.class);

    private final Set<String> ALLOWED_SCHEMES = new HashSet<>(Arrays.asList("file", "http", "https"));

    private final Pattern SCHEME_PATTERN = Pattern.compile("^(http|https|file):/.*");

    private final Pattern RELATIVE_MOD_PATTERN = Pattern.compile(".*(^|/)\\.\\.($|/).*");

    private final Pattern NORMALIZE_FILE_URI = Pattern.compile("^file:/{2,3}");

    private String configPath;

    private List<String> allowedList;

    private boolean monitorForChanges;

    private Thread monitorThread;

    private boolean monitorRunning;

    /**
     * Validates that an external path is valid. The path must be an HTTP or file URI within the allow list of paths,
     * be absolute, and contain no relative modifier.
     *
     * @param extPath
     * @throws ExternalMessageBodyException
     */
    public void validate(final String extPath) throws ExternalMessageBodyException {
        if (allowedList == null || allowedList.size() == 0) {
            throw new ExternalMessageBodyException("External content is disallowed by the server");
        }

        if (isEmpty(extPath)) {
            throw new ExternalMessageBodyException("External content path was empty");
        }

        final String path = normalizePath(extPath.toLowerCase());
        if (RELATIVE_MOD_PATTERN.matcher(path).matches()) {
            throw new ExternalMessageBodyException("Path was not absolute: " + extPath);
        }

        final URI uri;
        try {
            uri = new URI(path);
        } catch (final URISyntaxException e) {
            throw new ExternalMessageBodyException("Path was not a valid URI: " + extPath);
        }
        if (!uri.isAbsolute()) {
            throw new ExternalMessageBodyException("Path was not absolute: " + extPath);
        }
        if (!ALLOWED_SCHEMES.contains(uri.getScheme())) {
            throw new ExternalMessageBodyException("Path did not provide an accept scheme: " + extPath);
        }

        if (allowedList.stream().anyMatch(allowed -> path.startsWith(allowed))) {
            return;
        }
        throw new ExternalMessageBodyException("Path did not match any allowed external content paths: " + extPath);
    }

    private String normalizePath(final String path) {
        // file uris can have between 1 and 3 slashes depending on if the authority is present
        if (path.startsWith("file://")) {
            return NORMALIZE_FILE_URI.matcher(path).replaceFirst("file:/");
        }
        return path;
    }

    /**
     * Initialize the allow list
     */
    public void init() throws IOException {
        if (isEmpty(configPath)) {
            return;
        }

        loadAllowedPaths();

        if (monitorForChanges) {
            monitorForChanges();
        }
    }

    /**
     * Shut down the validator's change monitoring thread
     */
    public void shutdown() {
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
    }

    /**
     * Loads the allowed list
     *
     * @throws IOException
     */
    private synchronized void loadAllowedPaths() throws IOException {
        try (final Stream<String> stream = Files.lines(Paths.get(configPath))) {
            allowedList = stream.map(line -> normalizePath(line.trim().toLowerCase()))
                .filter(line -> {
                        if (RELATIVE_MOD_PATTERN.matcher(line).matches()
                            || !SCHEME_PATTERN.matcher(line).matches()) {
                        LOGGER.error("Invalid path {} specified in external path configuration {}",
                                line, configPath);
                        return false;
                    }
                    return true;
                }).collect(Collectors.toList());
        }
    }

    /**
     * Starts up monitoring of the allowed list configuration for changes.
     */
    private void monitorForChanges() {
        if (monitorRunning) {
            return;
        }

        final Path path = Paths.get(configPath);
        if (!path.toFile().exists()) {
            LOGGER.debug("Allow list configuration {} does not exist, disabling monitoring", configPath);
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
                            WatchKey key;
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
                                            "External binary configuration {} has been updated, reloading.",
                                            path);
                                    try {
                                        loadAllowedPaths();
                                    } catch (final IOException e) {
                                        LOGGER.error("Failed to reload external locations configuration", e);
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
     * Set the file path for the allowed external path configuration
     *
     * @param configPath
     */
    public void setConfigPath(final String configPath) {
        this.configPath = configPath;
    }

    /**
     * Get the file path for the configuration file
     *
     * @return
     */
    public String getConfigPath() {
        return configPath;
    }

    /**
     * Set whether to monitor the configuration file for changes
     *
     * @param monitorForChanges
     */
    public void setMonitorForChanges(final boolean monitorForChanges) {
        this.monitorForChanges = monitorForChanges;
    }
}
