/*
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
package org.fcrepo.kernel.api.identifiers;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author acoburn
 * @since 6/20/16
 * @param <A> the type from which we are translating
 * @param <B> the type to which we are translating
 */
public interface Converter<A, B> {

    /**
     * Return a function that maps A to B
     * @return the mapping function
     */
    public Function<A, B> to();

    /**
     * Return a function that maps B to A
     * @return an inverse of the mapping function
     */
    public Function<B, A> from();

    /**
     * Test whether the object of type A is in the domain of the converter
     * @return a predicate for testing whether an object of type A is in domain
     */
    public Predicate<A> inDomain();

    /**
     * Convert the given resource into a plain string representation after converting
     * it to type B.
     * @param resource the given resource
     * @return a plain string representation of the converted resource.
     */
    public String asString(final A resource);
}
