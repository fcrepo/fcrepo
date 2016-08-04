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
package org.fcrepo.kernel.modeshape;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static com.hp.hpl.jena.update.UpdateAction.execute;
import static com.hp.hpl.jena.update.UpdateFactory.create;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedNamespace;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RequiredRdfContext.EMBED_RESOURCES;
import static org.fcrepo.kernel.api.RequiredRdfContext.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RequiredRdfContext.LDP_CONTAINMENT;
import static org.fcrepo.kernel.api.RequiredRdfContext.LDP_MEMBERSHIP;
import static org.fcrepo.kernel.api.RequiredRdfContext.MINIMAL;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.RequiredRdfContext.SERVER_MANAGED;
import static org.fcrepo.kernel.api.RequiredRdfContext.VERSIONS;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_CREATED;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.fcrepo.kernel.modeshape.services.functions.JcrPropertyFunctions.property2values;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getContainingNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.hasInternalNamespace;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isFrozenNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isFrozenResource;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isInternalNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.ldpInsertedContentProperty;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.resourceToProperty;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.touchLdpMembershipResource;
import static org.fcrepo.kernel.modeshape.utils.NamespaceTools.getNamespaceRegistry;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import static org.fcrepo.kernel.modeshape.utils.UncheckedFunction.uncheck;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.NamespaceRegistry;
import javax.jcr.version.VersionManager;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.graph.Triple;

import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.InvalidPrefixException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.functions.Converter;
import org.fcrepo.kernel.modeshape.identifiers.InternalPathToNodeConverter;
import org.fcrepo.kernel.modeshape.rdf.converters.PropertyConverter;
import org.fcrepo.kernel.api.TripleCategory;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.utils.GraphDifferencer;
import org.fcrepo.kernel.modeshape.rdf.impl.AclRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.ChildrenRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.ContentRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.HashRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.LdpContainerRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.LdpIsMemberOfRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.LdpRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.ParentRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.TypeRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.RootRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.SkolemNodeRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.VersionsRdfContext;
import org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils;
import org.fcrepo.kernel.modeshape.utils.JcrPropertyStatementListener;
import org.fcrepo.kernel.modeshape.utils.PropertyChangedListener;
import org.fcrepo.kernel.modeshape.utils.UncheckedPredicate;
import org.fcrepo.kernel.modeshape.utils.iterators.RdfAdder;
import org.fcrepo.kernel.modeshape.utils.iterators.RdfRemover;

import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.modify.request.UpdateData;
import com.hp.hpl.jena.sparql.modify.request.UpdateDeleteWhere;
import com.hp.hpl.jena.sparql.modify.request.UpdateModify;
import com.hp.hpl.jena.update.UpdateRequest;

/**
 * Common behaviors across {@link org.fcrepo.kernel.api.models.Container} and
 * {@link org.fcrepo.kernel.api.models.NonRdfSourceDescription} types; also used
 * when the exact type of an object is irrelevant
 *
 * @author ajs6f
 */
public class FedoraResourceImpl extends JcrTools implements FedoraTypes, FedoraResource {

    private static final Logger LOGGER = getLogger(FedoraResourceImpl.class);

    private static final long NO_TIME = 0L;
    private static final String JCR_CHILD_VERSION_HISTORY = "jcr:childVersionHistory";
    private static final String JCR_VERSIONABLE_UUID = "jcr:versionableUuid";
    private static final String JCR_FROZEN_UUID = "jcr:frozenUuid";
    private static final String JCR_VERSION_STORAGE = "jcr:versionStorage";

    private static final PropertyConverter propertyConverter = new PropertyConverter();

    // A curried type accepting resource, translator, and "minimality", returning triples.
    private static interface RdfGenerator extends Function<FedoraResource,
    Function<Converter<Resource, String>, Function<Boolean, Stream<Triple>>>> {}

    @SuppressWarnings("resource")
    private static RdfGenerator getDefaultTriples = resource -> translator -> uncheck(minimal -> {
        final Stream<Stream<Triple>> min = of(
            new TypeRdfContext(resource, translator),
            new PropertiesRdfContext(resource, translator));
        if (!minimal) {
            final Stream<Stream<Triple>> extra = of(
                new HashRdfContext(resource, translator),
                new SkolemNodeRdfContext(resource, translator));
            return concat(min, extra).reduce(empty(), Stream::concat);
        }
        return min.reduce(empty(), Stream::concat);
    });

    private static RdfGenerator getEmbeddedResourceTriples = resource -> translator -> uncheck(minimal ->
            resource.getChildren().flatMap(child -> child.getTriples(translator, PROPERTIES)));

    private static RdfGenerator getInboundTriples = resource -> translator -> uncheck(_minimal -> {
        return new ReferencesRdfContext(resource, translator);
    });

    private static RdfGenerator getLdpContainsTriples = resource -> translator -> uncheck(_minimal -> {
        return new ChildrenRdfContext(resource, translator);
    });

    private static RdfGenerator getVersioningTriples = resource -> translator -> uncheck(_minimal -> {
        return new VersionsRdfContext(resource, translator);
    });

    @SuppressWarnings("resource")
    private static RdfGenerator getServerManagedTriples = resource -> translator -> uncheck(minimal -> {
        if (minimal) {
            return new LdpRdfContext(resource, translator);
        }
        final Stream<Stream<Triple>> streams = of(
            new LdpRdfContext(resource, translator),
            new AclRdfContext(resource, translator),
            new RootRdfContext(resource, translator),
            new ContentRdfContext(resource, translator),
            new ParentRdfContext(resource, translator));
        return streams.reduce(empty(), Stream::concat);
    });

    @SuppressWarnings("resource")
    private static RdfGenerator getLdpMembershipTriples = resource -> translator -> uncheck(_minimal -> {
        final Stream<Stream<Triple>> streams = of(
            new LdpContainerRdfContext(resource, translator),
            new LdpIsMemberOfRdfContext(resource, translator));
        return streams.reduce(empty(), Stream::concat);
    });

    private static final Map<TripleCategory, RdfGenerator> contextMap =
            ImmutableMap.<TripleCategory, RdfGenerator>builder()
                    .put(PROPERTIES, getDefaultTriples)
                    .put(VERSIONS, getVersioningTriples)
                    .put(EMBED_RESOURCES, getEmbeddedResourceTriples)
                    .put(INBOUND_REFERENCES, getInboundTriples)
                    .put(SERVER_MANAGED, getServerManagedTriples)
                    .put(LDP_MEMBERSHIP, getLdpMembershipTriples)
                    .put(LDP_CONTAINMENT, getLdpContainsTriples)
                    .build();

    protected Node node;

    /**
     * Construct a {@link org.fcrepo.kernel.api.models.FedoraResource} from an existing JCR Node
     * @param node an existing JCR node to treat as an fcrepo object
     */
    public FedoraResourceImpl(final Node node) {
        this.node = node;
    }

    /**
     * Return the underlying JCR Node for this resource
     *
     * @return the JCR Node
     */
    public Node getNode() {
        return node;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getPath()
     */
    @Override
    public String getPath() {
        try {
            final String path = node.getPath();
            return path.endsWith("/" + JCR_CONTENT) ? path.substring(0, path.length() - JCR_CONTENT.length() - 1)
                : path;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getChildren(Boolean recursive)
     */
    @Override
    public Stream<FedoraResource> getChildren(final Boolean recursive) {
        try {
            if (recursive) {
                return nodeToGoodChildren(node).flatMap(FedoraResourceImpl::getAllChildren);
            }
            return nodeToGoodChildren(node);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getDescription()
     */
    @Override
    public FedoraResource getDescription() {
        return this;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getDescribedResource()
     */
    @Override
    public FedoraResource getDescribedResource() {
        return this;
    }

    /**
     * Get the "good" children for a node by skipping all pairtree nodes in the way.
     * @param input
     * @return
     * @throws RepositoryException
     */
    @SuppressWarnings("unchecked")
    private Stream<FedoraResource> nodeToGoodChildren(final Node input) throws RepositoryException {
        return iteratorToStream(input.getNodes()).filter(nastyChildren.negate())
            .flatMap(uncheck((final Node child) -> child.isNodeType(FEDORA_PAIRTREE) ? nodeToGoodChildren(child) :
                        of(nodeToObjectBinaryConverter.apply(child))));
    }

    /**
     * Get all children recursively, and flatten into a single Stream.
     */
    private static Stream<FedoraResource> getAllChildren(final FedoraResource resource) {
        return concat(of(resource), resource.getChildren().flatMap(FedoraResourceImpl::getAllChildren));
    }

    /**
     * Children for whom we will not generate triples.
     */
    private static Predicate<Node> nastyChildren = isInternalNode
                    .or(TombstoneImpl::hasMixin)
                    .or(UncheckedPredicate.uncheck(p -> p.getName().equals(JCR_CONTENT)))
                    .or(UncheckedPredicate.uncheck(p -> p.getName().equals("#")));

    private static final Converter<FedoraResource, FedoraResource> datastreamToBinary
            = new Converter<FedoraResource, FedoraResource>() {

        @Override
        public FedoraResource apply(final FedoraResource fedoraResource) {
            return fedoraResource.getDescribedResource();
        }

        @Override
        public FedoraResource toDomain(final FedoraResource fedoraResource) {
            return fedoraResource.getDescription();
        }

        @Override
        public boolean inDomain(final FedoraResource a) {
            return a != null;
        }
    };

    private static final Converter<Node, FedoraResource> nodeToObjectBinaryConverter
            = nodeConverter.andThen(datastreamToBinary);

    @Override
    public FedoraResource getContainer() {
        return getContainingNode(getNode()).map(nodeConverter::apply).orElse(null);
    }

    @Override
    public FedoraResource getChild(final String relPath) {
        try {
            return nodeConverter.apply(getNode().getNode(relPath));
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public boolean hasProperty(final String relPath) {
        try {
            return getNode().hasProperty(relPath);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public void delete() {
        try {
            @SuppressWarnings("unchecked")
            final Iterator<Property> references = node.getReferences();
            @SuppressWarnings("unchecked")
            final Iterator<Property> weakReferences = node.getWeakReferences();
            concat(iteratorToStream(references), iteratorToStream(weakReferences)).forEach(prop -> {
                try {
                    final List<Value> newVals = property2values.apply(prop).filter(
                            UncheckedPredicate.uncheck(value ->
                                !node.equals(getSession().getNodeByIdentifier(value.getString()))))
                        .collect(toList());

                    if (newVals.size() == 0) {
                        prop.remove();
                    } else {
                        prop.setValue(newVals.toArray(new Value[newVals.size()]));
                    }
                } catch (final RepositoryException ex) {
                    // Ignore error from trying to update properties on versioned resources
                    if (ex instanceof javax.jcr.nodetype.ConstraintViolationException &&
                            ex.getMessage().contains(JCR_VERSION_STORAGE)) {
                        LOGGER.debug("Ignoring exception trying to remove property from versioned resource: {}",
                                ex.getMessage());
                    } else {
                        throw new RepositoryRuntimeException(ex);
                    }
                }
            });

            final Node parent = getNode().getDepth() > 0 ? getNode().getParent() : null;

            final String name = getNode().getName();

            // This is resolved immediately b/c we delete the node before updating an indirect container's target
            final boolean shouldUpdateIndirectResource = ldpInsertedContentProperty(node)
                .flatMap(resourceToProperty(getSession())).filter(this::hasProperty).isPresent();

            final Optional<Node> containingNode = getContainingNode(getNode());

            node.remove();

            if (parent != null) {
                createTombstone(parent, name);

                // also update membershipResources for Direct/Indirect Containers
                containingNode.filter(UncheckedPredicate.uncheck((final Node ancestor) ->
                            ancestor.hasProperty(LDP_MEMBER_RESOURCE) && (ancestor.isNodeType(LDP_DIRECT_CONTAINER) ||
                            shouldUpdateIndirectResource)))
                    .ifPresent(ancestor -> {
                        try {
                            FedoraTypesUtils.touch(ancestor.getProperty(LDP_MEMBER_RESOURCE).getNode());
                        } catch (final RepositoryException ex) {
                            throw new RepositoryRuntimeException(ex);
                        }
                    });
            }
        } catch (final javax.jcr.AccessDeniedException e) {
            throw new AccessDeniedException(e);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private void createTombstone(final Node parent, final String path) throws RepositoryException {
        findOrCreateChild(parent, path, FEDORA_TOMBSTONE);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getCreatedDate()
     */
    @Override
    public Date getCreatedDate() {
        try {
            if (hasProperty(JCR_CREATED)) {
                return new Date(getTimestamp(JCR_CREATED, NO_TIME));
            }
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        LOGGER.debug("Node {} does not have a createdDate", node);
        return null;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getLastModifiedDate()
     */

    /**
     * This method gets the last modified date for this FedoraResource.  Because
     * the last modified date is managed by fcrepo (not ModeShape) while the created
     * date *is* managed by ModeShape in the current implementation it's possible that
     * the last modified date will be before the created date.  Instead of making
     * a second update to correct the modified date, in cases where the modified
     * date is ealier than the created date, this class presents the created date instead.
     *
     * Any method that exposes the last modified date must maintain this illusion so
     * that that external callers are presented with a sensible and consistent
     * representation of this resource.
     * @return the last modified Date (or the created date if it was after the last
     *         modified date)
     */
    @Override
    public Date getLastModifiedDate() {

        final Date createdDate = getCreatedDate();
        try {
            final long created = createdDate == null ? NO_TIME : createdDate.getTime();
            if (hasProperty(FEDORA_LASTMODIFIED)) {
                return new Date(getTimestamp(FEDORA_LASTMODIFIED, created));
            } else if (hasProperty(JCR_LASTMODIFIED)) {
                return new Date(getTimestamp(JCR_LASTMODIFIED, created));
            }
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        LOGGER.debug("Could not get last modified date property for node {}", node);

        if (createdDate != null) {
            LOGGER.trace("Using created date for last modified date for node {}", node);
            return createdDate;
        }

        return null;
    }

    private long getTimestamp(final String property, final long created) throws RepositoryException {
        LOGGER.trace("Using {} date", property);
        final long timestamp = getProperty(property).getDate().getTimeInMillis();
        if (timestamp < created && created > NO_TIME) {
            LOGGER.trace("Returning the later created date ({} > {}) for {}", created, timestamp, property);
            return created;
        }
        return timestamp;
    }

    /**
     * Set the last-modified date to the current date.
     */
    public void touch() {
        FedoraTypesUtils.touch(getNode());
    }

    @Override
    public boolean hasType(final String type) {
        return FedoraTypesUtils.typePredicate(type).test(node);
    }

    @Override
    public List<URI> getTypes() {
        try {
            final List<NodeType> nodeTypes = new ArrayList<>();
            final NodeType primaryNodeType = node.getPrimaryNodeType();
            nodeTypes.add(primaryNodeType);
            nodeTypes.addAll(asList(primaryNodeType.getSupertypes()));
            final List<NodeType> mixinTypes = asList(node.getMixinNodeTypes());

            nodeTypes.addAll(mixinTypes);
            mixinTypes.stream()
                .map(NodeType::getSupertypes)
                .flatMap(Arrays::stream)
                .forEach(nodeTypes::add);

            final List<URI> types = nodeTypes.stream()
                .map(uncheck(NodeType::getName))
                .filter(hasInternalNamespace.negate())
                .distinct()
                .map(nodeTypeNameToURI)
                .peek(x -> LOGGER.debug("node has rdf:type {}", x))
                .collect(Collectors.toList());

            if (isFrozenResource()) {
                types.add(URI.create(REPOSITORY_NAMESPACE + "Version"));
            }

            return types;

        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private final Function<String, URI> nodeTypeNameToURI = uncheck(name -> {
        final String prefix = name.split(":")[0];
        final String typeName = name.split(":")[1];
        final String namespace = getSession().getWorkspace().getNamespaceRegistry().getURI(prefix);
        return URI.create(getRDFNamespaceForJcrNamespace(namespace) + typeName);
    });

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#updateProperties
     *     (org.fcrepo.kernel.api.identifiers.IdentifierConverter, java.lang.String, RdfStream)
     */
    @Override
    public void updateProperties(final Converter<Resource, String> idTranslator,
                                 final String sparqlUpdateStatement, final RdfStream originalTriples)
            throws MalformedRdfException, AccessDeniedException {

        final Model model = originalTriples.collect(toModel());

        final Resource resourceSubject = graphResource(idTranslator);

        final UpdateRequest request = create(sparqlUpdateStatement,
                resourceSubject.toString());

        final Collection<IllegalArgumentException> errors = checkInvalidPredicates(request);

        final NamespaceRegistry namespaceRegistry = getNamespaceRegistry(getSession());

        request.getPrefixMapping().getNsPrefixMap().forEach(
            (k,v) -> {
                try {
                    LOGGER.debug("Prefix mapping is key:{} -> value:{}", k, v);
                    if (Arrays.asList(namespaceRegistry.getPrefixes()).contains(k)
                        &&  !v.equals(namespaceRegistry.getURI(k))) {

                        final String namespaceURI = namespaceRegistry.getURI(k);
                        LOGGER.debug("Prefix has already been defined: {}:{}", k, namespaceURI);
                        throw new InvalidPrefixException("Prefix already exists as: " + k + " -> " + namespaceURI);
                   }

                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
           });

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.stream().map(Exception::getMessage).collect(joining(",\n")));
        }

        final JcrPropertyStatementListener listener = new JcrPropertyStatementListener(
                idTranslator, getSession(), resourceSubject.asNode());

        model.register(listener);

        // If this resource's structural parent is an IndirectContainer, check whether the
        // ldp:insertedContentRelation property is present in the stream of changed triples.
        // If so, set the propertyChanged value to true.
        final AtomicBoolean propertyChanged = new AtomicBoolean();
        ldpInsertedContentProperty(getNode()).ifPresent(resource -> {
            model.register(new PropertyChangedListener(resource, propertyChanged));
        });

        model.setNsPrefixes(request.getPrefixMapping());
        execute(request, model);

        removeEmptyFragments();

        listener.assertNoExceptions();

        // Update the fedora:lastModified property
        touch();

        // Update the fedora:lastModified property of the ldp:memberResource
        // resource, if necessary.
        if (propertyChanged.get()) {
            touchLdpMembershipResource(getNode());
        }
    }

    @Override
    public RdfStream getTriples(final Converter<Resource, String> idTranslator,
                                final TripleCategory context) {
        return getTriples(idTranslator, singleton(context));
    }

    @Override
    public RdfStream getTriples(final Converter<Resource, String> idTranslator,
                                final Set<? extends TripleCategory> contexts) {

        final Converter<Resource, Node> toNodes =
                idTranslator.andThen(new InternalPathToNodeConverter(getSession()));
        //final Converter<Resource, FedoraResource> toResources = toNodes.andThen(NodeResourceConverter.nodeConverter);
        return new DefaultRdfStream(toNodes.toDomain(getNode()).asNode(), contexts.stream()
                .filter(contextMap::containsKey)
                .map(x -> contextMap.get(x).apply(this).apply(idTranslator).apply(contexts.contains(MINIMAL)))
                .reduce(empty(), Stream::concat));
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getBaseVersion()
     */
    @Override
    public Version getBaseVersion() {
        try {
            return getVersionManager().getBaseVersion(getPath());
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getVersionHistory()
     */
    @Override
    public VersionHistory getVersionHistory() {
        try {
            return getVersionManager().getVersionHistory(getPath());
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#isNew()
     */
    @Override
    public Boolean isNew() {
        return node.isNew();
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#replaceProperties
     *     (org.fcrepo.kernel.api.identifiers.IdentifierConverter, com.hp.hpl.jena.rdf.model.Model)
     */
    @Override
    public void replaceProperties(final Converter<Resource, String> idTranslator,
        final Model inputModel, final RdfStream originalTriples) throws MalformedRdfException {

        try (final RdfStream replacementStream =
                new DefaultRdfStream(this.graphResource(idTranslator).asNode())) {

            final GraphDifferencer differencer =
                new GraphDifferencer(inputModel, originalTriples);

            final StringBuilder exceptions = new StringBuilder();
            try (final DefaultRdfStream diffStream =
                    new DefaultRdfStream(replacementStream.topic(), differencer.difference())) {
                new RdfRemover(idTranslator, getSession(), diffStream).consume();
            } catch (final ConstraintViolationException e) {
                throw e;
            } catch (final MalformedRdfException e) {
                exceptions.append(e.getMessage());
                exceptions.append("\n");
            }

            try (final DefaultRdfStream notCommonStream =
                    new DefaultRdfStream(replacementStream.topic(), differencer.notCommon())) {
                new RdfAdder(idTranslator, getSession(), notCommonStream).consume();
            } catch (final ConstraintViolationException e) {
                throw e;
            } catch (final MalformedRdfException e) {
                exceptions.append(e.getMessage());
            }

            // If this resource's structural parent is an IndirectContainer, check whether the
            // ldp:insertedContentRelation property is present in the stream of changed triples.
            // If so, set the propertyChanged value to true.
            final AtomicBoolean propertyChanged = new AtomicBoolean();
            ldpInsertedContentProperty(getNode()).ifPresent(resource -> {
                propertyChanged.set(differencer.notCommon().map(Triple::getPredicate).anyMatch(resource::equals));
            });

            removeEmptyFragments();

            if (exceptions.length() > 0) {
                throw new MalformedRdfException(exceptions.toString());
            }

            // Update the fedora:lastModified property
            touch();

            // If the ldp:insertedContentRelation property was changed, update the
            // ldp:membershipResource resource.
            if (propertyChanged.get()) {
                touchLdpMembershipResource(getNode());
            }
        }
    }

    private void removeEmptyFragments() {
        try {
            if (node.hasNode("#")) {
                @SuppressWarnings("unchecked")
                final Iterator<Node> nodes = node.getNode("#").getNodes();
                nodes.forEachRemaining(n -> {
                    try {
                        @SuppressWarnings("unchecked")
                        final Iterator<Property> properties = n.getProperties();
                        final boolean hasUserProps = iteratorToStream(properties).map(propertyConverter::convert)
                            .anyMatch(isManagedPredicate.negate());

                        final boolean hasUserTypes = Arrays.stream(n.getMixinNodeTypes())
                            .map(uncheck(NodeType::getName)).filter(hasInternalNamespace.negate())
                            .map(uncheck(type ->
                                getSession().getWorkspace().getNamespaceRegistry().getURI(type.split(":")[0])))
                            .anyMatch(isManagedNamespace.negate());

                        if (!hasUserProps && !hasUserTypes && !n.getWeakReferences().hasNext() &&
                                !n.getReferences().hasNext()) {
                            LOGGER.debug("Removing empty hash URI node: {}", n.getName());
                            n.remove();
                        }
                    } catch (final RepositoryException ex) {
                        throw new RepositoryRuntimeException("Error removing empty fragments", ex);
                    }
                });
            }
        } catch (final RepositoryException ex) {
            throw new RepositoryRuntimeException("Error removing empty fragments", ex);
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getEtagValue()
     */
    @Override
    public String getEtagValue() {
        final Date lastModifiedDate = getLastModifiedDate();

        if (lastModifiedDate != null) {
            return shaHex(getPath() + lastModifiedDate.getTime());
        }
        return "";
    }

    @Override
    public void enableVersioning() {
        try {
            node.addMixin("mix:versionable");
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public void disableVersioning() {
        try {
            node.removeMixin("mix:versionable");
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

    }

    public static Predicate<Node> isVersioned =
            UncheckedPredicate.uncheck((final Node node) -> node.isNodeType("mix:versionable"));

    @Override
    public boolean isVersioned() {
        return isVersioned.test(node);
    }

    @Override
    public boolean isFrozenResource() {
        return isFrozenResource.test(this);
    }

    @Override
    public FedoraResource getVersionedAncestor() {

        try {
            if (!isFrozenResource()) {
                return null;
            }

            Node versionableFrozenNode = getNode();
            FedoraResource unfrozenResource = getUnfrozenResource();

            // traverse the frozen tree looking for a node whose unfrozen equivalent is versioned
            while (!unfrozenResource.isVersioned()) {

                if (versionableFrozenNode.getDepth() == 0) {
                    return null;
                }

                // node in the frozen tree
                versionableFrozenNode = versionableFrozenNode.getParent();

                // unfrozen equivalent
                unfrozenResource = new FedoraResourceImpl(versionableFrozenNode).getUnfrozenResource();
            }

            return new FedoraResourceImpl(versionableFrozenNode);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

    }

    /**
     * 
     * @param node
     * @return
     */
    public static Node getVersionedAncestor(final Node node) {

        try {
            if (!isFrozenNode.test(node)) {
                return null;
            }

            Node versionableFrozenNode = node;
            Node unfrozenNode = getUnfrozenNode(node);

            // traverse the frozen tree looking for a node whose unfrozen equivalent is versioned
            while (!isVersioned.test(unfrozenNode)) {

                if (versionableFrozenNode.getDepth() == 0) {
                    return null;
                }

                // node in the frozen tree
                versionableFrozenNode = versionableFrozenNode.getParent();

                // unfrozen equivalent
                unfrozenNode = getUnfrozenNode(versionableFrozenNode);
            }

            return versionableFrozenNode;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

    }

    @Override
    public FedoraResource getUnfrozenResource() {
        if (!isFrozenResource()) {
            return this;
        }

        try {
            // Either this resource is frozen
            if (hasProperty(JCR_FROZEN_UUID)) {
                try {
                    return new FedoraResourceImpl(getNodeByProperty(getProperty(JCR_FROZEN_UUID)));
                } catch (final ItemNotFoundException e) {
                    // The unfrozen resource has been deleted, return the tombstone.
                    return new TombstoneImpl(getNode());
                }

                // ..Or it is a child-version-history on a frozen path
            } else if (hasProperty(JCR_CHILD_VERSION_HISTORY)) {
                final Node childVersionHistory = getNodeByProperty(getProperty(JCR_CHILD_VERSION_HISTORY));
                try {
                    final Node childNode = getNodeByProperty(childVersionHistory.getProperty(JCR_VERSIONABLE_UUID));
                    return new FedoraResourceImpl(childNode);
                } catch (final ItemNotFoundException e) {
                    // The unfrozen resource has been deleted, return the tombstone.
                    return new TombstoneImpl(childVersionHistory);
                }

            } else {
                throw new RepositoryRuntimeException("Resource must be frozen or a child-history!");
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * 
     * @param input
     * @return
     */
    public static Node getUnfrozenNode(final Node input) {
        if (!FedoraTypesUtils.isFrozenNode.test(input)) {
            return input;
        }

        try {
            final Session session = input.getSession();
            // Either this resource is frozen
            if (input.hasProperty(JCR_FROZEN_UUID)) {
                try {
                    return getNodeByProperty(session, input.getProperty(JCR_FROZEN_UUID));
                } catch (final ItemNotFoundException e) {
                    // The unfrozen resource has been deleted, return the tombstone.
                    return input;
                }

                // ..Or it is a child-version-history on a frozen path
            } else if (input.hasProperty(JCR_CHILD_VERSION_HISTORY)) {
                final Node childVersionHistory = getNodeByProperty(session,
                        input.getProperty(JCR_CHILD_VERSION_HISTORY));
                try {
                    return getNodeByProperty(
                            session,
                            childVersionHistory.getProperty(JCR_VERSIONABLE_UUID));
                } catch (final ItemNotFoundException e) {
                    // The unfrozen resource has been deleted, return the tombstone.
                    return childVersionHistory;
                }

            } else {
                throw new RepositoryRuntimeException("Resource must be frozen or a child-history!");
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public FedoraResource getVersion(final String label) {
        try {
            final Node n = getFrozenNode(label);

            if (n != null) {
                return new FedoraResourceImpl(n);
            }

            if (isVersioned()) {
                final VersionHistory hist = getVersionManager().getVersionHistory(getPath());

                if (hist.hasVersionLabel(label)) {
                    LOGGER.debug("Found version for {} by label {}.", this, label);
                    return new FedoraResourceImpl(hist.getVersionByLabel(label).getFrozenNode());
                }
            }

            LOGGER.warn("Unknown version {} with label {}!", getPath(), label);
            return null;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

    }

    @Override
    public String getVersionLabelOfFrozenResource() {
        return getVersionLabelOfFrozenResource(node);
    }

    /**
     * 
     * @param node
     * @return
     */
    public static String getVersionLabelOfFrozenResource(final Node node) {
        if (!isFrozenNode.test(node)) {
            return null;
        }

        // Frozen node is required to find associated version label
        final Node frozenResource;
        try {
            final VersionManager versionManager = getVersionManager(node);
            // Version History associated with this resource
            final VersionHistory versionHistory = versionManager.getVersionHistory(getUnfrozenNode(node).getPath());

            final Session session = node.getSession();
            // Possibly the frozen node is nested inside of current child-version-history
            if (node.hasProperty(JCR_CHILD_VERSION_HISTORY)) {
                final Node childVersionHistory =
                        getNodeByProperty(session, node.getProperty(JCR_CHILD_VERSION_HISTORY));
                final Node childNode =
                        getNodeByProperty(session, childVersionHistory.getProperty(JCR_VERSIONABLE_UUID));
                final Version childVersion = versionManager.getBaseVersion(childNode.getPath());
                frozenResource = childVersion.getFrozenNode();

            } else {
                frozenResource = node;
            }

            // Loop versions
            @SuppressWarnings("unchecked")
            final Stream<Version> versions = iteratorToStream(versionHistory.getAllVersions());
            return versions
                .filter(UncheckedPredicate.uncheck(version -> version.getFrozenNode().equals(frozenResource)))
                .map(uncheck(versionHistory::getVersionLabels))
                .flatMap(Arrays::stream)
                .findFirst().orElse(null);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public Resource graphResource(final Converter<Resource, String> idTranslator) {
        return idTranslator.andThen(new InternalPathToNodeConverter(getSession())).toDomain(getNode());
    }

    private Node getNodeByProperty(final Property property) throws RepositoryException {
        return getSession().getNodeByIdentifier(property.getString());
    }

    private static Node getNodeByProperty(final Session session, final Property property) throws RepositoryException {
        return session.getNodeByIdentifier(property.getString());
    }

    protected VersionManager getVersionManager() {
        return getVersionManager(getNode());
    }

    protected static VersionManager getVersionManager(final Node node) {
        try {
            return node.getSession().getWorkspace().getVersionManager();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Helps ensure that there are no terminating slashes in the predicate.
     * A terminating slash means ModeShape has trouble extracting the localName, e.g., for
     * http://myurl.org/.
     *
     * @see <a href="https://jira.duraspace.org/browse/FCREPO-1409"> FCREPO-1409 </a> for details.
     */
    private static Collection<IllegalArgumentException> checkInvalidPredicates(final UpdateRequest request) {
        return request.getOperations().stream()
                .flatMap(x -> {
                    if (x instanceof UpdateModify) {
                        final UpdateModify y = (UpdateModify)x;
                        return concat(y.getInsertQuads().stream(), y.getDeleteQuads().stream());
                    } else if (x instanceof UpdateData) {
                        return ((UpdateData)x).getQuads().stream();
                    } else if (x instanceof UpdateDeleteWhere) {
                        return ((UpdateDeleteWhere)x).getQuads().stream();
                    } else {
                        return empty();
                    }
                })
                .filter(x -> x.getPredicate().isURI() && x.getPredicate().getURI().endsWith("/"))
                .map(x -> new IllegalArgumentException("Invalid predicate ends with '/': " + x.getPredicate().getURI()))
                .collect(Collectors.toList());
    }

    private Node getFrozenNode(final String label) throws RepositoryException {
        try {
            final Session session = getSession();

            final Node frozenNode = session.getNodeByIdentifier(label);

            final String baseUUID = getNode().getIdentifier();

            /*
             * We found a node whose identifier is the "label" for the version.  Now
             * we must do due dilligence to make sure it's a frozen node representing
             * a version of the subject node.
             */
            final Property p = frozenNode.getProperty(JCR_FROZEN_UUID);
            if (p != null) {
                if (p.getString().equals(baseUUID)) {
                    return frozenNode;
                }
            }
            /*
             * Though a node with an id of the label was found, it wasn't the
             * node we were looking for, so fall through and look for a labeled
             * node.
             */
        } catch (final ItemNotFoundException ex) {
            /*
             * the label wasn't a uuid of a frozen node but
             * instead possibly a version label.
             */
        }
        return null;
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof FedoraResourceImpl) {
            return ((FedoraResourceImpl) object).getNode().equals(this.getNode());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getNode().hashCode();
    }

    protected Session getSession() {
        try {
            return getNode().getSession();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return getNode().toString();
    }

    protected Property getProperty(final String relPath) {
        try {
            return getNode().getProperty(relPath);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * A method that takes a Triple and returns a Triple that is the correct representation of
     * that triple for the given resource.  The current implementation of this method is used by
     * {@link PropertiesRdfContext} to replace the reported {@link org.fcrepo.kernel.api.RdfLexicon#LAST_MODIFIED_DATE}
     * with the one produced by {@link #getLastModifiedDate}.
     * @param r the Fedora resource
     * @param translator a converter to get the external identifier from a jcr node
     * @return a function to convert triples
     */
    public static Function<Triple, Triple> fixDatesIfNecessary(final FedoraResource r,
                                                      final Converter<Node, Resource> translator) {
        return t -> {
            if (t.getPredicate().toString().equals(LAST_MODIFIED_DATE.toString())
                    && t.getSubject().equals(translator.apply(getJcrNode(r)).asNode())) {
                final Calendar c = Calendar.getInstance();
                c.setTime(r.getLastModifiedDate());
                return new Triple(t.getSubject(), t.getPredicate(), createTypedLiteral(c).asNode());
            }
            return t;
            };
    }

}
