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

import static com.google.common.base.Throwables.propagate;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import javax.jcr.Property;

import org.fcrepo.kernel.impl.services.functions.GetClusterExecutor;
import org.fcrepo.kernel.utils.FixityResult;
import org.infinispan.distexec.DistributedExecutorService;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.infinispan.ChunkBinaryMetadata;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.modeshape.jcr.value.binary.infinispan.InfinispanUtils;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;

/**
 * @author cabeer
 */
public class InfinispanCacheStoreEntry extends LocalBinaryStoreEntry {
    private static final Logger LOGGER = getLogger(InfinispanCacheStoreEntry.class);

    private static final GetClusterExecutor EXECUTOR_FACTORY = new GetClusterExecutor();
    /**
     *
     * @param store
     * @param property
     */
    public InfinispanCacheStoreEntry(final InfinispanBinaryStore store, final Property property) {
        super(store, property);
    }

    @Override
    public Collection<FixityResult> checkFixity(final String algorithm) {
        final BinaryKey key = binaryKey();
        final ImmutableSet.Builder<FixityResult> fixityResults = new ImmutableSet.Builder<>();

        if (store().hasBinary(key)) {
            final String dataKey = InfinispanUtils.dataKeyFrom((InfinispanBinaryStore)store(), key);
            final ChunkBinaryMetadata metadata = InfinispanUtils.getMetadata((InfinispanBinaryStore)store(), key);

            final DistributedFixityCheck task = new DistributedFixityCheck(dataKey, algorithm, metadata.getChunkSize(),
                    metadata.getLength());

            final List<Future<Collection<FixityResult>>> futures
                = clusterExecutor().submitEverywhere(task, dataKey + "-0");

            while (!futures.isEmpty()) {
                final Iterator<Future<Collection<FixityResult>>> iterator = futures.iterator();
                while (iterator.hasNext()) {
                    final Future<Collection<FixityResult>> future = iterator.next();
                    try {
                        final Collection<FixityResult> result = future.get(100, MILLISECONDS);
                        iterator.remove();

                        fixityResults.addAll(result);
                    } catch (final TimeoutException e) {
                        LOGGER.trace("Going to retry cluster transform after timeout", e);
                    } catch (InterruptedException | ExecutionException e) {
                        throw propagate(e);
                    }
                }
            }
        }
        return fixityResults.build();
    }

    private DistributedExecutorService clusterExecutor() {
        return EXECUTOR_FACTORY.apply((InfinispanBinaryStore)store());
    }
}
