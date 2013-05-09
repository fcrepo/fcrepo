package org.fcrepo.services;

import com.google.common.collect.ImmutableSet;
import org.fcrepo.FedoraObject;
import org.fcrepo.FedoraResource;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.builder;
import static org.slf4j.LoggerFactory.getLogger;

public class NodeService extends RepositoryService implements FedoraJcrTypes {

	private static final Logger logger = getLogger(NodeService.class);

	public FedoraResource findOrCreateObject(final Session session, final String path) throws RepositoryException {
		return new FedoraResource(findOrCreateNode(session, path));
	}

	public FedoraResource getObject(final Session session, final String path) throws RepositoryException {
		return new FedoraResource(session.getNode(path));
	}

	/**
	 * @return A Set of object names (identifiers)
	 * @throws RepositoryException
	 */
	public Set<String> getObjectNames(final Session session, String path) throws RepositoryException {
		return getObjectNames(session, path, null);
	}

	public Set<String> getObjectNames(final Session session, String path, String mixin) throws RepositoryException {

		final Node objects = session.getNode(path);
		final ImmutableSet.Builder<String> b = builder();
		final NodeIterator i = objects.getNodes();
		while (i.hasNext()) {
			Node n = i.nextNode();
			logger.info("child of type {} is named {} at {}", n.getPrimaryNodeType(), n.getName(), n.getPath());
			if (mixin == null || n.isNodeType(mixin)) b.add(n.getName());
		}
		return b.build();

	}

	public void deleteObject(final Session session, final String path)
			throws RepositoryException {
		final Node obj = session.getNode(path);
		obj.remove();
		session.save();
	}
}
