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

import java.util.function.BiConsumer;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;

/**
 * It is sometimes convenient to use an operation that throws checked exceptions inside of a functional incantation.
 * This type permits that. It should be used with great caution! Only uncheck exceptions for which no reasonable
 * recovery is available.
 *
 * @author ajs6f
 * @param <S> the type of the first argument to the operation
 * @param <T> the type of the second argument to the operation
 */
@FunctionalInterface
public interface UncheckedBiConsumer<S, T> extends BiConsumer<S, T> {

    @Override
    default void accept(final S first, final T second) {
        try {
            acceptThrows(first, second);
        } catch (final Exception e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * The same semantic as {@link #accept(Object, Object)}, but allowed to throw exceptions.
     *
     * @param first the first input argument
     * @param second the second input argument
     * @throws Exception the underlying exception
     */
    void acceptThrows(final S first, final T second) throws Exception;

    /**
     * A convenience method to construct <code>UncheckedBiConsumers</code> from lambda syntax.
     *
     * @param <S> the type of the first argument
     * @param <T> the type of the second argument
     * @param c an arity-2 lambda function
     * @return an UncheckedBiConsumer
     */
    static <S, T> UncheckedBiConsumer<S, T> uncheck(final UncheckedBiConsumer<S, T> c) {
        return c;
    }
}
