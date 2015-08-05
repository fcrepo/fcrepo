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
package org.fcrepo.kernel.api.services.functions;

import static java.util.UUID.randomUUID;

import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.StringJoiner;

/**
 * Unique value minter that creates hierarchical IDs from a UUID
 *
 * @author awoods
 * @author acoburn
 */
public interface UniqueValueSupplier extends Supplier<String> {

    /**
     * Mint a unique identifier as a UUID
     *
     * @return uuid
     */
    default public String get() {
        final int defaultLength = 2;
        final int defaultCount = 4;

        final String s = randomUUID().toString();
        final StringJoiner joiner = new StringJoiner("/", "", "/" + s);

        IntStream.rangeClosed(0, defaultCount - 1)
                 .forEach(x -> joiner.add(s.substring(x * defaultLength, (x + 1) * defaultLength)));

        return joiner.toString();
    }
}
