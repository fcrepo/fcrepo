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

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;
import org.fcrepo.kernel.api.utils.AutoReloadingConfiguration;
import org.slf4j.Logger;

/**
 * Validates external content paths to ensure that they are within a configured allowed list of paths.
 *
 * @author bbpennel
 */
public class ExternalContentPathValidator extends AutoReloadingConfiguration {

    private static final Logger LOGGER = getLogger(ExternalContentPathValidator.class);

    private final Set<String> ALLOWED_SCHEMES = new HashSet<>(Arrays.asList("file", "http", "https"));

    private final Pattern SCHEME_PATTERN = Pattern.compile("^(http|https|file):/.*");

    private final Pattern RELATIVE_MOD_PATTERN = Pattern.compile(".*(^|/)\\.\\.($|/).*");

    private final Pattern NORMALIZE_FILE_URI = Pattern.compile("^file:/{2,3}");

    private List<String> allowedList;

    /**
     * Validates that an external path is valid. The path must be an HTTP or file URI within the allow list of paths,
     * be absolute, and contain no relative modifier.
     *
     * @param extPath external binary path to validate
     * @throws ExternalMessageBodyException thrown if the path is invalid.
     */
    public void validate(final String extPath) throws ExternalMessageBodyException {
        if (allowedList == null || allowedList.size() == 0) {
            throw new ExternalMessageBodyException("External content is disallowed by the server");
        }

        if (isEmpty(extPath)) {
            throw new ExternalMessageBodyException("External content path was empty");
        }

        final String path = normalizePath(extPath.toLowerCase());

        final URI uri;
        try {
            // Ensure that the path is a valid URL
            uri = new URI(path);
            uri.toURL();
        } catch (final Exception e) {
            throw new ExternalMessageBodyException("Path was not a valid URI: " + extPath);
        }

        // Decode the uri and ensure that it does not contain modifiers
        final String decodedPath = uri.getPath();
        if (RELATIVE_MOD_PATTERN.matcher(decodedPath).matches()) {
            throw new ExternalMessageBodyException("Path was not absolute: " + extPath);
        }

        // Require that the path is absolute
        if (!uri.isAbsolute()) {
            throw new ExternalMessageBodyException("Path was not absolute: " + extPath);
        }

        // Ensure that an accept scheme was provided
        final String scheme = uri.getScheme();
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw new ExternalMessageBodyException("Path did not provide an allowed scheme: " + extPath);
        }

        // If a file, verify that it exists
        if (scheme.equals("file") && !Paths.get(uri).toFile().exists()) {
            throw new ExternalMessageBodyException("Path did not match any allowed external content paths: " +
                    extPath);
        }

        // Check that the uri is within an allowed path
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
     * Loads the allowed list.
     *
     * @throws IOException thrown if the allowed list configuration file cannot be read.
     */
    @Override
    protected synchronized void loadConfiguration() throws IOException {
        LOGGER.info("Loading list of allowed external content locations from {}", configPath);
        try (final Stream<String> stream = Files.lines(Paths.get(configPath))) {
            allowedList = stream.map(line -> normalizePath(line.trim().toLowerCase()))
                .filter(line -> {
                    final Matcher schemeMatcher = SCHEME_PATTERN.matcher(line);
                    final boolean schemeMatches = schemeMatcher.matches();
                    if (!schemeMatches || RELATIVE_MOD_PATTERN.matcher(line).matches()) {
                        LOGGER.error("Invalid path {} specified in external path configuration {}",
                                line, configPath);
                        return false;
                    }
                    if ("file".equals(schemeMatcher.group(1))) {
                        // If a file uri ends with / it must be a directory, otherwise it must be a file.
                        final File allowing = new File(URI.create(line).getPath());
                        if ((line.endsWith("/") && !allowing.isDirectory())
                                || (!line.endsWith("/") && !allowing.isFile())) {
                            LOGGER.error("Invalid path {} in configuration {}, directories must end with a '/'",
                                    line, configPath);
                            return false;
                        }
                    }
                    return true;
                }).collect(Collectors.toList());
        }
    }
}
