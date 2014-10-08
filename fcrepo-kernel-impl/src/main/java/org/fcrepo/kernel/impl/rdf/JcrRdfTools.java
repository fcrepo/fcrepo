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

import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.PropertyType.URI;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static org.fcrepo.kernel.RdfLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.isManagedPredicate;
import static org.fcrepo.kernel.impl.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.exception.MalformedRdfException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.exception.ServerManagedPropertyException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.converters.ValueConverter;
import org.fcrepo.kernel.impl.utils.NodePropertiesTools;
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

    private final IdentifierConverter<Resource,Node> graphSubjects;
    private final ValueConverter valueConverter;

    private Session session;
    private NodePropertiesTools nodePropertiesTools = new NodePropertiesTools();

    /**
     * Constructor with even more context.
     *
     * @param graphSubjects
     * @param session
     */
    public JcrRdfTools(final IdentifierConverter<Resource,Node> graphSubjects, final Session session) {
        this.graphSubjects = graphSubjects;
        this.session = session;
        this.valueConverter = new ValueConverter(session, graphSubjects);
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
                final Node nodeFromGraphSubject = graphSubjects.convert(data.asResource());
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
     * @param node
     * @param mixinResource
     * @param namespaces
     * @throws RepositoryException
     */
    public void addMixin(final Node node, final Resource mixinResource, final Map<String,String> namespaces)
            throws RepositoryException {

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
     * @param node
     * @param predicate
     * @param value
     * @param namespaces
     * @throws RepositoryException
     */
    public void addProperty(final Node node,
                            final com.hp.hpl.jena.rdf.model.Property predicate,
                            final RDFNode value,
                            final Map<String,String> namespaces) throws RepositoryException {

        if (isManagedPredicate.apply(predicate)) {

            throw new ServerManagedPropertyException("Could not persist triple containing predicate "
                    + predicate.toString()
                    + " to node "
                    + node.getPath());
        }

        final String propertyName =
                getPropertyNameFromPredicate(node, predicate, namespaces);
        final Value v = createValue(node, value, propertyName);
        nodePropertiesTools.appendOrReplaceNodeProperty(graphSubjects, node, propertyName, v);
    }

    protected boolean repositoryHasType(final Session session, final String mixinName) throws RepositoryException {
        return session.getWorkspace().getNodeTypeManager().hasNodeType(mixinName);
    }

    /**
     * Remove a mixin from a node
     * @param subjectNode
     * @param mixinResource
     * @param nsPrefixMap
     * @throws RepositoryException
     */
    public void removeMixin(final Node subjectNode,
                            final Resource mixinResource,
                            final Map<String, String> nsPrefixMap) throws RepositoryException {

        final String mixinName = getPropertyNameFromPredicate(subjectNode, mixinResource, nsPrefixMap);
        if (repositoryHasType(session, mixinName) && subjectNode.isNodeType(mixinName)) {
            subjectNode.removeMixin(mixinName);
        }

    }

    /**
     * Remove a property from a node
     * @param node
     * @param predicate
     * @param objectNode
     * @param nsPrefixMap
     * @throws RepositoryException
     */
    public void removeProperty(final Node node,
                               final com.hp.hpl.jena.rdf.model.Property predicate,
                               final RDFNode objectNode,
                               final Map<String, String> nsPrefixMap) throws RepositoryException {

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

            nodePropertiesTools.removeNodeProperty(graphSubjects, node, propertyName, v);
        }
    }

}
