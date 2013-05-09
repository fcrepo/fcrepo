package org.fcrepo;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateAction;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.JcrPropertyStatementListener;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.Namespaced;
import org.slf4j.Logger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.util.Collection;

import static org.fcrepo.services.ServiceHelpers.getObjectSize;
import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.nodetype2name;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
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
	 * @param n an existing JCR node to treat as an fcrepo object
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
	 * Get the date this object was created
	 * @return
	 * @throws RepositoryException
	 */
	public String getCreated() throws RepositoryException {
		return node.getProperty(JCR_CREATED).getString();
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
	 * Get the date this object was last modified (whatever that means)
	 * @return
	 * @throws RepositoryException
	 */
	public String getLastModified() throws RepositoryException {
		if (node.hasProperty(JCR_LASTMODIFIED)) {
			return node.getProperty(JCR_LASTMODIFIED).getString();
		} else {
			logger.warn("{} was loaded as a Fedora object, but does not have {} defined.", node.getName(), JCR_LASTMODIFIED);
			return null;
		}
	}

	/**
	 * Get the mixins this object uses
	 * @return a collection of mixin names
	 * @throws javax.jcr.RepositoryException
	 */
	public Collection<String> getModels() throws RepositoryException {
		return map(node.getMixinNodeTypes(), nodetype2name);
	}


	public Model getPropertiesModel() throws RepositoryException {

		final Resource subject = getGraphSubject();

		final Model model = ModelFactory.createDefaultModel();

		final NamespaceRegistry namespaceRegistry = getNode().getSession().getWorkspace().getNamespaceRegistry();
		for (final String prefix : namespaceRegistry.getPrefixes()) {
			final String nsURI = namespaceRegistry.getURI(prefix);
			if (nsURI != null && !nsURI.equals("") &&
						!prefix.equals("xmlns")) {
				model.setNsPrefix(prefix, nsURI);
			}
		}

		final PropertyIterator properties = node.getProperties();

		while (properties.hasNext()) {
			final Property property = properties.nextProperty();

			Namespaced nsProperty = (Namespaced)property;
			if (property.isMultiple()) {
				final Value[] values = property.getValues();

				for(Value v : values) {
					model.add(subject, ResourceFactory.createProperty(nsProperty.getNamespaceURI(), nsProperty.getLocalName()), v.getString());
				}

			} else {
				final Value value = property.getValue();
				model.add(subject, ResourceFactory.createProperty(nsProperty.getNamespaceURI(), nsProperty.getLocalName()), value.getString());
			}

		}

		listener = new JcrPropertyStatementListener(subject, getNode());

		model.register(listener);

		return model;
	}

	public Problems getGraphProblems() throws RepositoryException {
		if (listener != null) {
			return listener.getProblems();
		} else {
			return null;
		}
	}

	public Resource getGraphSubject() throws RepositoryException {
		return ResourceFactory.createResource("info:fedora" + node.getPath());
	}

	public GraphStore updateGraph(String sparqlUpdateStatement) throws RepositoryException {
		final GraphStore store = getGraphStore();
		UpdateAction.parseExecute(sparqlUpdateStatement, store);

		return store;
	}

	public GraphStore getGraphStore() throws RepositoryException {
		GraphStore graphStore = GraphStoreFactory.create(getPropertiesModel());

		return graphStore;
	}

	public void setGraphStore() {

	}

}
