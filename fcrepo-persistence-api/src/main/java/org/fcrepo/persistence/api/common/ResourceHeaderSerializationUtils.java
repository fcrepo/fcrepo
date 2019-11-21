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
package org.fcrepo.persistence.api.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Utility for working with serializations of resource headers
 *
 * @author bbpennel
 */
public class ResourceHeaderSerializationUtils {

    public static final String RESOURCE_HEADER_EXTENSION = ".json";

    private static final ObjectReader HEADER_READER = new ObjectMapper().readerFor(ResourceHeadersImpl.class);

    private static final ObjectWriter HEADER_WRITER = new ObjectMapper().writerFor(ResourceHeaders.class);

    private ResourceHeaderSerializationUtils() {
    }

    /**
     * Deserialize the provided inputstream containing JSON as a ResourceHeaders object.
     *
     * @param bodyStream inputstream containing json
     * @return Deserialized resource headers
     * @throws PersistentStorageException thrown if unable to deserialize the inputstream
     */
    public static ResourceHeaders deserializeHeaders(final InputStream bodyStream) throws PersistentStorageException {
        try {
            return HEADER_READER.readValue(bodyStream);
        } catch (final IOException e) {
            throw new PersistentStorageException("Unable to read resource headers", e);
        }
    }

    /**
     * Serializes the provided headers to JSON contained by an InputStream
     *
     * @param headers resource headers to serialize
     * @return
     */
    public static InputStream serializeHeaders(final ResourceHeaders headers) {
        if (headers == null) {
            throw new IllegalArgumentException("Must provide non-null resource headers object");
        }

        try {
            final byte[] bytes = HEADER_WRITER.writeValueAsBytes(headers);
            return new ByteArrayInputStream(bytes);
        } catch (final JsonProcessingException e) {
            throw new RepositoryRuntimeException("Failed to serialize headers", e);
        }
    }
}
