/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services.functions;

import static com.google.common.base.Preconditions.checkArgument;

import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;

import com.google.common.base.Function;

/**
 * @todo Add Documentation.
 * @author barmintor
 * @date Apr 2, 2013
 */
public class GetCacheStore implements Function<Cache<?, ?>, CacheStore> {

    /**
     * @todo Add Documentation.
     */
    @Override
    public CacheStore apply(final Cache<?, ?> input) {
        checkArgument(input != null, "null cannot have a CacheStore!");
        return input.getAdvancedCache()
            .getComponentRegistry()
            .getComponent(CacheLoaderManager.class).getCacheStore();
    }

}
