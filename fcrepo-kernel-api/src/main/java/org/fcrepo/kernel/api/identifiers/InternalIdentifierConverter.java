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
 * Translates internal {@link String} identifiers to internal {@link String}
 * identifiers.
 *
 * @author ajs6f
 * @since Apr 1, 2014
 */
public abstract class InternalIdentifierConverter extends Converter<String, String> {

    /*
     * (non-Javadoc)
     * @see com.google.common.base.Converter#doForward(java.lang.Object)
     */
    @Override
    protected String doForward(final String a) {
        return a;
    }

    /*
     * (non-Javadoc)
     * @see com.google.common.base.Converter#doBackward(java.lang.Object)
     */
    @Override
    protected String doBackward(final String b) {
        return b;
    }

}
