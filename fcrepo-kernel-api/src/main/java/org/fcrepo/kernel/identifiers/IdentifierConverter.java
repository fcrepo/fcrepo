/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.identifiers;

import com.google.common.base.Converter;

/**
 * An {@link IdentifierConverter} accepts and returns identifiers, translating
 * them in some type-specific manner. The typical use of this
 * contract is for translating between internal and external identifiers.
 *
 * @author ajs6f
 * @since Mar 26, 2014
 * @param <T> the type to and from which we are translating
 */
public abstract class IdentifierConverter<T> extends Converter<String, T> {
    protected int hierarchyLevels = 0;

    /**
     * Get hierarchy levels for path conversion
     * @return
     **/
    public int getLevels() {
        return hierarchyLevels;
    }
}
