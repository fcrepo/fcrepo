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
package org.fcrepo.kernel.modeshape.identifiers;

import org.fcrepo.kernel.api.functions.Converter;

/**
 * An {@link IdentifierConverter} accepts and returns identifiers, translating
 * them in some type-specific manner. The typical use of this
 * contract is for translating between internal and external identifiers.
 *
 * @author ajs6f
 * @since Mar 26, 2014
 * @param <B> the type to and from which we are translating
 */
public abstract class IdentifierConverter<A, B> implements Converter<A, B> {

    /**
     * Invert this conversion function
     */
    @Override
    public IdentifierConverter<B, A> inverse() {
        return new InverseIdentifierConverter<B,A>(this);
    }

    /**
     * Check if the given resource is in the domain of this converter
     * @param resource the given resource
     * @return boolean value of the check
     */
    public boolean inDomain(final A resource) {
        return apply(resource) != null;
    }

    /**
     * Convert the given resource into a plain string representation of the conversion to the resource
     * @param resource the given resource
     * @return a plain string representation of the conversion to the resource
     */
    abstract public String asString(final A resource);

    /**
     * Compose a function from this and a second IdentifierConverter
     * @param second
     * @return
     */
    public <C> IdentifierConverter<A, C> andThen(final IdentifierConverter<B, C> second) {
        return new CompositeIdentifierConverter<>(this, second);
    }

    /**
     * Compose a function from a second IdentifierConverter and this
     * @param second
     * @return
     */
    public <C> IdentifierConverter<C, B> compose(final IdentifierConverter<C, A> second) {
        return new CompositeIdentifierConverter<>(second, this);
    }

    protected <T> Converter<T,T> identity() {
        return new Identity<>();
    }

    static class Identity<T> implements Converter<T, T> {

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
        public Converter<T, T> inverse() {
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
}
