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
 * 
 * @author barmintor
 *
 * @param <A>
 * @param <B>
 */
public class InverseConverterWrapper<A, B> implements Converter<A, B> {

    private final Converter<B, A> original;
    /**
     * 
     * @param original
     */
    public InverseConverterWrapper(final Converter<B, A> original) {
        this.original = original;
    }

    @Override
    public A toDomain(final B rangeValue) {
        return original.apply(rangeValue);
    }

    @Override
    public B apply(final A t) {
        return original.toDomain(t);
    }

    @Override
    public boolean inDomain(final A a) {
        return original.inRange(a);
    }

    @Override
    public Converter<B, A> inverse() {
        return original;
    }

    @Override
    public <C> Converter<A, C> andThen(final Converter<B, C> after) {
        return new CompositeConverter<>(this, after);
    }

    @Override
    public <C> Converter<C, B> compose(final Converter<C, A> before) {
        return new CompositeConverter<>(before, this);
    }
}
