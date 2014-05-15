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
import static java.util.UUID.randomUUID;
import static org.fcrepo.metrics.RegistryService.getMetrics;

import com.codahale.metrics.Timer;

/**
 * Simple PidMinter that replies on Java's inbuilt UUID minting.
 *
 * @author eddies
 * @author ajs6f
 * @since Feb 7, 2013
 */
public class UUIDPidMinter extends BasePidMinter {

    static final Timer timer = getMetrics().timer(
            name(UUIDPidMinter.class, "mint"));

    /**
     * Mint a unique identifier as a UUID
     * @return uuid
     */
    @Override
    public String mintPid() {

        final Timer.Context context = timer.time();

        try {
            return randomUUID().toString();
        } finally {
            context.stop();
        }
    }
}
