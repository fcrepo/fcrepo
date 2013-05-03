package org.fcrepo.utils;

import com.hp.hpl.jena.rdf.listeners.StatementListener;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class JcrPropertyStatementListener extends StatementListener {

	private static final Logger logger = getLogger(JcrPropertyStatementListener.class);

	private final Node node;
	private final Resource subject;

	public JcrPropertyStatementListener(final Resource subject, final Node node) {
		this.node = node;
		this.subject = subject;
	}

	/**
	 * When a statement is added to the graph, serialize it to a JCR property
	 * @param s
	 */
	@Override
	public void addedStatement( Statement s ) {
		logger.trace(">> added statement " + s);

		try {
			// if it's not about our node, ignore it.
			if(!s.getSubject().equals(subject)) {
				return;
			}

			// extract the JCR propertyName from the predicate
			final String propertyName = getPropertyNameFromPredicate(s.getPredicate());

			// if it already exists, we can take some shortcuts
			if (node.hasProperty(propertyName)) {

				final Property property = node.getProperty(propertyName);

				final Value newValue = createValue(s.getObject(), property.getType());

				if (property.isMultiple()) {

					// if the property is multi-valued, go ahead and append to it.
					ArrayList<Value> newValues = new ArrayList<Value>();
					Collections.addAll(newValues, node.getProperty(propertyName).getValues());
					newValues.add(newValue);

					property.setValue((Value[]) newValues.toArray(new Value[0]));
				} else {
					// or we'll just overwrite it
					property.setValue(newValue);
				}
			} else {
				// the property isn't already set.. so we need to gather some information about the property

				final int type;
				final boolean isMultiple;

				final PropertyDefinition definition = getDefinitionForPropertyName(propertyName);

				if ( definition == null) {
					// couldn't find a property definition..
					// probably not going to go well for us..
					// but when has that stopped us before?
					type = PropertyType.UNDEFINED;
					isMultiple = true;
				} else {
					type = definition.getRequiredType();
					isMultiple = definition.isMultiple();
				}

				final Value value = createValue(s.getObject(), type);

				if (isMultiple) {
					node.setProperty(propertyName, new Value[]{value});
				} else {
					node.setProperty(propertyName, value);
				}
			}
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * When a statement is removed, remove it from the JCR properties
	 * @param s
	 */
	@Override
	public void removedStatement( Statement s ) {
		logger.trace(">> removed statement " + s);

		try {
			// if it's not about our node, we don't care.
			if(!s.getSubject().equals(subject)) {
				return;
			}

			final String propertyName = getPropertyNameFromPredicate(s.getPredicate());

			// if the property doesn't exist, we don't need to worry about it.
			if (node.hasProperty(propertyName)) {

				final Property property = node.getProperty(propertyName);

				if (property.isMultiple()) {

					ArrayList<Value> newValues = new ArrayList<Value>();

					final Value valueToRemove = createValue(s.getObject(), property.getType());

					Collections.addAll(newValues, node.getProperty(propertyName).getValues());
					final boolean remove = newValues.remove(valueToRemove);

					// we only need to update the property if we did anything.
					if (remove) {
						if (newValues.size() == 0) {
							property.setValue((Value[])null);
						} else {
							property.setValue((Value[]) newValues.toArray(new Value[0]));
						}
					}

				} else {
					property.setValue((Value)null);
				}
			}
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}

	}


	/**
	 * Create a JCR value from our object data. Presumably we could infer type information from the RDFNode?
	 *
	 * @param data
	 * @param type
	 * @return
	 * @throws RepositoryException
	 */
	private Value createValue(RDFNode data, int type) throws RepositoryException {
        // if JCR didn't tell us anything about the data type..
        if (data.isURIResource() && type == PropertyType.REFERENCE) {
			return createValue(data.toString(), type);
		} else if (data.isURIResource()) {
			return createValue(data.toString(), PropertyType.URI);
		} else if (data.isResource()) {
			return createValue(data.toString(), PropertyType.UNDEFINED);
		} else if (data.isLiteral() && type == PropertyType.UNDEFINED) {
			final ValueFactory valueFactory = node.getSession().getValueFactory();
			final Object rdfValue = data.asLiteral().getValue();

			if (rdfValue instanceof Boolean ) {
				return valueFactory.createValue((Boolean)rdfValue);
			} else if (rdfValue instanceof Byte ) {
				return valueFactory.createValue((Byte)rdfValue);
			} else if (rdfValue instanceof Double ) {
				return valueFactory.createValue((Double)rdfValue);
			} else if (rdfValue instanceof Float ) {
				return valueFactory.createValue((Float)rdfValue);
			} else if (rdfValue instanceof Integer ) {
				return valueFactory.createValue((Integer)rdfValue);
			} else if (rdfValue instanceof Long ) {
				return valueFactory.createValue((Long)rdfValue);
			} else if (rdfValue instanceof Short ) {
				return valueFactory.createValue((Short)rdfValue);
			}

			return valueFactory.createValue(data.toString(), PropertyType.STRING);
		} else {
			return createValue(data.toString(), type);
		}
	}

	/**
	 * Create a JCR Value from a String as the appropriate type. Do some special value lookups if we have a REFERENCE
	 * @param data
	 * @param type
	 * @return
	 * @throws RepositoryException
	 */
	private Value createValue(String data, int type) throws RepositoryException {

		logger.trace("Creating value {} as a {}", data, PropertyType.nameFromValue(type));

		final Value value;
		if (type == PropertyType.REFERENCE) {
			value = node.getSession().getValueFactory().createValue(getNodeFromObjectPath(data));
		} else {
			value = node.getSession().getValueFactory().createValue(data, type);
		}

		return value;
	}


	/**
	 * Get the property definition information (containing type and multi-value information)
	 * @param propertyName
	 * @return
	 * @throws RepositoryException
	 */
	private PropertyDefinition getDefinitionForPropertyName(String propertyName) throws RepositoryException {
		final PropertyDefinition[] propertyDefinitions = node.getSession().getWorkspace().getNodeTypeManager().getNodeType("fedora:resource").getPropertyDefinitions();

		for (PropertyDefinition p : propertyDefinitions) {
			if (p.getName().equals(propertyName)) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Strip our silly "namespace" stuff from the object
	 * @param s
	 * @return
	 * @throws RepositoryException
	 */
	private Node getNodeFromObjectPath(String s) throws RepositoryException {
		return node.getSession().getNode(s.substring("info:fedora".length()));
	}

	/**
	 * Given an RDF predicate value (namespace URI + local name), figure out what JCR property to use
	 * @param predicate
	 * @return
	 * @throws RepositoryException
	 */
	private String getPropertyNameFromPredicate(com.hp.hpl.jena.rdf.model.Property predicate) throws RepositoryException {

		final String prefix;

		final NamespaceRegistry namespaceRegistry = getNamespaceRegistry();

		if (namespaceRegistry.isRegisteredUri(predicate.getNameSpace())) {
			prefix = namespaceRegistry.getPrefix(predicate.getNameSpace());
		} else {
			prefix = namespaceRegistry.registerNamespace(predicate.getNameSpace());
        }

		final String localName = predicate.getLocalName();

		final String propertyName = prefix + ":" + localName;

		logger.trace("Took RDF predicate {} and translated it to JCR property {}", predicate, propertyName);

		return propertyName;

	}

	/**
	 * We need the Modeshape NamespaceRegistry, because it allows us to register anonymous namespaces.
	 * @return
	 * @throws RepositoryException
	 */
	private NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
		return (org.modeshape.jcr.api.NamespaceRegistry)node.getSession().getWorkspace().getNamespaceRegistry();
	}



}
