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

import static com.hp.hpl.jena.update.UpdateAction.execute;
import static com.hp.hpl.jena.update.UpdateFactory.create;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.rdf.GraphProperties;
import org.fcrepo.kernel.rdf.GraphSubjects;
import org.fcrepo.kernel.rdf.impl.JcrGraphProperties;
import org.fcrepo.kernel.utils.JcrRdfTools;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

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

    private static final GraphProperties DEFAULT_PROPERTY_FACTORY =
            new JcrGraphProperties();

    protected Node node;

    private GraphProperties properties;

    /**
     * Construct a FedoraObject without a backing JCR Node
     */
    public FedoraResource() {
        node = null;
        this.properties = DEFAULT_PROPERTY_FACTORY;
    }

    /**
     * Construct a FedoraObject from an existing JCR Node
     * @param node an existing JCR node to treat as an fcrepo object
     */
    public FedoraResource(final Node node) {
        super(false);
        this.node = node;
    }

    /**
     * Create or find a FedoraObject at the given path
     * @param session the JCR session to use to retrieve the object
     * @param path the absolute path to the object
     * @throws RepositoryException
     */
    public FedoraResource(final Session session, final String path,
                          final String nodeType)
        throws RepositoryException {
        super(false);
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
            LOGGER.info("Node {} does not have a createdDate", node);
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
            LOGGER.info(
                        "Could not get last modified date property for node {}",
                        node);
        }

        final Date createdDate = getCreatedDate();

        if (createdDate != null) {
            LOGGER.info(
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
        final Dataset dataset = getPropertiesDataset(subjects, 0, 0);
        final UpdateRequest request =
            create(sparqlUpdateStatement, dataset.getContext().getAsString(
                    URI_SYMBOL));
        dataset.getDefaultModel().setNsPrefixes(request.getPrefixMapping());
        execute(request, dataset);
        return dataset;
    }

    /**
     * Serialize the JCR properties as an RDF Dataset
     *
     *
     * @param subjects
     * @param offset
     * @param limit
     * @return
     * @throws RepositoryException
     */
    public Dataset getPropertiesDataset(final GraphSubjects subjects,
            final long offset, final int limit)
        throws RepositoryException {

        if (this.properties != null) {
            return this.properties.getProperties(
                    node, subjects, offset, limit);
        } else {
            return null;
        }
    }

    /**
     * Serialize the JCR properties of this object as an RDF Dataset
     * @return
     * @throws RepositoryException
     */
    public Dataset getPropertiesDataset(final GraphSubjects subjects)
        throws RepositoryException {
        return getPropertiesDataset(subjects, 0, -1);
    }

    /**
     * Serialize the JCR versions information as an RDF dataset
     * @param subjects
     * @return
     * @throws RepositoryException
     */
    public Dataset getVersionDataset(final GraphSubjects subjects)
        throws RepositoryException {
        final Model model =
            JcrRdfTools.withContext(subjects, node.getSession())
                    .getJcrPropertiesModel(getVersionHistory(node),
                            subjects.getGraphSubject(node));

        final Dataset dataset = DatasetFactory.create(model);

        final String uri = subjects.getGraphSubject(node).getURI();
        final com.hp.hpl.jena.sparql.util.Context context = dataset.getContext();
        context.set(URI_SYMBOL,uri);

        return dataset;
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
     * @throws RepositoryException
     */
    public Dataset replacePropertiesDataset(final GraphSubjects subjects,
        final Model inputModel) throws RepositoryException {
        final Dataset propertiesDataset = getPropertiesDataset(subjects, 0, -2);
        final Model model = propertiesDataset.getDefaultModel();

        final Model removed = model.difference(inputModel);
        model.remove(removed.listStatements());

        final Model created = inputModel.difference(model);
        model.add(created.listStatements());

        return propertiesDataset;
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
