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
package org.fcrepo.integration;

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static org.fcrepo.utils.FedoraTypesUtils.getVersionHistory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.FedoraResource;
import org.fcrepo.RdfLexicon;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.util.Symbol;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraResourceIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    NodeService nodeService;

    @Inject
    ObjectService objectService;

    @Inject
    DatastreamService datastreamService;
    private Session session;
    private DefaultGraphSubjects subjects;

    @Before
    public void setUp() throws RepositoryException {
        session = repo.login();
        subjects = new DefaultGraphSubjects(session);
    }

    @After
    public void tearDown() {
        session.logout();
    }

    @Test
    public void testGetRootNode() throws IOException, RepositoryException {
        Session session = repo.login();
        final FedoraResource object = nodeService.getObject(session, "/");
        assertEquals("/", object.getPath());
        session.logout();
    }

    @Test
    public void testRandomNodeGraph() throws IOException, RepositoryException {
        final FedoraResource object =
                nodeService.findOrCreateObject(session, "/testNodeGraph");

        logger.warn(object.getPropertiesDataset(new DefaultGraphSubjects(session)).toString());
        Node s = createURI("info:fedora/testNodeGraph");
        Node p =
                createURI("info:fedora/fedora-system:def/internal"
                        + "#primaryType");
        Node o = createLiteral("nt:unstructured");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph().contains(
                Node.ANY, s, p, o));
    }

    @Test
    public void testRepositoryRootGraph() throws IOException,
            RepositoryException {

        final FedoraResource object = nodeService.getObject(session, "/");

        logger.warn(object.getPropertiesDataset(subjects).toString());
        Node s = createURI("info:fedora/");
        Node p =
                createURI("info:fedora/fedora-system:def/internal"
                        + "#primaryType");
        Node o = createLiteral("mode:root");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph().contains(
                Node.ANY, s, p, o));

        p =
                createURI("info:fedora/fedora-system:def/internal"
                        + "#repository/jcr.repository.vendor.url");
        o = createLiteral("http://www.modeshape.org");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph().contains(
                Node.ANY, s, p, o));

        p =
                createURI("info:fedora/fedora-system:def/internal"
                        + "#hasNodeType");
        o = createLiteral("fedora:resource");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph().contains(
                Node.ANY, s, p, o));

    }

    @Test
    public void testObjectGraph() throws IOException, RepositoryException {

        final FedoraResource object =
                objectService.createObject(session, "/testObjectGraph");

        logger.warn(object.getPropertiesDataset(subjects).toString());

        // jcr property
        Node s = createURI("info:fedora/testObjectGraph");
        Node p = createURI("info:fedora/fedora-system:def/internal#uuid");
        Node o = createLiteral(object.getNode().getIdentifier());
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph().contains(
                Node.ANY, s, p, o));

        // multivalued property
        p = createURI("info:fedora/fedora-system:def/internal#mixinTypes");
        o = createLiteral("fedora:resource");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph().contains(
                Node.ANY, s, p, o));

        o = createLiteral("fedora:object");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph().contains(
                Node.ANY, s, p, o));

    }

    @Test
    public void testDatastreamGraph() throws IOException, RepositoryException,
            InvalidChecksumException {

        objectService.createObject(session, "/testDatastreamGraphParent");

        datastreamService.createDatastreamNode(session, "/testDatastreamGraph",
                "text/plain", new ByteArrayInputStream("123456789test123456789"
                        .getBytes()));

        final FedoraResource object =
                nodeService.getObject(session, "/testDatastreamGraph");

        object.getNode().setProperty("fedorarelsext:isPartOf",
                session.getNode("/testDatastreamGraphParent"));

        final Dataset propertiesDataset = object.getPropertiesDataset(subjects);

        assertTrue(propertiesDataset.getContext().isDefined(
                Symbol.create("uri")));

        logger.warn(propertiesDataset.toString());

        // jcr property
        Node s = createURI("info:fedora/testDatastreamGraph");
        Node p = createURI("info:fedora/fedora-system:def/internal#uuid");
        Node o = createLiteral(object.getNode().getIdentifier());
        final DatasetGraph datasetGraph = propertiesDataset.asDatasetGraph();

        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        // multivalued property
        p = createURI("info:fedora/fedora-system:def/internal#mixinTypes");
        o = createLiteral("fedora:resource");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        o = createLiteral("fedora:datastream");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        // structure
        p =
                createURI("info:fedora/fedora-system:def/internal"
                        + "#numberOfChildren");
        RDFDatatype long_datatype =
                ResourceFactory.createTypedLiteral(0L).getDatatype();
        o = createLiteral("0", long_datatype);

        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));
        // relations
        p =
                createURI("info:fedora/fedora-system:def/relations-external"
                        + "#isPartOf");
        o = createURI("info:fedora/testDatastreamGraphParent");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        p = createURI("info:fedora/fedora-system:def/internal#hasContent");
        o = createURI("info:fedora/testDatastreamGraph/fcr:content");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        // content properties
        s = createURI("info:fedora/testDatastreamGraph/fcr:content");
        p = createURI("info:fedora/fedora-system:def/internal#mimeType");
        o = createLiteral("text/plain");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        p = createURI("info:fedora/size");
        o =
                createLiteral("22", ModelFactory.createDefaultModel()
                        .createTypedLiteral(22L).getDatatype());
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        // location

        p =
                createURI("info:fedora/fedora-system:def/internal"
                        + "#hasLocation");
        o = Node.ANY;

        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

    }

    @Test
    public void testObjectGraphWindow() throws IOException, RepositoryException {

        final FedoraResource object =
                objectService.createObject(session, "/testObjectGraphWindow");

        objectService.createObject(session, "/testObjectGraphWindow/a");
        objectService.createObject(session, "/testObjectGraphWindow/b");
        objectService.createObject(session, "/testObjectGraphWindow/c");

        final Dataset propertiesDataset =
                object.getPropertiesDataset(subjects, 1, 1);

        logger.warn(propertiesDataset.toString());

        final DatasetGraph datasetGraph = propertiesDataset.asDatasetGraph();

        // jcr property
        Node s = createURI("info:fedora/testObjectGraphWindow");
        Node p = RdfLexicon.HAS_PRIMARY_IDENTIFIER.asNode();
        Node o = createLiteral(object.getNode().getIdentifier());
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        p = RdfLexicon.HAS_CHILD.asNode();
        o = createURI("info:fedora/testObjectGraphWindow/a");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        o = createURI("info:fedora/testObjectGraphWindow/b");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        o = createURI("info:fedora/testObjectGraphWindow/c");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        s = createURI("info:fedora/testObjectGraphWindow/b");
        p = RdfLexicon.HAS_PRIMARY_IDENTIFIER.asNode();
        assertTrue(datasetGraph.contains(Node.ANY, s, p, Node.ANY));

        s = createURI("info:fedora/testObjectGraphWindow/c");
        assertFalse(datasetGraph.contains(Node.ANY, s, p, Node.ANY));

    }

    @Test
    public void testUpdatingObjectGraph() throws RepositoryException {

        final FedoraResource object =
                objectService.createObject(session, "/testObjectGraphUpdates");

        object.updatePropertiesDataset(subjects, "INSERT { "
                + "<info:fedora/testObjectGraphUpdates> "
                + "<info:fcrepo/zyx> \"a\" } WHERE {} ");

        // jcr property
        Node s = createURI("info:fedora/testObjectGraphUpdates");
        Node p = createURI("info:fcrepo/zyx");
        Node o = createLiteral("a");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph().contains(
                Node.ANY, s, p, o));

        object.updatePropertiesDataset(subjects, "DELETE { "
                + "<info:fedora/testObjectGraphUpdates> "
                + "<info:fcrepo/zyx> ?o }\n" + "INSERT { "
                + "<info:fedora/testObjectGraphUpdates> "
                + "<info:fcrepo/zyx> \"b\" } " + "WHERE { "
                + "<info:fedora/testObjectGraphUpdates> "
                + "<info:fcrepo/zyx> ?o } ");

        assertFalse("found value we should have removed", object
                .getPropertiesDataset(subjects).asDatasetGraph().contains(Node.ANY, s,
                        p, o));
        o = createLiteral("b");
        assertTrue("could not find new value", object.getPropertiesDataset(subjects)
                .asDatasetGraph().contains(Node.ANY, s, p, o));

    }

    @Test
    public void testAddVersionLabel() throws RepositoryException {


        final FedoraResource object =
                objectService.createObject(session, "/testObjectVersionLabel");

        session.save();

        object.addVersionLabel("v0.0.1");

        session.save();

        assertTrue(Arrays.asList(
                getVersionHistory(object.getNode()).getVersionLabels())
                .contains("v0.0.1"));

    }

    @Test
    public void testGetObjectVersionGraph() throws RepositoryException {

        final FedoraResource object =
                objectService.createObject(session, "/testObjectVersionGraph");

        session.save();

        object.addVersionLabel("v0.0.1");

        session.save();

        final Dataset graphStore = object.getVersionDataset(subjects);

        logger.info(graphStore.toString());

        // go querying for the version URI
        Node s = createURI("info:fedora/testObjectVersionGraph");
        Node p =
                createURI("info:fedora/fedora-system:def/internal"
                        + "#hasVersion");
        final ExtendedIterator<Triple> triples =
                graphStore.asDatasetGraph().getDefaultGraph().find(
                        Triple.createMatch(s, p, Node.ANY));

        List<Triple> list = triples.toList();
        assertEquals(1, list.size());

        // make sure it matches the label
        s = list.get(0).getMatchObject();
        p =
                createURI("info:fedora/fedora-system:def/internal"
                        + "#hasVersionLabel");
        Node o = createLiteral("v0.0.1");

        assertTrue(graphStore.asDatasetGraph().contains(Node.ANY, s, p, o));

    }
}
