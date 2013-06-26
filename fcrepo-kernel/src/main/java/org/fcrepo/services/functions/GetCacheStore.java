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
package org.fcrepo.services.functions;

import static com.google.common.base.Preconditions.checkArgument;

import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;

import com.google.common.base.Function;

/**
 * Function encapsulating the various Infinispan API calls to retrieve
 * a CacheStore from a Cache.
 * @author barmintor
 * @date Apr 2, 2013
 */
public class GetCacheStore implements Function<Cache<?, ?>, CacheStore> {

    /**
     * Check the Infinispan ComponentRegistry for the CacheStore for a given
     * associated with a given Cache.
     */
    @Override
    public CacheStore apply(final Cache<?, ?> input) {
        checkArgument(input != null, "null cannot have a CacheStore!");
        return input.getAdvancedCache()
            .getComponentRegistry()
            .getComponent(CacheLoaderManager.class).getCacheStore();
    }

}
