
package org.fcrepo.kernel.utils.iterators;

import java.util.Iterator;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Implemented by anything that can consume an {@link Iterator}.
 *
 * @author ajs6f
 * @date Oct 24, 2013
 * @param <E>
 * @param <T>
 */
public interface IteratorConsumer<E, T> {

    /**
     * Synchronous consumption.
     *
     * @param i
     */
    void consume(final Iterator<E> i);

    /**
     * Asynchronous consumption.
     *
     * @param i
     * @return
     */
    ListenableFuture<T> consumeAsync(final Iterator<E> i);

}
