/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.integration;

import static org.fcrepo.utils.FedoraTypesUtils.getVersionHistory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.util.Symbol;
import org.fcrepo.FedoraResource;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.NodeService;
import org.fcrepo.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date May 9, 2013
 */
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


    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetRootNode() throws IOException, RepositoryException {
        Session session = repo.login();

        final FedoraResource object = nodeService.getObject(session, "/");

        session.logout();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testRandomNodeGraph() throws IOException, RepositoryException {
        Session session = repo.login();

        final FedoraResource object =
            nodeService.findOrCreateObject(session, "/testNodeGraph");

        logger.warn(object.getPropertiesDataset().toString());
        Node s = Node.createURI("info:fedora/testNodeGraph");
        Node p = Node.createURI("info:fedora/fedora-system:def/internal" +
                                "#primaryType");
        Node o = Node.createLiteral("nt:unstructured");
        assertTrue(object.getPropertiesDataset().asDatasetGraph()
                   .contains(Node.ANY, s, p, o));
        session.logout();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testRepositoryRootGraph()
        throws IOException, RepositoryException {
        Session session = repo.login();

        final FedoraResource object = nodeService.getObject(session, "/");

        logger.warn(object.getPropertiesDataset().toString());
        Node s = Node.createURI("info:fedora/");
        Node p = Node.createURI("info:fedora/fedora-system:def/internal" +
                                "#primaryType");
        Node o = Node.createLiteral("mode:root");
        assertTrue(object.getPropertiesDataset().asDatasetGraph()
                   .contains(Node.ANY, s, p, o));

        p = Node.createURI("info:fedora/fedora-system:def/internal" +
                           "#repository/jcr.repository.vendor.url");
        o = Node.createLiteral("http://www.modeshape.org");
        assertTrue(object.getPropertiesDataset().asDatasetGraph()
                   .contains(Node.ANY, s, p, o));

        p = Node.createURI("info:fedora/fedora-system:def/internal" +
                           "#hasNodeType");
        o = Node.createLiteral("fedora:resource");
        assertTrue(object.getPropertiesDataset().asDatasetGraph()
                   .contains(Node.ANY, s, p, o));

        session.logout();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testObjectGraph() throws IOException, RepositoryException {
        Session session = repo.login();

        final FedoraResource object =
            objectService.createObject(session, "/testObjectGraph");

        logger.warn(object.getPropertiesDataset().toString());

        // jcr property
        Node s = Node.createURI("info:fedora/testObjectGraph");
        Node p = Node.createURI("info:fedora/fedora-system:def/internal#uuid");
        Node o = Node.createLiteral(object.getNode().getIdentifier());
        assertTrue(object.getPropertiesDataset().asDatasetGraph()
                   .contains(Node.ANY, s, p, o));


        // multivalued property
        p = Node.createURI("info:fedora/fedora-system:def/internal#mixinTypes");
        o = Node.createLiteral("fedora:resource");
        assertTrue(object.getPropertiesDataset().asDatasetGraph()
                   .contains(Node.ANY, s, p, o));

        o = Node.createLiteral("fedora:object");
        assertTrue(object.getPropertiesDataset().asDatasetGraph()
                   .contains(Node.ANY, s, p, o));

        session.logout();
    }


    /**
     * @todo Add Documentation.
     */
    @Test
    public void testDatastreamGraph()
        throws IOException, RepositoryException, InvalidChecksumException {
        Session session = repo.login();

        objectService.createObject(session, "/testDatastreamGraphParent");

        datastreamService
            .createDatastreamNode(session, "/testDatastreamGraph", "text/plain",
                                  new ByteArrayInputStream("123456789test123456789".getBytes()));


        final FedoraResource object =
            nodeService.getObject(session, "/testDatastreamGraph");

        object.getNode()
            .setProperty("fedorarelsext:isPartOf",
                         session.getNode("/testDatastreamGraphParent"));

        final Dataset propertiesDataset = object.getPropertiesDataset();

        assertTrue(propertiesDataset.getContext()
                   .isDefined(Symbol.create("uri")));

        logger.warn(propertiesDataset.toString());

        // jcr property
        Node s = Node.createURI("info:fedora/testDatastreamGraph");
        Node p = Node.createURI("info:fedora/fedora-system:def/internal#uuid");
        Node o = Node.createLiteral(object.getNode().getIdentifier());
        final DatasetGraph datasetGraph = propertiesDataset.asDatasetGraph();

        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));


        // multivalued property
        p = Node.createURI("info:fedora/fedora-system:def/internal#mixinTypes");
        o = Node.createLiteral("fedora:resource");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        o = Node.createLiteral("fedora:datastream");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        // structure
        p = Node.createURI("info:fedora/fedora-system:def/internal" +
                           "#numberOfChildren");
        RDFDatatype long_datatype =
            ResourceFactory.createTypedLiteral(0L).getDatatype();
        o = Node.createLiteral("0", long_datatype);

        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));
        // relations
        p = Node.createURI("info:fedora/fedora-system:def/relations-external" +
                           "#isPartOf");
        o = Node.createURI("info:fedora/testDatastreamGraphParent");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        p = Node.createURI("info:fedora/fedora-system:def/internal#hasContent");
        o = Node.createURI("info:fedora/testDatastreamGraph/fcr:content");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        // content properties
        s = Node.createURI("info:fedora/testDatastreamGraph/fcr:content");
        p = Node.createURI("info:fedora/fedora-system:def/internal#mimeType");
        o = Node.createLiteral("text/plain");
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        p = Node.createURI("info:fedora/size");
        o = Node.createLiteral("22",
                               ModelFactory.createDefaultModel()
                               .createTypedLiteral(22L).getDatatype());
        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));

        // location

        p = Node.createURI("info:fedora/fedora-system:def/internal" +
                           "#hasLocation");
        o = Node.ANY;

        assertTrue(datasetGraph.contains(Node.ANY, s, p, o));


        session.logout();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testUpdatingObjectGraph() throws RepositoryException {

        Session session = repo.login();

        final FedoraResource object =
            objectService.createObject(session, "/testObjectGraphUpdates");

        object
            .updatePropertiesDataset("INSERT { " +
                                     "<info:fedora/testObjectGraphUpdates> " +
                                     "<info:fcrepo/zyx> \"a\" } WHERE {} ");

        // jcr property
        Node s = Node.createURI("info:fedora/testObjectGraphUpdates");
        Node p = Node.createURI("info:fcrepo/zyx");
        Node o = Node.createLiteral("a");
        assertTrue(object.getPropertiesDataset().asDatasetGraph()
                   .contains(Node.ANY, s, p, o));

        object.updatePropertiesDataset("DELETE { " +
                                       "<info:fedora/testObjectGraphUpdates> " +
                                       "<info:fcrepo/zyx> ?o }\n" +
                                       "INSERT { " +
                                       "<info:fedora/testObjectGraphUpdates> " +
                                       "<info:fcrepo/zyx> \"b\" } " +
                                       "WHERE { " +
                                       "<info:fedora/testObjectGraphUpdates> " +
                                       "<info:fcrepo/zyx> ?o } ");

        assertFalse("found value we should have removed",
                    object.getPropertiesDataset().asDatasetGraph()
                    .contains(Node.ANY, s, p, o));
        o = Node.createLiteral("b");
        assertTrue("could not find new value",
                   object.getPropertiesDataset().asDatasetGraph()
                   .contains(Node.ANY, s, p, o));

        session.logout();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testAddVersionLabel() throws RepositoryException {

        Session session = repo.login();

        final FedoraResource object =
            objectService.createObject(session, "/testObjectVersionLabel");

        session.save();

        object.addVersionLabel("v0.0.1");

        session.save();


        assertTrue(Arrays.asList(getVersionHistory(object.getNode())
                                 .getVersionLabels()).contains("v0.0.1"));

        session.logout();
    }

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testGetObjectVersionGraph() throws RepositoryException {

        Session session = repo.login();

        final FedoraResource object =
            objectService.createObject(session, "/testObjectVersionGraph");

        session.save();

        object.addVersionLabel("v0.0.1");

        session.save();


        final Dataset graphStore = object.getVersionDataset();

        logger.info(graphStore.toString());

        // go querying for the version URI
        Node s = Node.createURI("info:fedora/testObjectVersionGraph");
        Node p = Node.createURI("info:fedora/fedora-system:def/internal" +
                                "#hasVersion");
        final ExtendedIterator<Triple> triples =
            graphStore.asDatasetGraph().getDefaultGraph()
            .find(Triple.createMatch(s, p, Node.ANY));

        List<Triple> list = triples.toList();
        assertEquals(1, list.size());

        // make sure it matches the label
        s = list.get(0).getMatchObject();
        p = Node.createURI("info:fedora/fedora-system:def/internal" +
                           "#hasVersionLabel");
        Node o = Node.createLiteral("v0.0.1");

        assertTrue(graphStore.asDatasetGraph().contains(Node.ANY, s, p, o));

        session.logout();
    }
}
