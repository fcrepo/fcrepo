/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.kernel.services.functions;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.propagate;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

import com.google.common.base.Function;

/**
 * Get the internal Modeshape BinaryKey for a binary property
 *
 * @author awoods
 */
public class GetBinaryKey implements Function<Property, BinaryKey> {

    @Override
    public BinaryKey apply(final Property input) {
        checkArgument(input != null, "null cannot have a Binarykey!");
        try {
            return ((BinaryValue) input.getBinary()).getKey();
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

}
