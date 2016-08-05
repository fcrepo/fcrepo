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

/**
 * @author barmintor
 * @author ajs6f
 * @param <A> the domain of the first chained function
 * @param <C> the range of the second function
 */
class CompositeConverter<A, C> implements Converter<A, C> {

    private final Converter<A, ?> first;

    private final Converter<?, C> second;

    @SuppressWarnings("unchecked")
    protected <B> Converter<A, B> first() {
        return (Converter<A, B>) first;
    }

    @SuppressWarnings("unchecked")
    protected <B> Converter<B, C> second() {
        return (Converter<B, C>) second;
    }

    /**
     * @param first the first function
     * @param second the function to be applied to outputs from the first
     */
    public <B> CompositeConverter(final Converter<A, B> first, final Converter<B, C> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public C apply(final A a) {
        return second().apply(first().apply(a));
    }

    @Override
    public boolean inDomain(final A a) {
        return first().inDomain(a) && second().inDomain(first().apply(a));
    }

    @Override
    public Converter<C, A> reverse() {
        return second().reverse().andThen(first().reverse());
    }
}
