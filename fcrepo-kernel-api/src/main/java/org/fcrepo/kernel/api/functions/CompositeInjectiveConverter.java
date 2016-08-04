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
 * @param <A> the domain of the first chained function
 * @param <B> the range of the first and domain of the second functions
 * @param <C> the range of the second function
 */
public class CompositeInjectiveConverter<A, B, C> implements InjectiveConverter<A, C> {

    final InjectiveConverter<A,B> first;
    final InjectiveConverter<B,A> firstReverse;
    final InjectiveConverter<B,C> second;
    final InjectiveConverter<C,B> secondReverse;

    /**
     * 
     * @param first the first function
     * @param second the function to be applied to outputs from the first
     */
    public CompositeInjectiveConverter(final InjectiveConverter<A,B> first, final InjectiveConverter<B,C> second) {
        this.first = first;
        this.firstReverse = first.inverse();
        this.second = second;
        this.secondReverse = second.inverse();
    }

    @Override
    public A toDomain(final C resource) {
        return first.toDomain(second.toDomain(resource));
    }

    @Override
    public C apply(final A a) {
        return second.apply(first.apply(a));
    }

    @Override
    public boolean inDomain(final A a) {
        return first.inDomain(a);
    }

    @Override
    public InjectiveConverter<C, A> inverse() {
        return secondReverse.andThen(firstReverse);
    }

}
