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
package org.fcrepo.kernel.modeshape.observer;

import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.kernel.modeshape.observer.FedoraEventImpl.getResourceTypes;

import javax.jcr.observation.Event;

import org.slf4j.Logger;

import java.util.Set;

/**
 * Suppresses events emitted by nodes with a provided set of mixins.
 *
 * @author escowles
 * @author ajs6f
 * @since 2015-04-15
 */
public class SuppressByMixinFilter extends DefaultFilter {

    private static final Logger LOGGER = getLogger(SuppressByMixinFilter.class);
    private final Set<String> suppressedMixins;

    /**
     * @param suppressedMixins Resources with these mixins will be filtered out
     */
    public SuppressByMixinFilter(final Set<String> suppressedMixins) {
        this.suppressedMixins = suppressedMixins;
        LOGGER.info("Suppressing events for nodes with mixins: {}", suppressedMixins);
    }

    @Override
    public boolean test(final Event event) {
        return super.test(event) && !getResourceTypes(event).anyMatch(suppressedMixins::contains);
    }

}
