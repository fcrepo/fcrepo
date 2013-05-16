package org.fcrepo.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static org.fcrepo.utils.FedoraJcrTypes.FCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.services.LowLevelStorageService;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.slf4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

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
     * Translate a JCR node into an RDF Resource
     * @param node
     * @return an RDF URI resource
     * @throws RepositoryException
     */
    public static Resource getGraphSubject(final GraphSubjects factory,
    		final Node node) throws RepositoryException {
        return factory.getGraphSubject(node);
    }
    
    /**
     * Translate an RDF resource into a JCR node
     * @param session
     * @param subject an RDF URI resource
     * @return a JCR node, or null if one couldn't be found
     * @throws RepositoryException
     */
    public static Node getNodeFromGraphSubject(final GraphSubjects factory,
    		final Session session, final Resource subject)
    		throws RepositoryException {
        return factory.getNodeFromGraphSubject(session, subject);
    }
    
    /**
     * Predicate for determining whether this {@link Node} is a Fedora object.
     */
    public static boolean isFedoraGraphSubject(final GraphSubjects factory,
    		final Resource subject) {
        return factory.isFedoraGraphSubject(subject);
    }

    private static Model createDefaultJcrModel(final Session session) throws RepositoryException {
        final Model model = ModelFactory.createDefaultModel();

        final javax.jcr.NamespaceRegistry namespaceRegistry = NamespaceTools.getNamespaceRegistry(session);
        assert(namespaceRegistry != null);

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

        return model;
    }

    /**
     * Get an RDF Model for a node that includes all its own JCR properties, as well as the properties of its
     * immediate children.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public static Model getJcrPropertiesModel(
    		final GraphSubjects factory, final Node node)
    		throws RepositoryException {

        final Model model = createDefaultJcrModel(node.getSession());

        addJcrPropertiesToModel(factory, node, model);

        addJcrTreePropertiesToModel(factory, node, model);

        if (node.hasNode(JcrConstants.JCR_CONTENT)) {
            addJcrContentLocationInformationToModel(factory, node, model);
        }

        return model;
    }

    private static void addJcrContentLocationInformationToModel(
    		final GraphSubjects factory, final Node node,
    		final Model model) throws RepositoryException {
        final Node contentNode = node.getNode(JcrConstants.JCR_CONTENT);
        final Resource contentNodeSubject = factory.getGraphSubject(contentNode);

        // TODO: get this from somewhere else.
        LowLevelStorageService llstore = new LowLevelStorageService();
        llstore.setRepository(node.getSession().getRepository());

        final Set<LowLevelCacheEntry> cacheEntries = llstore.getLowLevelCacheEntries(node);

        for (LowLevelCacheEntry e : cacheEntries) {
            model.add(contentNodeSubject, model.createProperty("info:fedora/fedora-system:def/internal#hasLocation"), e.getExternalIdentifier());
        }


    }

    /**
     * Add the properties of a Node's parent and immediate children (as well as the jcr:content of children) to the given
     * RDF model
     *
     * @param node
     * @param model
     * @throws RepositoryException
     */
    private static void addJcrTreePropertiesToModel(
    		final GraphSubjects factory, final Node node, final Model model)
    		throws RepositoryException {
        final Resource subject = getGraphSubject(factory, node);

        // don't do this if the node is the root node.
        if (node.getDepth() != 0) {
            final Node parentNode = node.getParent();
            model.add(subject,
        		model.createProperty("info:fedora/fedora-system:def/internal#hasParent"),
         		getGraphSubject(factory, parentNode));
            addJcrPropertiesToModel(factory, parentNode, model);
        }

        final javax.jcr.NodeIterator nodeIterator = node.getNodes();

        long excludedNodes = 0L;
        while (nodeIterator.hasNext()) {
            final Node childNode = nodeIterator.nextNode();

            if (FedoraTypesUtils.isInternalNode.apply(childNode)) {
                continue;
            }

            final Resource childNodeSubject = getGraphSubject(factory, childNode);

            addJcrPropertiesToModel(factory, childNode, model);

            if (childNode.getName().equals(JcrConstants.JCR_CONTENT)) {
                model.add(subject, model.createProperty("info:fedora/fedora-system:def/internal#hasContent"), childNodeSubject);
                model.add(childNodeSubject, model.createProperty("info:fedora/fedora-system:def/internal#isContentOf"), subject);
                excludedNodes += 1;
            } else {
                model.add(subject, model.createProperty("info:fedora/fedora-system:def/internal#hasChild"), childNodeSubject);
                model.add(childNodeSubject, model.createProperty("info:fedora/fedora-system:def/internal#hasParent"), subject);
            }

            // always include the jcr:content node information
            if (childNode.hasNode(JcrConstants.JCR_CONTENT)) {
                addJcrPropertiesToModel(factory, childNode.getNode(JcrConstants.JCR_CONTENT), model);
            }

        }

        model.add(subject, model.createProperty("info:fedora/fedora-system:def/internal#numberOfChildren"), ResourceFactory.createTypedLiteral(nodeIterator.getSize() - excludedNodes));
    }

    /**
     * Add all of a node's properties to the given model
     *
     * @param node
     * @param model
     * @throws RepositoryException
     */
    private static void addJcrPropertiesToModel(
    		final GraphSubjects factory, final Node node,  Model model)
    		throws RepositoryException {

        final Resource subject = getGraphSubject(factory, node);
        final javax.jcr.PropertyIterator properties = node.getProperties();

        while (properties.hasNext()) {
            final Property property = properties.nextProperty();

            addPropertyToModel(subject, model, property);
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

        if (v.getType() == PropertyType.BINARY) {
            // exclude binary types from property serialization
            return;
        }

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

        final NamespaceRegistry namespaceRegistry = NamespaceTools.getNamespaceRegistry(node);

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
    
    public static Model getJcrVersionsModel(
    		final GraphSubjects factory, final Node node)
    		throws RepositoryException {

        final Resource subject = getGraphSubject(factory, node);

        final Model model = createDefaultJcrModel(node.getSession());

        final VersionHistory versionHistory = FedoraTypesUtils.getVersionHistory(node);

        final VersionIterator versionIterator = versionHistory.getAllVersions();

        while (versionIterator.hasNext()) {
            final Version version = versionIterator.nextVersion();
            final Node frozenNode = version.getFrozenNode();
            final Resource versionSubject = getGraphSubject(factory,frozenNode);

            model.add(subject, model.createProperty("info:fedora/fedora-system:def/internal#hasVersion"), versionSubject);

            final String[] versionLabels = versionHistory.getVersionLabels(version);
            for (String label : versionLabels ) {
                model.add(versionSubject, model.createProperty("info:fedora/fedora-system:def/internal#hasVersionLabel"), label);
            }
            final javax.jcr.PropertyIterator properties = frozenNode.getProperties();

            while (properties.hasNext()) {
                final Property property = properties.nextProperty();

                addPropertyToModel(versionSubject, model, property);
            }

        }

        return model;


    }
}