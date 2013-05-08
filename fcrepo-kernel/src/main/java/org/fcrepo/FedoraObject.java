
package org.fcrepo;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkArgument;
import static com.codahale.metrics.MetricRegistry.name;
import static org.fcrepo.services.RepositoryService.metrics;
import static org.fcrepo.services.ServiceHelpers.getObjectSize;
import static org.fcrepo.utils.FedoraTypesUtils.isOwned;
import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.nodetype2name;
import static org.fcrepo.utils.FedoraTypesUtils.value2string;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Calendar;
import java.util.Collection;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateAction;
import org.apache.commons.io.IOUtils;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.JcrPropertyStatementListener;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.Namespaced;
import org.slf4j.Logger;

import com.codahale.metrics.Timer;

/**
 * An abstraction that represents a Fedora Object backed by
 * a JCR node.
 * 
 * @author ajs6f
 *
 */
public class FedoraObject extends JcrTools implements FedoraJcrTypes {

    static final Logger logger = getLogger(FedoraObject.class);

    /**
     * Timer for the time to create/initialize a FedoraObject
     */
    static final Timer timer = metrics.timer(name(FedoraObject.class,
            "FedoraObject"));

    private Node node;

	private JcrPropertyStatementListener listener;

    /**
     * Construct a FedoraObject from an existing JCR Node
     * @param n an existing JCR node to treat as an fcrepo object
     */
    public FedoraObject(final Node n) {
        node = n;
        try {
            if (node.isNew() || !hasMixin(node)) {
                logger.debug("Setting fedora:object properties on a nt:folder node {}...", node.getPath());
                node.addMixin(FEDORA_OBJECT);
                node.addMixin(FEDORA_OWNED);
                node.setProperty(FEDORA_OWNERID, "System");
                node.setProperty(JCR_LASTMODIFIED, Calendar.getInstance());
                node.setProperty(DC_IDENTIFIER, new String[] {
                        node.getIdentifier(), node.getName()});
            }
        } catch(RepositoryException e) {
            try {
                logger.error("Could not add fedora:object mixin properties on {}", node.getPath());
            } catch (RepositoryException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Create or find a FedoraObject at the given path
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     * @throws RepositoryException
     */
    public FedoraObject(final Session session, final String path)
            throws RepositoryException {

        checkArgument(session != null, "null cannot create a Fedora object!");
        checkArgument(path != null, "Cannot create a Fedora object at null!");

        final Timer.Context context = timer.time();

        try {
            node = findOrCreateNode(session, path, NT_FOLDER);
            if (node.isNew() || !hasMixin(node)) {
                logger.debug("Setting fedora:object properties on a nt:folder node {}...", node.getPath());
                node.addMixin(FEDORA_OBJECT);
                node.addMixin(FEDORA_OWNED);
                node.setProperty(FEDORA_OWNERID, session.getUserID());
                node.setProperty(JCR_LASTMODIFIED, Calendar.getInstance());
                node.setProperty(DC_IDENTIFIER, new String[] {
                        node.getIdentifier(), node.getName()});
            }
        } finally {
            context.stop();
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
     * @return The JCR node that backs this object.
     */
    public Node getNode() {
        return node;
    }

    /**
     * Get the purported owner of this object
     * @return the owner id
     * @throws RepositoryException
     */
    public String getOwnerId() throws RepositoryException {
        if (isOwned.apply(node)) {
            return node.getProperty(FEDORA_OWNERID).getString();
        } else {
            return null;
        }
    }

    /**
     * Set the owner id for the object
     * @param ownerId
     * @throws RepositoryException
     */
    public void setOwnerId(final String ownerId) throws RepositoryException {
        if (isOwned.apply(node)) {
            node.setProperty(FEDORA_OWNERID, ownerId);
        } else {
            node.addMixin(FEDORA_OWNED);
            node.setProperty(FEDORA_OWNERID, ownerId);
        }
    }

    /**
     * Get the "label" (read: administrative title) of the object
     * @return object label
     * @throws RepositoryException
     */
    public String getLabel() throws RepositoryException {
        if (node.hasProperty(DC_TITLE)) {
            final Property dcTitle = node.getProperty(DC_TITLE);
            if (!dcTitle.isMultiple()) {
                return dcTitle.getString();
            } else {
                return on('/').join(map(dcTitle.getValues(), value2string));
            }
        }
        return null;
    }

    /**
     * Set the "label" (read: administrative title) for the object
     * @param label
     * @throws RepositoryException
     */
    public void setLabel(final String label) throws RepositoryException {
        node.setProperty(DC_TITLE, label);
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
