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
package org.fcrepo.persistence.api;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;

/**
 * Information describing the outcome of a write operation.
 *
 * @author bbpennel
 */
public interface WriteOutcome {

    /**
     * The size of the file written.
     *
     * @return content size
     */
    Long getContentSize();

    /**
     * The time at which the write completed
     *
     * @return instant representing the time the write completed
     */
    Instant getTimeWritten();

    /**
     * Digests calculated from the content during writing
     *
     * @return digests
     */
    Collection<URI> getDigests();
}
