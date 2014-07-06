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
package org.fcrepo.kernel;


import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.update.UpdateAction.execute;
import static com.hp.hpl.jena.update.UpdateFactory.create;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.fcrepo.kernel.rdf.GraphProperties.PROBLEMS_MODEL_NAME;
import static org.fcrepo.kernel.rdf.GraphProperties.URI_SYMBOL;
import static org.fcrepo.kernel.services.ServiceHelpers.getObjectSize;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraResource;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFrozen;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.map;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.nodetype2name;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.property2values;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.value2string;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.isInternalNode;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.NT_FOLDER;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.fcrepo.jcr.FedoraJcrTypes;
import org.fcrepo.kernel.rdf.HierarchyRdfContextOptions;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
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
        }
        LOGGER.debug("Node {} does not have a createdDate", node);
        return null;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getLastModifiedDate()
     */
    @Override
    public Date getLastModifiedDate() throws RepositoryException {
        if (node.hasProperty(JCR_LASTMODIFIED)) {
            return new Date(node.getProperty(JCR_LASTMODIFIED).getDate()
                            .getTimeInMillis());
        }
        LOGGER.debug(
                    "Could not get last modified date property for node {}",
                    node);

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
            return newArrayList(
                Iterators.transform(
                    property2values.apply(node.getProperty(FROZEN_MIXIN_TYPES)), value2string));
        }
        return map(node.getMixinNodeTypes(), nodetype2name);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#updatePropertiesDataset
     *     (org.fcrepo.kernel.rdf.IdentifierTranslator, java.lang.String)
     */
    @Override
    public Dataset updatePropertiesDataset(final IdentifierTranslator subjects,
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
     * @see org.fcrepo.kernel.FedoraResource#getPropertiesDataset(org.fcrepo.kernel.rdf.IdentifierTranslator, int, int)
     */
    @Override
    public Dataset getPropertiesDataset(final IdentifierTranslator graphSubjects,
        final int offset, final int limit)
        throws RepositoryException {

        final JcrRdfTools jcrRdfTools =
            JcrRdfTools.withContext(graphSubjects, getNode().getSession());

        final RdfStream propertiesStream =
            jcrRdfTools.getJcrTriples(getNode());

        final HierarchyRdfContextOptions serializationOptions = new HierarchyRdfContextOptions(limit, offset);

        propertiesStream.concat(jcrRdfTools.getTreeTriples(getNode(), serializationOptions));

        final Dataset dataset = DatasetFactory.create(propertiesStream.asModel());

        final Model problemsModel = createDefaultModel();

        final JcrPropertyStatementListener listener =
            JcrPropertyStatementListener.getListener(graphSubjects, node
                    .getSession(), problemsModel);

        dataset.getDefaultModel().register(listener);

        dataset.addNamedModel(PROBLEMS_MODEL_NAME, problemsModel);

        dataset.getContext().set(URI_SYMBOL, graphSubjects.getSubject(getNode().getPath()));


        return dataset;
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getPropertiesDataset(org.fcrepo.kernel.rdf.IdentifierTranslator)
     */
    @Override
    public Dataset getPropertiesDataset(final IdentifierTranslator subjects)
        throws RepositoryException {
        return getPropertiesDataset(subjects, 0, -1);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getTriples(org.fcrepo.kernel.rdf.IdentifierTranslator)
     */
    @Override
    public RdfStream getTriples(final IdentifierTranslator graphSubjects)
        throws RepositoryException {

        final JcrRdfTools jcrRdfTools =
                JcrRdfTools.withContext(graphSubjects, getNode().getSession());

        return jcrRdfTools.getJcrTriples(getNode());
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getHierarchyTriples(org.fcrepo.kernel.rdf.IdentifierTranslator)
     */
    @Override
    public RdfStream getHierarchyTriples(final IdentifierTranslator graphSubjects,
                                         final HierarchyRdfContextOptions serializationOptions)
        throws RepositoryException {

        final JcrRdfTools jcrRdfTools =
                JcrRdfTools.withContext(graphSubjects, getNode().getSession());

        return jcrRdfTools.getTreeTriples(getNode(), serializationOptions);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getVersionTriples(org.fcrepo.kernel.rdf.IdentifierTranslator)
     */
    @Override
    public RdfStream getVersionTriples(final IdentifierTranslator graphSubjects)
        throws RepositoryException {
        return JcrRdfTools.withContext(graphSubjects, node.getSession())
                .getVersionTriples(node);
    }

    @Override
    public RdfStream getReferencesTriples(final IdentifierTranslator graphSubjects) throws RepositoryException {
        return JcrRdfTools.withContext(graphSubjects, node.getSession()).getReferencesTriples(node);
    }

    /* (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#addVersionLabel(java.lang.String)
     */
    @Override
    public void addVersionLabel(final String label) throws RepositoryException {
        final VersionHistory versionHistory = getVersionHistory();
        versionHistory.addVersionLabel(getBaseVersion().getName(), label,
                                       true);
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getBaseVersion()
     */
    @Override
    public Version getBaseVersion() throws RepositoryException {
        return node.getSession().getWorkspace().getVersionManager()
                .getBaseVersion(node.getPath());
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.kernel.FedoraResource#getVersionHistory()
     */
    @Override
    public VersionHistory getVersionHistory() throws RepositoryException {
        return node.getSession().getWorkspace().getVersionManager()
                .getVersionHistory(node.getPath());
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
        }
        return "";
    }

    @Override
    public Iterator<Node> getChildren(final IdentifierTranslator graphSubjects) throws RepositoryException {
        return getChildren(node, graphSubjects);
    }

    /**
     * Retrieve children of a node
     * @param node
     * @param graphSubjects
     * @return
     * @throws RepositoryException
     */
    public static Iterator<Node> getChildren(final Node node, final IdentifierTranslator graphSubjects)
            throws RepositoryException {
        final int hierarchyLevels = graphSubjects.getHierarchyLevels();
        return findChildren(node, hierarchyLevels);
    }

    /**
     * Find the child resources of a path.
     * @param recurse If true, find all descenant resources, not just direct children.
     * @throws RepositoryException
    **/
    private static Iterator<Node> findChildren(final Node node, final int hierarchyLevels) throws RepositoryException {
        final Map<String, Node> childrenMap = new TreeMap<>();
        final List<Node> children = new ArrayList<>();
        findChildren( node, children, false );
        for (int i = 0; i < hierarchyLevels && children.size() > 0; i++) {
            final List<Node> childrenCopy = new ArrayList<>();
            childrenCopy.addAll(children);
            children.clear();
            for (final Node child : childrenCopy) {
                findChildren( child, children, false );
            }
        }

        for (final Node child : children) {
            childrenMap.put(child.getPath(), child);
        }
        return childrenMap.values().iterator();
    }

    /**
     * Find children of a node.
     * @param node Repository node to find children of
     * @param children Set to add child paths to
     * @param If true, find all descendant paths, not just direct child paths
    **/
    private static void findChildren(final Node node, final List<Node> children, final boolean recurse)
        throws RepositoryException {
        if (node.getNodes() == null) {
            LOGGER.debug("Null children nodes returned from {}", node.getPath());
            return;
        }
        for (final NodeIterator nodes = node.getNodes(); nodes.hasNext();) {
            final Node child = nodes.nextNode();
            if (!isInternalNode.apply(child) && !child.getName().equals(JCR_CONTENT)) {

                children.add(child);

                if (recurse) {
                    findChildren(child, children, recurse);
                }
            }
        }
    }

    /**
     * Get the parent of the current node
     * @param graphSubjects
     * @return
     * @throws RepositoryException
     */
    @Override
    public Node getParent(final IdentifierTranslator graphSubjects) throws RepositoryException {
        return findParent(node, graphSubjects);
    }

    /**
     * Find the parent of the node
     * @param node
     * @param graphSubjects
     * @return
     * @throws RepositoryException
     */
    public static Node findParent(final Node node, final IdentifierTranslator graphSubjects)
            throws RepositoryException {
        final int hierarchyLevels = graphSubjects.getHierarchyLevels();
        Node parent = node.getParent();
        for (int i = 0; i < hierarchyLevels; i++) {
            parent = parent.getParent();
        }
        return parent;
    }

     /*
     * Get the path of the object
     */
    @Override
    public String getPath(final IdentifierTranslator graphSubjects) throws RepositoryException {
        return graphSubjects.getSubjectPath(graphSubjects.getSubject(node.getPath()));
    }
}
