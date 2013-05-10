package org.fcrepo.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;


import static org.slf4j.LoggerFactory.getLogger;

public abstract class JcrRdfTools {

    private static final Logger logger = getLogger(JcrRdfTools.class);


    public static BiMap<String, String> jcrNamespacesToRDFNamespaces = ImmutableBiMap.of(
                                                                                                "http://www.jcp.org/jcr/1.0", "info:fedora/fedora-system:def/internal#"
    );

    public static BiMap<String, String> rdfNamespacesToJcrNamespaces = jcrNamespacesToRDFNamespaces.inverse();

    /**
     * Convert a Fedora RDF Namespace into its JCR equivalent
     * @param rdfNamespaceUri a namespace from an RDF document
     * @return the JCR namespace, or the RDF namespace if no matching JCR namespace is found
     */
    public static String getJcrNamespaceForRDFNamespace(final String rdfNamespaceUri) {
        if (rdfNamespacesToJcrNamespaces.containsKey(rdfNamespaceUri)) {
            return rdfNamespacesToJcrNamespaces.get(rdfNamespaceUri);
        } else {
            return rdfNamespaceUri;
        }
    }

    /**
     * Convert a JCR namespace into an RDF namespace fit for downstream consumption
     * @param jcrNamespaceUri a namespace from the JCR NamespaceRegistry
     * @return an RDF namespace for downstream consumption.
     */
    public static String getRDFNamespaceForJcrNamespace(final String jcrNamespaceUri) {
        if (jcrNamespacesToRDFNamespaces.containsKey(jcrNamespaceUri)) {
            return jcrNamespacesToRDFNamespaces.get(jcrNamespaceUri);
        } else {
            return jcrNamespaceUri;
        }
    }

    /**
     * Create a JCR value from an RDFNode, either by using the given JCR PropertyType or
     * by looking at the RDFNode Datatype
     *
     * @param data an RDF Node (possibly with a DataType)
     * @param type a JCR PropertyType value
     *
     * @return a JCR Value
     *
     * @throws javax.jcr.RepositoryException
     */
    public static Value createValue(final Node node, final RDFNode data, final int type) throws RepositoryException {
        final ValueFactory valueFactory = FedoraTypesUtils.getValueFactory.apply(node);
        assert(valueFactory !=  null);

        if(data.isURIResource() && (type == PropertyType.REFERENCE || type == PropertyType.WEAKREFERENCE)) {
            // reference to another node (by path)
            return valueFactory.createValue(getNodeFromObjectPath(node, data.toString()));
        } else if (data.isURIResource() || type == PropertyType.URI) {
            // some random opaque URI
            return valueFactory.createValue(data.toString(), PropertyType.URI);
        } else if (data.isResource()) {
            // a non-URI resource (e.g. a blank node)
            return valueFactory.createValue(data.toString(), PropertyType.UNDEFINED);
        } else if (data.isLiteral() && type == PropertyType.UNDEFINED) {
            // the JCR schema doesn't know what this should be; so introspect the RDF and try to figure it out
            final Object rdfValue = data.asLiteral().getValue();

            if (rdfValue instanceof Boolean) {
                return valueFactory.createValue((Boolean) rdfValue);
            } else if (rdfValue instanceof Byte) {
                return valueFactory.createValue((Byte) rdfValue);
            } else if (rdfValue instanceof Double) {
                return valueFactory.createValue((Double) rdfValue);
            } else if (rdfValue instanceof Float) {
                return valueFactory.createValue((Float) rdfValue);
            } else if (rdfValue instanceof Integer) {
                return valueFactory.createValue((Integer) rdfValue);
            } else if (rdfValue instanceof Long) {
                return valueFactory.createValue((Long) rdfValue);
            } else if (rdfValue instanceof Short) {
                return valueFactory.createValue((Short) rdfValue);
            } else if (rdfValue instanceof XSDDateTime) {
                return valueFactory.createValue(((XSDDateTime)rdfValue).asCalendar());
            } else {
                return valueFactory.createValue(data.asLiteral().getString(), PropertyType.STRING);
            }

        } else {
            return valueFactory.createValue(data.asLiteral().getString(), type);
        }
    }

    /**
     * Add a JCR property to the given RDF Model (with the given subject)
     * @param subject the RDF subject to use in the assertions
     * @param model the RDF graph to insert the triple into
     * @param property the JCR property (multivalued or not) to convert to triples
     *
     * @throws RepositoryException
     */
    public static void addPropertyToModel(final Resource subject, final Model model, final Property property) throws RepositoryException {
        if (property.isMultiple()) {
            final Value[] values = property.getValues();

            for(Value v : values) {
                addPropertyToModel(subject, model, property, v);
            }

        } else {
            addPropertyToModel(subject, model, property, property.getValue());
        }
    }


    /**
     * Add a JCR property to the given RDF Model (with the given subject)
     *
     * @param subject the RDF subject to use in the assertions
     * @param model the RDF graph to insert the triple into
     * @param property the JCR property (multivalued or not) to convert to triples
     * @param v the actual JCR Value to insert into the graph
     * @throws RepositoryException
     */
    public static void addPropertyToModel(final Resource subject, final Model model, final Property property, Value v) throws RepositoryException {

        final com.hp.hpl.jena.rdf.model.Property predicate = FedoraTypesUtils.getPredicateForProperty.apply(property);

        final String stringValue = v.getString();

        RDFDatatype datatype = null;

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
                model.add(subject, predicate, model.createResource(stringValue));
                return;
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                model.add(subject, predicate, model.createResource("info:fedora" + property.getSession().getNodeByIdentifier(stringValue).getPath()));
                return;
            case PropertyType.PATH:
                model.add(subject, predicate, model.createResource("info:fedora" + stringValue));
                return;

        }

        if ( datatype == null) {
            model.add(subject, predicate, stringValue);
        } else {
            model.add(subject, predicate, stringValue, datatype);
        }
    }

    /**
     * Given an RDF predicate value (namespace URI + local name), figure out what JCR property to use
     * @param node the JCR node we want a property for
     * @param predicate the predicate to map to a property name
     *
     * @return the JCR property name
     * @throws RepositoryException
     */
    public static String getPropertyNameFromPredicate(final Node node, final com.hp.hpl.jena.rdf.model.Property predicate) throws RepositoryException {

        final String prefix;

        final String namespace = getJcrNamespaceForRDFNamespace(predicate.getNameSpace());

        final NamespaceRegistry namespaceRegistry = FedoraTypesUtils.getNamespaceRegistry.apply(node);

        assert(namespaceRegistry != null);

        if (namespaceRegistry.isRegisteredUri(namespace)) {
            prefix = namespaceRegistry.getPrefix(namespace);
        } else {
            prefix = namespaceRegistry.registerNamespace(namespace);
        }

        final String localName = predicate.getLocalName();

        final String propertyName = prefix + ":" + localName;

        logger.trace("Took RDF predicate {} and translated it to JCR property {}", predicate, propertyName);

        return propertyName;

    }

    /**
     * Strip our silly "namespace" stuff from the object
     * @param node an existing JCR node
     * @param path the RDF URI to look up
     * @return the JCR node at the given RDF path
     * @throws RepositoryException
     */
    private static Node getNodeFromObjectPath(final Node node, final String path) throws RepositoryException {
        return node.getSession().getNode(path.substring("info:fedora".length()));
    }
}