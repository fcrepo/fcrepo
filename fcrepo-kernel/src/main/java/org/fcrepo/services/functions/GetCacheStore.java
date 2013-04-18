
package org.fcrepo.services.functions;

import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;

import com.google.common.base.Function;

public class GetCacheStore implements Function<Cache<?, ?>, CacheStore> {

    @Override
    public CacheStore apply(final Cache<?, ?> input) {
        return input.getAdvancedCache().getComponentRegistry().getComponent(
                CacheLoaderManager.class).getCacheStore();
    }

}
