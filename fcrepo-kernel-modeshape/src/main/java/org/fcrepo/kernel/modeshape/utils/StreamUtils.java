/*
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
package org.fcrepo.kernel.modeshape.utils;

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * @author acoburn
 * @since February 10, 2016
 */
public class StreamUtils {

    /**
     * Convert an Iterator to a Stream
     *
     * @param iterator the iterator
     * @param <T> the type of the Stream
     * @return the stream
     */
    @SuppressWarnings("unchecked")
    public static <T> Stream<T> iteratorToStream(final Iterator<T> iterator) {
        return iteratorToStream(iterator, false);
    }

    /**
     * Convert an Iterator to a Stream
     *
     * @param <T> the type of the Stream
     * @param iterator the iterator
     * @param parallel whether to parallelize the stream
     * @return the stream
     */
    @SuppressWarnings("unchecked")
    public static <T> Stream<T> iteratorToStream(final Iterator<T> iterator, final Boolean parallel) {
        return stream(spliteratorUnknownSize(iterator, IMMUTABLE), parallel);
    }
    private StreamUtils() {
        // prevent instantiation
    }
}

