/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.rdf;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static java.util.UUID.randomUUID;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_LASTMODIFIED;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_LASTMODIFIEDBY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CREATED;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CREATEDBY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_PAIRTREE;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_LASTMODIFIEDBY;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_CREATED;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_CREATEDBY;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.jcrProperties;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.modeshape.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getClosestExistingAncestor;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getPropertyType;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isReferenceProperty;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import com.google.common.annotations.VisibleForTesting;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter;
import org.fcrepo.kernel.modeshape.utils.NodePropertiesTools;

import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

/**
 * A set of helpful tools for converting JCR properties to RDF
 *
 * @author Chris Beer
 * @author ajs6f
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

    private final IdentifierConverter<Resource, FedoraResource> idTranslator;
    private final ValueConverter valueConverter;

    private final Session session;
    private final NodePropertiesTools nodePropertiesTools = new NodePropertiesTools();

    @VisibleForTesting
    protected JcrTools jcrTools = new JcrTools();

    private final Map<AnonId, Resource> skolemizedBnodeMap;

    private static final Model m = createDefaultModel();

    /**
     * Constructor with even more context.
     *
     * @param idTranslator the id translator
     * @param session the session
     */
    public JcrRdfTools(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                       final Session session) {
        this.idTranslator = idTranslator;
        this.session = session;
        this.valueConverter = new ValueConverter(session, idTranslator);
        this.skolemizedBnodeMap = new HashMap<>();
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
     * Create a JCR value from an RDFNode for a given JCR property
     * @param node the JCR node we want a property for
     * @param data an RDF Node (possibly with a DataType)
     * @param propertyName name of the property to populate (used to use the right type for the value)
     * @return the JCR value from an RDFNode for a given JCR property
     * @throws RepositoryException if repository exception occurred
     */
    public Value createValue(final Node node,
                             final RDFNode data,
                             final String propertyName) throws RepositoryException {
        final ValueFactory valueFactory = node.getSession().getValueFactory();
        return createValue(valueFactory, data, getPropertyType(node, propertyName).orElse(UNDEFINED));
    }

    /**
     * Create a JCR value from an RDF node with the given JCR type
     * @param valueFactory the given value factory
     * @param data the rdf node data
     * @param type the given JCR type
     * @return created value
     * @throws RepositoryException if repository exception occurred
     */
    public Value createValue(final ValueFactory valueFactory, final RDFNode data, final int type)
        throws RepositoryException {
        assert (valueFactory != null);


        if (type == UNDEFINED || type == STRING) {
            return valueConverter.reverse().convert(data);
        } else if (type == REFERENCE || type == WEAKREFERENCE) {
            // reference to another node (by path)
            if (!data.isURIResource()) {
                throw new ValueFormatException("Reference properties can only refer to URIs, not literals");
            }

            try {
                final Node nodeFromGraphSubject = getJcrNode(idTranslator.convert(data.asResource()));
                return valueFactory.createValue(nodeFromGraphSubject, type == WEAKREFERENCE);
            } catch (final RepositoryRuntimeException e) {
                throw new MalformedRdfException("Unable to find referenced node", e);
            }
        } else if (data.isResource()) {
            LOGGER.debug("Using default JCR value creation for RDF resource: {}",
                    data);
            return valueFactory.createValue(data.asResource().getURI(), type);
        } else {
            LOGGER.debug("Using default JCR value creation for RDF literal: {}",
                    data);
            return valueFactory.createValue(data.asLiteral().getString(), type);
        }
    }

    /**
     * Add a mixin to a node
     * @param resource the fedora resource
     * @param mixinResource the mixin resource
     * @param namespaces the namespace
     * @throws RepositoryException if repository exception occurred
     */
    public void addMixin(final FedoraResource resource,
                         final Resource mixinResource,
                         final Map<String,String> namespaces)
            throws RepositoryException {

        final Node node = getJcrNode(resource);
        final Session session = node.getSession();
        final String mixinName = getPropertyNameFromPredicate(node, mixinResource, namespaces);
        if (!repositoryHasType(session, mixinName)) {
            final NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();
            final NodeTypeTemplate type = mgr.createNodeTypeTemplate();
            type.setName(mixinName);
            type.setMixin(true);
            type.setQueryable(true);
            mgr.registerNodeType(type, false);
        }

        if (node.isNodeType(mixinName)) {
            LOGGER.trace("Subject {} is already a {}; skipping", node, mixinName);
            return;
        }

        if (node.canAddMixin(mixinName)) {
            LOGGER.debug("Adding mixin: {} to node: {}.", mixinName, node.getPath());
            node.addMixin(mixinName);
        } else {
            throw new MalformedRdfException("Could not persist triple containing type assertion: "
                    + mixinResource.toString()
                    + " because no such mixin/type can be added to this node: "
                    + node.getPath() + "!");
        }
    }

    /**
     * Add property to a node
     * @param resource the fedora resource
     * @param predicate the predicate
     * @param value the value
     * @param namespaces the namespace
     * @throws RepositoryException if repository exception occurred
     */
    public void addProperty(final FedoraResource resource,
            final org.apache.jena.rdf.model.Property predicate,
            final RDFNode value,
            final Map<String, String> namespaces) throws RepositoryException {
        addProperty(resource, predicate, value, namespaces, false);
    }

    /**
     * Add property to a node
     * @param resource the fedora resource
     * @param predicate the predicate
     * @param value the value
     * @param namespaces the namespace
     * @param allowRelaxedProperties if true, relaxed server managed properties are allowed
     * @throws RepositoryException if repository exception occurred
     */
    public void addProperty(final FedoraResource resource,
                            final org.apache.jena.rdf.model.Property predicate,
                            final RDFNode value,
                            final Map<String, String> namespaces,
                            final boolean allowRelaxedProperties) throws RepositoryException {

        final Node node = getJcrNode(resource);

        if ((!allowRelaxedProperties && isManagedPredicate.test(predicate)) || jcrProperties.contains(predicate)) {

            throw new ServerManagedPropertyException("Could not persist triple containing predicate "
                    + predicate.toString()
                    + " to node "
                    + node.getPath());
        }

        String propertyName =
                getPropertyNameFromPredicate(node, predicate, namespaces);

        // In earlier versions of Fedora the following JCR properties were
        // used for properties.  When the behavior changed such that they could
        // be set explicitly in some cases, we changed the underlying representation
        // such that the "fedora"-namespaced property could be set, and when present
        // would be presented instead of the underlying JCR-managed property.
        if (propertyName.equals(JCR_LASTMODIFIEDBY)) {
            propertyName = FEDORA_LASTMODIFIEDBY;
        } else if (propertyName.equals(JCR_LASTMODIFIED)) {
            propertyName = FEDORA_LASTMODIFIED;
        } else if (propertyName.equals(JCR_CREATEDBY)) {
            propertyName = FEDORA_CREATEDBY;
        } else if (propertyName.equals(JCR_CREATED)) {
            propertyName = FEDORA_CREATED;
        }

        if (value.isURIResource()
                && idTranslator.inDomain(value.asResource())
                && !isReferenceProperty(node, propertyName)) {
            nodePropertiesTools.addReferencePlaceholders(idTranslator, node, propertyName, value.asResource());
        } else {
            final Value v = createValue(node, value, propertyName);
            nodePropertiesTools.appendOrReplaceNodeProperty(node, propertyName, v);
        }
    }

    protected boolean repositoryHasType(final Session session, final String mixinName) throws RepositoryException {
        return session.getWorkspace().getNodeTypeManager().hasNodeType(mixinName);
    }

    /**
     * Remove a mixin from a node
     * @param resource the resource
     * @param mixinResource the mixin resource
     * @param nsPrefixMap the prefix map
     * @throws RepositoryException if repository exception occurred
     */
    public void removeMixin(final FedoraResource resource,
                            final Resource mixinResource,
                            final Map<String, String> nsPrefixMap) throws RepositoryException {

        final Node node = getJcrNode(resource);
        final String mixinName = getPropertyNameFromPredicate(node, mixinResource, nsPrefixMap);
        if (repositoryHasType(session, mixinName) && node.isNodeType(mixinName)) {
            node.removeMixin(mixinName);
        }

    }

    /**
     * Remove a property from a node
     * @param resource the fedora resource
     * @param predicate the predicate
     * @param objectNode the object node
     * @param nsPrefixMap the prefix map
     * @throws RepositoryException if repository exception occurred
     */
    public void removeProperty(final FedoraResource resource,
                               final org.apache.jena.rdf.model.Property predicate,
                               final RDFNode objectNode,
                               final Map<String, String> nsPrefixMap) throws RepositoryException {

        final Node node = getJcrNode(resource);
        final String propertyName = getPropertyNameFromPredicate(node, predicate, nsPrefixMap);

        if (isManagedPredicate.test(predicate) || jcrProperties.contains(predicate)) {

            throw new ServerManagedPropertyException("Could not remove triple containing predicate "
                    + predicate.toString()
                    + " to node "
                    + node.getPath());
        }

        if (objectNode.isURIResource()
                && idTranslator.inDomain(objectNode.asResource())
                && !isReferenceProperty(node, propertyName)) {
            nodePropertiesTools.removeReferencePlaceholders(idTranslator,
                    node,
                    propertyName,
                    objectNode.asResource());
        } else {
            final Value v = createValue(node, objectNode, propertyName);
            nodePropertiesTools.removeNodeProperty(node, propertyName, v);
        }
    }

    /**
     * Convert an external statement into a persistable statement by skolemizing
     * blank nodes, creating hash-uri subjects, etc
     *
     * @param idTranslator the property of idTranslator
     * @param t the statement
     * @param topic the topic
     * @return the persistable statement
     * @throws RepositoryException if repository exception occurred
     */
    public Statement skolemize(final IdentifierConverter<Resource, FedoraResource> idTranslator, final Statement t,
            final String topic) throws RepositoryException {

        Statement skolemized = t;

        if (t.getSubject().isAnon()) {
            skolemized = m.createStatement(getSkolemizedResource(idTranslator, skolemized.getSubject(), topic),
                    skolemized.getPredicate(),
                    skolemized.getObject());
        } else if (idTranslator.inDomain(t.getSubject()) && t.getSubject().getURI().contains("#")) {
            findOrCreateHashUri(idTranslator, t.getSubject());
        }

        if (t.getObject().isAnon()) {
            skolemized = m.createStatement(skolemized.getSubject(), skolemized.getPredicate(), getSkolemizedResource
                    (idTranslator, skolemized.getObject(), topic));
        } else if (t.getObject().isResource()
                && idTranslator.inDomain(t.getObject().asResource())
                && t.getObject().asResource().getURI().contains("#")) {
            findOrCreateHashUri(idTranslator, t.getObject().asResource());
        }

        return skolemized;
    }

    private void findOrCreateHashUri(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                     final Resource s) throws RepositoryException {
        final String absPath = idTranslator.asString(s);

        if (!absPath.isEmpty() && !session.nodeExists(absPath)) {
            final Node closestExistingAncestor = getClosestExistingAncestor(session, absPath);

            final Node orCreateNode = jcrTools.findOrCreateNode(session, absPath, NT_FOLDER);
            orCreateNode.addMixin(FEDORA_RESOURCE);

            final Node parent = orCreateNode.getParent();

            if (!parent.getName().equals("#")) {
                throw new AssertionError("Hash URI resource created with too much hierarchy: " + s);
            }

            // We require the closest node to be either "#" resource, or its parent.
            if (!parent.equals(closestExistingAncestor)
                    && !parent.getParent().equals(closestExistingAncestor)) {
                throw new PathNotFoundException("Unexpected request to create new resource " + s);
            }

            if (parent.isNew()) {
                parent.addMixin(FEDORA_PAIRTREE);
            }
        }
    }

    private Resource getSkolemizedResource(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                           final RDFNode resource, final String topic) throws RepositoryException {
        final AnonId id = resource.asResource().getId();

        if (!skolemizedBnodeMap.containsKey(id)) {
            final String base = idTranslator.asString(createResource(topic));
            final Resource skolemizedSubject = idTranslator.toDomain(base + "#" + blankNodeIdentifier());
            findOrCreateHashUri(idTranslator, skolemizedSubject);
            skolemizedBnodeMap.put(id, skolemizedSubject);
        }

        return skolemizedBnodeMap.get(id);
    }

    private String blankNodeIdentifier() {
        return "genid" + randomUUID().toString();
    }
}
