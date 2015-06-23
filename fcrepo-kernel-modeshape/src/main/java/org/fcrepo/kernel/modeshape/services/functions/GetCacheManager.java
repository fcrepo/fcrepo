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
package org.fcrepo.kernel.modeshape.services.functions;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.function.Function;

import javax.jcr.Repository;

import org.infinispan.manager.DefaultCacheManager;
import org.modeshape.jcr.JcrRepository;
import org.slf4j.Logger;

/**
 * Get the internal Infinispan CacheManager from a Repository
 *
 * @author acoburn
 */
class GetCacheManager implements Function<Repository, DefaultCacheManager> {

    private static final Logger LOGGER = getLogger(GetCacheManager.class);

    @Override
    public DefaultCacheManager apply(final Repository repo) {
        requireNonNull(repo, "null cannot have a CacheManager");
        try {
            if (repo instanceof JcrRepository) {
                return new DefaultCacheManager(((JcrRepository)repo).getConfiguration().getCacheConfiguration());
            }
        } catch (final IOException ex) {
            LOGGER.debug("Could not extract JcrRepository cache configuration", ex);
        }
        return null;
    }
}

