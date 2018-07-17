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

import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;

/**
 * Validates external content paths to ensure that they are within a configured allowed list of paths.
 *
 * @author bbpennel
 */
public class ExternalContentPathValidator {

    private String allowListPath;

    /**
     * Validates that an external path is valid. The path must be an HTTP or file URI within the allow list of paths,
     * be absolute, and contain no relative modifier.
     *
     * @param extPath
     * @throws ExternalMessageBodyException
     */
    public void validate(final String extPath) throws ExternalMessageBodyException {

    }

    /**
     * Initialize the allow list
     */
    public void init() {

    }

    public void setAllowListPath(final String allowListPath) {
        this.allowListPath = allowListPath;
    }
}
