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
package org.fcrepo.kernel.modeshape;

import static com.google.common.collect.Iterators.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.hp.hpl.jena.update.UpdateAction.execute;
import static com.hp.hpl.jena.update.UpdateFactory.create;
import static java.util.Arrays.asList;
import static java.util.EnumSet.of;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.empty;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfContext.MINIMAL;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_CREATED;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FROZEN_MIXIN_TYPES;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.rdf.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.fcrepo.kernel.modeshape.services.functions.JcrPropertyFunctions.isFrozen;
import static org.fcrepo.kernel.modeshape.services.functions.JcrPropertyFunctions.property2values;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isFrozenNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isInternalNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isInternalType;
import static org.fcrepo.kernel.modeshape.utils.NamespaceTools.getNamespaceRegistry;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import static org.fcrepo.kernel.modeshape.utils.UncheckedFunction.uncheck;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.AccessDeniedException;
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

import com.google.common.base.Converter;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.graph.Triple;

import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.InvalidPrefixException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.modeshape.rdf.converters.PropertyConverter;
import org.fcrepo.kernel.api.RdfContext;
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
import org.fcrepo.kernel.modeshape.utils.JcrPropertyStatementListener;
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

    private static final PropertyConverter propertyConverter = new PropertyConverter();

    private static Function<FedoraResource, Function<IdentifierConverter<Resource, FedoraResource>, Function<Boolean,
            Stream<Triple>>>> getDefaultTriples = resource -> translator -> uncheck(minimal -> {
        final Stream<Stream<Triple>> min = Stream.of(
            new TypeRdfContext(resource, translator),
            new PropertiesRdfContext(resource, translator));
        if (!minimal) {
            final Stream<Stream<Triple>> extra = Stream.of(
                new HashRdfContext(resource, translator),
                new SkolemNodeRdfContext(resource, translator));
            return Stream.concat(min, extra).reduce(empty(), Stream::concat);
        }
        return min.reduce(empty(), Stream::concat);
    });

    private static Function<FedoraResource, Function<IdentifierConverter<Resource, FedoraResource>, Function<Boolean,
            Stream<Triple>>>> getEmbeddedResourceTriples = resource -> translator -> uncheck(_minimal -> {
        return resource.getChildren().flatMap(child -> child.getTriples(translator, RdfContext.PROPERTIES));
    });

    private static Function<FedoraResource, Function<IdentifierConverter<Resource, FedoraResource>, Function<Boolean,
            Stream<Triple>>>> getInboundTriples = resource -> translator -> uncheck(_minimal -> {
        return new ReferencesRdfContext(resource, translator);
    });

    private static Function<FedoraResource, Function<IdentifierConverter<Resource, FedoraResource>, Function<Boolean,
            Stream<Triple>>>> getLdpContainsTriples = resource -> translator -> uncheck(_minimal -> {
        return new ChildrenRdfContext(resource, translator);
    });

    private static Function<FedoraResource, Function<IdentifierConverter<Resource, FedoraResource>, Function<Boolean,
            Stream<Triple>>>> getVersioningTriples = resource -> translator -> uncheck(_minimal -> {
        return new VersionsRdfContext(resource, translator);
    });

    private static Function<FedoraResource, Function<IdentifierConverter<Resource, FedoraResource>, Function<Boolean,
            Stream<Triple>>>> getServerManagedTriples = resource -> translator -> uncheck(minimal -> {
        if (minimal) {
            return new LdpRdfContext(resource, translator);
        } else {
            final Stream<Stream<Triple>> streams = Stream.of(
                new LdpRdfContext(resource, translator),
                new AclRdfContext(resource, translator),
                new RootRdfContext(resource, translator),
                new ContentRdfContext(resource, translator),
                new ParentRdfContext(resource, translator));
            return streams.reduce(empty(), Stream::concat);
        }
    });

    private static Function<FedoraResource, Function<IdentifierConverter<Resource, FedoraResource>, Function<Boolean,
            Stream<Triple>>>> getLdpMembershipTriples = resource -> translator -> uncheck(_minimal -> {
        final Stream<Stream<Triple>> streams = Stream.of(
            new LdpContainerRdfContext(resource, translator),
            new LdpIsMemberOfRdfContext(resource, translator));
        return streams.reduce(empty(), Stream::concat);
    });

    private static final Map<RdfContext, Function<FedoraResource,
            Function<IdentifierConverter<Resource, FedoraResource>,
            Function<Boolean, Stream<Triple>>>>> contextMap = Collections.unmodifiableMap(Stream.of(
                new SimpleEntry<>(RdfContext.PROPERTIES, getDefaultTriples),
                new SimpleEntry<>(RdfContext.VERSIONS, getVersioningTriples),
                new SimpleEntry<>(RdfContext.EMBED_RESOURCES, getEmbeddedResourceTriples),
                new SimpleEntry<>(RdfContext.INBOUND_REFERENCES, getInboundTriples),
                new SimpleEntry<>(RdfContext.SERVER_MANAGED, getServerManagedTriples),
                new SimpleEntry<>(RdfContext.LDP_MEMBERSHIP, getLdpMembershipTriples),
                new SimpleEntry<>(RdfContext.LDP_CONTAINMENT, getLdpContainsTriples))
                .collect(toMap(x -> x.getKey(), x -> x.getValue())));

    protected Node node;

    /**
     * Construct a {@link org.fcrepo.kernel.api.models.FedoraResource} from an existing JCR Node
     * @param node an existing JCR node to treat as an fcrepo object
     */
    public FedoraResourceImpl(final Node node) {
        this.node = node;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getNode()
     */
    @Override
    public Node getNode() {
        return node;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getPath()
     */
    @Override
    public String getPath() {
        try {
            return node.getPath();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.api.models.FedoraResource#getChildren()
     */
    @Override
    public Stream<FedoraResource> getChildren() {
        try {
            return nodeToGoodChildren(node);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
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
            .flatMap(uncheck((final Node x) -> {
                if (x.isNodeType(FEDORA_PAIRTREE)) {
                    return nodeToGoodChildren(x);
                }
                return Stream.of(nodeToObjectBinaryConverter.convert(x));
            }));
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
        protected FedoraResource doForward(final FedoraResource fedoraResource) {
            if (fedoraResource instanceof NonRdfSourceDescription) {
                return ((NonRdfSourceDescription) fedoraResource).getDescribedResource();
            }
            return fedoraResource;
        }

        @Override
        protected FedoraResource doBackward(final FedoraResource fedoraResource) {
            if (fedoraResource instanceof FedoraBinary) {
                return ((FedoraBinary) fedoraResource).getDescription();
            }
            return fedoraResource;
        }
    };

    private static final Converter<Node, FedoraResource> nodeToObjectBinaryConverter
            = nodeConverter.andThen(datastreamToBinary);

    @Override
    public FedoraResource getContainer() {
        try {

            if (getNode().getDepth() == 0) {
                return null;
            }

            Node container = getNode().getParent();
            while (container.getDepth() > 0) {
                if (container.isNodeType(FEDORA_PAIRTREE)
                        || container.isNodeType(FEDORA_NON_RDF_SOURCE_DESCRIPTION)) {
                    container = container.getParent();
                } else {
                    return nodeConverter.convert(container);
                }
            }

            return nodeConverter.convert(container);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
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
            @SuppressWarnings("unchecked")
            final Iterator<Property> references = node.getReferences();
            @SuppressWarnings("unchecked")
            final Iterator<Property> weakReferences = node.getWeakReferences();
            final Iterator<Property> inboundProperties = Iterators.concat(references, weakReferences);

            while (inboundProperties.hasNext()) {
                final Property prop = inboundProperties.next();
                final List<Value> newVals = new ArrayList<>();
                final Iterator<Value> propIt = property2values.apply(prop);
                while (propIt.hasNext()) {
                    final Value v = propIt.next();
                    if (!node.equals(getSession().getNodeByIdentifier(v.getString()))) {
                        newVals.add(v);
                        LOGGER.trace("Keeping multivalue reference property when deleting node");
                    }
                }
                if (newVals.size() == 0) {
                    prop.remove();
                } else {
                    prop.setValue(newVals.toArray(new Value[newVals.size()]));
                }
            }

            final Node parent;

            if (getNode().getDepth() > 0) {
                parent = getNode().getParent();
            } else {
                parent = null;
            }
            final String name = getNode().getName();

            node.remove();

            if (parent != null) {
                createTombstone(parent, name);
            }

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
                return new Date(getProperty(JCR_CREATED).getDate().getTimeInMillis());
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
    @Override
    public Date getLastModifiedDate() {

        try {
            if (hasProperty(JCR_LASTMODIFIED)) {
                return new Date(getProperty(JCR_LASTMODIFIED).getDate().getTimeInMillis());
            }
        } catch (final PathNotFoundException e) {
            throw new PathNotFoundRuntimeException(e);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        LOGGER.debug("Could not get last modified date property for node {}", node);

        final Date createdDate = getCreatedDate();
        if (createdDate != null) {
            LOGGER.trace("Using created date for last modified date for node {}", node);
            return createdDate;
        }

        return null;
    }


    @Override
    public boolean hasType(final String type) {
        try {
            if (isFrozen.test(node) && hasProperty(FROZEN_MIXIN_TYPES)) {
                final List<String> types = newArrayList(
                    transform(property2values.apply(getProperty(FROZEN_MIXIN_TYPES)), uncheck(Value::getString)::apply)
                );
                return types.contains(type);
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
                .filter(isInternalType.negate())
                .map(uncheck(NodeType::getName))
                .distinct()
                .map(nodeTypeNameToURI::apply)
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

    private Function<String, URI> nodeTypeNameToURI = uncheck(name -> {
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

        final UpdateRequest request = create(sparqlUpdateStatement,
                idTranslator.reverse().convert(this).toString());

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
                idTranslator, getSession(), idTranslator.reverse().convert(this).asNode());

        model.register(listener);

        model.setNsPrefixes(request.getPrefixMapping());
        execute(request, model);

        removeEmptyFragments();

        listener.assertNoExceptions();
    }

    @Override
    public RdfStream getTriples(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                final RdfContext context) {
        return getTriples(idTranslator, of(context));
    }

    @Override
    public RdfStream getTriples(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                final EnumSet<RdfContext> contexts) {

        return new DefaultRdfStream(idTranslator.reverse().convert(this).asNode(), contexts.stream()
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
            return getSession().getWorkspace().getVersionManager().getBaseVersion(getPath());
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
            return getSession().getWorkspace().getVersionManager().getVersionHistory(getPath());
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
    public void replaceProperties(final IdentifierConverter<Resource, FedoraResource> idTranslator,
        final Model inputModel, final RdfStream originalTriples) throws MalformedRdfException {

        final RdfStream replacementStream = new DefaultRdfStream(idTranslator.reverse().convert(this).asNode());

        final GraphDifferencer differencer =
            new GraphDifferencer(inputModel, originalTriples);

        final StringBuilder exceptions = new StringBuilder();
        try {
            new RdfRemover(idTranslator, getSession(), new DefaultRdfStream(replacementStream.topic(),
                        differencer.difference())).consume();
        } catch (final ConstraintViolationException e) {
            throw e;
        } catch (final MalformedRdfException e) {
            exceptions.append(e.getMessage());
            exceptions.append("\n");
        }

        try {
            new RdfAdder(idTranslator, getSession(), new DefaultRdfStream(replacementStream.topic(),
                        differencer.notCommon())).consume();
        } catch (final ConstraintViolationException e) {
            throw e;
        } catch (final MalformedRdfException e) {
            exceptions.append(e.getMessage());
        }

        removeEmptyFragments();

        if (exceptions.length() > 0) {
            throw new MalformedRdfException(exceptions.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private void removeEmptyFragments() {
        try {
            if (node.hasNode("#")) {
                for (final Iterator<Node> hashNodes = node.getNode("#").getNodes(); hashNodes.hasNext(); ) {
                    final Node n = hashNodes.next();
                    final Iterator<Property> userProps = Iterators.filter((Iterator<Property>)n.getProperties(),
                            p -> !isManagedPredicate.test(propertyConverter.convert(p)));
                    if ( !userProps.hasNext() ) {
                        LOGGER.debug("Removing empty hash URI node: {}", n.getName());
                        n.remove();
                    }
                }
            }
        } catch (RepositoryException ex) {
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

    @Override
    public boolean isVersioned() {
        try {
            return node.isNodeType("mix:versionable");
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public boolean isFrozenResource() {
        return isFrozenNode.test(this);
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

    @Override
    public FedoraResource getUnfrozenResource() {
        if (!isFrozenResource()) {
            return this;
        }

        try {
            return new FedoraResourceImpl(getSession().getNodeByIdentifier(getProperty("jcr:frozenUuid").getString()));
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public Node getNodeVersion(final String label) {
        try {
            final Session session = getSession();

            final Node n = getFrozenNode(label);

            if (n != null) {
                return n;
            }

            if (isVersioned()) {
                final VersionHistory hist =
                        session.getWorkspace().getVersionManager().getVersionHistory(getPath());

                if (hist.hasVersionLabel(label)) {
                    LOGGER.debug("Found version for {} by label {}.", this, label);
                    return hist.getVersionByLabel(label).getFrozenNode();
                }
            }

            LOGGER.warn("Unknown version {} with label or uuid {}!", this, label);
            return null;
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
    private Collection<IllegalArgumentException> checkInvalidPredicates(final UpdateRequest request) {
        return request.getOperations().stream()
                .flatMap(x -> {
                    if (x instanceof UpdateModify) {
                        final UpdateModify y = (UpdateModify)x;
                        return Stream.concat(y.getInsertQuads().stream(), y.getDeleteQuads().stream());
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
            final Property p = frozenNode.getProperty("jcr:frozenUuid");
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
}
