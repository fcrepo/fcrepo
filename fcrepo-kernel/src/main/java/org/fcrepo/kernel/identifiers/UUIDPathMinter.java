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

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Joiner.on;
import static com.google.common.base.Splitter.fixedLength;
import static java.util.UUID.randomUUID;
import static org.fcrepo.metrics.RegistryService.getMetrics;

import com.codahale.metrics.Timer;

/**
 * PID minter that creates hierarchical IDs for a UUID
 *
 * @author awoods
 */
public class UUIDPathMinter extends BasePidMinter {

    static final Timer timer = getMetrics().timer(
            name(UUIDPathMinter.class, "mint"));

    private static final int DEFAULT_LENGTH = 2;

    private static final int DEFAULT_COUNT = 4;

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
    public String mintPid() {

        final Timer.Context context = timer.time();

        try {
            final String s = randomUUID().toString();
            final Iterable<String> split =
                    fixedLength(length).split(s.substring(0, length * count));

            return on("/").join(split) + "/" + s;
        } finally {
            context.stop();
        }
    }
}
