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
package org.fcrepo.kernel.api.functions;

import static java.util.Objects.requireNonNull;

/**
 *
 * @author barmintor
 * @author ajs6f
 *
 * @param <A> the input type
 * @param <B> the output type
 */
public interface InjectiveConverter<A,B> extends InjectiveFunction<A, B>, Converter<A, B> {

    public static final Identity<?> IDENTITY = new Identity<>();

    @Override
    default InjectiveConverter<B, A> reverse() {
        return new InverseConverterWrapper<>(this);
    }

    /**
     * A typed composition
     * @see java.util.function.Function
     * @param after the converter to append
     * @param <C> the output type of the result
     * @return a new converter snocing this and the input
     */
    default <C> InjectiveConverter<A, C> andThen(final InjectiveConverter<B, C> after) {
        return new CompositeInjectiveConverter<>(this, requireNonNull(after, "Cannot compose with null!"));
    }

    /**
     * A typed composition
     * @see java.util.function.Function
     * @param before the converter to prefix
     * @param <C> the input type of the result
     * @return a new converter consing the input and this
     */
    default <C> InjectiveConverter<C, B> compose(final InjectiveConverter<C, A> before) {
        return new CompositeInjectiveConverter<>(requireNonNull(before, "Cannot compose with null!"), this);
    }

    /**
     * Convenience method for building a typed identity function
     * @param <T> the type of the returned identity function
     * @return a typed identity function
     */
    @SuppressWarnings("unchecked")
    static <T> InjectiveConverter<T, T> identity() {
        return (InjectiveConverter<T, T>) IDENTITY;
    }

    static class Identity<T> implements InjectiveConverter<T, T> {
        @Override
        public T toDomain(final T rangeValue) {
            return rangeValue;
        }

        @Override
        public T apply(final T t) {
            return t;
        }

        @Override
        public boolean inDomain(final T a) {
            return a != null;
        }

        @Override
        public InjectiveConverter<T, T> reverse() {
            return this;
        }

        @Override
        public <C> Converter<T, C> andThen(final Converter<T, C> after) {
            return after;
        }

        @Override
        public <C> Converter<C, T> compose(final Converter<C, T> before) {
            return before;
        }
    }

    static class InverseConverterWrapper<A, B> implements InjectiveConverter<B, A> {

        private final InjectiveConverter<A, B> wrapped;

        public InverseConverterWrapper(final InjectiveConverter<A, B> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public B toDomain(final A a) {
            return wrapped.apply(a);
        }

        @Override
        public A apply(final B b) {
            return wrapped.toDomain(b);
        }

        @Override
        public InjectiveConverter<A, B> reverse() {
            return wrapped;
        }

        @Override
        public boolean inDomain(final B b) {
            return wrapped.inRange(b);
        }
    }
}
