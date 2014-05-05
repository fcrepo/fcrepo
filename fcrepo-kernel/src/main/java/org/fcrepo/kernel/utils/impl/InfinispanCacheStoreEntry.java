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

import static com.google.common.base.Throwables.propagate;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.BAD_CHECKSUM;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.BAD_SIZE;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.MISSING_STORED_FIXITY;
import static org.fcrepo.kernel.utils.FixityResult.FixityState.SUCCESS;

import com.google.common.collect.ImmutableSet;
import org.fcrepo.kernel.services.ServiceHelpers;
import org.fcrepo.kernel.utils.ContentDigest;
import org.fcrepo.kernel.utils.FixityResult;
import org.infinispan.distexec.DistributedExecutorService;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.slf4j.Logger;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 */
public class InfinispanCacheStoreEntry extends LocalBinaryStoreEntry {
    private static final Logger LOGGER = getLogger(InfinispanCacheStoreEntry.class);
    private static final String DATA_SUFFIX = "-data";

    /**
     *
     * @param store
     * @param property
     */
    public InfinispanCacheStoreEntry(final InfinispanBinaryStore store, final Property property) {
        super(store, property);
    }

    @Override
    public Collection<FixityResult> checkFixity(final URI checksum, final long size) throws RepositoryException {
        final BinaryKey key = binaryKey();
        final ImmutableSet.Builder<FixityResult> fixityResults = new ImmutableSet.Builder<>();

        if (store().hasBinary(key)) {
            final String dataKey = dataKeyFor(key);

            final DistributedFixityCheck task = new DistributedFixityCheck(dataKey);
            final List<Future<Collection<FixityResult>>> futures
                = clusterExecutor().submitEverywhere(task, dataKey + "-0");

            while (!futures.isEmpty()) {
                final Iterator<Future<Collection<FixityResult>>> iterator = futures.iterator();
                while (iterator.hasNext()) {
                    final Future<Collection<FixityResult>> future = iterator.next();
                    try {
                        final Collection<FixityResult> result = future.get(100, MILLISECONDS);
                        iterator.remove();
                        for (final FixityResult fixityResult : result) {
                            setFixityStatus(fixityResult, size, checksum);
                        }

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

    private String dataKeyFor(final BinaryKey key) {
        return key + DATA_SUFFIX;
    }

    private DistributedExecutorService clusterExecutor() {
        return ServiceHelpers.getClusterExecutor((InfinispanBinaryStore)store());
    }
    private void setFixityStatus(final FixityResult result, final long dsSize, final URI dsChecksum) {
        if (dsChecksum.equals(ContentDigest.missingChecksum()) || dsSize == -1L) {
            result.getStatus().add(MISSING_STORED_FIXITY);
        }

        if (!result.matches(dsChecksum)) {
            result.getStatus().add(BAD_CHECKSUM);
        }

        if (!result.matches(dsSize)) {
            result.getStatus().add(BAD_SIZE);
        }

        if (result.matches(dsSize, dsChecksum)) {
            result.getStatus().add(SUCCESS);
        }
    }
}
