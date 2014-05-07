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
package org.fcrepo.kernel.utils.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.utils.ContentDigest;
import org.fcrepo.kernel.utils.FixityInputStream;
import org.fcrepo.kernel.utils.FixityResult;
import org.fcrepo.kernel.utils.FixityResultImpl;
import org.fcrepo.kernel.utils.infinispan.StoreChunkInputStream;
import org.infinispan.Cache;
import org.infinispan.CacheImpl;
import org.infinispan.distexec.DistributedCallable;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.decorators.ChainingCacheStore;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Set;

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;

/**
 * Infinispan DistributedCallable for checking the fixity of a binary key in every cache loader
 *
 * @author cabeer
 */
public class DistributedFixityCheck implements DistributedCallable<String, byte[], Collection<FixityResult>>,
                                                   Serializable {
    private final String dataKey;
    private Cache<String, byte[]> cache;

    /**
     *
     * @param dataKey
     */
    public DistributedFixityCheck(final String dataKey) {
        this.dataKey = dataKey;
    }

    @Override
    public Collection<FixityResult> call() throws Exception {
        final ImmutableSet.Builder<FixityResult> fixityResults = new ImmutableSet.Builder<>();

        for (final CacheStore store : stores()) {

            final String digest = ContentDigest.getAlgorithm(new URI("urn:sha1"));

            FixityInputStream fixityInputStream;
            final InputStream cacheLoaderChunkInputStream = new StoreChunkInputStream(store, dataKey);

            fixityInputStream = new FixityInputStream(cacheLoaderChunkInputStream, MessageDigest.getInstance(digest));
            IOUtils.copy(fixityInputStream, NULL_OUTPUT_STREAM);

            final URI calculatedChecksum = ContentDigest.asURI(digest, fixityInputStream.getMessageDigest().digest());
            fixityResults.add(
                new FixityResultImpl(getExternalIdentifier(store), fixityInputStream.getByteCount(), calculatedChecksum)
            );
        }

        return fixityResults.build();
    }

    private String getExternalIdentifier(final CacheStore store) {
        final String address;

        if (cache.getCacheManager().getAddress() != null) {
            address = cache.getCacheManager().getAddress().toString();
        } else {
            address = "localhost";
        }

        return "infinispan-cache-loader:" + address + "/" + store.toString() + "#" +  dataKey;
    }

    @Override
    public void setEnvironment(final Cache<String, byte[]> cache, final Set<String> inputKeys) {
        this.cache = cache;
    }

    private Set<CacheStore> stores() {
        final CacheLoaderManager cacheLoaderManager
            = ((CacheImpl) cache).getComponentRegistry().getLocalComponent(CacheLoaderManager.class);
        if (cacheLoaderManager.getCacheLoader() instanceof ChainingCacheStore) {
            return ((ChainingCacheStore)cacheLoaderManager.getCacheLoader()).getStores().keySet();
        } else {
            return ImmutableSet.of(cacheLoaderManager.getCacheStore());
        }
    }
}
