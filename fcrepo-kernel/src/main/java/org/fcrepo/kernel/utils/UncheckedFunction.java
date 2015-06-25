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

import java.util.function.Function;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;

/**
 * Operations that throw {@link RepositoryException} cannot be used as lambdas without "unchecking" those exceptions.
 *
 * @author ajs6f
 * @param <T>
 */
@FunctionalInterface
public interface UncheckedFunction<T, R> extends Function<T, R> {

    @Override
    default R apply(final T elem) {
        try {
            return applyThrows(elem);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * The same semantic as {@link #apply(Object)}, but allowed to throw a {@link RepositoryException}
     *
     * @param elem
     * @return
     * @throws RepositoryException
     */
    R applyThrows(T elem) throws RepositoryException;

    /**
     * @param p a lambda expression
     * @return an unchecked version of that lambda
     */
    static <T, R> UncheckedFunction<T, R> uncheck(final UncheckedFunction<T, R> p) {
        return p;
    }
}
