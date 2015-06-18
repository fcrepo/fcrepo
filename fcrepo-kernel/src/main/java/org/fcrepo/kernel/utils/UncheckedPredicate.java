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

package org.fcrepo.kernel.utils;

import java.util.function.Predicate;

import javax.jcr.RepositoryException;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;

/**
 * A {@link Predicate} that permits {@RepositoryException}s.<T>
 *
 * @author ajs6f
 */
@FunctionalInterface
public interface UncheckedPredicate<T> extends Predicate<T>, com.google.common.base.Predicate<T> {

    @Override
    default boolean test(final T elem) {
        try {
            return testThrows(elem);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    default boolean apply(final T elem) {
        try {
            return testThrows(elem);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     *  This method has the same semantic as {@link #test} but permits {@link RepositoryException}s.
     *
     *  @param elem an item of type T
     *  @return the value of this predicate when applied to the input element.
     */
    boolean testThrows(T elem) throws RepositoryException;

    /**
     *  A static convenience method for constructing UncheckedPredicates from lambda syntax.
     *
     *  @param p an unchecked predicate
     *  @return the predicate given to this function
     */
    static <T> UncheckedPredicate<T> uncheck(final UncheckedPredicate<T> p) {
        return p;
    }
}
