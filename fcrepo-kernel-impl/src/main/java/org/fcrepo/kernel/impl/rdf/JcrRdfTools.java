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
package org.fcrepo.kernel.impl.rdf;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static java.util.UUID.randomUUID;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.PropertyType.URI;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_PAIRTREE;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.RdfLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.isManagedPredicate;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeToResource;
import static org.fcrepo.kernel.impl.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.getClosestExistingAncestor;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import com.google.common.annotations.VisibleForTesting;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.exception.MalformedRdfException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.exception.ServerManagedPropertyException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.converters.ValueConverter;
import org.fcrepo.kernel.impl.utils.NodePropertiesTools;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
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

    private final IdentifierConverter<Resource, FedoraResource> idTranslator;
    private final ValueConverter valueConverter;

    private Session session;
    private final NodePropertiesTools nodePropertiesTools = new NodePropertiesTools();

    @VisibleForTesting
    protected JcrTools jcrTools = new JcrTools();

    private final Map<AnonId, Resource> skolemizedBnodeMap;

    private static final Model m = createDefaultModel();


    /**
     * Constructor with even more context.
     *
     * @param idTranslator
     * @param session
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
     * @return
     * @throws RepositoryException
     */
    public Value createValue(final Node node,
                             final RDFNode data,
                             final String propertyName) throws RepositoryException {
        final ValueFactory valueFactory = node.getSession().getValueFactory();
        return createValue(valueFactory, data, nodePropertiesTools.getPropertyType(node, propertyName));
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

        if (type == UNDEFINED) {
            return valueConverter.reverse().convert(data);
        } else if (data.isURIResource()
                && (type == REFERENCE || type == WEAKREFERENCE)) {
            // reference to another node (by path)
            try {
                final Node nodeFromGraphSubject = idTranslator.convert(data.asResource()).getNode();
                return valueFactory.createValue(nodeFromGraphSubject,
                        type == WEAKREFERENCE);
            } catch (final RepositoryRuntimeException e) {
                throw new MalformedRdfException("Unable to find referenced node", e);
            }
        } else if (!data.isURIResource() && (type == REFERENCE || type == WEAKREFERENCE)) {
            throw new ValueFormatException("Reference properties can only refer to URIs, not literals");
        } else if (type == URI) {
            // some random opaque URI
            return valueFactory.createValue(data.toString(), PropertyType.URI);
        } else {
            LOGGER.debug("Using default JCR value creation for RDF literal: {}",
                    data);
            return valueFactory.createValue(data.asLiteral().getString(), type);
        }
    }

    /**
     * Add a mixin to a node
     * @param resource
     * @param mixinResource
     * @param namespaces
     * @throws RepositoryException
     */
    public void addMixin(final FedoraResource resource,
                         final Resource mixinResource,
                         final Map<String,String> namespaces)
            throws RepositoryException {

        final Node node = resource.getNode();
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
     * @param resource
     * @param predicate
     * @param value
     * @param namespaces
     * @throws RepositoryException
     */
    public void addProperty(final FedoraResource resource,
                            final com.hp.hpl.jena.rdf.model.Property predicate,
                            final RDFNode value,
                            final Map<String,String> namespaces) throws RepositoryException {

        final Node node = resource.getNode();

        if (isManagedPredicate.apply(predicate)) {

            throw new ServerManagedPropertyException("Could not persist triple containing predicate "
                    + predicate.toString()
                    + " to node "
                    + node.getPath());
        }

        final String propertyName =
                getPropertyNameFromPredicate(node, predicate, namespaces);
        final Value v = createValue(node, value, propertyName);
        nodePropertiesTools.appendOrReplaceNodeProperty(idTranslator, node, propertyName, v);
    }

    protected boolean repositoryHasType(final Session session, final String mixinName) throws RepositoryException {
        return session.getWorkspace().getNodeTypeManager().hasNodeType(mixinName);
    }

    /**
     * Remove a mixin from a node
     * @param resource
     * @param mixinResource
     * @param nsPrefixMap
     * @throws RepositoryException
     */
    public void removeMixin(final FedoraResource resource,
                            final Resource mixinResource,
                            final Map<String, String> nsPrefixMap) throws RepositoryException {

        final Node node = resource.getNode();
        final String mixinName = getPropertyNameFromPredicate(node, mixinResource, nsPrefixMap);
        if (repositoryHasType(session, mixinName) && node.isNodeType(mixinName)) {
            node.removeMixin(mixinName);
        }

    }

    /**
     * Remove a property from a node
     * @param resource
     * @param predicate
     * @param objectNode
     * @param nsPrefixMap
     * @throws RepositoryException
     */
    public void removeProperty(final FedoraResource resource,
                               final com.hp.hpl.jena.rdf.model.Property predicate,
                               final RDFNode objectNode,
                               final Map<String, String> nsPrefixMap) throws RepositoryException {

        final Node node = resource.getNode();
        final String propertyName = getPropertyNameFromPredicate(node, predicate, nsPrefixMap);

        if (isManagedPredicate.apply(predicate)) {

            throw new ServerManagedPropertyException("Could not remove triple containing predicate "
                    + predicate.toString()
                    + " to node "
                    + node.getPath());
        }

        // if the property doesn't exist, we don't need to worry about it.
        if (node.hasProperty(propertyName)) {
            final Value v = createValue(node, objectNode, propertyName);

            nodePropertiesTools.removeNodeProperty(idTranslator, node, propertyName, v);
        }
    }

    /**
     * Convert an external statement into a persistable statement by skolemizing
     * blank nodes, creating hash-uri subjects, etc
     *
     * @param idTranslator
     * @param t
     * @return
     * @throws RepositoryException
     */
    public Statement skolemize(final IdentifierConverter<Resource, FedoraResource> idTranslator, final Statement t)
            throws RepositoryException {

        Statement skolemized = t;

        if (t.getSubject().isAnon()) {
            skolemized = m.createStatement(getSkolemizedResource(idTranslator, skolemized.getSubject()),
                    t.getPredicate(),
                    t.getObject());
        } else if (idTranslator.inDomain(t.getSubject()) && t.getSubject().getURI().contains("#")) {
            findOrCreateHashUri(idTranslator, t.getSubject());
        }

        if (t.getObject().isAnon()) {
            skolemized = skolemized.changeObject(getSkolemizedResource(idTranslator, t.getObject()));
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
                                           final RDFNode resource) throws RepositoryException {
        final AnonId id = resource.asResource().getId();

        if (!skolemizedBnodeMap.containsKey(id)) {
            final Node orCreateNode = jcrTools.findOrCreateNode(session, skolemizedId());
            orCreateNode.addMixin("fedora:blanknode");
            final Resource skolemizedSubject = nodeToResource(idTranslator).convert(orCreateNode);
            skolemizedBnodeMap.put(id, skolemizedSubject);
        }

        return skolemizedBnodeMap.get(id);
    }

    private String skolemizedId() {
        return "/.well-known/genid/" + randomUUID().toString();
    }
}
