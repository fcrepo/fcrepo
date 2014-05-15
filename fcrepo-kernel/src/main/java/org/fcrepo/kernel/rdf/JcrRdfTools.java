/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel.rdf;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.any;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.PropertyType.URI;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.RdfLexicon.HAS_MEMBER_OF_RESULT;
import static org.fcrepo.kernel.RdfLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isReferenceProperty;
import static org.fcrepo.kernel.utils.NamespaceTools.getNamespaceRegistry;
import static org.fcrepo.kernel.utils.NodePropertiesTools.getReferencePropertyOriginalName;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.rdf.impl.FixityRdfContext;
import org.fcrepo.kernel.rdf.impl.HierarchyRdfContext;
import org.fcrepo.kernel.rdf.impl.NamespaceRdfContext;
import org.fcrepo.kernel.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.rdf.impl.VersionsRdfContext;
import org.fcrepo.kernel.rdf.impl.WorkspaceRdfContext;
import org.fcrepo.kernel.utils.FixityResult;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.modeshape.jcr.api.Namespaced;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A set of helpful tools for converting JCR properties to RDF
 *
 * @author Chris Beer
 * @since May 10, 2013
 */
public class JcrRdfTools {

    private static final Logger LOGGER = getLogger(JcrRdfTools.class);

    /**
     * A map of JCR namespaces to Fedora's RDF namespaces
     */
    public static BiMap<String, String> jcrNamespacesToRDFNamespaces =
        ImmutableBiMap.of(JCR_NAMESPACE,
                RdfLexicon.REPOSITORY_NAMESPACE);

    /**
     * A map of Fedora's RDF namespaces to the JCR equivalent
     */
    public static BiMap<String, String> rdfNamespacesToJcrNamespaces =
        jcrNamespacesToRDFNamespaces.inverse();

    private final IdentifierTranslator graphSubjects;

    private Session session;

    /**
     * Factory method to create a new JcrRdfTools utility with a graph subjects
     * converter
     *
     * @param graphSubjects
     */
    public JcrRdfTools(final IdentifierTranslator graphSubjects) {
        this(graphSubjects, null);
    }
    /**
     * Constructor with even more context.
     *
     * @param graphSubjects
     * @param session
     */
    public JcrRdfTools(final IdentifierTranslator graphSubjects, final Session session) {
        this.graphSubjects = graphSubjects;
        this.session = session;
    }

    /**
     * Factory method to create a new JcrRdfTools instance
     *
     * @param graphSubjects
     * @return new JcrRdfTools instance
     */
    public static JcrRdfTools withContext(final IdentifierTranslator graphSubjects) {
        return new JcrRdfTools(graphSubjects);
    }

    /**
     * Factory method to create a new JcrRdfTools instance
     *
     * @param idTranslator
     * @param session
     * @return new JcrRdfTools instance
     */
    public static JcrRdfTools withContext(final IdentifierTranslator idTranslator,
        final Session session) {
        checkNotNull(idTranslator, "JcrRdfTools must operate with a non-null IdentifierTranslator for context!");
        return new JcrRdfTools(idTranslator, session);
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
        }
        return rdfNamespaceUri;
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
        }
        return jcrNamespaceUri;
    }

    /**
     * Get a model in which to collect statements of RDF extraction problems
     *
     * @return an empty model
     */
    public static Model getProblemsModel() {
        return createDefaultModel();
    }

    /**
     * Using the same graph subjects, create a new JcrRdfTools with the given
     * session
     *
     * @param session
     * @return a new JcrRdfTools instance with the given session
     */
    public JcrRdfTools withSession(final Session session) {
        return new JcrRdfTools(graphSubjects, session);
    }

    /**
     * Get an {@link RdfStream} for the given JCR NodeIterator
     *
     * @param nodeIterator
     * @param iteratorSubject
     * @return RdfStream for the given JCR NodeIterator
     * @throws RepositoryException
     */
    public RdfStream getJcrPropertiesModel(final Iterator<Node> nodeIterator,
            final Resource iteratorSubject) throws RepositoryException {

        final RdfStream results = new RdfStream();
        while (nodeIterator.hasNext()) {
            final Node node = nodeIterator.next();
            results.concat(new PropertiesRdfContext(node, graphSubjects));
            if (iteratorSubject != null) {
                results.concat(singleton(create(iteratorSubject.asNode(), HAS_MEMBER_OF_RESULT.asNode(), graphSubjects
                        .getSubject(node.getPath()).asNode())));
            }
        }
        return results;
    }

    /**
     * Get an {@link RdfStream} for a node that includes all its own JCR properties,
     * as well as the properties of its immediate children. TODO add triples for
     * root node, ala addRepositoryMetricsToModel()
     *
     * @param node
     * @return RdfStream including all its own JCR properties and properties from immediate children
     * @throws RepositoryException
     */
    public RdfStream getJcrTriples(final Node node) throws RepositoryException {
        return new PropertiesRdfContext(node, graphSubjects);
    }

    /**
     * Get an {@link RdfStream} for the JCR version history information for a node
     *
     * @param node
     * @return RdfStream for the JCR version history information for the given node
     * @throws RepositoryException
     */
    public RdfStream getVersionTriples(final Node node)
        throws RepositoryException {
        return new VersionsRdfContext(node, graphSubjects);
    }

    /**
     * Serialize the JCR fixity information in an {@link RdfStream}
     *
     * @param node
     * @param blobs
     * @return fixity information triples as an RdfStream
     * @throws RepositoryException
     */
    public RdfStream getJcrTriples(final Node node,
            final Iterable<FixityResult> blobs) throws RepositoryException {
        return new FixityRdfContext(node, graphSubjects, blobs);
    }

    /**
     * Get an {@link RdfStream} of the registered JCR namespaces
     *
     * @return namespace triples as an RdfStream
     * @throws RepositoryException
     */
    public RdfStream getNamespaceTriples() throws RepositoryException {
        return new NamespaceRdfContext(session);
    }

    /**
     * Get an {@link RdfStream} of the registered JCR workspaces
     *
     * @return workspace triples as an RdfStream
     * @throws RepositoryException
     */
    public RdfStream getWorkspaceTriples(final IdentifierTranslator subjects) throws RepositoryException {
        return new WorkspaceRdfContext(session, subjects);
    }

    /**
     * Add the properties of a Node's parent and immediate children (as well as
     * the jcr:content of children) to the given {@link RdfStream}
     *
     * @param node
     * @throws RepositoryException
     */
    public RdfStream getTreeTriples(final Node node,
                                    final HierarchyRdfContextOptions options) throws RepositoryException {
        return new HierarchyRdfContext(node, graphSubjects, options);
    }

    /**
     * Add the properties of a Node's parent and immediate children using the default
     * serialization options
     * @param node
     * @return RdfStream of properties for the node's parent and immediate children
     * @throws RepositoryException
     */
    public RdfStream getTreeTriples(final Node node) throws RepositoryException {
        return getTreeTriples(node, HierarchyRdfContextOptions.DEFAULT);
    }


    /**
     * Add the properties for inbound references to this node
     * @param node
     * @return RdfStream containing propeties for inbound references to the given node
     * @throws RepositoryException
     */
    public RdfStream getReferencesTriples(final Node node) throws RepositoryException {
        return new ReferencesRdfContext(node, graphSubjects);
    }

    /**
     * Decides whether the RDF representation of this {@link Node} will receive LDP Container status.
     *
     * @param node
     * @return true if the node will receive LDP Container status
     * @throws RepositoryException
     */
    public static boolean isContainer(final Node node) throws RepositoryException {
        return HAS_CHILD_NODE_DEFINITIONS.apply(node.getPrimaryNodeType())
                || any(ImmutableList.copyOf(node.getMixinNodeTypes()),
                        HAS_CHILD_NODE_DEFINITIONS);
    }

    static Predicate<NodeType> HAS_CHILD_NODE_DEFINITIONS =
        new Predicate<NodeType>() {

            @Override
            public boolean apply(final NodeType input) {
                return input.getChildNodeDefinitions().length > 0;
            }
        };

    /**
     * Determine if a predicate is an internal property of a node (and should
     * not be modified from external sources)
     *
     * @param subjectNode
     * @param predicate
     * @return True if a predicate is an internal property of a node
     */
    public boolean isInternalProperty(final Node subjectNode,
            final Resource predicate) {
        switch (predicate.getNameSpace()) {
            case REPOSITORY_NAMESPACE:
            case JCR_NAMESPACE:
            case LDP_NAMESPACE:
                return true;
            default:
                return false;
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
    public Value createValue(final Node node, final RDFNode data, final int type)
        throws RepositoryException {
        final ValueFactory valueFactory = node.getSession().getValueFactory();
        return createValue(valueFactory, data, type);

    }

    /**
     * Create a JCR value (with an undefined type) from a RDFNode
     * @param data
     * @return created JCR value
     * @throws RepositoryException
     */
    public Value createValue(final RDFNode data) throws RepositoryException {
        return createValue(data, UNDEFINED);
    }

    /**
     * Create a JCR value from an RDFNode with the given JCR type
     * @param data
     * @param type
     * @return created JCR value
     * @throws RepositoryException
     */
    public Value createValue(final RDFNode data, final int type) throws RepositoryException {
        return createValue(session.getValueFactory(), data, type);
    }

    /**
     * Create a JCR value from an RDF node with the given JCR type
     * @param valueFactory
     * @param data
     * @param type
     * @return created value
     * @throws RepositoryException
     */
    public Value createValue(final ValueFactory valueFactory, final RDFNode data, final int type)
        throws RepositoryException {
        assert (valueFactory != null);

        if (data.isURIResource()
                && (type == REFERENCE || type == WEAKREFERENCE)) {
            // reference to another node (by path)
            final Node nodeFromGraphSubject = session.getNode(graphSubjects.getPathFromSubject(data.asResource()));
            return valueFactory.createValue(nodeFromGraphSubject,
                    type == WEAKREFERENCE);
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
            } else if (rdfValue instanceof Byte
                    || (dataType != null && dataType.getJavaClass() == Byte.class)) {
                return valueFactory.createValue(literal.getByte());
            } else if (rdfValue instanceof Double) {
                return valueFactory.createValue((Double) rdfValue);
            } else if (rdfValue instanceof Float) {
                return valueFactory.createValue((Float) rdfValue);
            } else if (rdfValue instanceof Long
                    || (dataType != null && dataType.getJavaClass() == Long.class)) {
                return valueFactory.createValue(literal.getLong());
            } else if (rdfValue instanceof Short
                    || (dataType != null && dataType.getJavaClass() == Short.class)) {
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
            LOGGER.debug("Using default JCR value creation for RDF literal: {}",
                    data);
            return valueFactory.createValue(data.asLiteral().getString(), type);
        }
    }

    /**
     * Given an RDF predicate value (namespace URI + local name), figure out
     * what JCR property to use
     *
     * @param node the JCR node we want a property for
     * @param predicate the predicate to map to a property name
     * @return JCR property name
     * @throws RepositoryException
     */
    public String getPropertyNameFromPredicate(final Node node,
        final com.hp.hpl.jena.rdf.model.Property predicate)
        throws RepositoryException {
        final Map<String, String> s = emptyMap();
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

    public String getPropertyNameFromPredicate(final Node node, final com.hp.hpl.jena.rdf.model.Property predicate,
                                               final Map<String, String> namespaceMapping) throws RepositoryException {

        final NamespaceRegistry namespaceRegistry =
            getNamespaceRegistry.apply(node);

        return getJcrNameForRdfNode(namespaceRegistry,
                                    predicate.getNameSpace(),
                                    predicate.getLocalName(),
                                    namespaceMapping);
    }

    /**
     * Get a property name for an RDF predicate
     * @param predicate
     * @return property name from the given predicate
     * @throws RepositoryException
     */
    public String getPropertyNameFromPredicate(final com.hp.hpl.jena.rdf.model.Property predicate)
        throws RepositoryException {

        final NamespaceRegistry namespaceRegistry =
            (org.modeshape.jcr.api.NamespaceRegistry) session.getWorkspace().getNamespaceRegistry();

        final Map<String, String> namespaceMapping = emptyMap();
        return getJcrNameForRdfNode(namespaceRegistry,
                                    predicate.getNameSpace(),
                                    predicate.getLocalName(),
                                    namespaceMapping);
    }

    /**
     * Get the JCR name for the given RDF resource
     * @param node
     * @param resource
     * @param namespaces
     * @return JCR name for the given RDF resource
     * @throws RepositoryException
     */
    public String getPropertyNameFromPredicate(final Node node,
                                               final Resource resource,
                                               final Map<String,String> namespaces) throws RepositoryException {
        final NamespaceRegistry namespaceRegistry = getNamespaceRegistry.apply(node);
        return getJcrNameForRdfNode(namespaceRegistry,
                                    resource.getNameSpace(),
                                    resource.getLocalName(),
                                    namespaces);
    }

    /**
     * Get the JCR property name for an RDF predicate
     *
     * @param namespaceRegistry
     * @param rdfNamespace
     * @param rdfLocalname
     * @param namespaceMapping
     * @return JCR property name for an RDF predicate
     * @throws RepositoryException
     */
    private String getJcrNameForRdfNode(final NamespaceRegistry namespaceRegistry,
                                        final String rdfNamespace,
                                        final String rdfLocalname,
                                        final Map<String, String> namespaceMapping)
        throws RepositoryException {

        final String prefix;

        final String namespace =
            getJcrNamespaceForRDFNamespace(rdfNamespace);

        assert (namespaceRegistry != null);

        if (namespaceRegistry.isRegisteredUri(namespace)) {
            LOGGER.debug("Discovered namespace: {} in namespace registry.",namespace);
            prefix = namespaceRegistry.getPrefix(namespace);
        } else {
            LOGGER.debug("Didn't discover namespace: {} in namespace registry.",namespace);
            final ImmutableBiMap<String, String> nsMap =
                ImmutableBiMap.copyOf(namespaceMapping);
            if (nsMap.containsValue(namespace)) {
                LOGGER.debug("Discovered namespace: {} in namespace map: {}.", namespace,
                        nsMap);
                prefix = nsMap.inverse().get(namespace);
                namespaceRegistry.registerNamespace(prefix, namespace);
            } else {
                prefix = namespaceRegistry.registerNamespace(namespace);
            }
        }

        final String propertyName = prefix + ":" + rdfLocalname;

        LOGGER.debug("Took RDF predicate {} and translated it to JCR property {}", namespace, propertyName);

        return propertyName;

    }

    /**
     * Given a node type and a property name, figure out an appropriate jcr value type
     * @param nodeType
     * @param propertyName
     * @return jcr value type
     * @throws RepositoryException
     */
    public int getPropertyType(final String nodeType, final String propertyName) throws RepositoryException {
        return getPropertyType(session.getWorkspace().getNodeTypeManager().getNodeType(nodeType), propertyName);

    }

    /**
     * Given a node type and a property name, figure out an appropraite jcr value type
     * @param nodeType
     * @param propertyName
     * @return jcr value type
     */
    public int getPropertyType(final NodeType nodeType, final String propertyName) {
        final PropertyDefinition[] propertyDefinitions = nodeType.getPropertyDefinitions();
        int type = UNDEFINED;
        for (final PropertyDefinition propertyDefinition : propertyDefinitions) {
            if (propertyDefinition.getName().equals(propertyName)) {
                if (type != UNDEFINED) {
                    return UNDEFINED;
                }

                type = propertyDefinition.getRequiredType();
            }
        }

        return type;
    }

    /**
     * Map a JCR property to an RDF property with the right namespace URI and
     * local name
     */
    public static Function<Property, com.hp.hpl.jena.rdf.model.Property> getPredicateForProperty =
            new Function<Property, com.hp.hpl.jena.rdf.model.Property>() {

                @Override
                public com.hp.hpl.jena.rdf.model.Property apply(
                        final Property property) {
                    LOGGER.trace("Creating predicate for property: {}",
                            property);
                    try {
                        if (property instanceof Namespaced) {
                            final Namespaced nsProperty = (Namespaced) property;
                            final String uri = nsProperty.getNamespaceURI();
                            final String localName = nsProperty.getLocalName();
                            final String rdfLocalName;

                            if (isReferenceProperty.apply(property)) {
                                rdfLocalName = getReferencePropertyOriginalName(localName);
                            } else {
                                rdfLocalName = localName;
                            }
                            return createProperty(
                                    getRDFNamespaceForJcrNamespace(uri),
                                                     rdfLocalName);
                        }
                        return createProperty(property.getName());
                    } catch (final RepositoryException e) {
                        throw propagate(e);
                    }

                }
            };
}
