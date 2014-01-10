/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.kernel;


import static com.google.common.collect.ImmutableSet.copyOf;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.update.UpdateAction.execute;
import static com.hp.hpl.jena.update.UpdateFactory.create;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.fcrepo.kernel.rdf.GraphProperties.PROBLEMS_MODEL_NAME;
import static org.fcrepo.kernel.rdf.GraphProperties.URI_SYMBOL;
import static org.fcrepo.kernel.services.ServiceHelpers.getObjectSize;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getBaseVersion;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getVersionHistory;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraResource;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFrozen;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.map;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.nodetype2name;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.property2values;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.value2string;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.JcrRdfTools;
import org.fcrepo.kernel.utils.JcrPropertyStatementListener;
import org.fcrepo.kernel.utils.iterators.DifferencingIterator;
import org.fcrepo.kernel.utils.iterators.RdfAdder;
import org.fcrepo.kernel.utils.iterators.RdfRemover;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateRequest;

/**
 * Common behaviors across FedoraObject and Datastream types; also used
 * when the exact type of an object is irrelevant
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
     * @throws RepositoryException
     */
    public FedoraResourceImpl(final Session session, final String path,
        final String nodeType) throws RepositoryException {
        this();
        initializeNewResourceProperties(session, path, nodeType);
    }

    private void initializeNewResourceProperties(final Session session,
                                                 final String path,
                                                 final String nodeType) throws RepositoryException {
        this.node = findOrCreateNode(
                session, path, NT_FOLDER, nodeType);

        if (node.isNew()) {

            if (!isFedoraResource.apply(node) && !isFrozen.apply(node)) {
                node.addMixin(FEDORA_RESOURCE);
            }

            node.setProperty(JCR_LASTMODIFIED, Calendar.getInstance());
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#hasContent()
     */
    @Override
    public boolean hasContent() throws RepositoryException {
        return node.hasNode(JCR_CONTENT);
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
    public String getPath() throws RepositoryException {
        return node.getPath();
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getCreatedDate()
     */
    @Override
    public Date getCreatedDate() throws RepositoryException {
        if (node.hasProperty(JCR_CREATED)) {
            return new Date(node.getProperty(JCR_CREATED).getDate()
                            .getTimeInMillis());
        } else {
            LOGGER.debug("Node {} does not have a createdDate", node);
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getLastModifiedDate()
     */
    @Override
    public Date getLastModifiedDate() throws RepositoryException {
        if (node.hasProperty(JCR_LASTMODIFIED)) {
            return new Date(node.getProperty(JCR_LASTMODIFIED).getDate()
                            .getTimeInMillis());
        } else {
            LOGGER.debug(
                        "Could not get last modified date property for node {}",
                        node);
        }

        final Date createdDate = getCreatedDate();

        if (createdDate != null) {
            LOGGER.trace(
                        "Using created date for last modified date for node {}",
                        node);
            return createdDate;
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getSize()
     */
    @Override
    public Long getSize() throws RepositoryException {
        return getObjectSize(node);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getModels()
     */
    @Override
    public Collection<String> getModels() throws RepositoryException {
        if (isFrozen.apply(node)) {
            return Lists.newArrayList(
                Iterators.transform(
                    property2values.apply(node.getProperty(FROZEN_MIXIN_TYPES)), value2string));
        } else {
            return map(node.getMixinNodeTypes(), nodetype2name);
        }
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#updatePropertiesDataset
     *     (org.fcrepo.kernel.rdf.GraphSubjects, java.lang.String)
     */
    @Override
    public Dataset updatePropertiesDataset(final GraphSubjects subjects,
            final String sparqlUpdateStatement) throws RepositoryException {
        final Dataset dataset = getPropertiesDataset(subjects);
        final UpdateRequest request =
            create(sparqlUpdateStatement, dataset.getContext().getAsString(
                    URI_SYMBOL));
        dataset.getDefaultModel().setNsPrefixes(request.getPrefixMapping());
        execute(request, dataset);
        return dataset;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getPropertiesDataset(org.fcrepo.kernel.rdf.GraphSubjects, int, int)
     */
    @Override
    public Dataset getPropertiesDataset(final GraphSubjects graphSubjects,
        final int offset, final int limit)
        throws RepositoryException {

        final JcrRdfTools jcrRdfTools =
            JcrRdfTools.withContext(graphSubjects, getNode().getSession());

        final RdfStream propertiesStream =
            jcrRdfTools.getJcrTriples(getNode());

        propertiesStream.concat(jcrRdfTools.getTreeTriples(getNode()));

        final Dataset dataset = DatasetFactory.create(propertiesStream.limit(limit).skip(offset).asModel());

        final Model problemsModel = createDefaultModel();

        final JcrPropertyStatementListener listener =
            JcrPropertyStatementListener.getListener(graphSubjects, node
                    .getSession(), problemsModel);

        dataset.getDefaultModel().register(listener);

        dataset.addNamedModel(PROBLEMS_MODEL_NAME, problemsModel);

        dataset.getContext().set(URI_SYMBOL,
                graphSubjects.getGraphSubject(getNode()));



        return dataset;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getPropertiesDataset(org.fcrepo.kernel.rdf.GraphSubjects)
     */
    @Override
    public Dataset getPropertiesDataset(final GraphSubjects subjects)
        throws RepositoryException {
        return getPropertiesDataset(subjects, 0, -1);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getTriples(org.fcrepo.kernel.rdf.GraphSubjects)
     */
    @Override
    public RdfStream getTriples(final GraphSubjects graphSubjects)
        throws RepositoryException {

        final JcrRdfTools jcrRdfTools =
                JcrRdfTools.withContext(graphSubjects, getNode().getSession());

        return jcrRdfTools.getJcrTriples(getNode());
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getHierarchyTriples(org.fcrepo.kernel.rdf.GraphSubjects)
     */
    @Override
    public RdfStream getHierarchyTriples(final GraphSubjects graphSubjects)
        throws RepositoryException {

        final JcrRdfTools jcrRdfTools =
                JcrRdfTools.withContext(graphSubjects, getNode().getSession());

        return jcrRdfTools.getTreeTriples(getNode());
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getVersionTriples(org.fcrepo.kernel.rdf.GraphSubjects)
     */
    @Override
    public RdfStream getVersionTriples(final GraphSubjects graphSubjects)
        throws RepositoryException {
        return JcrRdfTools.withContext(graphSubjects, node.getSession())
                .getVersionTriples(node);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#addVersionLabel(java.lang.String)
     */
    @Override
    public void addVersionLabel(final String label) throws RepositoryException {
        final VersionHistory versionHistory = getVersionHistory(node);
        versionHistory.addVersionLabel(getBaseVersion(node).getName(), label,
                                       true);
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
     *     (org.fcrepo.kernel.rdf.GraphSubjects, com.hp.hpl.jena.rdf.model.Model)
     */
    @Override
    public RdfStream replaceProperties(final GraphSubjects graphSubjects,
        final Model inputModel) throws RepositoryException {
        final RdfStream originalTriples = getTriples(graphSubjects);

        final RdfStream replacementStream = RdfStream.fromModel(inputModel);

        final Set<Triple> replacementTriples =
            copyOf(replacementStream.iterator());

        final DifferencingIterator<Triple> differencer =
            new DifferencingIterator<>(replacementTriples, originalTriples);

        new RdfRemover(graphSubjects, getNode().getSession(), replacementStream
                .withThisContext(differencer)).consume();

        new RdfAdder(graphSubjects, getNode().getSession(), replacementStream
                .withThisContext(differencer.notCommon())).consume();

        return replacementStream.withThisContext(Iterables.concat(differencer
                .common(), differencer.notCommon()));
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getEtagValue()
     */
    @Override
    public String getEtagValue() throws RepositoryException {
        final Date lastModifiedDate = getLastModifiedDate();

        if (lastModifiedDate != null) {
            return shaHex(node.getPath() + lastModifiedDate);
        } else {
            return "";
        }
    }
}
