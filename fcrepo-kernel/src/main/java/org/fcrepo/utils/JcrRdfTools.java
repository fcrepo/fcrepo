/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.utils;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static javax.jcr.PropertyType.BINARY;
import static javax.jcr.PropertyType.BOOLEAN;
import static javax.jcr.PropertyType.DATE;
import static javax.jcr.PropertyType.DECIMAL;
import static javax.jcr.PropertyType.DOUBLE;
import static javax.jcr.PropertyType.LONG;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.PropertyType.URI;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.RdfLexicon.HAS_COMPUTED_CHECKSUM;
import static org.fcrepo.RdfLexicon.HAS_COMPUTED_SIZE;
import static org.fcrepo.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.RdfLexicon.HAS_LOCATION;
import static org.fcrepo.RdfLexicon.HAS_VERSION;
import static org.fcrepo.RdfLexicon.HAS_VERSION_LABEL;
import static org.fcrepo.RdfLexicon.IS_FIXITY_RESULT_OF;
import static org.fcrepo.metrics.RegistryService.getMetrics;
import static org.fcrepo.utils.FedoraTypesUtils.getNodeTypeManager;
import static org.fcrepo.utils.FedoraTypesUtils.getPredicateForProperty;
import static org.fcrepo.utils.FedoraTypesUtils.getRepositoryCount;
import static org.fcrepo.utils.FedoraTypesUtils.getRepositorySize;
import static org.fcrepo.utils.FedoraTypesUtils.getValueFactory;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.fcrepo.RdfLexicon;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.services.functions.GetClusterConfiguration;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * A set of helpful tools for converting JCR properties to RDF
 * 
 * @author Chris Beer
 * @date May 10, 2013
 */
public abstract class JcrRdfTools {

    private static final Logger LOGGER = getLogger(JcrRdfTools.class);

    private static GetClusterConfiguration getClusterConfiguration =
            new GetClusterConfiguration();

    /**
     * A map of JCR namespaces to Fedora's RDF namespaces
     */
    public static BiMap<String, String> jcrNamespacesToRDFNamespaces =
            ImmutableBiMap.of("http://www.jcp.org/jcr/1.0",
                    "info:fedora/fedora-system:def/internal#");

    /**
     * A map of Fedora's RDF namespaces to the JCR equivalent
     */
    public static BiMap<String, String> rdfNamespacesToJcrNamespaces =
            jcrNamespacesToRDFNamespaces.inverse();

    private static LowLevelStorageService llstore;

    /**
     * Convert a Fedora RDF Namespace into its JCR equivalent
     * 
     * @param rdfNamespaceUri a namespace from an RDF document
     * @return the JCR namespace, or the RDF namespace if no matching JCR
     *         namespace is found
     */
    public static String getJcrNamespaceForRDFNamespace(
            final String rdfNamespaceUri) {
        if (rdfNamespacesToJcrNamespaces.containsKey(rdfNamespaceUri)) {
            return rdfNamespacesToJcrNamespaces.get(rdfNamespaceUri);
        } else {
            return rdfNamespaceUri;
        }
    }

    /**
     * Convert a JCR namespace into an RDF namespace fit for downstream
     * consumption.
     * 
     * @param jcrNamespaceUri a namespace from the JCR NamespaceRegistry
     * @return an RDF namespace for downstream consumption.
     */
    public static String getRDFNamespaceForJcrNamespace(
            final String jcrNamespaceUri) {
        if (jcrNamespacesToRDFNamespaces.containsKey(jcrNamespaceUri)) {
            return jcrNamespacesToRDFNamespaces.get(jcrNamespaceUri);
        } else {
            return jcrNamespaceUri;
        }
    }

    /**
     * Translate a JCR node into an RDF Resource
     * 
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
     * 
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

    /**
     * Create a default Jena Model populated with the registered JCR namespaces
     * 
     * @param session
     * @return
     * @throws RepositoryException
     */
    private static Model createDefaultJcrModel(final Session session)
        throws RepositoryException {
        final Model model = ModelFactory.createDefaultModel();

        final javax.jcr.NamespaceRegistry namespaceRegistry =
                NamespaceTools.getNamespaceRegistry(session);
        assert (namespaceRegistry != null);

        for (final String prefix : namespaceRegistry.getPrefixes()) {
            final String nsURI = namespaceRegistry.getURI(prefix);
            if (nsURI != null && !nsURI.equals("") && !prefix.equals("xmlns")) {

                if (prefix.equals("jcr")) {
                    model.setNsPrefix("fedora-internal",
                            getRDFNamespaceForJcrNamespace(nsURI));
                } else {
                    model.setNsPrefix(prefix,
                            getRDFNamespaceForJcrNamespace(nsURI));
                }
            }
        }

        return model;
    }

    /**
     * Get an RDF model of the registered JCR namespaces
     * 
     * @param session
     * @return
     * @throws RepositoryException
     */
    public static Model getJcrNamespaceModel(final Session session)
        throws RepositoryException {
        final Model model = createDefaultJcrModel(session);

        final Map<String, String> prefixMap = model.getNsPrefixMap();

        for (final Map.Entry<String, String> entry : prefixMap.entrySet()) {
            if (entry.getKey().isEmpty()) {
                continue;
            }
            model.add(ResourceFactory.createResource(entry.getValue()),
                    RdfLexicon.HAS_NAMESPACE_PREFIX, ResourceFactory
                            .createPlainLiteral(entry.getKey()));
        }

        return model;
    }

    /**
     * Get an RDF model for the given JCR NodeIterator
     * 
     * @param factory
     * @param nodeIterator
     * @param iteratorSubject
     * @return
     * @throws RepositoryException
     */
    public static Model getJcrNodeIteratorModel(final GraphSubjects factory,
            final Iterator<Node> nodeIterator, final Resource iteratorSubject)
        throws RepositoryException {

        if (!nodeIterator.hasNext()) {
            return ModelFactory.createDefaultModel();
        }

        final PeekingIterator<Node> iterator =
                Iterators.peekingIterator(nodeIterator);
        final Model model =
                createDefaultJcrModel((iterator.peek()).getSession());

        while (iterator.hasNext()) {
            final Node node = iterator.next();
            addJcrPropertiesToModel(factory, node, model);
            if (iteratorSubject != null) {
                model.add(iteratorSubject, RdfLexicon.HAS_MEMBER_OF_RESULT,
                        getGraphSubject(factory, node));
            }
        }

        return model;
    }

    /**
     * Get an RDF Model for a node that includes all its own JCR properties, as
     * well as the properties of its immediate children.
     * 
     * @param node
     * @return
     * @throws RepositoryException
     */
    public static Model getJcrPropertiesModel(final GraphSubjects factory,
            final Node node) throws RepositoryException {

        final Model model = createDefaultJcrModel(node.getSession());

        if (node.getPrimaryNodeType().getName().equals(FedoraJcrTypes.ROOT)) {
            /* a rdf description of the root node */
            LOGGER.debug("Creating RDF response for repository description");
            addRepositoryMetricsToModel(factory, node, model);
        }

        addJcrPropertiesToModel(factory, node, model);

        return model;
    }

    /**
     * Add repository metrics data to the given JCR model
     * 
     * @param factory
     * @param node
     * @param model
     * @throws RepositoryException
     */
    private static void addRepositoryMetricsToModel(
            final GraphSubjects factory, final Node node, final Model model)
        throws RepositoryException {

        final Repository repository = node.getSession().getRepository();
        /* retreive the metrics from the service */
        final SortedMap<String, Counter> counters = getMetrics().getCounters();

        final Resource subject = factory.getGraphSubject(node);
        for (final String key : repository.getDescriptorKeys()) {
            final String descriptor = repository.getDescriptor(key);
            if (descriptor != null) {
                final String uri =
                        "info:fedora/fedora-system:def/internal#repository/" +
                                key;
                model.add(subject, model.createProperty(uri), descriptor);
            }
        }

        final NodeTypeManager nodeTypeManager = getNodeTypeManager(node);

        final NodeTypeIterator nodeTypes = nodeTypeManager.getAllNodeTypes();

        while (nodeTypes.hasNext()) {
            final NodeType nodeType = nodeTypes.nextNodeType();
            model.add(subject, RdfLexicon.HAS_NODE_TYPE, nodeType.getName());
        }
        model.add(subject, RdfLexicon.HAS_OBJECT_COUNT, ResourceFactory
                .createTypedLiteral(getRepositoryCount(repository)));
        model.add(subject, RdfLexicon.HAS_OBJECT_SIZE, ResourceFactory
                .createTypedLiteral(getRepositorySize(repository)));

        /* TODO: Get and add the Storage policy to the RDF response */

        /* add the configuration information */

        /* Get the cluster configuration for the RDF response */
        final Map<String, String> config =
                getClusterConfiguration.apply(repository);

        assert (config != null);

        for (final Map.Entry<String, String> entry : config.entrySet()) {
            model.add(subject, model
                    .createProperty("info:fedora/fedora-system:def/internal#" +
                            entry.getKey()), entry.getValue());
        }

        /* and add the repository metrics to the RDF model */
        if (counters
                .containsKey("org.fcrepo.services.LowLevelStorageService.fixity-check-counter")) {
            model.add(subject, RdfLexicon.HAS_FIXITY_CHECK_COUNT,
                    ResourceFactory.createTypedLiteral(counters.get(
                            "org.fcrepo.services." + "LowLevelStorageService."
                                    + "fixity-check-counter").getCount()));
        }

        if (counters
                .containsKey("org.fcrepo.services.LowLevelStorageService.fixity-error-counter")) {
            model.add(subject, RdfLexicon.HAS_FIXITY_ERROR_COUNT,
                    ResourceFactory.createTypedLiteral(counters.get(
                            "org.fcrepo.services." + "LowLevelStorageService."
                                    + "fixity-error-counter").getCount()));
        }

        if (counters
                .containsKey("org.fcrepo.services.LowLevelStorageService.fixity-repaired-counter")) {

            model.add(subject, RdfLexicon.HAS_FIXITY_REPAIRED_COUNT,
                    ResourceFactory.createTypedLiteral(counters.get(
                            "org.fcrepo.services." + "LowLevelStorageService."
                                    + "fixity-repaired-counter").getCount()));
        }

    }

    /**
     * Add information about a jcr:content node to the model
     * 
     * @param factory
     * @param node
     * @param model
     * @throws RepositoryException
     */
    private static void addJcrContentLocationInformationToModel(
            final GraphSubjects factory, final Node node, final Model model)
        throws RepositoryException {
        final Node contentNode = node.getNode(JcrConstants.JCR_CONTENT);
        final Resource contentNodeSubject =
                factory.getGraphSubject(contentNode);

        // TODO: get this from somewhere else.

        if (llstore == null) {
            llstore = new LowLevelStorageService();
            llstore.setRepository(node.getSession().getRepository());
        }

        final Set<LowLevelCacheEntry> cacheEntries =
                llstore.getLowLevelCacheEntries(contentNode);

        for (final LowLevelCacheEntry e : cacheEntries) {
            model.add(contentNodeSubject, RdfLexicon.HAS_LOCATION, e
                    .getExternalIdentifier());
        }

    }

    /**
     * Add the properties of a Node's parent and immediate children (as well as
     * the jcr:content of children) to the given RDF model
     * 
     * @param node
     * @param offset
     * @param limit @throws RepositoryException
     */
    public static Model getJcrTreeModel(final GraphSubjects factory,
            final Node node, final long offset, final int limit)
        throws RepositoryException {

        final Model model = createDefaultJcrModel(node.getSession());

        final Resource subject = getGraphSubject(factory, node);

        // don't do this if the node is the root node.
        if (node.getDepth() != 0) {
            final Node parentNode = node.getParent();
            model.add(subject, RdfLexicon.HAS_PARENT, getGraphSubject(factory,
                    parentNode));
            addJcrPropertiesToModel(factory, parentNode, model);
        }

        final javax.jcr.NodeIterator nodeIterator = node.getNodes();

        int i = 0;
        long excludedNodeCount = 0;

        while (nodeIterator.hasNext()) {
            final Node childNode = nodeIterator.nextNode();

            // exclude jcr system nodes or jcr:content nodes
            if (FedoraTypesUtils.isInternalNode.apply(childNode) ||
                    childNode.getName().equals(JcrConstants.JCR_CONTENT)) {
                excludedNodeCount++;
            } else {
                final Resource childNodeSubject =
                        getGraphSubject(factory, childNode);

                if (i >= offset && (limit == -1 || i < (offset + limit))) {
                    addJcrPropertiesToModel(factory, childNode, model);
                }

                i++;

                model.add(subject, RdfLexicon.HAS_CHILD, childNodeSubject);
                model.add(childNodeSubject, RdfLexicon.HAS_PARENT, subject);
            }

        }

        model.add(subject, RdfLexicon.HAS_CHILD_COUNT, ResourceFactory
                .createTypedLiteral(nodeIterator.getSize() - excludedNodeCount));

        return model;
    }

    /**
     * Add all of a node's properties to the given model
     * 
     * @param node
     * @param model
     * @throws RepositoryException
     */
    private static void addJcrPropertiesToModel(final GraphSubjects factory,
            final Node node, final Model model) throws RepositoryException {

        final Resource subject = getGraphSubject(factory, node);
        final javax.jcr.PropertyIterator properties = node.getProperties();

        while (properties.hasNext()) {
            final Property property = properties.nextProperty();

            addPropertyToModel(subject, model, property);
        }

        // always include the jcr:content node information
        if (node.hasNode(JcrConstants.JCR_CONTENT)) {
            final Node contentNode = node.getNode(JcrConstants.JCR_CONTENT);
            final Resource contentSubject =
                    getGraphSubject(factory, contentNode);

            model.add(subject, RdfLexicon.HAS_CONTENT, contentSubject);
            model.add(contentSubject, RdfLexicon.IS_CONTENT_OF, subject);

            addJcrPropertiesToModel(factory, contentNode, model);
            addJcrContentLocationInformationToModel(factory, node, model);
        }
    }

    /**
     * Create a JCR value from an RDFNode, either by using the given JCR
     * PropertyType or by looking at the RDFNode Datatype
     * 
     * @param data an RDF Node (possibly with a DataType)
     * @param type a JCR PropertyType value
     * @return a JCR Value
     * @throws javax.jcr.RepositoryException
     */
    public static Value createValue(final Node node, final RDFNode data,
            final int type) throws RepositoryException {
        final ValueFactory valueFactory = getValueFactory.apply(node);
        assert (valueFactory != null);

        if (data.isURIResource() &&
                (type == REFERENCE || type == WEAKREFERENCE)) {
            // reference to another node (by path)
            return valueFactory.createValue(getNodeFromObjectPath(node, data
                    .toString()));
        } else if (data.isURIResource() || type == URI) {
            // some random opaque URI
            return valueFactory.createValue(data.toString(), PropertyType.URI);
        } else if (data.isResource()) {
            // a non-URI resource (e.g. a blank node)
            return valueFactory.createValue(data.toString(), UNDEFINED);
        } else if (data.isLiteral() && type == UNDEFINED) {
            // the JCR schema doesn't know what this should be; so introspect
            // the RDF and try to figure it out
            final Literal literal = data.asLiteral();
            final RDFDatatype dataType = literal.getDatatype();
            final Object rdfValue = literal.getValue();

            if (rdfValue instanceof Boolean) {
                return valueFactory.createValue((Boolean) rdfValue);
            } else if (rdfValue instanceof Byte ||
                    (dataType != null && dataType.getJavaClass() == Byte.class)) {
                return valueFactory.createValue(literal.getByte());
            } else if (rdfValue instanceof Double) {
                return valueFactory.createValue((Double) rdfValue);
            } else if (rdfValue instanceof Float) {
                return valueFactory.createValue((Float) rdfValue);
            } else if (rdfValue instanceof Long ||
                    (dataType != null && dataType.getJavaClass() == Long.class)) {
                return valueFactory.createValue(literal.getLong());
            } else if (rdfValue instanceof Short ||
                    (dataType != null && dataType.getJavaClass() == Short.class)) {
                return valueFactory.createValue(literal.getShort());
            } else if (rdfValue instanceof Integer) {
                return valueFactory.createValue((Integer) rdfValue);
            } else if (rdfValue instanceof XSDDateTime) {
                return valueFactory.createValue(((XSDDateTime) rdfValue)
                        .asCalendar());
            } else {
                return valueFactory.createValue(literal.getString(), STRING);
            }

        } else {
            return valueFactory.createValue(data.asLiteral().getString(), type);
        }
    }

    /**
     * Add a JCR property to the given RDF Model (with the given subject)
     * 
     * @param subject the RDF subject to use in the assertions
     * @param model the RDF graph to insert the triple into
     * @param property the JCR property (multivalued or not) to convert to
     *        triples
     * @throws RepositoryException
     */
    public static void addPropertyToModel(final Resource subject,
            final Model model, final Property property)
        throws RepositoryException {
        if (property.isMultiple()) {
            final Value[] values = property.getValues();

            for (final Value v : values) {
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
     * @param property the JCR property (multivalued or not) to convert to
     *        triples
     * @param v the actual JCR Value to insert into the graph
     * @throws RepositoryException
     */
    public static void addPropertyToModel(final Resource subject,
            final Model model, final Property property, final Value v)
        throws RepositoryException {

        if (v.getType() == BINARY) {
            // exclude binary types from property serialization
            return;
        }

        final com.hp.hpl.jena.rdf.model.Property predicate =
                getPredicateForProperty.apply(property);

        switch (v.getType()) {
            case BOOLEAN:
                model.addLiteral(subject, predicate, v.getBoolean());
                break;
            case DATE:
                model.add(subject, predicate, ResourceFactory
                        .createTypedLiteral(v.getDate()));
                break;
            case DECIMAL:
                model.add(subject, predicate, ResourceFactory
                        .createTypedLiteral(v.getDecimal()));
                break;
            case DOUBLE:
                model.addLiteral(subject, predicate, v.getDouble());
                break;
            case LONG:
                model.addLiteral(subject, predicate, v.getLong());
                break;
            case URI:
                model.add(subject, predicate, model.createResource(v
                        .getString()));
                return;
            case REFERENCE:
            case WEAKREFERENCE:
                model.add(subject, predicate, model
                        .createResource("info:fedora" +
                                property.getSession().getNodeByIdentifier(
                                        v.getString()).getPath()));
                break;
            case PATH:
                model.add(subject, predicate, model
                        .createResource("info:fedora" + v.getString()));
                break;

            default:
                model.add(subject, predicate, v.getString());

        }

    }

    /**
     * Given an RDF predicate value (namespace URI + local name), figure out
     * what JCR property to use
     * 
     * @param node the JCR node we want a property for
     * @param predicate the predicate to map to a property name
     * @return the JCR property name
     * @throws RepositoryException
     */
    public static String getPropertyNameFromPredicate(final Node node,
            final com.hp.hpl.jena.rdf.model.Property predicate)
        throws RepositoryException {

        final String prefix;

        final String namespace =
                getJcrNamespaceForRDFNamespace(predicate.getNameSpace());

        final NamespaceRegistry namespaceRegistry =
                NamespaceTools.getNamespaceRegistry(node);

        assert (namespaceRegistry != null);

        if (namespaceRegistry.isRegisteredUri(namespace)) {
            prefix = namespaceRegistry.getPrefix(namespace);
        } else {
            prefix = namespaceRegistry.registerNamespace(namespace);
        }

        final String localName = predicate.getLocalName();

        final String propertyName = prefix + ":" + localName;

        LOGGER.trace("Took RDF predicate {} and translated it to "
                + "JCR property {}", predicate, propertyName);

        return propertyName;

    }

    /**
     * Strip our silly "namespace" stuff from the object
     * 
     * @param node an existing JCR node
     * @param path the RDF URI to look up
     * @return the JCR node at the given RDF path
     * @throws RepositoryException
     */
    private static Node
    getNodeFromObjectPath(final Node node, final String path)
        throws RepositoryException {
        return node.getSession()
                .getNode(path.substring("info:fedora".length()));
    }

    /**
     * Get a Jena RDF model for the JCR version history information for a node
     * 
     * @param factory
     * @param node
     * @return
     * @throws RepositoryException
     */
    public static Model getJcrVersionsModel(final GraphSubjects factory,
            final Node node) throws RepositoryException {

        final Resource subject = getGraphSubject(factory, node);

        final Model model = createDefaultJcrModel(node.getSession());

        final VersionHistory versionHistory =
                FedoraTypesUtils.getVersionHistory(node);

        final VersionIterator versionIterator = versionHistory.getAllVersions();

        while (versionIterator.hasNext()) {
            final Version version = versionIterator.nextVersion();
            final Node frozenNode = version.getFrozenNode();
            final Resource versionSubject =
                    getGraphSubject(factory, frozenNode);

            model.add(subject, HAS_VERSION, versionSubject);

            final String[] versionLabels =
                    versionHistory.getVersionLabels(version);
            for (final String label : versionLabels) {
                model.add(versionSubject, HAS_VERSION_LABEL, label);
            }
            final javax.jcr.PropertyIterator properties =
                    frozenNode.getProperties();

            while (properties.hasNext()) {
                final Property property = properties.nextProperty();

                addPropertyToModel(versionSubject, model, property);
            }

        }

        return model;
    }

    /**
     * Serialize the JCR fixity information in a Jena Model
     * 
     * @param factory
     * @param node
     * @param blobs
     * @return
     * @throws RepositoryException
     */
    public static Model getFixityResultsModel(final GraphSubjects factory,
            final Node node, final Iterable<FixityResult> blobs)
        throws RepositoryException {

        final Model model = createDefaultJcrModel(node.getSession());

        addJcrPropertiesToModel(factory, node, model);

        for (final FixityResult result : blobs) {
            // fixity results are just blank nodes
            final Resource resultSubject = createResource();

            model.add(resultSubject, IS_FIXITY_RESULT_OF, factory
                    .getGraphSubject(node));
            model.add(factory.getGraphSubject(node), HAS_FIXITY_RESULT,
                    resultSubject);

            model.add(resultSubject, HAS_LOCATION, createResource(result
                    .getStoreIdentifier()));

            for (final FixityResult.FixityState state : result.status) {
                model.add(resultSubject, HAS_FIXITY_STATE,
                        createTypedLiteral(state.toString()));
            }

            final String checksum = result.computedChecksum.toString();
            model.add(resultSubject, HAS_COMPUTED_CHECKSUM,
                    createResource(checksum));
            model.add(resultSubject, HAS_COMPUTED_SIZE,
                    createTypedLiteral(result.computedSize));
        }
        return model;
    }

    /**
     * Set the function used to get the cluster configuration for Infinispan
     */
    public static void setGetClusterConfiguration(
            final GetClusterConfiguration newClusterConfiguration) {
        getClusterConfiguration = newClusterConfiguration;
    }

    /**
     * Set the Low-level storage server implementation
     */
    public static void setLlstore(
            final LowLevelStorageService lowLevelStorageService) {
        llstore = lowLevelStorageService;
    }
}
