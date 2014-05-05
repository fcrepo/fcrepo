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
package org.fcrepo.kernel.services;

import static javax.jcr.query.Query.JCR_SQL2;
import static org.fcrepo.jcr.FedoraJcrTypes.CONTENT_SIZE;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_BINARY;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_OBJECT;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.JCR_PATH;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;

import java.net.URI;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.fcrepo.kernel.services.functions.CheckCacheEntryFixity;
import org.fcrepo.kernel.utils.FixityResult;
import org.fcrepo.kernel.utils.LowLevelCacheEntry;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutorService;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;

/**
 * Uncategorized helper methods
 *
 * @author awoods
 */
@Component
public abstract class ServiceHelpers {

    /**
     * Get the total size of a Node's properties
     * 
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
    public static Long getObjectSize(final Node obj) throws RepositoryException {
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
     * Get the size of a datastream by calculating the size of the properties
     * and the binary properties
     * 
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
     * 
     * @param ds
     * @return
     * @throws RepositoryException
     */
    public static Long getContentSize(final Node ds) throws RepositoryException {
        long size = 0L;
        if (ds.hasNode(JCR_CONTENT)) {
            final Node contentNode = ds.getNode(JCR_CONTENT);

            if (contentNode.hasProperty(JCR_DATA)) {
                size =
                        ds.getNode(JCR_CONTENT).getProperty(JCR_DATA)
                                .getBinary().getSize();
            }
        }

        return size;
    }

    /**
     * A static factory function to insulate services from the details of
     * building a DistributedExecutorService
     * 
     * @param cache
     * @return
     */
    public static DistributedExecutorService getClusterExecutor(
            final InfinispanBinaryStore cacheStore) {
        // Watch out! This is trying to pluck out the blob cache store. This
        // works as long as
        // modeshape continues to be ordered..
        return new DefaultExecutorService(cacheStore.getCaches().get(1));
    }

    /**
     * Get the fixity function to map a low-level cache entry to its fixity
     * result
     * 
     * @param dsChecksum
     * @param dsSize
     * @return
     */
    public static
    Function<LowLevelCacheEntry, FixityResult>
    getCheckCacheFixityFunction(final URI dsChecksum, final long dsSize) {
        return new CheckCacheEntryFixity(dsChecksum, dsSize);
    }

    /**
     * @return a double of the size of the fedora:datastream binary content
     * @throws RepositoryException
     */
    public static long getRepositoryCount(final Repository repository)
        throws RepositoryException {
        final Session session = repository.login();
        try {
            final QueryManager queryManager =
                session.getWorkspace().getQueryManager();

            final String querystring =
                "SELECT [" + JCR_PATH + "] FROM ["
                        + FEDORA_OBJECT + "]";

            final QueryResult queryResults =
                queryManager.createQuery(querystring, JCR_SQL2).execute();

            return queryResults.getRows().getSize();
        } finally {

            session.logout();
        }
    }

    /**
     * @return a double of the size of the fedora:datastream binary content
     * @throws RepositoryException
     */
    public static long getRepositorySize(final Repository repository)
        throws RepositoryException {
        final Session session = repository.login();
        long sum = 0;
        final QueryManager queryManager =
            session.getWorkspace().getQueryManager();

        final String querystring =
                "SELECT [" + CONTENT_SIZE + "] FROM [" +
                        FEDORA_BINARY + "]";

        final QueryResult queryResults =
            queryManager.createQuery(querystring, JCR_SQL2).execute();

        for (final RowIterator rows = queryResults.getRows(); rows.hasNext();) {
            final Value value =
                    rows.nextRow().getValue(CONTENT_SIZE);
            sum += value.getLong();
        }

        session.logout();

        return sum;
    }
}
