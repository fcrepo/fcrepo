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

package org.fcrepo.kernel.utils.iterators;

import java.util.Iterator;
import java.util.Queue;

import com.google.common.base.Predicate;

/**
 * An {@link Iterator} that splits its produced elements between two
 * {@link Queue}s. One contains all elements that matched against a provided
 * {@link Set}, the other contains those that did not.
 *
 * @author ajs6f
 * @date Oct 23, 2013
 * @param <T>
 */
public class DifferencingIterator<T> {

    private Predicate<T> toBeMatched;

    private Queue<T> matched;

    private Queue<T> notMatched;

    private Iterator<T> source;

    /**
     * Default constructor.
     *
     * @param toBeMatched
     * @param source
     */
    public DifferencingIterator(final Predicate<T> toBeMatched,
            final Iterator<T> source) {
        this.toBeMatched = toBeMatched;
        this.source = source;
    }

    /**
     * Executes the differencing.
     */
    public void divide() {
        while (source.hasNext()) {
            final T next = source.next();
            if (toBeMatched.apply(next)) {
                matched.add(next);
            } else {
                notMatched.add(next);
            }
        }
    }

    /**
     * This method will return {@code null} until after the wrapped
     * {@link Iterator} is exhausted.
     *
     * @return A {@link Queue} of those elements that matched against the
     *         provided {@link Set}
     */
    public Queue<T> matched() {
        return source.hasNext() ? null : matched;
    }

    /**
     * This method will return {@code null} until after the wrapped
     * {@link Iterator} is exhausted.
     *
     * @return A {@link Queue} of those elements that did not match against the
     *         provided {@link Set}
     */
    public Queue<T> notMatched() {
        return source.hasNext() ? null : notMatched;
    }

}
