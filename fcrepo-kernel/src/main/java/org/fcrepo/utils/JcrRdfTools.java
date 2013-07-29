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
import static org.fcrepo.RdfLexicon.PAGE;
import static org.fcrepo.RdfLexicon.PAGE_OF;
import static org.fcrepo.metrics.RegistryService.getMetrics;
import static org.fcrepo.utils.FedoraTypesUtils.getNodeTypeManager;
import static org.fcrepo.utils.FedoraTypesUtils.getPredicateForProperty;
import static org.fcrepo.utils.FedoraTypesUtils.getRepositoryCount;
import static org.fcrepo.utils.FedoraTypesUtils.getRepositorySize;
import static org.fcrepo.utils.FedoraTypesUtils.getValueFactory;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
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
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * A set of helpful tools for converting JCR properties to RDF
 * 
 * @author Chris Beer
 * @date May 10, 2013
 */
public class JcrRdfTools {

    private static final Logger LOGGER = getLogger(JcrRdfTools.class);
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
    private static GetClusterConfiguration getClusterConfiguration =
            new GetClusterConfiguration();
    private static LowLevelStorageService llstore;
    private final GraphSubjects graphSubjects;
    private Session session;

    /**
     * Factory method to create a new JcrRdfTools utility
     * with a graph subjects converter
     * @param graphSubjects
     */
    public JcrRdfTools(final GraphSubjects graphSubjects) {
        this.graphSubjects = graphSubjects;
    }

    /**
     * Factory method to create a new JcrRdfTools utility
     * with a graph subjects converter
     * @param graphSubjects
     * @param session
     */
    public JcrRdfTools(final GraphSubjects graphSubjects, final Session session) {
        this.graphSubjects = graphSubjects;
        this.session = session;
    }

    /**
     * Factory method to create a new JcrRdfTools  instance
     * @param graphSubjects
     * @return
     */
    public static JcrRdfTools withContext(final GraphSubjects graphSubjects) {
        return new JcrRdfTools(graphSubjects);
    }

    /**
     * Factory method to create a new JcrRdfTools instance
     * @param graphSubjects
     * @param session
     * @return
     */
    public static JcrRdfTools withContext(final GraphSubjects graphSubjects, final Session session) {
        return new JcrRdfTools(graphSubjects, session);
    }

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
     * Get a model in which to collect statements of RDF extraction problems
     * @return
     */
    public static Model getProblemsModel() {
        return ModelFactory.createDefaultModel();
    }

    /**
     * Using the same graph subjects, create a new JcrRdfTools with the given
     * session
     * @param session
     * @return
     */
    public JcrRdfTools withSession(final Session session) {
        return new JcrRdfTools(graphSubjects, session);
    }

    /**
     * Create a default Jena Model populated with the registered JCR namespaces
     *
     * @return
     * @throws RepositoryException
     */
    public Model getJcrPropertiesModel()
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
     * Get an RDF model for the given JCR NodeIterator
     *
     * @param nodeIterator
     * @param iteratorSubject
     * @return
     * @throws RepositoryException
     */
    public Model getJcrPropertiesModel(final Iterator<Node> nodeIterator,
                                       final Resource iteratorSubject)
        throws RepositoryException {

        if (!nodeIterator.hasNext()) {
            return ModelFactory.createDefaultModel();
        }

        final PeekingIterator<Node> iterator =
                Iterators.peekingIterator(nodeIterator);
        final Model model =
                getJcrPropertiesModel();

        while (iterator.hasNext()) {
            final Node node = iterator.next();
            addJcrPropertiesToModel(node, model);
            if (iteratorSubject != null) {
                model.add(iteratorSubject, RdfLexicon.HAS_MEMBER_OF_RESULT,
                             graphSubjects.getGraphSubject(node));
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
    public Model getJcrPropertiesModel(final Node node)
        throws RepositoryException {

        final Model model = getJcrPropertiesModel();

        if (node.getPrimaryNodeType().getName().equals(FedoraJcrTypes.ROOT)) {
            /* a rdf description of the root node */
            LOGGER.debug("Creating RDF response for repository description");
            addRepositoryMetricsToModel(node, model);
        }

        addJcrPropertiesToModel(node, model);

        return model;
    }


    /**
     * Get a Jena RDF model for the JCR version history information for a node
     *
     *
     * @param versionHistory
     * @return
     * @throws RepositoryException
     */
    public Model getJcrPropertiesModel(final VersionHistory versionHistory, final Resource subject) throws RepositoryException {

        final Model model = getJcrPropertiesModel();

        final VersionIterator versionIterator = versionHistory.getAllVersions();

        while (versionIterator.hasNext()) {
            final Version version = versionIterator.nextVersion();
            final Node frozenNode = version.getFrozenNode();
            final Resource versionSubject =
                graphSubjects.getGraphSubject(frozenNode);

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
     * @param node
     * @param blobs
     * @return
     * @throws RepositoryException
     */
    public Model getJcrPropertiesModel(final Node node,
                                       final Iterable<FixityResult> blobs)
        throws RepositoryException {

        final Model model = getJcrPropertiesModel();

        addJcrPropertiesToModel(node, model);

        for (final FixityResult result : blobs) {
            // fixity results are just blank nodes
            final Resource resultSubject = createResource();

            model.add(resultSubject, IS_FIXITY_RESULT_OF, graphSubjects
                                                              .getGraphSubject(node));
            model.add(graphSubjects.getGraphSubject(node), HAS_FIXITY_RESULT,
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
     * Get an RDF model of the registered JCR namespaces
     *
     * @return
     * @throws RepositoryException
     */
    public Model getJcrNamespaceModel()
        throws RepositoryException {
        final Model model = getJcrPropertiesModel();

        final Map<String, String> prefixMap = model.getNsPrefixMap();

        for (final Map.Entry<String, String> entry : prefixMap.entrySet()) {
            if (entry.getKey().isEmpty()) {
                continue;
            }
            final Resource nsSubject =
                ResourceFactory.createResource(entry.getValue());

            model.add(nsSubject, RDF.type, RdfLexicon.VOAF_VOCABULARY);
            model.add(nsSubject,
                         RdfLexicon.HAS_NAMESPACE_PREFIX, ResourceFactory
                                                              .createPlainLiteral(entry.getKey()));

            model.add(nsSubject,
                         RdfLexicon.HAS_NAMESPACE_URI, ResourceFactory
                                                           .createPlainLiteral(entry.getValue()));
        }

        return model;
    }

    /**
     * Add the properties of a Node's parent and immediate children (as well as
     * the jcr:content of children) to the given RDF model
     *
     * @param node
     * @param offset
     * @param limit @throws RepositoryException
     */
    public Model getJcrTreeModel(final Node node, final long offset, final int limit)
        throws RepositoryException {

        final Model model = getJcrPropertiesModel();

        if (limit < -1) {
            return model;
        }

        final Resource subject = graphSubjects.getGraphSubject(node);
        final Resource pageContext = graphSubjects.getContext();

        model.add(pageContext, RDF.type, model.createResource("http://www.w3.org/ns/ldp#Page"));
        model.add(pageContext, PAGE_OF, subject);

        if (isContainer(node)) {
            model.add(pageContext, model.createProperty("http://www.w3.org/ns/ldp#membersInlined"), model.createTypedLiteral(true));

            model.add(subject, RDF.type, model.createResource("http://www.w3.org/ns/ldp#Container"));
            model.add(subject, model.createProperty("http://www.w3.org/ns/ldp#membershipSubject"), subject);
            model.add(subject, model.createProperty("http://www.w3.org/ns/ldp#membershipPredicate"), RdfLexicon.HAS_CHILD);
            model.add(subject, model.createProperty("http://www.w3.org/ns/ldp#membershipObject"), model.createResource("http://www.w3.org/ns/ldp#MemberSubject"));
        }

        // don't do this if the node is the root node.
        if (node.getDepth() > 0) {

            final Node parentNode = node.getParent();
            final Resource parentNodeSubject =
                graphSubjects.getGraphSubject(parentNode);
            model.add(subject, RdfLexicon.HAS_PARENT, parentNodeSubject);
            model.add(parentNodeSubject, RdfLexicon.HAS_CHILD, subject);
            addJcrPropertiesToModel(parentNode, model);
            model.add(pageContext,
                         model.createProperty("http://www.w3.org/ns/ldp#inlinedResource"),
                         parentNodeSubject);
        }

        if (node.hasNodes()) {

            if (limit == -1) {
                model.add(pageContext, PAGE, RDF.nil);
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
                            graphSubjects.getGraphSubject(childNode);

                    if (i >= offset && (limit == -1 || i < (offset + limit))) {
                        addJcrPropertiesToModel(childNode, model);
                        model.add(pageContext, model.createProperty("http://www.w3.org/ns/ldp#inlinedResource"), childNodeSubject);
                        model.add(childNodeSubject, RdfLexicon.HAS_PARENT, subject);
                    }

                    i++;

                    model.add(subject, RdfLexicon.HAS_CHILD, childNodeSubject);
                }

            }

            model.add(subject, RdfLexicon.HAS_CHILD_COUNT, ResourceFactory
                    .createTypedLiteral(nodeIterator.getSize() - excludedNodeCount));
        }

        return model;
    }

    private boolean isContainer(final Node node) throws RepositoryException {
        return HAS_CHILD_NODE_DEFINITIONS.apply(node.getPrimaryNodeType())
                   || Iterables.any(ImmutableList.copyOf(node.getMixinNodeTypes()),
                                       HAS_CHILD_NODE_DEFINITIONS);
    }

    Predicate<NodeType> HAS_CHILD_NODE_DEFINITIONS = new Predicate<NodeType>() {
        @Override
        public boolean apply(NodeType input) {
            return input.getChildNodeDefinitions().length > 0;
        }
    };

    /**
     * Determine if a predicate is an internal property of a node (and
     * should not be modified from external sources)
     * @param subjectNode
     * @param predicate
     * @return
     */
    public boolean isInternalProperty(final Node subjectNode, final Resource predicate) {
        switch (predicate.getNameSpace()) {
            case RdfLexicon.INTERNAL_NAMESPACE:
            case "http://www.jcp.org/jcr/1.0":
            case "http://www.w3.org/ns/ldp#":
                return true;
            default:
                return false;
        }
    }

    /**
     * Add all of a node's properties to the given model
     *
     * @param node
     * @param model
     * @throws RepositoryException
     */
    private void addJcrPropertiesToModel(final Node node, final Model model)
        throws RepositoryException {

        final Resource subject = graphSubjects.getGraphSubject(node);
        final javax.jcr.PropertyIterator properties = node.getProperties();

        while (properties.hasNext()) {
            final Property property = properties.nextProperty();

            addPropertyToModel(subject, model, property);
        }

        // always include the jcr:content node information
        if (node.hasNode(JcrConstants.JCR_CONTENT)) {
            final Node contentNode = node.getNode(JcrConstants.JCR_CONTENT);
            final Resource contentSubject =
                    graphSubjects.getGraphSubject(contentNode);

            model.add(subject, RdfLexicon.HAS_CONTENT, contentSubject);
            model.add(contentSubject, RdfLexicon.IS_CONTENT_OF, subject);

            addJcrPropertiesToModel(contentNode, model);
            addJcrContentLocationInformationToModel(node, model);
        }
    }

    /**
     * Add repository metrics data to the given JCR model
     *
     * @param node
     * @param model
     * @throws RepositoryException
     */
    private void addRepositoryMetricsToModel(
                                                final Node node, final Model model)
        throws RepositoryException {

        final Repository repository = node.getSession().getRepository();
        /* retreive the metrics from the service */
        final SortedMap<String, Counter> counters = getMetrics().getCounters();

        final Resource subject = graphSubjects.getGraphSubject(node);
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
     * @param node
     * @param model
     * @throws RepositoryException
     */
    private void addJcrContentLocationInformationToModel(
                                                            final Node node, final Model model)
        throws RepositoryException {
        final Node contentNode = node.getNode(JcrConstants.JCR_CONTENT);
        final Resource contentNodeSubject =
            graphSubjects.getGraphSubject(contentNode);

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
     * Create a JCR value from an RDFNode, either by using the given JCR
     * PropertyType or by looking at the RDFNode Datatype
     *
     * @param data an RDF Node (possibly with a DataType)
     * @param type a JCR PropertyType value
     * @return a JCR Value
     * @throws javax.jcr.RepositoryException
     */
    Value createValue(final Node node, final RDFNode data,
                      final int type) throws RepositoryException {
        final ValueFactory valueFactory = getValueFactory.apply(node);
        assert (valueFactory != null);

        if (data.isURIResource() &&
                (type == REFERENCE || type == WEAKREFERENCE)) {
            // reference to another node (by path)
            final Node nodeFromGraphSubject = graphSubjects.getNodeFromGraphSubject(data.asResource());
            return valueFactory.createValue(nodeFromGraphSubject, type == WEAKREFERENCE);
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
    void addPropertyToModel(final Resource subject,
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
    void addPropertyToModel(final Resource subject,
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
                final Node refNode = session.getNodeByIdentifier(v.getString());
                model.add(subject,
                             predicate,
                             graphSubjects.getGraphSubject(refNode));
                break;
            case PATH:
                model.add(subject,
                             predicate,
                             graphSubjects.getGraphSubject(v.getString()));
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
     * @return
     * @throws RepositoryException
     */
    String getPropertyNameFromPredicate(final Node node,
                                        final com.hp.hpl.jena.rdf.model.Property predicate) throws RepositoryException {
        Map<String, String> s = Collections.emptyMap();
        return getPropertyNameFromPredicate(node, predicate, s);

    }

    /**
     * Given an RDF predicate value (namespace URI + local name), figure out
     * what JCR property to use
     *
     * @param node the JCR node we want a property for
     * @param predicate the predicate to map to a property name
     * @param namespaceMapping prefix => uri namespace mapping
     * @return the JCR property name
     * @throws RepositoryException
     */
    String getPropertyNameFromPredicate(final Node node,
                                        final com.hp.hpl.jena.rdf.model.Property predicate,
                                        final Map<String, String> namespaceMapping)
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
            final ImmutableBiMap<String, String> nsMap = ImmutableBiMap.copyOf(namespaceMapping);
            if (nsMap.containsValue(namespace)
                    && !namespaceRegistry.isRegisteredPrefix(nsMap.inverse().get(namespace))) {
                prefix = nsMap.inverse().get(namespace);
                namespaceRegistry.registerNamespace(prefix, namespace);
            } else {
                prefix = namespaceRegistry.registerNamespace(namespace);
            }
        }

        final String localName = predicate.getLocalName();

        final String propertyName = prefix + ":" + localName;

        LOGGER.trace("Took RDF predicate {} and translated it to "
                         + "JCR property {}", predicate, propertyName);

        return propertyName;

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
