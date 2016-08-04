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


/**
 * 
 * @author barmintor
 * @param <A>
 *
 */
public class InverseIdentifierConverter<A,B> extends IdentifierConverter<A,B> {

    private final IdentifierConverter<B,A> original;

    /**
     * 
     * @param original
     */
    public InverseIdentifierConverter(final IdentifierConverter<B,A> original) {
        this.original = original;
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
    public IdentifierConverter<B, A> inverse() {
        return original;
    }

    @Override
    public A toDomain(final B resource) {
        return original.apply(resource);
    }

    @Override
    public String asString(final A resource) {
        return apply(resource).toString();
    }

}
