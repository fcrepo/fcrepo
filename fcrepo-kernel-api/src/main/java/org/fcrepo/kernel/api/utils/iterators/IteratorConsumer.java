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
package org.fcrepo.kernel.api.utils.iterators;

import com.google.common.util.concurrent.ListenableFuture;
import org.fcrepo.kernel.api.exception.MalformedRdfException;

/**
 * Implemented by something that can consume an {@link java.util.Iterator}. The
 * assumption is that a reference to the appropriate iterator is managed as part
 * of the state of any implementation.
 * 
 * @author ajs6f
 * @since Oct 24, 2013
 * @param <E> E
 * @param <T> T
 */
public interface IteratorConsumer<E, T> {

    /**
     * Synchronous consumption.
     * @throws MalformedRdfException if malformed rdf exception occurred
     */
    void consume() throws MalformedRdfException;

    /**
     * Asynchronous consumption.
     *
     * @return ListenableFuture
     */
    ListenableFuture<T> consumeAsync();

}
