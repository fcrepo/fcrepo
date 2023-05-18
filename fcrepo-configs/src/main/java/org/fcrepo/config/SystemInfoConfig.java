/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Small config to get info from git properties and the implementation version
 *
 * @author mikejritter
 */
@Configuration
public class SystemInfoConfig extends BasePropsConfig {

    public static final String GIT_COMMIT = "git.commit.id.abbrev";

    @Value("${" + GIT_COMMIT + ":}")
    private String gitCommit;

    public String getGitCommit() {
        return gitCommit;
    }

    public String getImplementationVersion() {
        final var version = SystemInfoConfig.class.getPackage().getImplementationVersion();
        return version != null ? version : "";
    }
}
