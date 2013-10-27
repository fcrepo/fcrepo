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

import static com.google.common.collect.Sets.newHashSet;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.AbstractIterator;

/**
 * A wrapping {@link Iterator} that calculates two differences between a
 * {@link Set} A and a source Iterator B. The differences are (A - (A ∩ B)) and
 * (B - (A ∩ B)). The ordinary output of this iterator is (B - (A ∩ B)), and
 * after exhaustion, sets containing (A - (A ∩ B)) and (A ∩ B) are available.
 *
 * @author ajs6f
 * @date Oct 24, 2013
 * @param <E> The type of element common to both source and set.
 */
public class DifferencingIterator<E> extends AbstractIterator<E> {

    Set<? extends E> notCommon;

    private Set<E> common;

    private Iterator<E> source;

    /**
     * Ordinary constructor.
     *
     * @param original
     * @param source
     */
    public DifferencingIterator(final Set<? extends E> original,
            final Iterator<E> source) {
        super();
        this.notCommon = newHashSet(original);
        this.common = newHashSet();
        this.source = source;
    }

    @Override
    protected E computeNext() {
        if (source.hasNext()) {
            E next = source.next();
            // we only want to return this element if it is not common
            // to the two inputs
            while (common.contains(next) || notCommon.contains(next)) {
                // it was common, so shift it to common
                if (notCommon.remove(next)) {
                    common.add(next);
                }
                // move onto the next candidate
                if (!source.hasNext()) {
                    return endOfData();
                } else {
                    next = source.next();
                }
            }
            // it was not common so return it
            return next;
        } else {
            return endOfData();
        }
    }

    /**
     * This method will return null until the source iterator is exhausted.
     *
     * @return The elements that turned out to be common to the two inputs.
     */
    public Set<E> common() {
        return source.hasNext() ? null : common;
    }

    /**
     * This method will return null until the source iterator is exhausted.
     *
     * @return The elements that turned out not to be common to the two inputs.
     */
    public Set<? extends E> notCommon() {
        return source.hasNext() ? null : notCommon;
    }

}
