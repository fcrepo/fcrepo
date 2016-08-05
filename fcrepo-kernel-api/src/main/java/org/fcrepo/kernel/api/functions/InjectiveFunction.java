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
 * A reversible function that is injective.
 * @author barmintor
 *
 * @param <A> the input type
 * @param <B> the output type
 */

public interface InjectiveFunction<A, B> extends ReversibleFunction<A, B> {
    /**
     * Whether the value is in the range of this function
     * @param b a result of the converter function
     * @return whether b is in the range of the converter
     */
    default boolean inRange(final B b) {
        return true;
    }

    /**
     * Reverse the function.
     * @param b the candidate value for which a domain value should be calculated
     * @return a domain value or null
     */
    A toDomain(B b);

    /**
     * The inverse of the defined function
     * @return the inverse of the defined function.
     */
    @Override
    default ReversibleFunction<B, A> reverse() {
        return new InverseFunctionWrapper<>(this);
    }
}
