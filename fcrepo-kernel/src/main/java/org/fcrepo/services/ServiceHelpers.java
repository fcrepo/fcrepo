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
package org.fcrepo.services;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;

import java.net.URI;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.fcrepo.services.functions.CheckCacheEntryFixity;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.LowLevelCacheEntry;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutorService;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;

import com.google.common.base.Function;
import org.springframework.stereotype.Component;

@Component
public abstract class ServiceHelpers {

    /**
     * Get the total size of a Node's properties
     * @param node
     * @return size in bytes
     * @throws RepositoryException
     */
    public static Long getNodePropertySize(final Node node)
        throws RepositoryException {
        Long size = 0L;
        for (final PropertyIterator i = node.getProperties(); i.hasNext();) {
            final Property p = i.nextProperty();
            if (p.isMultiple()) {
                for (final Value v : p.getValues()) {
                    size += v.getBinary().getSize();
                }
            } else {
                size += p.getBinary().getSize();
            }
        }
        return size;
    }

    /**
     * @param obj
     * @return object size in bytes
     * @throws RepositoryException
     */
    public static Long getObjectSize(final Node obj)
        throws RepositoryException {
        return getNodePropertySize(obj) + getObjectDSSize(obj);
    }

    /**
     * @param obj
     * @return object's datastreams' total size in bytes
     * @throws RepositoryException
     */
    private static Long getObjectDSSize(final Node obj)
        throws RepositoryException {
        Long size = 0L;
        for (final NodeIterator i = obj.getNodes(); i.hasNext();) {
            final Node node = i.nextNode();

            if (node.isNodeType(NT_FILE)) {
                size += getDatastreamSize(node);
            }
        }
        return size;
    }

    /**
     * Get the size of a datastream by calculating the size of the
     * properties and the binary properties
     * @param ds
     * @return
     * @throws RepositoryException
     */
    public static Long getDatastreamSize(final Node ds)
        throws RepositoryException {
        return getNodePropertySize(ds) + getContentSize(ds);
    }

    /**
     * Get the size of the JCR content binary property
     * @param ds
     * @return
     * @throws RepositoryException
     */
    public static Long getContentSize(final Node ds)
        throws RepositoryException {
        long size = 0L;
        if (ds.hasNode(JCR_CONTENT)) {
            final Node contentNode = ds.getNode(JCR_CONTENT);

            if (contentNode.hasProperty(JCR_DATA)) {
                size = ds.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary()
                    .getSize();
            }
        }

        return size;
    }

    /**
     * A static factory function to insulate services from the details of building
     * a DistributedExecutorService
     * @param cache
     * @return
     */
    public static DistributedExecutorService getClusterExecutor(InfinispanBinaryStore cacheStore) {
        // Watch out! This is trying to pluck out the blob cache store. This works as long as
        // modeshape continues to be ordered..
        return new DefaultExecutorService(cacheStore.getCaches().get(1));
    }

    /**
     * Get the fixity function to map a low-level cache entry to its fixity result
     * @param dsChecksum
     * @param dsSize
     * @return
     */
    public static Function<LowLevelCacheEntry, FixityResult> getCheckCacheFixityFunction(final URI dsChecksum,
                                                                                         final long dsSize) {
        return new CheckCacheEntryFixity(dsChecksum, dsSize);
    }

}
