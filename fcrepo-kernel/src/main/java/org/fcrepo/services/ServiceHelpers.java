package org.fcrepo.services;

import static com.google.common.collect.ImmutableSet.copyOf;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public class ServiceHelpers {
	
    public static Long getNodePropertySize(Node node)
            throws RepositoryException {
        Long size = 0L;
        PropertyIterator i = node.getProperties();
        while (i.hasNext()) {
            Property p = i.nextProperty();
            if (p.isMultiple()) {
                for (Value v : copyOf(p.getValues())) {
                    size = size + v.getBinary().getSize();
                }
            } else {
                size = size + p.getBinary().getSize();
            }
        }
        return size;
    }

	/**
	 * @param obj
	 * @return object size in bytes
	 * @throws RepositoryException
	 */
	public static Long getObjectSize(Node obj) throws RepositoryException {
		return getNodePropertySize(obj) + getObjectDSSize(obj);
	}

	/**
	 * @param obj
	 * @return object's datastreams' total size in bytes
	 * @throws RepositoryException
	 */
	private static Long getObjectDSSize(Node obj) throws RepositoryException {
		Long size = 0L;
		NodeIterator i = obj.getNodes();
		while (i.hasNext()) {
			Node ds = i.nextNode();
			size += getDatastreamSize(ds);
		}
		return size;
	}

	public static Long getDatastreamSize(Node ds) throws ValueFormatException,
	PathNotFoundException, RepositoryException {
		return getNodePropertySize(ds) + getContentSize(ds);
	}

	public static Long getContentSize(Node ds) throws ValueFormatException,
	PathNotFoundException, RepositoryException {
		return ds.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary()
				.getSize();
	}

}
