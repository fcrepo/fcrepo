/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.jcr;

/**
 * Convenience class with constants for commonly used JCR types.
 *
 * @author ajs6f
 * @date Apr 25, 2013
 */
public interface FedoraJcrTypes {

    String FEDORA_RESOURCE = "fedora:resource";

    String FEDORA_DATASTREAM = "fedora:datastream";

    String FEDORA_OBJECT = "fedora:object";

    String FEDORA_BINARY = "fedora:binary";

    String JCR_LASTMODIFIED = "jcr:lastModified";

    String JCR_CREATED = "jcr:created";

    String JCR_CREATEDBY = "jcr:createdBy";

    String CONTENT_SIZE = "fedora:size";

    String CONTENT_DIGEST = "fedora:digest";

    String FCR_CONTENT = "fcr:content";

    String ROOT = "mode:root";
}
