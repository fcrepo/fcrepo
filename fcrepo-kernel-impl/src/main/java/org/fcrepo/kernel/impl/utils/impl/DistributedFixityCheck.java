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
package org.fcrepo.kernel.impl.utils.impl;

import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Set;

import org.fcrepo.kernel.impl.utils.FixityInputStream;
import org.fcrepo.kernel.impl.utils.FixityResultImpl;
import org.fcrepo.kernel.impl.utils.infinispan.CacheLoaderChunkInputStream;
import org.fcrepo.kernel.utils.ContentDigest;
import org.fcrepo.kernel.utils.FixityResult;
import org.infinispan.Cache;
import org.infinispan.cache.impl.CacheImpl;
import org.infinispan.distexec.DistributedCallable;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.spi.CacheLoader;

import com.google.common.collect.ImmutableSet;

/**
 * Infinispan DistributedCallable for checking the fixity of a binary key in every cache loader
 *
 * @author cabeer
 */
public class DistributedFixityCheck implements DistributedCallable<String, byte[], Collection<FixityResult>>,
                                                   Serializable {
    private static final long serialVersionUID = 1L;

    private final String dataKey;
    private final String digest;
    private final int chunkSize;
    private final long length;
    private Cache<String, byte[]> cache;

    /**
     *
     * @param dataKey the data key
     * @param digest the digest
     * @param chunkSize the chunk size
     * @param length the given length
     */
    public DistributedFixityCheck(final String dataKey, final String digest, final int chunkSize, final long length) {
        this.dataKey = dataKey;
        this.digest = digest;
        this.chunkSize = chunkSize;
        this.length = length;
    }

    @Override
    public Collection<FixityResult> call() throws Exception {
        final ImmutableSet.Builder<FixityResult> fixityResults = new ImmutableSet.Builder<>();

        for (final CacheLoader<String, byte[]> store : stores()) {

            try (final InputStream cacheLoaderChunkInputStream = new CacheLoaderChunkInputStream(
                    store, dataKey, chunkSize, length);

                    final FixityInputStream fixityInputStream = new FixityInputStream(
                            cacheLoaderChunkInputStream, MessageDigest.getInstance(digest))) {

                copy(fixityInputStream, NULL_OUTPUT_STREAM);

                final URI calculatedChecksum =
                        ContentDigest.asURI(digest, fixityInputStream.getMessageDigest().digest());
                fixityResults.add(
                        new FixityResultImpl(getExternalIdentifier(store), fixityInputStream.getByteCount(),
                                calculatedChecksum));
            }
        }

        return fixityResults.build();
    }

    private String getExternalIdentifier(final CacheLoader<String, byte[]> store) {
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

    @SuppressWarnings("rawtypes")
    private Set<CacheLoader> stores() {
        return ((CacheImpl<String, byte[]>)cache).getComponentRegistry().getLocalComponent(PersistenceManager.class)
                .getStores(CacheLoader.class);
    }
}
