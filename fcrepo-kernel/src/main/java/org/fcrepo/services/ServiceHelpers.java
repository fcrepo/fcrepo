/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.services;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.NT_FILE;

import java.net.URI;
import java.security.MessageDigest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.fcrepo.services.functions.CheckCacheEntryFixity;
import org.fcrepo.utils.FixityResult;
import org.fcrepo.utils.LowLevelCacheEntry;

import com.google.common.base.Function;

/**
 * @todo Add Documentation.
 * @author barmintor
 * @date Mar 23, 2013
 */
public abstract class ServiceHelpers {

    /**
     * @todo Add Documentation.
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
     * @todo Add Documentation.
     */
    public static Long getDatastreamSize(final Node ds)
        throws RepositoryException {
        return getNodePropertySize(ds) + getContentSize(ds);
    }

    /**
     * @todo Add Documentation.
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
     * @todo Add Documentation.
     */
    public static Function<LowLevelCacheEntry, FixityResult> getCheckCacheFixityFunction(final MessageDigest digest,
                                                                                         final URI dsChecksum,
                                                                                         final long dsSize) {
        return new CheckCacheEntryFixity(digest, dsChecksum, dsSize);
    }

}
