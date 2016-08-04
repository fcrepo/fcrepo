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
package org.fcrepo.kernel.api.identifiers;

import org.fcrepo.kernel.api.functions.Converter;

/**
 * Translates internal {@link String} identifiers to internal {@link String}
 * identifiers.
 *
 * @author ajs6f
 * @since Apr 1, 2014
 */
public abstract class InternalIdentifierConverter implements Converter<String, String> {

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.functions.Converter#apply(java.lang.Object)
     */
    @Override
    public String apply(final String a) {
        return a;
    }

    @Override
    public boolean inDomain(final String value) {
        return value != null;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.functions.Converter#toDomain(java.lang.Object)
     */
    @Override
    public String toDomain(final String b) {
        return b;
    }

}
