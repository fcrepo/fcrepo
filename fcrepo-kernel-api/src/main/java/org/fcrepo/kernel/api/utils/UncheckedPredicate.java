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

package org.fcrepo.kernel.api.utils;

import java.util.function.Predicate;

import javax.jcr.RepositoryException;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

/**
 * Operations that throw {@link RepositoryException} cannot be used as lambdas without "unchecking" those exceptions.
 *
 * @author ajs6f
 * @param <T> the type of the input to the predicate
 */
@FunctionalInterface
public interface UncheckedPredicate<T> extends Predicate<T> {

    @Override
    default boolean test(final T elem) {
        try {
            return testThrows(elem);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * The same semantic as {@link #test(Object)}, but allowed to throw a {@link RepositoryException}
     *
     * @param elem the input argument
     * @return true if the input matches the predicate, otherwise false
     * @throws RepositoryException a repository-related exception
     */
    boolean testThrows(T elem) throws RepositoryException;

    /**
     * @param <T> the type of the input to the predicate
     * @param p a lambda expression
     * @return an unchecked version of that lambda
     */
    static <T> UncheckedPredicate<T> uncheck(final UncheckedPredicate<T> p) {
        return p;
    }
}
