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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private String allowedListPath;

    private List<String> allowedList;

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

        if (extPath.contains("../")) {
            throw new ExternalMessageBodyException("Path was not absolute: " + extPath);
        }

        final URI uri;
        try {
            uri = new URI(extPath);
        } catch (final URISyntaxException e) {
            throw new ExternalMessageBodyException("Path was not a valid URI: " + extPath);
        }
        if (!uri.isAbsolute()) {
            throw new ExternalMessageBodyException("Path was not absolute: " + extPath);
        }
        if (!ALLOWED_SCHEMES.contains(uri.getScheme())) {
            throw new ExternalMessageBodyException("Path did not provide an accept scheme: " + extPath);
        }

        if (allowedList.stream().anyMatch(allowed -> extPath.startsWith(allowed))) {
            return;
        }
        throw new ExternalMessageBodyException("Path did not match any allowed external content paths: " + extPath);
    }

    /**
     * Initialize the allow list
     */
    public void init() throws IOException {
        loadAllowedPaths();
    }

    private void loadAllowedPaths() throws IOException {
        if (isEmpty(allowedListPath)) {
            return;
        }
        try (final Stream<String> stream = Files.lines(Paths.get(allowedListPath))) {
            allowedList = stream.map(line -> line.trim())
                .filter(line -> {
                    if (line.contains("../")) {
                        return false;
                    }
                    final URI uri;
                    try {
                        uri = new URI(line);
                    } catch (final URISyntaxException e) {
                        LOGGER.error("Invalid URL {} provided in allowed external path configuration", line);
                        return false;
                    }
                    if (!uri.isAbsolute() || !ALLOWED_SCHEMES.contains(uri.getScheme())) {
                        LOGGER.error("Invalid URL or scheme {} provided in allowed external path configuration",
                                line);
                        return false;
                    }
                    return true;
                }).collect(Collectors.toList());
        }
    }

    public void setAllowListPath(final String allowListPath) {
        this.allowedListPath = allowListPath;
    }
}
