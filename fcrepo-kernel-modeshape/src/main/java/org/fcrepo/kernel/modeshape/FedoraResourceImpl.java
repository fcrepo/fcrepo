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

import static com.google.common.net.MediaType.parse;
import static java.time.Instant.ofEpochMilli;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.apache.jena.update.UpdateAction.execute;
import static org.apache.jena.update.UpdateFactory.create;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfLexicon.INTERACTION_MODELS;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedNamespace;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.fcrepo.kernel.api.RdfLexicon.isRelaxed;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isMemento;
import static org.fcrepo.kernel.api.RequiredRdfContext.EMBED_RESOURCES;
import static org.fcrepo.kernel.api.RequiredRdfContext.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RequiredRdfContext.LDP_CONTAINMENT;
import static org.fcrepo.kernel.api.RequiredRdfContext.LDP_MEMBERSHIP;
import static org.fcrepo.kernel.api.RequiredRdfContext.MINIMAL;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.RequiredRdfContext.SERVER_MANAGED;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FROZEN_MIXIN_TYPES;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_CREATED;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.ROOT;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.jcrProperties;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.fcrepo.kernel.modeshape.services.functions.JcrPropertyFunctions.isFrozen;
import static org.fcrepo.kernel.modeshape.services.functions.JcrPropertyFunctions.property2values;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getContainingNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.hasInternalNamespace;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isInternalNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.ldpInsertedContentProperty;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.resourceToProperty;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.touchLdpMembershipResource;
import static org.fcrepo.kernel.modeshape.utils.NamespaceTools.getNamespaceRegistry;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import static org.fcrepo.kernel.modeshape.utils.UncheckedFunction.uncheck;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
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

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.FedoraVersion;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.TripleCategory;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.InvalidPrefixException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.InteractionModelViolationException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.utils.GraphDifferencer;
import org.fcrepo.kernel.api.utils.RelaxedPropertiesHelper;
import org.fcrepo.kernel.modeshape.rdf.converters.PropertyConverter;
import org.fcrepo.kernel.modeshape.rdf.impl.AclRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.ChildrenRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.ContentRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.HashRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.LdpContainerRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.LdpIsMemberOfRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.LdpRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.ParentRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.RootRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.SkolemNodeRdfContext;
import org.fcrepo.kernel.modeshape.rdf.impl.TypeRdfContext;
import org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils;
import org.fcrepo.kernel.modeshape.utils.FilteringJcrPropertyStatementListener;
import org.fcrepo.kernel.modeshape.utils.PropertyChangedListener;
import org.fcrepo.kernel.modeshape.utils.UncheckedPredicate;
import org.fcrepo.kernel.modeshape.utils.iterators.RdfAdder;
import org.fcrepo.kernel.modeshape.utils.iterators.RdfRemover;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.UpdateData;
import org.apache.jena.sparql.modify.request.UpdateDeleteWhere;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateRequest;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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

    private static final PropertyConverter propertyConverter = new PropertyConverter();

    public static final String LDPCV_TIME_MAP = "fedora:timemap";

    public static final String LDPCV_BINARY_TIME_MAP = "fedora:binaryTimemap";

    public static final String CONTAINER_WEBAC_ACL = "fedora:acl";

    // A curried type accepting resource, translator, and "minimality", returning triples.
    protected static interface RdfGenerator extends Function<FedoraResource,
    Function<IdentifierConverter<Resource, FedoraResource>, Function<Boolean, Stream<Triple>>>> {}

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

    protected static final Map<TripleCategory, RdfGenerator> contextMap =
            ImmutableMap.<TripleCategory, RdfGenerator>builder()
                    .put(PROPERTIES, getDefaultTriples)
                    .put(EMBED_RESOURCES, getEmbeddedResourceTriples)
                    .put(INBOUND_REFERENCES, getInboundTriples)
                    .put(SERVER_MANAGED, getServerManagedTriples)
                    .put(LDP_MEMBERSHIP, getLdpMembershipTriples)
                    .put(LDP_CONTAINMENT, getLdpContainsTriples)
                    .build();

    protected Node node;

    /*
     * A terminating slash means ModeShape has trouble extracting the localName, e.g., for http://myurl.org/.
     *
     * @see <a href="https://jira.duraspace.org/browse/FCREPO-1409"> FCREPO-1409 </a> for details.
     */
    private static final Function<Quad, IllegalArgumentException> validatePredicateEndsWithSlash = uncheck(x -> {
        if (x.getPredicate().isURI() && x.getPredicate().getURI().endsWith("/")) {
            return new IllegalArgumentException("Invalid predicate ends with '/': " + x.getPredicate().getURI());
        }
        return null;
    });

    /*
     * Ensures the object URI is valid
     */
    private static final Function<Quad, IllegalArgumentException> validateObjectUrl = uncheck(x -> {
        if (x.getObject().isURI()) {
            final String uri = x.getObject().toString();
            try {
                new URI(uri);
            } catch (final Exception ex) {
                return new IllegalArgumentException("Invalid object URI (" + uri + " ) : " + ex.getMessage());
            }
        }
        return null;
    });

    private static final Function<Quad, IllegalArgumentException> validateMimeTypeTriple = uncheck(x -> {

        /* only look at the mime type if it's not a sparql variable */
        if (x.getPredicate().toString().equals(RdfLexicon.HAS_MIME_TYPE.toString()) &&
                !x.getObject().toString(false).startsWith("?")) {
            try {
                parse(x.getObject().toString(false));
            } catch (final Exception ex) {
                return new IllegalArgumentException("Invalid value for '" + RdfLexicon.HAS_MIME_TYPE +
                        "' encountered : " + x.getObject().toString());
            }
        }
        return null;
    });

    private static final List<Function<Quad, IllegalArgumentException>> quadValidators =
            ImmutableList.<Function<Quad, IllegalArgumentException>>builder()
                    .add(validatePredicateEndsWithSlash)
                    .add(validateObjectUrl)
                    .add(validateMimeTypeTriple).build();

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

    protected Node getDescriptionNode() {
        return getNode();
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
                        of(nodeToObjectBinaryConverter.convert(child))));
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
                    .or(FedoraTimeMapImpl::hasMixin)
                    .or(FedoraWebacAclImpl::hasMixin)
                    .or(UncheckedPredicate.uncheck(p -> p.getName().equals(JCR_CONTENT)))
                    .or(UncheckedPredicate.uncheck(p -> p.getName().equals("#")));

    private static final Converter<FedoraResource, FedoraResource> datastreamToBinary
            = new Converter<FedoraResource, FedoraResource>() {

        @Override
        protected FedoraResource doForward(final FedoraResource fedoraResource) {
            return fedoraResource.getDescribedResource();
        }

        @Override
        protected FedoraResource doBackward(final FedoraResource fedoraResource) {
            return fedoraResource.getDescription();
        }
    };

    private static final Converter<Node, FedoraResource> nodeToObjectBinaryConverter
            = nodeConverter.andThen(datastreamToBinary);

    @Override
    public FedoraResource getContainer() {
        return getContainingNode(getNode()).map(nodeConverter::convert).orElse(null);
    }

    @Override
    public FedoraResource getTimeMap() {
        try {
            final Node timeMapNode;
            if (this instanceof FedoraBinary) {
                timeMapNode = getNode().getParent().getNode(LDPCV_BINARY_TIME_MAP);
            } else {
                timeMapNode = node.getNode(LDPCV_TIME_MAP);
            }
            return Optional.of(timeMapNode).map(nodeConverter::convert).orElse(null);
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public FedoraResource findOrCreateTimeMap() {
        final Node ldpcvNode;
        try {
            ldpcvNode = findOrCreateChild(getNode(), LDPCV_TIME_MAP, NT_FOLDER);

            if (ldpcvNode.isNew()) {
                LOGGER.debug("Created TimeMap LDPCv {}", ldpcvNode.getPath());

                // add mixin type fedora:Resource
                if (node.canAddMixin(FEDORA_RESOURCE)) {
                    node.addMixin(FEDORA_RESOURCE);
                }

                // add mixin type fedora:TimeMap
                if (ldpcvNode.canAddMixin(FEDORA_TIME_MAP)) {
                    ldpcvNode.addMixin(FEDORA_TIME_MAP);
                }

                // Set reference from timegate/map to original resource
                ldpcvNode.setProperty(MEMENTO_ORIGINAL, getNode());
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return Optional.of(ldpcvNode).map(nodeConverter::convert).orElse(null);
    }

    @Override
    public Instant getMementoDatetime() {
        try {
            final Node node = getNode();
            if (!isMemento() || !node.hasProperty(MEMENTO_DATETIME)) {
                return null;
            }

            final Calendar calDate = node.getProperty(MEMENTO_DATETIME).getDate();
            return calDate.toInstant();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public boolean isMemento() {
        return isMemento.test(getNode());
    }

    @Override
    public FedoraResource getAcl() {
        final Node parentNode;

        try {
            if (this instanceof FedoraBinary) {
                parentNode = getNode().getParent();
            } else {
                parentNode = getNode();
            }

            if (!parentNode.hasNode(CONTAINER_WEBAC_ACL)) {
                return null;
            }

            final Node aclNode = parentNode.getNode(CONTAINER_WEBAC_ACL);
            return Optional.of(aclNode).map(nodeConverter::convert).orElse(null);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public FedoraResource findOrCreateAcl() {
        final Node aclNode;
        try {
            final Node parentNode;
            if (this instanceof NonRdfSourceDescription) {
                parentNode = getNode().getParent();
            } else {
                parentNode = getNode();
            }

            aclNode = findOrCreateChild(parentNode, CONTAINER_WEBAC_ACL, NT_FOLDER);
            if (aclNode.isNew()) {
                LOGGER.debug("Created Webac ACL {}", aclNode.getPath());

                // add mixin type fedora:Resource
                if (node.canAddMixin(FEDORA_RESOURCE)) {
                    node.addMixin(FEDORA_RESOURCE);
                }

                // add mixin type webac:Acl
                if (aclNode.canAddMixin(FEDORA_WEBAC_ACL)) {
                    aclNode.addMixin(FEDORA_WEBAC_ACL);
                }
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return Optional.of(aclNode).map(nodeConverter::convert).orElse(null);
    }

    @Override
    public FedoraResource getChild(final String relPath) {
        try {
            return nodeConverter.convert(getNode().getNode(relPath));
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
            // Remove inbound references to this resource and, recursively, any of its children
            removeReferences(node);

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

                // update the lastModified date on the parent node
                containingNode.ifPresent(ancestor -> {
                    FedoraTypesUtils.touch(ancestor);
                });
            }
        } catch (final javax.jcr.AccessDeniedException e) {
            throw new AccessDeniedException(e);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    protected void removeReferences(final Node n) {
        try {
            // Remove references to this resource
            doRemoveReferences(n);

            // Recurse over children of this resource
            if (n.hasNodes()) {
                @SuppressWarnings("unchecked")
                final Iterator<Node> nodes = n.getNodes();
                nodes.forEachRemaining(this::removeReferences);
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private void doRemoveReferences(final Node n) throws RepositoryException {
        @SuppressWarnings("unchecked")
        final Iterator<Property> references = n.getReferences();
        @SuppressWarnings("unchecked")
        final Iterator<Property> weakReferences = n.getWeakReferences();
        concat(iteratorToStream(references), iteratorToStream(weakReferences)).forEach(prop -> {
            try {
                final List<Value> newVals = property2values.apply(prop).filter(
                        UncheckedPredicate.uncheck(value ->
                                !n.equals(getSession().getNodeByIdentifier(value.getString()))))
                        .collect(toList());

                if (newVals.size() == 0) {
                    prop.remove();
                } else {
                    prop.setValue(newVals.toArray(new Value[newVals.size()]));
                }
            } catch (final RepositoryException ex) {
                throw new RepositoryRuntimeException(ex);
            }
        });
    }

    private void createTombstone(final Node parent, final String path) throws RepositoryException {
        findOrCreateChild(parent, path, FEDORA_TOMBSTONE);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getCreatedDate()
     */
    @Override
    public Instant getCreatedDate() {
        try {
            if (hasProperty(FEDORA_CREATED)) {
                return ofEpochMilli(getTimestamp(FEDORA_CREATED, NO_TIME));
            }
            if (hasProperty(JCR_CREATED)) {
                return ofEpochMilli(getTimestamp(JCR_CREATED, NO_TIME));
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
     * date *is* sometimes managed by ModeShape in the current implementation it's
     * possible that the last modified date will be before the created date.  Instead
     * of making a second update to correct the modified date, in cases where the modified
     * date is ealier than the created date, this class presents the created date instead.
     *
     * Any method that exposes the last modified date must maintain this illusion so
     * that that external callers are presented with a sensible and consistent
     * representation of this resource.
     * @return the last modified Instant (or the created Instant if it was after the last
     *         modified date)
     */
    @Override
    public Instant getLastModifiedDate() {

        final Instant createdDate = getCreatedDate();
        try {
            final long created = createdDate == null ? NO_TIME : createdDate.toEpochMilli();
            if (hasProperty(FEDORA_LASTMODIFIED)) {
                return ofEpochMilli(getTimestamp(FEDORA_LASTMODIFIED, created));
            } else if (hasProperty(JCR_LASTMODIFIED)) {
                return ofEpochMilli(getTimestamp(JCR_LASTMODIFIED, created));
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

    @Override
    public boolean hasType(final String type) {
        try {
            if (type.equals(FEDORA_REPOSITORY_ROOT)) {
                return node.isNodeType(ROOT);
            } else if (isFrozen.test(node) && hasProperty(FROZEN_MIXIN_TYPES)) {
                return property2values.apply(getProperty(FROZEN_MIXIN_TYPES)).map(uncheck(Value::getString))
                    .anyMatch(type::equals);
            }
            return node.isNodeType(type);
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
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
    public void updateProperties(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                 final String sparqlUpdateStatement, final RdfStream originalTriples)
            throws MalformedRdfException, AccessDeniedException {

        final Model model = originalTriples.collect(toModel());

        final FedoraResource described = getDescribedResource();

        final UpdateRequest request = create(sparqlUpdateStatement,
                idTranslator.reverse().convert(described).toString());

        final Collection<IllegalArgumentException> errors = validateUpdateRequest(request);

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

        checkInteractionModel(request);

        final FilteringJcrPropertyStatementListener listener = new FilteringJcrPropertyStatementListener(
                idTranslator, getSession(), idTranslator.reverse().convert(described).asNode());

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

        try {
            touch(propertyChanged.get(), listener.getAddedCreatedDate(), listener.getAddedCreatedBy(),
                    listener.getAddedModifiedDate(), listener.getAddedModifiedBy());
        } catch (final RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Check the SPARQLUpdate statements for the violation of changing interaction model
     * @param request the UpdateRequest
     * @throws InteractionModelViolationException when attempting to change the interaction model
     */
    private void checkInteractionModel(final UpdateRequest request) {
        final List<Quad> deleteQuads = new ArrayList<>();
        final List<Quad> updateQuads = new ArrayList<>();

        for (final Update operation : request.getOperations()) {
            if (operation instanceof UpdateModify) {
                final UpdateModify op = (UpdateModify) operation;
                deleteQuads.addAll(op.getDeleteQuads());
                updateQuads.addAll(op.getInsertQuads());
            } else if (operation instanceof UpdateData) {
                final UpdateData op = (UpdateData) operation;
                updateQuads.addAll(op.getQuads());
            } else if (operation instanceof UpdateDeleteWhere) {
                final UpdateDeleteWhere op = (UpdateDeleteWhere) operation;
                deleteQuads.addAll(op.getQuads());
            }

            final Optional<String> resIxn = INTERACTION_MODELS.stream().filter(x -> hasType(x)).findFirst();
            if (resIxn.isPresent()) {
                updateQuads.stream().forEach(e -> {
                    final String ixn = getInteractionModel.apply(e.asTriple());
                    if (StringUtils.isNotBlank(ixn) && !ixn.equals(resIxn.get())) {
                        throw new InteractionModelViolationException("Changing the interaction model "
                            + resIxn.get() + " to " + ixn + " is not allowed!");
                    }
                });
            }

            deleteQuads.stream().forEach(e -> {
                final String ixn = getInteractionModel.apply(e.asTriple());
                if (StringUtils.isNotBlank(ixn)) {
                    throw new InteractionModelViolationException("Delete the interaction model "
                            + ixn + " is not allowed!");
                }
            });
        }
    }

    /*
     * Dynamic function to extract the interaction model from Triple.
     */
    private static final Function<Triple, String> getInteractionModel =
            uncheck( x -> {
                if (x.getPredicate().hasURI(RDF_NAMESPACE + "type") && x.getObject().isURI()
                        && INTERACTION_MODELS.contains((x.getObject().getURI().replace(LDP_NAMESPACE, "ldp:")))) {
                return x.getObject().getURI().replace(LDP_NAMESPACE, "ldp:");
            }
            return null;
    });

    @Override
    public RdfStream getTriples(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                final TripleCategory context) {
        return getTriples(idTranslator, singleton(context));
    }

    @Override
    public RdfStream getTriples(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                final Set<? extends TripleCategory> contexts) {

        return new DefaultRdfStream(idTranslator.reverse().convert(this).asNode(), contexts.stream()
                .filter(contextMap::containsKey)
                .map(x -> contextMap.get(x).apply(this).apply(idTranslator).apply(contexts.contains(MINIMAL)))
                .reduce(empty(), Stream::concat));
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
     *     (org.fcrepo.kernel.api.identifiers.IdentifierConverter, org.apache.jena.rdf.model.Model,
     *     org.fcrepo.kernel.api.RdfStream)
     */
    @Override
    public void replaceProperties(final IdentifierConverter<Resource, FedoraResource> idTranslator,
        final Model inputModel, final RdfStream originalTriples) throws MalformedRdfException {
        final Optional<String> resIxn = INTERACTION_MODELS.stream().filter(x -> hasType(x)).findFirst();

        // remove any statements that update "relaxed" server-managed triples so they can be updated separately
        final List<Statement> filteredStatements = new ArrayList<>();
        final StmtIterator it = inputModel.listStatements();
        while (it.hasNext()) {
            final Statement next = it.next();
            if (RdfLexicon.isRelaxed.test(next.getPredicate())) {
                filteredStatements.add(next);
                it.remove();
            } else {
                // check for interaction model change violation
                final String ixn = getInteractionModel.apply(next.asTriple());
                if (StringUtils.isNotBlank(ixn) && resIxn.isPresent() && !ixn.equals(resIxn.get())) {
                    throw new InteractionModelViolationException("Changing the interaction model "
                        + resIxn.get() + " to " + ixn + " is not allowed!");
                }
            }
        }
        // remove any "relaxed" server-managed triples from the existing triples
        final RdfStream filteredTriples = new DefaultRdfStream(originalTriples.topic(),
                originalTriples.filter(triple -> !isRelaxed.test(createProperty(triple.getPredicate().getURI()))));



        try (final RdfStream replacementStream =
                new DefaultRdfStream(idTranslator.reverse().convert(this).asNode())) {

            final GraphDifferencer differencer =
                new GraphDifferencer(inputModel, filteredTriples);

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

            try (
                final DefaultRdfStream notCommonStream =
                        new DefaultRdfStream(replacementStream.topic(), differencer.notCommon());
                final DefaultRdfStream testStream =
                        new DefaultRdfStream(replacementStream.topic(), differencer.notCommon())) {

                // do some very basic validation to catch invalid RDF
                // this uses the same checks that updateProperties() uses
                final Collection<IllegalArgumentException> errors = testStream
                        .map(x -> Quad.create(x.getSubject(), x))
                        .flatMap(FedoraResourceImpl::validateQuad)
                        .filter(x -> x != null)
                        .collect(Collectors.toList());

                if (!errors.isEmpty()) {
                    throw new ConstraintViolationException(
                            errors.stream().map(Exception::getMessage).collect(joining(", \n")));
                }

                new RdfAdder(idTranslator, getSession(), notCommonStream, inputModel.getNsPrefixMap()).consume();
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

            try {
                touch(propertyChanged.get(), RelaxedPropertiesHelper.getCreatedDate(filteredStatements),
                        RelaxedPropertiesHelper.getCreatedBy(filteredStatements),
                        RelaxedPropertiesHelper.getModifiedDate(filteredStatements),
                        RelaxedPropertiesHelper.getModifiedBy(filteredStatements));
            } catch (final RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Touches a resource to ensure that the implicitly updated properties are updated if
     * not explicitly set.
     * @param includeMembershipResource true if this touch should propagate through to
     *                                  ldp membership resources
     * @param createdDate the date to which the created date should be set or null to leave it unchanged
     * @param createdUser the user to which the created by should be set or null to leave it unchanged
     * @param modifiedDate the date to which the modified date should be set or null to use now
     * @param modifyingUser the user making the modification or null to use the current user
     * @throws RepositoryException an error occurs while updating the repository
     */
    @VisibleForTesting
    public void touch(final boolean includeMembershipResource, final Calendar createdDate, final String createdUser,
                      final Calendar modifiedDate, final String modifyingUser) throws RepositoryException {
        FedoraTypesUtils.touch(getNode(), createdDate, createdUser, modifiedDate, modifyingUser);

        // If the ldp:insertedContentRelation property was changed, update the
        // ldp:membershipResource resource.
        if (includeMembershipResource) {
            touchLdpMembershipResource(getNode(), modifiedDate, modifyingUser);
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
                            .filter(p -> !jcrProperties.contains(p))
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
        final Instant lastModifiedDate = getLastModifiedDate();

        if (lastModifiedDate != null) {
            return sha1Hex(getPath() + lastModifiedDate.toEpochMilli());
        }
        return "";
    }

    private static Collection<IllegalArgumentException> validateUpdateRequest(final UpdateRequest request) {
        return request.getOperations().stream()
                .flatMap(x -> {
                    if (x instanceof UpdateModify) {
                        final UpdateModify y = (UpdateModify) x;
                        return concat(y.getInsertQuads().stream(), y.getDeleteQuads().stream());
                    } else if (x instanceof UpdateData) {
                        return ((UpdateData) x).getQuads().stream();
                    } else if (x instanceof UpdateDeleteWhere) {
                        return ((UpdateDeleteWhere) x).getQuads().stream();
                    } else {
                        return empty();
                    }
                })
                .flatMap(FedoraResourceImpl::validateQuad)
                .filter(x -> x != null)
                .collect(Collectors.toList());
    }

    private static Stream<IllegalArgumentException> validateQuad(final Quad quad) {
        return quadValidators.stream().map(x -> x.apply(quad));
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

    @Override
    public void addType(final String type) {
        try {
            if (node.canAddMixin(type)) {
                node.addMixin(type);
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
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
                    && t.getSubject().equals(translator.convert(getJcrNode(r)).asNode())) {
                final Calendar c = new Calendar.Builder().setInstant(r.getLastModifiedDate().toEpochMilli()).build();
                return new Triple(t.getSubject(), t.getPredicate(), createTypedLiteral(c).asNode());
            }
            return t;
        };
    }

  @Override
  public FedoraResource getBaseVersion() {
    return null;
  }

  @Override
  public Stream<FedoraVersion> getVersions() {
    return null;
  }

  @Override
  public void enableVersioning() {
        if (!isVersioned()) {
            findOrCreateTimeMap();
        }
  }

  @Override
  public void disableVersioning() {
        getTimeMap().delete();
  }

    @Override
    public boolean isVersioned() {
        try {
            return getNode(getNode(), LDPCV_TIME_MAP, false) != null;
        } catch (final RepositoryException ex) {
            throw new RepositoryRuntimeException(ex);
        }
    }

  @Override
  public FedoraResource findMementoByDatetime(final Instant mementoDatetime) {
      if (isVersioned()) {
            final FedoraResource timemap = this.getTimeMap();
            if (timemap != null) {
                final Stream<FedoraResource> mementos = timemap.getChildren();
                final Optional<FedoraResource> closest =
                    mementos.filter(t -> t.getMementoDatetime().compareTo(mementoDatetime) <= 0)
                        .reduce((a,
                            b) -> dateTimeDifference(a.getMementoDatetime(), mementoDatetime,
                                ChronoUnit.SECONDS) <= dateTimeDifference(b.getMementoDatetime(), mementoDatetime,
                                    ChronoUnit.SECONDS) ? a : b);
                if (closest.isPresent()) {
                    // Return the closest version older than the requested date.
                    return closest.get();
                } else {
                    // Otherwise you requested before the first version, so return the first version if is exists.
                    // If there are no Mementos return null.
                    final Optional<FedoraResource> earliest = timemap.getChildren()
                        .sorted((a, b) -> a.getMementoDatetime().compareTo(b.getMementoDatetime()))
                        .findFirst();
                    return earliest.orElse(null);
                }
            }
      }
      return null;
  }

    /**
     * Calculate the difference between two datetime to the unit.
     *
     * @param d1 first datetime
     * @param d2 second datetime
     * @param unit unit to compare down to
     * @return the difference
     */
  static long dateTimeDifference(final Temporal d1, final Temporal d2, final ChronoUnit unit) {
      return unit.between(d1, d2);
  }

  @Override
  public boolean isFrozenResource() {
      LOGGER.warn("Review if method (isFrozenResource) can be removed after implementing Memento!");
      return false;
  }

  @Override
  public FedoraResource getVersionedAncestor() {
      LOGGER.warn("Review if method (getVersionedAncestor) can be removed after implementing Memento!");
      return null;
  }

  @Override
  public FedoraResource getUnfrozenResource() {
      LOGGER.warn("Review if method (getUnfrozenResource) can be removed after implementing Memento!");
      return null;
  }

  @Override
  public FedoraResource getVersion(final String label) {
      LOGGER.warn("Review if method (getVersion) can be removed after implementing Memento!");
      return null;
  }

  @Override
  public String getVersionLabelOfFrozenResource() {
      LOGGER.warn("Review if method (getVersionLabelOfFrozenResource) can be removed after implementing Memento!");
      return null;
  }
}
