
package org.fcrepo;

import static org.fcrepo.services.ServiceHelpers.getObjectSize;
import static org.fcrepo.utils.FedoraTypesUtils.getBaseVersion;
import static org.fcrepo.utils.FedoraTypesUtils.getVersionHistory;
import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.nodetype2name;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionHistory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.sparql.util.Symbol;
import org.fcrepo.rdf.GraphSubjects;
import org.fcrepo.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.JcrPropertyStatementListener;
import org.fcrepo.utils.JcrRdfTools;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.update.UpdateAction;

public class FedoraResource extends JcrTools implements FedoraJcrTypes {

    private static final Logger LOGGER = getLogger(FedoraResource.class);

    public static final GraphSubjects DEFAULT_SUBJECT_FACTORY =
            new DefaultGraphSubjects();

    protected Node node;

    private JcrPropertyStatementListener listener;

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
        this.node = findOrCreateNode(session, path, nodeType);

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

    public static boolean hasMixin(final Node node) throws RepositoryException {
        final NodeType[] nodeTypes = node.getMixinNodeTypes();
        if (nodeTypes == null) {
            return false;
        }
        for (final NodeType nodeType : nodeTypes) {
            if (FEDORA_RESOURCE.equals(nodeType.getName())) {
                return true;
            }
        }
        return false;
    }

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

    public Problems getDatasetProblems() throws RepositoryException {
        if (listener != null) {
            return listener.getProblems();
        } else {
            return null;
        }
    }

    public void updatePropertiesDataset(final GraphSubjects subjects,
                                        final String sparqlUpdateStatement) throws RepositoryException {
        final Dataset dataset = getPropertiesDataset(subjects);
        UpdateAction.parseExecute(sparqlUpdateStatement, dataset);
    }

    public void updatePropertiesDataset(final String sparqlUpdateStatement)
            throws RepositoryException {
        updatePropertiesDataset(DEFAULT_SUBJECT_FACTORY, sparqlUpdateStatement);
    }

    public Dataset getPropertiesDataset(final GraphSubjects subjects)
            throws RepositoryException {

        final Model model = JcrRdfTools.getJcrPropertiesModel(subjects, node);

        listener =
                new JcrPropertyStatementListener(subjects, node.getSession());

        model.register(listener);

        final Dataset dataset = DatasetFactory.create(model);

        String uri = JcrRdfTools.getGraphSubject(subjects, node).getURI();
        com.hp.hpl.jena.sparql.util.Context context = dataset.getContext();
        if ( context == null ) { context = new com.hp.hpl.jena.sparql.util.Context(); }
        context.set(Symbol.create("uri"),uri);

        return dataset;
    }

    public Dataset getPropertiesDataset() throws RepositoryException {
        return getPropertiesDataset(DEFAULT_SUBJECT_FACTORY);
    }

    public Dataset getVersionDataset(final GraphSubjects subjects)
            throws RepositoryException {
        final Model model = JcrRdfTools.getJcrVersionsModel(subjects, node);

        final Dataset dataset = DatasetFactory.create(model);

        String uri = JcrRdfTools.getGraphSubject(subjects, node).getURI();
        com.hp.hpl.jena.sparql.util.Context context = dataset.getContext();
        if ( context == null ) { context = new com.hp.hpl.jena.sparql.util.Context(); }
        context.set(Symbol.create("uri"),uri);

        return dataset;
    }

    public Dataset getVersionDataset() throws RepositoryException {
        return getVersionDataset(DEFAULT_SUBJECT_FACTORY);
    }

    public void addVersionLabel(final String label) throws RepositoryException {
        final VersionHistory versionHistory = getVersionHistory(node);
        versionHistory.addVersionLabel(getBaseVersion(node).getName(), label,
                true);
    }
}
