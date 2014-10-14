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
package org.fcrepo.kernel.impl;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.hp.hpl.jena.update.UpdateAction.execute;
import static com.hp.hpl.jena.update.UpdateFactory.create;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.fcrepo.kernel.rdf.GraphProperties.URI_SYMBOL;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.isFrozen;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.property2values;
import static org.fcrepo.kernel.services.functions.JcrPropertyFunctions.value2string;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.impl.rdf.impl.AclRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ContainerRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.impl.ParentRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.TypeRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.mappings.PropertyToTriple;
import org.fcrepo.kernel.impl.utils.JcrPropertyStatementListener;
import org.fcrepo.kernel.utils.iterators.DifferencingIterator;
import org.fcrepo.kernel.impl.utils.iterators.RdfAdder;
import org.fcrepo.kernel.impl.utils.iterators.RdfRemover;
import org.fcrepo.kernel.utils.iterators.NodeIterator;
import org.fcrepo.kernel.utils.iterators.PropertyIterator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateRequest;

/**
 * Common behaviors across FedoraObject and Datastream types; also used
 * when the exact type of an object is irrelevant
 *
 * @author ajs6f
 */
public class FedoraResourceImpl extends JcrTools implements FedoraJcrTypes, FedoraResource {

    private static final Logger LOGGER = getLogger(FedoraResourceImpl.class);

    protected Node node;

    /**
     * Construct a FedoraObject from an existing JCR Node
     * @param node an existing JCR node to treat as an fcrepo object
     */
    public FedoraResourceImpl(final Node node) {
        this.node = node;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#hasContent()
     */
    @Override
    public boolean hasContent() {
        try {
            return node.hasNode(JCR_CONTENT);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getNode()
     */
    @Override
    public Node getNode() {
        return node;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getPath()
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
     * @see org.fcrepo.kernel.FedoraResource#getChildren()
     */
    @Override
    public Iterator<FedoraResource> getChildren() {
        try {
           return Iterators.transform(new NodeIterator(node.getNodes()), new Function<Node, FedoraResource>() {

               @Override
               public FedoraResource apply(final Node node) {
                   final FedoraResourceImpl fedoraResource = new FedoraResourceImpl(node);

                   if (fedoraResource.hasContent()) {
                       return new DatastreamImpl(node).getBinary();
                   } else {
                       return fedoraResource;
                   }
               }
           });
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public void delete() {
        try {

            final PropertyIterator inboundProperties = new PropertyIterator(node.getReferences());

            if (softDelete()) {
                final Session session = node.getSession();
                final String path = node.getPath();

                final PropertyToTriple propertyToTriple = new PropertyToTriple(getSession(),
                        new DefaultIdentifierTranslator(getSession()));


                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                new RdfStream(Iterators.concat(Iterators.transform(inboundProperties, propertyToTriple)))
                        .asModel().write(os, "TURTLE");

                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                node.getSession().exportSystemView(node.getPath(), out, false, false);

                for (final Property inboundProperty : inboundProperties) {
                    inboundProperty.remove();
                }

                node.remove();

                final Node deletedNode = findOrCreateNode(session, path);

                deletedNode.setProperty("fedora:inboundReferences", os.toString("UTF-8"));
                deletedNode.addMixin("fedora:deleted");
                deletedNode.setProperty("fedora:serializedData", out.toString("UTF-8"));
            } else {
                for (final Property inboundProperty : inboundProperties) {
                    inboundProperty.remove();
                }

                node.remove();
            }
        } catch (final RepositoryException | IOException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    protected boolean softDelete() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getCreatedDate()
     */
    @Override
    public Date getCreatedDate() {
        try {
            if (node.hasProperty(JCR_CREATED)) {
                return new Date(node.getProperty(JCR_CREATED).getDate().getTimeInMillis());
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
     * @see org.fcrepo.kernel.FedoraResource#getLastModifiedDate()
     */
    @Override
    public Date getLastModifiedDate() {

        try {
            if (node.hasProperty(JCR_LASTMODIFIED)) {
                return new Date(node.getProperty(JCR_LASTMODIFIED).getDate().getTimeInMillis());
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
            if (isFrozen.apply(node) && node.hasProperty(FROZEN_MIXIN_TYPES)) {
                final List<String> types = newArrayList(
                    Iterators.transform(property2values.apply(node.getProperty(FROZEN_MIXIN_TYPES)), value2string)
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
     * @see org.fcrepo.kernel.FedoraResource#updatePropertiesDataset
     *     (org.fcrepo.kernel.identifiers.IdentifierConverter, java.lang.String)
     */
    @Override
    public void updatePropertiesDataset(final IdentifierConverter<Resource,Node> subjects,
            final String sparqlUpdateStatement) {
        final Dataset dataset = getPropertiesDataset(subjects);

        final JcrPropertyStatementListener listener =
                new JcrPropertyStatementListener(subjects, getSession());

        dataset.getDefaultModel().register(listener);

        final UpdateRequest request =
            create(sparqlUpdateStatement, subjects.reverse().convert(getNode()).toString());
        dataset.getDefaultModel().setNsPrefixes(request.getPrefixMapping());
        execute(request, dataset);

        listener.assertNoExceptions();
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getPropertiesDataset(org.fcrepo.kernel.identifiers.IdentifierConverter,
      * int,
      * int)
     */
    @Override
    public Dataset getPropertiesDataset(final IdentifierConverter<Resource,Node> graphSubjects,
        final int offset, final int limit) {

        final RdfStream propertiesStream = getTriples(graphSubjects, ImmutableSet.of(
                PropertiesRdfContext.class,
                ParentRdfContext.class,
                ChildrenRdfContext.class,
                ContainerRdfContext.class,
                AclRdfContext.class,
                TypeRdfContext.class));

        final Dataset dataset = DatasetFactory.create(propertiesStream.asModel());

        dataset.getContext().set(URI_SYMBOL, graphSubjects.reverse().convert(getNode()));

        return dataset;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getPropertiesDataset(org.fcrepo.kernel.identifiers.IdentifierConverter)
     */
    @Override
    public Dataset getPropertiesDataset(final IdentifierConverter<Resource,Node> subjects) {
        return getPropertiesDataset(subjects, 0, -1);
    }


    @Override
    public RdfStream getTriples(final IdentifierConverter<Resource,Node> graphSubjects,
                                final Class<? extends RdfStream> context) {
        return getTriples(graphSubjects, Collections.singleton(context));
    }

    @Override
    public RdfStream getTriples(final IdentifierConverter<Resource,Node> graphSubjects,
                                final Iterable<? extends Class<? extends RdfStream>> contexts) {
        final RdfStream stream = new RdfStream();

        for (final Class<? extends RdfStream> context : contexts) {
            try {
                final Constructor<? extends RdfStream> declaredConstructor
                        = context.getDeclaredConstructor(Node.class, IdentifierConverter.class);

                final RdfStream rdfStream = declaredConstructor.newInstance(node, graphSubjects);

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

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#addVersionLabel(java.lang.String)
     */
    @Override
    public void addVersionLabel(final String label) {
        try {
            final VersionHistory versionHistory = getVersionHistory();
            versionHistory.addVersionLabel(getBaseVersion().getName(), label, true);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getBaseVersion()
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
     * @see org.fcrepo.kernel.FedoraResource#getVersionHistory()
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
     * @see org.fcrepo.kernel.FedoraResource#isNew()
     */
    @Override
    public Boolean isNew() {
        return node.isNew();
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#replaceProperties
     *     (org.fcrepo.kernel.identifiers.IdentifierConverter, com.hp.hpl.jena.rdf.model.Model)
     */
    @Override
    public RdfStream replaceProperties(final IdentifierConverter<Resource,Node> graphSubjects,
        final Model inputModel, final RdfStream originalTriples) {

        final RdfStream replacementStream = RdfStream.fromModel(inputModel);

        final Set<Triple> replacementTriples = copyOf(replacementStream);

        final DifferencingIterator<Triple> differencer =
            new DifferencingIterator<>(replacementTriples, originalTriples);

        try {
            new RdfRemover(graphSubjects, getSession(), replacementStream
                    .withThisContext(differencer)).consume();

            new RdfAdder(graphSubjects, getSession(), replacementStream
                    .withThisContext(differencer.notCommon())).consume();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

        return replacementStream.withThisContext(Iterables.concat(differencer
                .common(), differencer.notCommon()));
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getEtagValue()
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
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public void disableVersioning() {
        try {
            node.removeMixin("mix:versionable");
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

    }

    @Override
    public boolean isVersioned() {
        try {
            return node.isNodeType("mix:versionable");
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    public Node getNodeVersion(final String label) {
        try {
            final Session session = getSession();
            try {

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

    private Session getSession() {
        try {
            return getNode().getSession();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
