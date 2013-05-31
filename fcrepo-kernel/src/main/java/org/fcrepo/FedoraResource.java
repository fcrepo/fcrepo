/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo;

import static org.fcrepo.services.ServiceHelpers.getObjectSize;
import static org.fcrepo.utils.FedoraTypesUtils.getBaseVersion;
import static org.fcrepo.utils.FedoraTypesUtils.getVersionHistory;
import static org.fcrepo.utils.FedoraTypesUtils.isFedoraResource;
import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.nodetype2name;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;

import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.JcrPropertyStatementListener;
import org.fcrepo.utils.JcrRdfTools;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.util.Symbol;
import com.hp.hpl.jena.update.UpdateAction;

/**
 * @todo Add Documentation.
 * @author cbeer
 * @date May 9, 2013
 */
public class FedoraResource extends JcrTools implements FedoraJcrTypes {

    private static final Logger LOGGER = getLogger(FedoraResource.class);

    public static final GraphSubjects DEFAULT_SUBJECT_FACTORY =
        new DefaultGraphSubjects();

    protected Node node;

    private JcrPropertyStatementListener listener;

    /**
     * @todo Add Documentation.
     */
    public FedoraResource() {
        node = null;
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
        this.node = findOrCreateNode(session, path, JcrConstants.NT_FOLDER, nodeType);

        if (!hasMixin(node)) {
            node.addMixin(FEDORA_RESOURCE);
        }

        if (node.isNew()) {
            if (node.getSession() != null) {
                node.setProperty(JCR_CREATEDBY, node.getSession().getUserID());
            }

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
        return node.hasNode(JcrConstants.JCR_CONTENT);
    }

    /**
     * @return The JCR node that backs this object.
     */
    public Node getNode() {
        return node;
    }

    /**
     * @todo Add Documentation.
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
     * After applying a SPARQL Update to the properties dataset, this may
     * contain Problems with applying the update.
     *
     * @return
     * @throws RepositoryException
     */
    public Problems getDatasetProblems() throws RepositoryException {
        if (listener != null) {
            return listener.getProblems();
        } else {
            return null;
        }
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
    public void updatePropertiesDataset(final GraphSubjects subjects,
                                        final String sparqlUpdateStatement)
        throws RepositoryException {
        final Dataset dataset = getPropertiesDataset(subjects);
        UpdateAction.parseExecute(sparqlUpdateStatement, dataset);
    }

    /**
     * Update the properties Dataset with a SPARQL Update query. The updated
     * properties may be serialized to the JCR store.
     *
     * After applying the statement, clients SHOULD check the result
     * of #getDatasetProblems, which may include problems when attempting to
     * serialize the data to JCR.
     *
     * @param sparqlUpdateStatement
     * @throws RepositoryException
     */
    public void updatePropertiesDataset(final String sparqlUpdateStatement)
        throws RepositoryException {
        updatePropertiesDataset(DEFAULT_SUBJECT_FACTORY, sparqlUpdateStatement);
    }

    /**
     * Serialize the JCR properties as an RDF Dataset
     *
     * @param subjects
     * @return
     * @throws RepositoryException
     */
    public Dataset getPropertiesDataset(final GraphSubjects subjects)
        throws RepositoryException {

        final Model model = JcrRdfTools.getJcrPropertiesModel(subjects, node);

        listener =
            new JcrPropertyStatementListener(subjects, node.getSession());

        model.register(listener);

        final Dataset dataset = DatasetFactory.create(model);

        String uri = JcrRdfTools.getGraphSubject(subjects, node).getURI();
        com.hp.hpl.jena.sparql.util.Context context = dataset.getContext();
        if ( context == null ) {
            context = new com.hp.hpl.jena.sparql.util.Context();
        }
        context.set(Symbol.create("uri"),uri);

        return dataset;
    }

    /**
     * Serialize the JCR properties of this object as an RDF Dataset
     * @return
     * @throws RepositoryException
     */
    public Dataset getPropertiesDataset() throws RepositoryException {
        return getPropertiesDataset(DEFAULT_SUBJECT_FACTORY);
    }

    /**
     * Serialize the JCR versions information as an RDF dataset
     * @param subjects
     * @return
     * @throws RepositoryException
     */
    public Dataset getVersionDataset(final GraphSubjects subjects)
        throws RepositoryException {
        final Model model = JcrRdfTools.getJcrVersionsModel(subjects, node);

        final Dataset dataset = DatasetFactory.create(model);

        String uri = JcrRdfTools.getGraphSubject(subjects, node).getURI();
        com.hp.hpl.jena.sparql.util.Context context = dataset.getContext();
        if ( context == null ) {
            context = new com.hp.hpl.jena.sparql.util.Context();
        }
        context.set(Symbol.create("uri"),uri);

        return dataset;
    }

    /**
     * Serialize the JCR versions information as an RDF dataset
     * @return
     * @throws RepositoryException
     */
    public Dataset getVersionDataset() throws RepositoryException {
        return getVersionDataset(DEFAULT_SUBJECT_FACTORY);
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
}
