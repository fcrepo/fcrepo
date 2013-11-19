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
import static org.fcrepo.kernel.utils.FedoraTypesUtils.map;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.nodetype2name;
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
import org.fcrepo.kernel.utils.JcrPropertyStatementListener;
import org.fcrepo.kernel.utils.JcrRdfTools;
import org.fcrepo.kernel.utils.iterators.DifferencingIterator;
import org.fcrepo.kernel.utils.iterators.RdfAdder;
import org.fcrepo.kernel.utils.iterators.RdfRemover;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.google.common.collect.Iterables;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateRequest;

/**
 * Common behaviors across FedoraObject and Datastream types; also used
 * when the exact type of an object is irrelevant
 */
public class FedoraResource extends JcrTools implements FedoraJcrTypes {

    private static final Logger LOGGER = getLogger(FedoraResource.class);

    protected Node node;

    /**
     * Construct a FedoraObject without a backing JCR Node
     */
    public FedoraResource() {
        super(false);
        node = null;
    }

    /**
     * Construct a FedoraObject from an existing JCR Node
     * @param node an existing JCR node to treat as an fcrepo object
     */
    public FedoraResource(final Node node) {
        this();
        this.node = node;
    }

    /**
     * Create or find a FedoraObject at the given path
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     * @throws RepositoryException
     */
    public FedoraResource(final Session session, final String path,
        final String nodeType) throws RepositoryException {
        this();
        this.node = findOrCreateNode(
                session, path, NT_FOLDER, nodeType);

        if (!hasMixin(node)) {
            node.addMixin(FEDORA_RESOURCE);
        }

        if (node.isNew()) {
            node.setProperty(JCR_LASTMODIFIED, Calendar.getInstance());
        }
    }

    /**
     * Is the given node a Fedora resource
     * (because it has a fedora:resource mixin)?
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public static boolean hasMixin(final Node node) throws RepositoryException {
        return isFedoraResource.apply(node);
    }

    /**
     * Does the resource have a jcr:content child node?
     * @return
     * @throws RepositoryException
     */
    public boolean hasContent() throws RepositoryException {
        return node.hasNode(JCR_CONTENT);
    }

    /**
     * @return The JCR node that backs this object.
     */
    public Node getNode() {
        return node;
    }

    /**
     * Get the path to the JCR node
     * @return
     * @throws RepositoryException
     */
    public String getPath() throws RepositoryException {
        return node.getPath();
    }

    /**
     * Get the date this datastream was created
     * @return
     * @throws RepositoryException
     */
    public Date getCreatedDate() throws RepositoryException {
        if (node.hasProperty(JCR_CREATED)) {
            return new Date(node.getProperty(JCR_CREATED).getDate()
                            .getTimeInMillis());
        } else {
            LOGGER.debug("Node {} does not have a createdDate", node);
            return null;
        }
    }

    /**
     * Get the date this datastream was last modified
     * @return
     * @throws RepositoryException
     */
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

    /**
     * Get the total size of this object and its datastreams
     * @return size in bytes
     * @throws RepositoryException
     */
    public long getSize() throws RepositoryException {
        return getObjectSize(node);
    }

    /**
     * Get the mixins this object uses
     * @return a collection of mixin names
     * @throws javax.jcr.RepositoryException
     */
    public Collection<String> getModels() throws RepositoryException {
        return map(node.getMixinNodeTypes(), nodetype2name);
    }

    /**
     * Update the properties Dataset with a SPARQL Update query. The updated
     * properties may be serialized to the JCR store.
     *
     * After applying the statement, clients SHOULD check the result
     * of #getDatasetProblems, which may include problems when attempting to
     * serialize the data to JCR.
     *
     * @param subjects
     * @param sparqlUpdateStatement
     * @throws RepositoryException
     */
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

    /**
     * Return the JCR properties of this object as a Jena {@link Dataset}
     *
     * @param subjects
     * @param offset
     * @param limit
     * @return
     * @throws RepositoryException
     */
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

    /**
     * Return the JCR properties of this object as a Jena {@link Dataset}
     * @return
     * @throws RepositoryException
     */
    public Dataset getPropertiesDataset(final GraphSubjects subjects)
        throws RepositoryException {
        return getPropertiesDataset(subjects, 0, -1);
    }

    /**
     * Return the JCR properties of this object as an {@link RdfStream}
     * @return
     * @throws RepositoryException
     */
    public RdfStream getTriples(final GraphSubjects graphSubjects)
        throws RepositoryException {

        final JcrRdfTools jcrRdfTools =
                JcrRdfTools.withContext(graphSubjects, getNode().getSession());

        return jcrRdfTools.getJcrTriples(getNode());
    }

    /**
     * Serialize the JCR versions information as an RDF dataset
     * @param subjects
     * @return
     * @throws RepositoryException
     */
    public RdfStream getVersionTriples(final GraphSubjects graphSubjects)
        throws RepositoryException {
        return JcrRdfTools.withContext(graphSubjects, node.getSession())
                .getVersionTriples(node);
    }

    /**
     * Tag the current version of the Node with a version label that
     * can be retrieved by name later.
     *
     * @param label
     * @throws RepositoryException
     */
    public void addVersionLabel(final String label) throws RepositoryException {
        final VersionHistory versionHistory = getVersionHistory(node);
        versionHistory.addVersionLabel(getBaseVersion(node).getName(), label,
                                       true);
    }

    /**
     * Check if a resource was created in this session
     * @return
     */
    public boolean isNew() {
        return node.isNew();
    }

    /**
     * Replace the properties of this object with the properties from the given
     * model
     *
     * @param subjects
     * @param inputModel
     * @return
     * @throws Exception
     */
    public RdfStream replaceProperties(final GraphSubjects graphSubjects,
        final Model inputModel) throws Exception {
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

    /**
     * Construct an ETag value from the last modified date and path. JCR has a
     * mix:etag type, but it only takes into account binary properties. We
     * actually want whole-object etag data. TODO : construct and store an ETag
     * value on object modify
     *
     * @return
     * @throws RepositoryException
     */
    public String getEtagValue() throws RepositoryException {
        final Date lastModifiedDate = getLastModifiedDate();

        if (lastModifiedDate != null) {
            return shaHex(node.getPath() + lastModifiedDate.toString());
        } else {
            return "";
        }
    }
}
