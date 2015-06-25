/**
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
package org.fcrepo.kernel.impl;

import static com.google.common.base.Predicates.not;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.singletonIterator;
import static com.google.common.collect.Iterators.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.hp.hpl.jena.update.UpdateAction.execute;
import static com.hp.hpl.jena.update.UpdateFactory.create;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.fcrepo.kernel.impl.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isFrozenNode;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isInternalNode;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.isFrozen;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.property2values;
import static org.fcrepo.kernel.utils.UncheckedFunction.uncheck;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import com.google.common.base.Converter;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.FedoraJcrTypes;
import org.fcrepo.kernel.models.NonRdfSourceDescription;
import org.fcrepo.kernel.models.FedoraBinary;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.ConstraintViolationException;
import org.fcrepo.kernel.exception.MalformedRdfException;
import org.fcrepo.kernel.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.utils.JcrPropertyStatementListener;
import org.fcrepo.kernel.utils.iterators.GraphDifferencingIterator;
import org.fcrepo.kernel.impl.utils.iterators.RdfAdder;
import org.fcrepo.kernel.impl.utils.iterators.RdfRemover;
import org.fcrepo.kernel.utils.iterators.RdfStream;

import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateRequest;

/**
 * Common behaviors across {@link org.fcrepo.kernel.models.Container} and
 * {@link org.fcrepo.kernel.models.NonRdfSourceDescription} types; also used
 * when the exact type of an object is irrelevant
 *
 * @author ajs6f
 */
public class FedoraResourceImpl extends JcrTools implements FedoraJcrTypes, FedoraResource {

    private static final Logger LOGGER = getLogger(FedoraResourceImpl.class);

    protected Node node;

    /*
     * Helps split SPARQL Update statements, e.g., on <>, to enable individual processing
     */
    private static final Pattern subject = compile(".+<[a-zA-Z]*>");

     /*
     * Helps ensure there's no terminating slash in the predicate
     */
    private static final Pattern terminated = compile("/>");

    /**
     * Construct a {@link org.fcrepo.kernel.models.FedoraResource} from an existing JCR Node
     * @param node an existing JCR node to treat as an fcrepo object
     */
    public FedoraResourceImpl(final Node node) {
        this.node = node;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.models.FedoraResource#getNode()
     */
    @Override
    public Node getNode() {
        return node;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.models.FedoraResource#getPath()
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
     * @see org.fcrepo.kernel.models.FedoraResource#getChildren()
     */
    @Override
    public Iterator<FedoraResource> getChildren() {
        try {
            return concat(nodeToGoodChildren(node));
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
    private Iterator<Iterator<FedoraResource>> nodeToGoodChildren(final Node input) throws RepositoryException {
        final Iterator<Node> allChildren = input.getNodes();
        final Iterator<Node> children = filter(allChildren, not(nastyChildren));
        return transform(children, new Function<Node, Iterator<FedoraResource>>() {

            @Override
            public Iterator<FedoraResource> apply(final Node input) {
                try {
                    if (input.isNodeType(FEDORA_PAIRTREE)) {
                        return concat(nodeToGoodChildren(input));
                    }
                    return singletonIterator(nodeToObjectBinaryConverter.convert(input));
                } catch (final RepositoryException e) {
                    throw new RepositoryRuntimeException(e);
                }
            }
        });
    }
    /**
     * Children for whom we will not generate triples.
     */
    private static Predicate<Node> nastyChildren =
            new Predicate<Node>() {

                @Override
                public boolean apply(final Node n) {
                    LOGGER.trace("Testing child node {}", n);
                    try {
                        return isInternalNode.test(n)
                                || n.getName().equals(JCR_CONTENT)
                                || TombstoneImpl.hasMixin(n)
                                || n.getName().equals("#");
                    } catch (final RepositoryException e) {
                        throw new RepositoryRuntimeException(e);
                    }
                }
            };


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
    public Property getProperty(final String relPath) {
        try {
            return getNode().getProperty(relPath);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * Set the given property value for this resource as a URI, without translating any URIs that
     * appear to be references to repository resources.  Using untranslated URIs to refer to
     * repository resources will disable referential integrity checking, but also allows referring
     * to resources that do not exist, have been deleted, etc.
     * @param relPath the given path
     * @param value the URI value
     */
    @Override
    public void setURIProperty(final String relPath, final URI value) {
        try {
            getNode().setProperty(relPath, value.toString(), PropertyType.URI);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public void delete() {
        try {
            final Iterator<Property> references = node.getReferences();
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
     * @see org.fcrepo.kernel.models.FedoraResource#getCreatedDate()
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
     * @see org.fcrepo.kernel.models.FedoraResource#getLastModifiedDate()
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

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.models.FedoraResource#updateProperties
     *     (org.fcrepo.kernel.identifiers.IdentifierConverter, java.lang.String, RdfStream)
     */
    @Override
    public void updateProperties(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                 final String sparqlUpdateStatement, final RdfStream originalTriples)
            throws MalformedRdfException, AccessDeniedException {

        if (!clean(sparqlUpdateStatement)) {
            throw new IllegalArgumentException("Invalid SPARQL UPDATE statement:"
                    + sparqlUpdateStatement);
        }

        final Model model = originalTriples.asModel();

        final JcrPropertyStatementListener listener =
                new JcrPropertyStatementListener(idTranslator, getSession());

        model.register(listener);

        final UpdateRequest request = create(sparqlUpdateStatement,
                idTranslator.reverse().convert(this).toString());
        model.setNsPrefixes(request.getPrefixMapping());
        execute(request, model);

        listener.assertNoExceptions();
    }

    /**
     * Helps ensure that there are no terminating slashes in the predicate.
     * A terminating slash means ModeShape has trouble extracting the localName, e.g., for
     * http://myurl.org/.
     *
     * @see <a href="https://jira.duraspace.org/browse/FCREPO-1409"> FCREPO-1409 </a> for details.
     *
     * @param updateStmt SPARQL Update statement specified by the user
     * @return whether the statement is deemed to be not problematic for ModeShape
     */
    private static boolean clean(final String updateStmt) {
        final int start = updateStmt.indexOf("INSERT");
        final int end = updateStmt.lastIndexOf("}");

        if (start < 0 || end < 0 || end < start) {
            return true;
        }

        final String insertStmt = updateStmt.substring(start, end);
        final String[] insert = subject.split(insertStmt);
        int count = 0;
        final String terminatorIndicator = terminated.pattern();

        for (final String s: insert) {
            if (s.contains(terminatorIndicator)) {
                final String[] p = terminated.split(s);
                count++;
                LOGGER.info("Problematic token({}):{}{} in statement:{}",
                        count, p[0], terminated, updateStmt);
            }
        }

        return count == 0;
    }

    @Override
    public RdfStream getTriples(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                final Class<? extends RdfStream> context) {
        return getTriples(idTranslator, Collections.singleton(context));
    }

    @Override
    public RdfStream getTriples(final IdentifierConverter<Resource, FedoraResource> idTranslator,
                                final Iterable<? extends Class<? extends RdfStream>> contexts) {
        final RdfStream stream = new RdfStream();

        for (final Class<? extends RdfStream> context : contexts) {
            try {
                final Constructor<? extends RdfStream> declaredConstructor
                        = context.getDeclaredConstructor(FedoraResource.class, IdentifierConverter.class);

                final RdfStream rdfStream = declaredConstructor.newInstance(this, idTranslator);
                rdfStream.session(getSession());

                stream.concat(rdfStream);
            } catch (final NoSuchMethodException |
                    InstantiationException |
                    IllegalAccessException e) {
                // Shouldn't happen.
                throw propagate(e);
            } catch (final InvocationTargetException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof RepositoryException) {
                    throw new RepositoryRuntimeException(cause);
                }
                throw propagate(cause);
            }
        }

        return stream;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.models.FedoraResource#getBaseVersion()
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
     * @see org.fcrepo.kernel.models.FedoraResource#getVersionHistory()
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
     * @see org.fcrepo.kernel.models.FedoraResource#isNew()
     */
    @Override
    public Boolean isNew() {
        return node.isNew();
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.models.FedoraResource#replaceProperties
     *     (org.fcrepo.kernel.identifiers.IdentifierConverter, com.hp.hpl.jena.rdf.model.Model)
     */
    @Override
    public void replaceProperties(final IdentifierConverter<Resource, FedoraResource> idTranslator,
        final Model inputModel, final RdfStream originalTriples) throws MalformedRdfException {

        final RdfStream replacementStream = new RdfStream().namespaces(inputModel.getNsPrefixMap());

        final GraphDifferencingIterator differencer =
            new GraphDifferencingIterator(inputModel, originalTriples);

        final StringBuilder exceptions = new StringBuilder();
        try {
            new RdfRemover(idTranslator, getSession(), replacementStream
                    .withThisContext(differencer)).consume();
        } catch (final ConstraintViolationException e) {
            throw e;
        } catch (final MalformedRdfException e) {
            exceptions.append(e.getMessage());
            exceptions.append("\n");
        }

        try {
            new RdfAdder(idTranslator, getSession(), replacementStream
                    .withThisContext(differencer.notCommon())).consume();
        } catch (final ConstraintViolationException e) {
            throw e;
        } catch (final MalformedRdfException e) {
            exceptions.append(e.getMessage());
        }

        if (exceptions.length() > 0) {
            throw new MalformedRdfException(exceptions.toString());
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.models.FedoraResource#getEtagValue()
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
}
