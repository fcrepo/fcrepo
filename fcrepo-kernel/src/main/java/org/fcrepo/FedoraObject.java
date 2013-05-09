
package org.fcrepo;

import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.nodetype2name;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.modeshape.jcr.api.JcrConstants;
import org.slf4j.Logger;

import com.codahale.metrics.Timer;

/**
 * An abstraction that represents a Fedora Object backed by
 * a JCR node.
 * 
 * @author ajs6f
 *
 */
public class FedoraObject extends FedoraResource {

    static final Logger logger = getLogger(FedoraObject.class);

	/**
	 * Construct a FedoraObject from an existing JCR Node
	 * @param n an existing JCR node to treat as an fcrepo object
	 */
	public FedoraObject(final Node node) {
		super(node);
		mixinTypeSpecificCrap();
	}

	/**
	 * Create or find a FedoraObject at the given path
	 * @param session the JCR session to use to retrieve the object
	 * @param path the absolute path to the object
	 * @throws RepositoryException
	 */
	public FedoraObject(final Session session, final String path, final String nodeType) throws RepositoryException {
		super(session, path, nodeType);
		mixinTypeSpecificCrap();
	}
	/**
	 * Create or find a FedoraDatastream at the given path
	 * @param session the JCR session to use to retrieve the object
	 * @param path the absolute path to the object
	 * @throws RepositoryException
	 */
	public FedoraObject(final Session session, final String path) throws RepositoryException {
		this(session, path, JcrConstants.NT_FOLDER);
	}


	private void mixinTypeSpecificCrap() {
		try {
			if (node.isNew() || !hasMixin(node)) {
				logger.debug("Setting fedora:object properties on a nt:folder node {}...", node.getPath());
				node.addMixin(FEDORA_OBJECT);
			}
		} catch (RepositoryException e) {
			logger.warn("Could not decorate jcr:content with fedora:object properties: {} ", e);
		}
	}

    /**
     * @return The JCR name of the node that backs this object.
     * @throws RepositoryException
     */
    public String getName() throws RepositoryException {
        return node.getName();
    }

    /**
     * Get the mixins this object uses
     * @return a collection of mixin names
     * @throws RepositoryException
     */
    public Collection<String> getModels() throws RepositoryException {
        return map(node.getMixinNodeTypes(), nodetype2name);
    }
    
    public static boolean hasMixin(Node node) throws RepositoryException {
        NodeType[] nodeTypes = node.getMixinNodeTypes();
        if (nodeTypes == null) return false;
        for (NodeType nodeType: nodeTypes) {
            if (FEDORA_OBJECT.equals(nodeType.getName())) {
                return true;
            }
        }
        return false;
    }



}
