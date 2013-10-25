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

package org.fcrepo.integration.kernel;

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static java.util.Arrays.asList;
import static javax.jcr.PropertyType.LONG;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_IDENTIFIER;
import static org.fcrepo.kernel.RdfLexicon.RELATIONS_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.RESTAPI_NAMESPACE;
import static org.fcrepo.kernel.utils.FedoraTypesUtils.getVersionHistory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.rdf.impl.DefaultGraphSubjects;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.util.Symbol;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

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
        final Session session = repo.login();
        final FedoraResource object = nodeService.getObject(session, "/");
        assertEquals("/", object.getPath());
        session.logout();
    }

    @Test
    public void testRandomNodeGraph() throws IOException, RepositoryException {
        final FedoraResource object =
            nodeService.findOrCreateObject(session, "/testNodeGraph");

        logger.warn(object.getPropertiesDataset(
                new DefaultGraphSubjects(session)).toString());
        final Node s = createURI(RESTAPI_NAMESPACE + "/testNodeGraph");
        final Node p = createURI(REPOSITORY_NAMESPACE + "primaryType");
        final Node o = createLiteral("nt:unstructured");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph()
                .contains(ANY, s, p, o));
    }

    @Test
    public void testRepositoryRootGraph() throws IOException,
                                         RepositoryException {

        final FedoraResource object = nodeService.getObject(session, "/");

        logger.warn(object.getPropertiesDataset(subjects).toString());
        final Node s = createURI(RESTAPI_NAMESPACE + "/");
        Node p = createURI(REPOSITORY_NAMESPACE + "primaryType");
        Node o = createLiteral("mode:root");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph()
                .contains(ANY, s, p, o));

        p =
            createURI(REPOSITORY_NAMESPACE
                    + "repository/jcr.repository.vendor.url");
        o = createLiteral("http://www.modeshape.org");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph()
                .contains(ANY, s, p, o));

        p = createURI(REPOSITORY_NAMESPACE + "hasNodeType");
        o = createLiteral("fedora:resource");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph()
                .contains(ANY, s, p, o));

    }

    @Test
    public void testObjectGraph() throws IOException, RepositoryException {

        final FedoraResource object =
            objectService.createObject(session, "/testObjectGraph");

        logger.warn(object.getPropertiesDataset(subjects).toString());

        // jcr property
        final Node s = createURI(RESTAPI_NAMESPACE + "/testObjectGraph");
        Node p = createURI(REPOSITORY_NAMESPACE + "uuid");
        Node o = createLiteral(object.getNode().getIdentifier());
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph()
                .contains(ANY, s, p, o));

        // multivalued property
        p = createURI(REPOSITORY_NAMESPACE + "mixinTypes");
        o = createLiteral("fedora:resource");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph()
                .contains(ANY, s, p, o));

        o = createLiteral("fedora:object");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph()
                .contains(ANY, s, p, o));

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
        Node s = createURI(RESTAPI_NAMESPACE + "/testDatastreamGraph");
        Node p = createURI(REPOSITORY_NAMESPACE + "uuid");
        Node o = createLiteral(object.getNode().getIdentifier());
        final DatasetGraph datasetGraph = propertiesDataset.asDatasetGraph();

        assertTrue(datasetGraph.contains(ANY, s, p, o));

        // multivalued property
        p = createURI(REPOSITORY_NAMESPACE + "mixinTypes");
        o = createLiteral("fedora:resource");
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        o = createLiteral("fedora:datastream");
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        // structure
        p = createURI(REPOSITORY_NAMESPACE + "numberOfChildren");
        //final RDFDatatype long_datatype = createTypedLiteral(0L).getDatatype();
        o = createLiteral("0");

        //TODO: re-enable number of children reporting, if practical

        //assertTrue(datasetGraph.contains(ANY, s, p, o));
        // relations
        p = createURI(RELATIONS_NAMESPACE + "isPartOf");
        o = createURI(RESTAPI_NAMESPACE + "/testDatastreamGraphParent");
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        p = createURI(REPOSITORY_NAMESPACE + "hasContent");
        o = createURI(RESTAPI_NAMESPACE + "/testDatastreamGraph/fcr:content");
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        // content properties
        s = createURI(RESTAPI_NAMESPACE + "/testDatastreamGraph/fcr:content");
        p = createURI(REPOSITORY_NAMESPACE + "mimeType");
        o = createLiteral("text/plain");
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        p = createURI(RESTAPI_NAMESPACE + "size");
        o = createLiteral("22", createTypedLiteral(22L).getDatatype());
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        // location

        p = createURI(REPOSITORY_NAMESPACE + "hasLocation");
        o = ANY;

        assertTrue(datasetGraph.contains(ANY, s, p, o));

    }

    @Test
    @Ignore("Skipping until we restablish paging behavior for RDF")
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
        Node s = createURI(RESTAPI_NAMESPACE + "/testObjectGraphWindow");
        Node p = HAS_PRIMARY_IDENTIFIER.asNode();
        Node o = createLiteral(object.getNode().getIdentifier());
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        p = HAS_CHILD.asNode();
        o = createURI(RESTAPI_NAMESPACE + "/testObjectGraphWindow/a");
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        o = createURI(RESTAPI_NAMESPACE + "/testObjectGraphWindow/b");
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        o = createURI(RESTAPI_NAMESPACE + "/testObjectGraphWindow/c");
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        s = createURI(RESTAPI_NAMESPACE + "/testObjectGraphWindow/b");
        p = HAS_PRIMARY_IDENTIFIER.asNode();
        assertTrue(datasetGraph.contains(ANY, s, p, ANY));

        s = createURI(RESTAPI_NAMESPACE + "/testObjectGraphWindow/c");
        assertFalse(datasetGraph.contains(ANY, s, p, ANY));

    }

    @Test
    public void testUpdatingObjectGraph() throws RepositoryException {

        final FedoraResource object =
            objectService.createObject(session, "/testObjectGraphUpdates");

        object.updatePropertiesDataset(subjects, "INSERT { " + "<"
                + RESTAPI_NAMESPACE + "/testObjectGraphUpdates> "
                + "<info:fcrepo/zyx> \"a\" } WHERE {} ");

        // jcr property
        final Node s = createURI(RESTAPI_NAMESPACE + "/testObjectGraphUpdates");
        final Node p = createURI("info:fcrepo/zyx");
        Node o = createLiteral("a");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph()
                .contains(ANY, s, p, o));

        object.updatePropertiesDataset(subjects, "DELETE { " + "<"
                + RESTAPI_NAMESPACE + "/testObjectGraphUpdates> "
                + "<info:fcrepo/zyx> ?o }\n" + "INSERT { " + "<"
                + RESTAPI_NAMESPACE + "/testObjectGraphUpdates> "
                + "<info:fcrepo/zyx> \"b\" } " + "WHERE { " + "<"
                + RESTAPI_NAMESPACE + "/testObjectGraphUpdates> "
                + "<info:fcrepo/zyx> ?o } ");

        assertFalse("found value we should have removed", object
                .getPropertiesDataset(subjects).asDatasetGraph().contains(ANY,
                        s, p, o));
        o = createLiteral("b");
        assertTrue("could not find new value", object.getPropertiesDataset(
                subjects).asDatasetGraph().contains(ANY, s, p, o));

    }

    @Test
    public void testAddVersionLabel() throws RepositoryException {

        final FedoraResource object =
            objectService.createObject(session, "/testObjectVersionLabel");

        session.save();

        object.addVersionLabel("v0.0.1");

        session.save();

        assertTrue(asList(
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

        final Model graphStore = object.getVersionDataset(subjects).asModel();

        logger.info(graphStore.toString());

        // go querying for the version URI
        Resource s = createResource(RESTAPI_NAMESPACE + "/testObjectVersionGraph");
        Property p = createProperty(REPOSITORY_NAMESPACE + "hasVersion");
        final ExtendedIterator<Statement> triples = graphStore.listStatements(s, p, (RDFNode)null);

        final List<Statement> list = triples.toList();
        assertEquals(1, list.size());

        // make sure it matches the label
        s = list.get(0).getObject().asResource();
        p = createProperty(REPOSITORY_NAMESPACE + "hasVersionLabel");
        final Literal o = createPlainLiteral("v0.0.1");

        assertTrue(graphStore.contains(s, p, o));

    }

    @Test
    public void testUpdatingRdfTypedValues() throws RepositoryException {
        final FedoraResource object =
            objectService.createObject(session, "/testObjectRdfType");

        final Dataset propertiesDataset =
            object.getPropertiesDataset(subjects, 0, -2);

        logger.warn(propertiesDataset.toString());

        object.updatePropertiesDataset(
                subjects,
                "PREFIX example: <http://example.org/>\n"
                        + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                        + "INSERT { <"
                        + RESTAPI_NAMESPACE
                        + "/testObjectRdfType> example:int-property \"0\"^^xsd:long } "
                        + "WHERE { }");
        assertEquals(LONG, object.getNode().getProperty("example:int-property")
                .getType());
        assertEquals(0L, object.getNode().getProperty("example:int-property")
                .getValue().getLong());
    }

    @Test
    public void testUpdatingRdfType() throws RepositoryException {
        final FedoraResource object =
            objectService.createObject(session, "/testObjectRdfType");

        final Dataset propertiesDataset =
            object.getPropertiesDataset(subjects, 0, -2);

        logger.warn(propertiesDataset.toString());

        object.updatePropertiesDataset(subjects, "INSERT { <"
                + RESTAPI_NAMESPACE + "/testObjectRdfType> <" + RDF.type
                + "> <http://some/uri> } WHERE { }");
        assertEquals(PropertyType.URI, object.getNode().getProperty("rdf:type")
                .getType());
        assertEquals("http://some/uri", object.getNode()
                .getProperty("rdf:type").getValue().getString());
    }

    @Test
    public void testEtagValue() throws RepositoryException {
        final FedoraResource object =
            objectService.createObject(session, "/testEtagObject");

        session.save();

        final String actual = object.getEtagValue();
        assertNotNull(actual);
        assertNotEquals("", actual);
    }
}
