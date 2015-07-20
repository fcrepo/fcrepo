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
package org.fcrepo.kernel.modeshape.identifiers;

import org.fcrepo.kernel.api.identifiers.InternalIdentifierConverter;

/**
 * @author cabeer
 * @since 10/9/14
 */
public class HashConverter extends InternalIdentifierConverter {

    @Override
    protected String doForward(final String externalId) {
        final int i = externalId.indexOf('#');
        if (i >= 0) {
            return externalId.substring(0, i) + "/#/" + externalId.substring(i + 1).replace("/", "%2F");
        }
        return externalId;
    }

    @Override
    protected String doBackward(final String internalId) {

        final int i = internalId.indexOf("/#/");

        if (i >= 0) {
            return internalId.substring(0, i) + "#" + internalId.substring(i + 3).replace("%2F", "/");
        }
        return internalId;
    }
}
