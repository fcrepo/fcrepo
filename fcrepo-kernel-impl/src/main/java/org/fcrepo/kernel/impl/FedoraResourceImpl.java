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
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.update.UpdateAction.execute;
import static com.hp.hpl.jena.update.UpdateFactory.create;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.fcrepo.kernel.rdf.GraphProperties.PROBLEMS_MODEL_NAME;
import static org.fcrepo.kernel.rdf.GraphProperties.URI_SYMBOL;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isFedoraResource;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isFrozen;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.property2values;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.value2string;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.rdf.impl.ChildrenRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ContainerRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ParentRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.impl.utils.JcrPropertyStatementListener;
import org.fcrepo.kernel.utils.iterators.DifferencingIterator;
import org.fcrepo.kernel.impl.utils.iterators.RdfAdder;
import org.fcrepo.kernel.impl.utils.iterators.RdfRemover;
import org.fcrepo.kernel.utils.iterators.NodeIterator;
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
     * Construct a FedoraObject without a backing JCR Node
     */
    public FedoraResourceImpl() {
        super(false);
        node = null;
    }

    /**
     * Construct a FedoraObject from an existing JCR Node
     * @param node an existing JCR node to treat as an fcrepo object
     */
    public FedoraResourceImpl(final Node node) {
        this();
        this.node = node;
    }

    /**
     * Create or find a FedoraObject at the given path
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     */
    public FedoraResourceImpl(final Session session, final String path,
        final String nodeType) {
        this();
        initializeNewResourceProperties(session, path, nodeType);
    }

    private void initializeNewResourceProperties(final Session session,
                                                 final String path,
                                                 final String nodeType) {
        try {
            this.node = findOrCreateNode(
                    session, path, NT_FOLDER, nodeType);

            if (node.isNew()) {

                if (!isFedoraResource.apply(node) && !isFrozen.apply(node)) {
                    node.addMixin(FEDORA_RESOURCE);
                }
            }
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
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
                   return new FedoraResourceImpl(node);
               }
           });
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
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
     *     (org.fcrepo.kernel.rdf.IdentifierTranslator, java.lang.String)
     */
    @Override
    public Dataset updatePropertiesDataset(final IdentifierTranslator subjects,
            final String sparqlUpdateStatement) {
        final Dataset dataset = getPropertiesDataset(subjects);
        final UpdateRequest request =
            create(sparqlUpdateStatement, dataset.getContext().getAsString(
                    URI_SYMBOL));
        dataset.getDefaultModel().setNsPrefixes(request.getPrefixMapping());
        execute(request, dataset);
        return dataset;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getPropertiesDataset(org.fcrepo.kernel.rdf.IdentifierTranslator, int, int)
     */
    @Override
    public Dataset getPropertiesDataset(final IdentifierTranslator graphSubjects,
        final int offset, final int limit) {
        try {

            final RdfStream propertiesStream = getTriples(graphSubjects, ImmutableSet.of(
                    PropertiesRdfContext.class,
                    ParentRdfContext.class,
                    ChildrenRdfContext.class,
                    ContainerRdfContext.class));

            final Dataset dataset = DatasetFactory.create(propertiesStream.asModel());

            final Model problemsModel = createDefaultModel();

            final JcrPropertyStatementListener listener =
                    JcrPropertyStatementListener.getListener(graphSubjects, getSession(), problemsModel);

            dataset.getDefaultModel().register(listener);

            dataset.addNamedModel(PROBLEMS_MODEL_NAME, problemsModel);

            dataset.getContext().set(URI_SYMBOL, graphSubjects.getSubject(getPath()));


            return dataset;
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getPropertiesDataset(org.fcrepo.kernel.rdf.IdentifierTranslator)
     */
    @Override
    public Dataset getPropertiesDataset(final IdentifierTranslator subjects) {
        return getPropertiesDataset(subjects, 0, -1);
    }


    @Override
    public RdfStream getTriples(final IdentifierTranslator graphSubjects,
                                final Class<? extends RdfStream> context) {
        return getTriples(graphSubjects, Collections.singleton(context));
    }

    @Override
    public RdfStream getTriples(final IdentifierTranslator graphSubjects,
                                final Iterable<? extends Class<? extends RdfStream>> contexts) {
        final RdfStream stream = new RdfStream();

        for (final Class<? extends RdfStream> context : contexts) {
            try {
                final Constructor<? extends RdfStream> declaredConstructor
                        = context.getDeclaredConstructor(Node.class, IdentifierTranslator.class);

                final RdfStream rdfStream = declaredConstructor.newInstance(node, graphSubjects);

                stream.concat(rdfStream);
            } catch (final NoSuchMethodException |
                    InstantiationException |
                    IllegalAccessException e) {
                // Shouldn't happen.
                propagate(e);
            } catch (final InvocationTargetException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof RepositoryException) {
                    throw new RepositoryRuntimeException(cause);
                } else {
                    propagate(cause);
                }
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
     *     (org.fcrepo.kernel.rdf.IdentifierTranslator, com.hp.hpl.jena.rdf.model.Model)
     */
    @Override
    public RdfStream replaceProperties(final IdentifierTranslator graphSubjects,
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

    private Session getSession() {
        try {
            return getNode().getSession();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
}
