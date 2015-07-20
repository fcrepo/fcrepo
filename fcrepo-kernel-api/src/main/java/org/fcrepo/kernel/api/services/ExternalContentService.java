/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.api.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * @author cabeer
 */
public interface ExternalContentService {
    /**
     * Fetch the content body at a given URI
     * @param sourceUri the source uri
     * @return an InputStream of the content body
     * @throws IOException if IO exception occurred
     */
    InputStream retrieveExternalContent(URI sourceUri) throws IOException;
}
