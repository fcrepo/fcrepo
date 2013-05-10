package org.fcrepo;

import com.hp.hpl.jena.datatypes.RDFDatatype;
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
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import static org.fcrepo.services.ServiceHelpers.getObjectSize;
import static org.fcrepo.utils.FedoraTypesUtils.getRDFNamespaceForJcrNamespace;
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
			Date createdDate = getCreatedDate();
			node.setProperty(JCR_LASTMODIFIED, createdDate.toString());
			node.getSession().save();
			return createdDate;
		} catch(RepositoryException e) {
			logger.error("Could not set new modified date - " + e.getMessage());
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


	public Model getPropertiesModel() throws RepositoryException {

		final Resource subject = getGraphSubject();

		final Model model = ModelFactory.createDefaultModel();

		final NamespaceRegistry namespaceRegistry = getNode().getSession().getWorkspace().getNamespaceRegistry();
		for (final String prefix : namespaceRegistry.getPrefixes()) {
			final String nsURI = namespaceRegistry.getURI(prefix);
			if (nsURI != null && !nsURI.equals("") &&
						!prefix.equals("xmlns")) {

				if (prefix.equals("jcr")) {
					model.setNsPrefix("fedora-internal", getRDFNamespaceForJcrNamespace(nsURI));
				} else {
					model.setNsPrefix(prefix, getRDFNamespaceForJcrNamespace(nsURI));
				}
			}
		}

		final PropertyIterator properties = node.getProperties();

		addJcrPropertiesToModel(subject, model, properties);

		if (node.hasNode(JcrConstants.JCR_CONTENT)) {
			final Resource contentSubject = ResourceFactory.createResource("info:fedora" + node.getPath() + "/fcr:content");
			model.add(subject, model.createProperty("info:fedora/fedora-system:def/internal#hasContent"), contentSubject);
			final PropertyIterator contentProperties = node.getNode(JcrConstants.JCR_CONTENT).getProperties();
			addJcrPropertiesToModel(contentSubject, model, contentProperties);
		}

		listener = new JcrPropertyStatementListener(subject, getNode());

		model.register(listener);

		return model;
	}

	private void addJcrPropertiesToModel(Resource subject, Model model, PropertyIterator properties) throws RepositoryException {
		while (properties.hasNext()) {
			final Property property = properties.nextProperty();

			Namespaced nsProperty = (Namespaced)property;
			if (property.isMultiple()) {
				final Value[] values = property.getValues();

				for(Value v : values) {
					addValueToModel(subject, model, nsProperty, v);
				}

			} else {
				final Value value = property.getValue();
				addValueToModel(subject, model, nsProperty, value);
			}

		}
	}

	private Model addValueToModel(final Resource subject, final Model model, final Namespaced nsProperty, Value v) throws RepositoryException {
		RDFDatatype datatype = null;
		final com.hp.hpl.jena.rdf.model.Property predicate = ResourceFactory.createProperty(getRDFNamespaceForJcrNamespace(nsProperty.getNamespaceURI()), nsProperty.getLocalName());

		final String stringValue = v.getString();

		switch (v.getType()) {

			case PropertyType.BOOLEAN:
				datatype = model.createTypedLiteral(v.getBoolean()).getDatatype();
				break;
			case PropertyType.DATE:
				datatype = model.createTypedLiteral(v.getDate()).getDatatype();
				break;
			case PropertyType.DECIMAL:
				datatype = model.createTypedLiteral(v.getDecimal()).getDatatype();
				break;
			case PropertyType.DOUBLE:
				datatype = model.createTypedLiteral(v.getDouble()).getDatatype();
				break;
			case PropertyType.LONG:
				datatype = model.createTypedLiteral(v.getLong()).getDatatype();
				break;
			case PropertyType.URI:
				return model.add(subject, predicate, model.createResource(stringValue));
			case PropertyType.REFERENCE:
			case PropertyType.WEAKREFERENCE:
				return model.add(subject, predicate, getGraphSubject(node.getSession().getNodeByIdentifier(stringValue)));
			case PropertyType.PATH:
				return model.add(subject, predicate, model.createResource("info:fedora" + stringValue));

		}

		if ( datatype == null) {
			return model.add(subject, predicate, stringValue);
		} else {
			return model.add(subject, predicate, stringValue, datatype);
		}
	}

	public Problems getGraphProblems() throws RepositoryException {
		if (listener != null) {
			return listener.getProblems();
		} else {
			return null;
		}
	}


	public static Resource getGraphSubject(final Node node) throws RepositoryException {
		return ResourceFactory.createResource("info:fedora" + node.getPath());
	}

	public Resource getGraphSubject() throws RepositoryException {
		return getGraphSubject(node);
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
