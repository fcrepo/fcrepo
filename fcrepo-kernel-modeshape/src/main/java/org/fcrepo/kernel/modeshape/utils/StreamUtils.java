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
package org.fcrepo.kernel.modeshape.utils;

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.stream.Stream;

import org.slf4j.Logger;

/**
 * @author acoburn
 * @since February 10, 2016
 */
public class StreamUtils {

    private static final Logger LOGGER = getLogger(StreamUtils.class);

    private static Boolean enableParallel = false;

    private static final String FCREPO_STEAMING_PARALLEL_KEY =
            "fcrepo.streaming.parallel";
    static {
        final String enableParallelVal = System.getProperty(FCREPO_STEAMING_PARALLEL_KEY, "false")
                .trim()
                .toLowerCase();
        if (!enableParallelVal.equals("true") && !enableParallelVal.equals("false")) {
            LOGGER.warn(
                    "The {} parameter contains an invalid value of {}:  " +
                            "allowed values are 'true' and 'false'. " +
                            "The default value of {} remain unchanged.",
                    FCREPO_STEAMING_PARALLEL_KEY, enableParallelVal, enableParallel);
        } else {
            StreamUtils.enableParallel = Boolean.valueOf(enableParallelVal);
        }
    }

    /**
     * Convert an Iterator to a Stream
     *
     * @param iterator the iterator
     * @param <T> the type of the Stream
     * @return the stream
     */
    public static <T> Stream<T> iteratorToStream(final Iterator<T> iterator) {
        return iteratorToStream(iterator, enableParallel);
    }

    /**
     * Convert an Iterator to a Stream
     *
     * @param <T> the type of the Stream
     * @param iterator the iterator
     * @param parallel whether to parallelize the stream
     * @return the stream
     */
    public static <T> Stream<T> iteratorToStream(final Iterator<T> iterator, final Boolean parallel) {
        return stream(spliteratorUnknownSize(iterator, IMMUTABLE), parallel);
    }
    private StreamUtils() {
        // prevent instantiation
    }
}

