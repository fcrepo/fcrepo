/*
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.modeshape.utils;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.services.functions.AnyTypesPredicate;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.cache.NodeKey;
import org.slf4j.Logger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import static java.util.Arrays.stream;
import static java.util.Calendar.getInstance;
import static java.util.Optional.empty;
import static java.util.TimeZone.getTimeZone;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static com.google.common.collect.ImmutableSet.of;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_LASTMODIFIED;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FROZEN_MIXIN_TYPES;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FROZEN_PRIMARY_TYPE;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FROZEN_NODE;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_CREATED;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_CREATEDBY;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_FROZEN_NODE;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_LASTMODIFIEDBY;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.ROOT;
import static org.fcrepo.kernel.modeshape.services.functions.JcrPropertyFunctions.isBinaryContentProperty;
import static org.fcrepo.kernel.modeshape.utils.NamespaceTools.getNamespaceRegistry;
import static org.fcrepo.kernel.modeshape.utils.UncheckedPredicate.uncheck;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_PRIMARY_TYPE;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIXIN_TYPES;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Convenience class with static methods for manipulating Fedora types in the
 * JCR.
 *
 * @author ajs6f
 * @since Feb 14, 2013
 */
public abstract class FedoraTypesUtils implements FedoraTypes {

    public static final String REFERENCE_PROPERTY_SUFFIX = "_ref";

    private static final Logger LOGGER = getLogger(FedoraTypesUtils.class);

    private static Set<String> privateProperties = of(
            "jcr:mime",
            "jcr:mimeType",
            "jcr:frozenUuid",
            "jcr:uuid",
            JCR_CONTENT,
            JCR_PRIMARY_TYPE,
            JCR_LASTMODIFIED,
            JCR_MIXIN_TYPES,
            FROZEN_MIXIN_TYPES,
            FROZEN_PRIMARY_TYPE);

    private static Set<String> validJcrProperties = of(
            JCR_CREATED,
            JCR_CREATEDBY,
            JCR_LASTMODIFIEDBY);

    /**
     * Predicate for determining whether this {@link Node} is a {@link org.fcrepo.kernel.api.models.Container}.
     */
    public static Predicate<Node> isContainer = new AnyTypesPredicate(FEDORA_CONTAINER);

    /**
     * Predicate for determining whether this {@link Node} is a
     * {@link org.fcrepo.kernel.api.models.NonRdfSourceDescription}.
     */
    public static Predicate<Node> isNonRdfSourceDescription = new AnyTypesPredicate(FEDORA_NON_RDF_SOURCE_DESCRIPTION);


    /**
     * Predicate for determining whether this {@link Node} is a Fedora
     * binary.
     */
    public static Predicate<Node> isFedoraBinary = new AnyTypesPredicate(FEDORA_BINARY);

    /**
     * Predicate for determining whether this {@link FedoraResource} has a frozen node
     */
    public static Predicate<FedoraResource> isFrozenNode = f -> f.hasType(FROZEN_NODE) ||
            f.getPath().contains(JCR_FROZEN_NODE);

    /**
     * Predicate for determining whether this {@link Node} is a Fedora Skolem node.
     */
    public static Predicate<Node> isSkolemNode = new AnyTypesPredicate(FEDORA_SKOLEM);

    /**
     * Check if a property is a reference property.
     */
    public static Predicate<Property> isInternalReferenceProperty = uncheck(p -> (p.getType() == REFERENCE ||
            p.getType() == WEAKREFERENCE) &&
            p.getName().endsWith(REFERENCE_PROPERTY_SUFFIX));

    /**
     *  Check whether a type should be internal.
     */
    public static Predicate<String> hasInternalNamespace = type ->
        type.startsWith("jcr:") || type.startsWith("mode:") || type.startsWith("nt:") ||
            type.startsWith("mix:");

    /**
     * Predicate for determining whether a JCR property should be converted to the fedora namespace.
     */
    public static Predicate<String> isPublicJcrProperty = validJcrProperties::contains;

    /**
     * Check whether a property is protected (ie, cannot be modified directly) but
     * is not one we've explicitly chosen to include.
     */
    private static Predicate<Property> isProtectedAndShouldBeHidden = uncheck(p -> {
        if (!p.getDefinition().isProtected()) {
            return false;
        } else if (p.getParent().isNodeType(FROZEN_NODE)) {
            // everything on a frozen node is protected
            // but we wish to display it anyway and there's
            // another mechanism in place to make clear that
            // things cannot be edited.
            return false;
        } else if (isPublicJcrProperty.test(p.getName())) {
            return false;
        }
        return hasInternalNamespace.test(p.getName());
    });

    /**
    * Check whether a property is an internal property that should be suppressed
    * from external output.
    */
    public static Predicate<Property> isInternalProperty = isBinaryContentProperty
                            .or(isProtectedAndShouldBeHidden::test)
                            .or(uncheck(p -> privateProperties.contains(p.getName())));

    /**
     * Check if a node is "internal" and should not be exposed e.g. via the REST
     * API
     */
    public static Predicate<Node> isInternalNode = uncheck(n -> n.isNodeType("mode:system"));

    /**
     * Check if a node is externally managed.
     *
     * Note: modeshape uses a source-workspace-identifier scheme
     * to identify whether a node is externally-managed.
     * Ordinary (non-external) nodes will have simple UUIDs
     * as an identifier. These are never external nodes.
     *
     * External nodes will have a 7-character hex code
     * identifying the "source", followed by another
     * 7-character hex code identifying the "workspace", followed
     * by a "/" and then the rest of the "identifier".
     *
     * Following that scheme, if a node's "source" key does not
     * match the repository's configured store name, then it is an
     * external node.
     */
    public static Predicate<Node> isExternalNode = uncheck(n ->  {
        if (NodeKey.isValidRandomIdentifier(n.getIdentifier())) {
            return false;
        } else if (n.getPrimaryNodeType().getName().equals(ROOT)) {
            return false;
        } else {
            final NodeKey key = new NodeKey(n.getIdentifier());
            final String source = NodeKey.keyForSourceName(
                    ((JcrRepository)n.getSession().getRepository()).getConfiguration().getName());
            return !key.getSourceKey().equals(source);
        }
    });

    /**
     * Get the JCR property type ID for a given property name. If unsure, mark
     * it as UNDEFINED.
     *
     * @param node the JCR node to add the property on
     * @param propertyName the property name
     * @return a PropertyType value
     * @throws RepositoryException if repository exception occurred
     */
    public static Optional<Integer> getPropertyType(final Node node, final String propertyName)
            throws RepositoryException {
        LOGGER.debug("Getting type of property: {} from node: {}", propertyName, node);
        return getDefinitionForPropertyName(node, propertyName).map(PropertyDefinition::getRequiredType);
    }

    /**
     * Determine if a given JCR property name is single- or multi- valued.
     * If unsure, choose the least restrictive option (multivalued = true)
     *
     * @param node the JCR node to check
     * @param propertyName the property name (which may or may not already exist)
     * @return true if the property is multivalued
     * @throws RepositoryException if repository exception occurred
     */
    public static boolean isMultivaluedProperty(final Node node, final String propertyName)
            throws RepositoryException {
        return getDefinitionForPropertyName(node, propertyName).map(PropertyDefinition::isMultiple).orElse(true);
    }

    /**
     * Get the property definition information (containing type and multi-value
     * information)
     *
     * @param node the node to use for inferring the property definition
     * @param propertyName the property name to retrieve a definition for
     * @return a JCR PropertyDefinition, if available
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public static Optional<PropertyDefinition> getDefinitionForPropertyName(final Node node, final String propertyName)
            throws RepositoryException {
        LOGGER.debug("Looking for property name: {}", propertyName);
        final Predicate<PropertyDefinition> sameName = p -> propertyName.equals(p.getName());

        final PropertyDefinition[] propDefs = node.getPrimaryNodeType().getPropertyDefinitions();
        final Optional<PropertyDefinition> primaryCandidate = stream(propDefs).filter(sameName).findFirst();
        return primaryCandidate.isPresent() ? primaryCandidate :
                stream(node.getMixinNodeTypes()).map(NodeType::getPropertyDefinitions).flatMap(Arrays::stream)
                        .filter(sameName).findFirst();
    }

    /**
     * When we add certain URI properties, we also want to leave a reference node
     * @param propertyName the property name
     * @return property name as a reference
     */
    public static String getReferencePropertyName(final String propertyName) {
        return propertyName + REFERENCE_PROPERTY_SUFFIX;
    }

    /**
     * Given an internal reference node property, get the original name
     * @param refPropertyName the reference node property name
     * @return original property name of the reference property
     */
    public static String getReferencePropertyOriginalName(final String refPropertyName) {
        final int i = refPropertyName.lastIndexOf(REFERENCE_PROPERTY_SUFFIX);
        return i < 0 ? refPropertyName : refPropertyName.substring(0, i);
    }

    /**
     * Check if a property definition is a reference property
     * @param node the given node
     * @param propertyName the property name
     * @return whether a property definition is a reference property
     * @throws RepositoryException if repository exception occurred
     */
    public static boolean isReferenceProperty(final Node node, final String propertyName) throws RepositoryException {
        final Optional<PropertyDefinition> propertyDefinition = getDefinitionForPropertyName(node, propertyName);

        return propertyDefinition.isPresent() &&
                (propertyDefinition.get().getRequiredType() == REFERENCE
                        || propertyDefinition.get().getRequiredType() == WEAKREFERENCE);
    }


    /**
     * Get the closest ancestor that current exists
     *
     * @param session the given session
     * @param path the given path
     * @return the closest ancestor that current exists
     * @throws RepositoryException if repository exception occurred
     */
    public static Node getClosestExistingAncestor(final Session session, final String path)
            throws RepositoryException {

        String potentialPath = path.startsWith("/") ? path : "/" + path;
        while (!potentialPath.isEmpty()) {
            if (session.nodeExists(potentialPath)) {
                return session.getNode(potentialPath);
            }
            potentialPath = potentialPath.substring(0, potentialPath.lastIndexOf('/'));
        }
        return session.getRootNode();
    }

    /**
     * Retrieve the underlying JCR Node from the FedoraResource
     *
     * @param resource the Fedora resource
     * @return the JCR Node
     */
    public static Node getJcrNode(final FedoraResource resource) {
        if (resource instanceof FedoraResourceImpl) {
            return ((FedoraResourceImpl)resource).getNode();
        }
        throw new IllegalArgumentException("FedoraResource is of the wrong type");
    }

    /**
     * Given a JCR Node, fetch the parent's ldp:insertedContentRelation value, if
     * one exists.
     *
     * @param node the JCR Node
     * @return the ldp:insertedContentRelation Resource, if one exists.
     */
    public static Optional<Resource> ldpInsertedContentProperty(final Node node) {
        return getContainingNode(node).filter(uncheck(parent -> parent.hasProperty(LDP_MEMBER_RESOURCE) &&
                parent.isNodeType(LDP_INDIRECT_CONTAINER) && parent.hasProperty(LDP_INSERTED_CONTENT_RELATION)))
            .map(UncheckedFunction.uncheck(parent ->
                        createResource(parent.getProperty(LDP_INSERTED_CONTENT_RELATION).getString())));
    }

    /**
     * Using a JCR session, return a function that maps an RDF Resource to a corresponding property name.
     *
     * @param session The JCR session
     * @return a Function that maps a Resource to an Optional-wrapped String
     */
    public static Function<Resource, Optional<String>> resourceToProperty(final Session session) {
        return resource -> {
            try {
                final NamespaceRegistry registry = getNamespaceRegistry(session);
                return Optional.of(registry.getPrefix(resource.getNameSpace()) + ":" + resource.getLocalName());
            } catch (final RepositoryException ex) {
                LOGGER.debug("Could not resolve resource namespace ({}): {}", resource.toString(), ex.getMessage());
            }
            return empty();
        };
    }


    /**
     * Update the fedora:lastModified date of the parent's ldp:membershipResource if that node is a direct
     * or indirect container, provided the LDP constraints are valid.
     *
     * @param node The JCR node
     */
    public static void touchLdpMembershipResource(final Node node) {
        getContainingNode(node).filter(uncheck(parent -> parent.hasProperty(LDP_MEMBER_RESOURCE))).ifPresent(parent -> {
            try {
                final Optional<String> hasInsertedContentProperty = ldpInsertedContentProperty(node)
                        .flatMap(resourceToProperty(node.getSession())).filter(uncheck(node::hasProperty));
                if (parent.isNodeType(LDP_DIRECT_CONTAINER) ||
                        (parent.isNodeType(LDP_INDIRECT_CONTAINER) && hasInsertedContentProperty.isPresent())) {
                    touch(parent.getProperty(LDP_MEMBER_RESOURCE).getNode());
                }
            } catch (final javax.jcr.AccessDeniedException ex) {
                throw new AccessDeniedException(ex);
            } catch (final RepositoryException ex) {
                throw new RepositoryRuntimeException(ex);
            }
        });
    }

    /**
     * Update the fedora:lastModified date of the node.
     *
     * @param node The JCR node
     */
    public static void touch(final Node node) {
        try {
            node.setProperty(FEDORA_LASTMODIFIED, getInstance(getTimeZone("UTC")));
        } catch (final javax.jcr.AccessDeniedException ex) {
            throw new AccessDeniedException(ex);
        } catch (final RepositoryException ex) {
            throw new RepositoryRuntimeException(ex);
        }
    }

    /**
     * Get the JCR Node that corresponds to the containing node in the repository.
     * This may be the direct parent node, but it may also be a more distant ancestor.
     *
     * @param node the JCR node
     * @return the containing node, if one is present
     */
    public static Optional<Node> getContainingNode(final Node node) {
        try {
            if (node.getDepth() == 0) {
                return empty();
            }

            final Node parent = node.getParent();
            if (parent.isNodeType(FEDORA_PAIRTREE) || parent.isNodeType(FEDORA_NON_RDF_SOURCE_DESCRIPTION)) {
                return getContainingNode(parent);
            }
            return Optional.of(parent);
        } catch (final RepositoryException ex) {
            throw new RepositoryRuntimeException(ex);
        }
    }
}
