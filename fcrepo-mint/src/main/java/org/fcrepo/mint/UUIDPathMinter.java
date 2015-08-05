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
package org.fcrepo.mint;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.UUID.randomUUID;

import java.util.stream.IntStream;
import java.util.StringJoiner;

import org.fcrepo.metrics.RegistryService;
import org.fcrepo.kernel.api.services.functions.HierarchicalIdentifierSupplier;

import com.codahale.metrics.Timer;

/**
 * PID minter that creates hierarchical IDs for a UUID
 *
 * @author awoods
 */
public class UUIDPathMinter implements HierarchicalIdentifierSupplier {

    static final Timer timer = RegistryService.getInstance().getMetrics().timer(
            name(UUIDPathMinter.class, "mint"));

    private final int length;

    private final int count;

    /**
     * Configure the path minter using some reasonable defaults for the length
     * and count of the branch nodes
     */
    public UUIDPathMinter() {
        this(DEFAULT_LENGTH, DEFAULT_COUNT);
    }

    /**
     * Configure the path minter for the length of the keys and depth of the
     * branch node prefix
     *
     * @param length how long the branch node identifiers should be
     * @param count how many branch nodes should be inserted
     */
    public UUIDPathMinter(final int length, final int count) {
        super();
        this.length = length;
        this.count = count;
    }

    /**
     * Mint a unique identifier as a UUID
     *
     * @return uuid
     */
    @Override
    public String get() {

        try (final Timer.Context context = timer.time()) {
            final String s = randomUUID().toString();

            if (length == 0 || count == 0) {
                return s;
            }

            final StringJoiner joiner = new StringJoiner("/", "", "/" + s);
            IntStream.rangeClosed(0, count - 1)
                     .forEach(x -> joiner.add(s.substring(x * length, (x + 1) * length)));

            return joiner.toString();
        }
    }
}
