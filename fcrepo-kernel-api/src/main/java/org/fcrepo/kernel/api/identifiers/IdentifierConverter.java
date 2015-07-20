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
package org.fcrepo.kernel.api.identifiers;

import com.google.common.base.Converter;

/**
 * An {@link IdentifierConverter} accepts and returns identifiers, translating
 * them in some type-specific manner. The typical use of this
 * contract is for translating between internal and external identifiers.
 *
 * @author ajs6f
 * @since Mar 26, 2014
 * @param <B> the type to and from which we are translating
 */
public abstract class IdentifierConverter<A, B> extends Converter<A, B> {

    /**
     * Check if the given resource is in the domain of this converter
     * @param resource the given resource
     * @return boolean value of the check
     */
    public boolean inDomain(final A resource) {
        return convert(resource) != null;
    }

    /**
     * Convert a plain string to a resource appropriate to this converter
     * @param resource the plain string resource
     * @return the resource appropriate to this converter
     */
    abstract public A toDomain(final String resource);

    /**
     * Convert the given resource into a plain string representation of the conversion to the resource
     * @param resource the given resource
     * @return a plain string representation of the conversion to the resource
     */
    abstract public String asString(final A resource);
}
