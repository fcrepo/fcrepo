package org.fcrepo;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateAction;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.JcrPropertyStatementListener;
import org.fcrepo.utils.JcrRdfTools;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionHistory;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import static org.fcrepo.services.ServiceHelpers.getObjectSize;
import static org.fcrepo.utils.FedoraTypesUtils.getBaseVersion;
import static org.fcrepo.utils.FedoraTypesUtils.getVersionHistory;
import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.nodetype2name;
import static org.slf4j.LoggerFactory.getLogger;

public class FedoraResource extends JcrTools implements FedoraJcrTypes {

	static final Logger logger = getLogger(FedoraObject.class);

	protected Node node;

	private JcrPropertyStatementListener listener;

	public FedoraResource() {
		node = null;
	}

	/**
	 * Construct a FedoraObject from an existing JCR Node
	 * @param node an existing JCR node to treat as an fcrepo object
	 */
	public FedoraResource(final Node node) {
		super(false);
		this.node = node;
	}

	/**
	 * Create or find a FedoraObject at the given path
	 * @param session the JCR session to use to retrieve the object
	 * @param path the absolute path to the object
	 * @throws RepositoryException
	 */
	public FedoraResource(final Session session, final String path, final String nodeType) throws RepositoryException {
		super(false);
		this.node = findOrCreateNode(session, path, nodeType);

		if (node.isNew() || !hasMixin(node)) {
			node.addMixin(FEDORA_RESOURCE);

			if (node.getSession() != null) {
				node.setProperty(JCR_CREATEDBY, node.getSession().getUserID());
			}

			node.setProperty(JCR_LASTMODIFIED, Calendar.getInstance());
		}

	}

	public static boolean hasMixin(Node node) throws RepositoryException {
		NodeType[] nodeTypes = node.getMixinNodeTypes();
		if (nodeTypes == null) return false;
		for (NodeType nodeType: nodeTypes) {
			if (FEDORA_RESOURCE.equals(nodeType.getName())) {
				return true;
			}
		}
		return false;
	}

	public boolean hasContent() throws RepositoryException {
		return node.hasNode(JcrConstants.JCR_CONTENT);
	}

	/**
	 * @return The JCR node that backs this object.
	 */
	public Node getNode() {
		return node;
	}


	/**
	 * Get the date this datastream was created
	 * @return
	 * @throws RepositoryException
	 */
	public Date getCreatedDate() throws RepositoryException {
		return new Date(node.getProperty(JCR_CREATED).getDate()
								.getTimeInMillis());
	}


	/**
	 * Get the date this datastream was last modified
	 * @return
	 * @throws RepositoryException
	 */
	public Date getLastModifiedDate() {
		//TODO no modified date stored
		//attempt to set as created date?
		try {
			return new Date(node.getProperty(JCR_LASTMODIFIED).getDate()
									.getTimeInMillis());
		} catch (RepositoryException e) {
			logger.error("Could not get last modified date");
		}
		logger.debug("Setting modified date");
		try {
			return getCreatedDate();
		} catch(RepositoryException e) {
			logger.error("Could not fall back to created date - {}", e.getMessage());
		}
		return null;
	}

	/**
	 * Get the total size of this object and its datastreams
	 * @return size in bytes
	 * @throws RepositoryException
	 */
	public long getSize() throws RepositoryException {
		return getObjectSize(node);
	}

	/**
	 * Get the mixins this object uses
	 * @return a collection of mixin names
	 * @throws javax.jcr.RepositoryException
	 */
	public Collection<String> getModels() throws RepositoryException {
		return map(node.getMixinNodeTypes(), nodetype2name);
	}

	public Problems getGraphProblems() throws RepositoryException {
		if (listener != null) {
			return listener.getProblems();
		} else {
			return null;
		}
	}

	public GraphStore updateGraph(String sparqlUpdateStatement) throws RepositoryException {
		final GraphStore store = getGraphStore();
		UpdateAction.parseExecute(sparqlUpdateStatement, store);

		return store;
	}

	public GraphStore getGraphStore() throws RepositoryException {

        final Model model = JcrRdfTools.getJcrPropertiesModel(node);

        listener = new JcrPropertyStatementListener(node.getSession());

        model.register(listener);

		GraphStore graphStore = GraphStoreFactory.create(model);

		return graphStore;
	}

    public GraphStore getVersionGraphStore() throws RepositoryException {
        final Model model = JcrRdfTools.getJcrVersionsModel(node);

        GraphStore graphStore = GraphStoreFactory.create(model);

        return graphStore;
    }

    public void addVersionLabel(final String label) throws RepositoryException {
        final VersionHistory versionHistory = getVersionHistory(node);
        versionHistory.addVersionLabel(getBaseVersion(node).getName(), label, true);
    }
}
