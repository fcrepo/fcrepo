/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * A base class for property configs
 *
 * @author dbernstein
 * @since 6.0.0
 */
@PropertySources({
        @PropertySource(value = BasePropsConfig.DEFAULT_FCREPO_CONFIG_FILE_PROP_SOURCE,
                ignoreResourceNotFound = true),
        @PropertySource(value = BasePropsConfig.FCREPO_CONFIG_FILE_PROP_SOURCE, ignoreResourceNotFound = true)
})
abstract class BasePropsConfig {

    public static final String FCREPO_HOME_PROPERTY = "fcrepo.home";
    public static final String DEFAULT_FCREPO_HOME_VALUE = "fcrepo-home";
    public static final String DEFAULT_FCREPO_CONFIG_FILE_PROP_SOURCE =
            "file:${" + FCREPO_HOME_PROPERTY + ":" + DEFAULT_FCREPO_HOME_VALUE + "}/config/fcrepo.properties";
    public static final String FCREPO_CONFIG_FILE_PROP_SOURCE = "file:${fcrepo.config.file}";

    protected Path createDirectories(final Path path) throws IOException {
        try {
            return Files.createDirectories(path);
        } catch (final FileAlreadyExistsException e) {
            // Ignore. This only happens with the path is a symlink
            return path;
        }
    }

}
