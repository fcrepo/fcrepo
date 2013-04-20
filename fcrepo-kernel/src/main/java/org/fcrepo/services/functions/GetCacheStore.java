
package org.fcrepo.services.functions;

import static com.google.common.base.Preconditions.checkArgument;

import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;

import com.google.common.base.Function;

public class GetCacheStore implements Function<Cache<?, ?>, CacheStore> {

    @Override
    public CacheStore apply(final Cache<?, ?> input) {
        checkArgument(input != null, "null cannot have a CacheStore!");
        return input.getAdvancedCache().getComponentRegistry().getComponent(
                CacheLoaderManager.class).getCacheStore();
    }

}
