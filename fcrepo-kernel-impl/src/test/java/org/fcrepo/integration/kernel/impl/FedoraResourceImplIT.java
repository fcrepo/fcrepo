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
package org.fcrepo.integration.kernel.impl;

import static com.google.common.collect.Iterators.filter;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static java.util.Arrays.asList;
import static javax.jcr.PropertyType.BINARY;
import static javax.jcr.PropertyType.LONG;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.RdfLexicon.HAS_CHILD;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_IDENTIFIER;
import static org.fcrepo.kernel.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.RdfLexicon.RDF_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.RELATIONS_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.inject.Inject;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.VersionsRdfContext;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.utils.iterators.PropertyIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.base.Predicate;
import com.google.common.collect.UnmodifiableIterator;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.util.Symbol;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * <p>FedoraResourceImplIT class.</p>
 *
 * @author ajs6f
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraResourceImplIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    NodeService nodeService;

    @Inject
    ObjectService objectService;

    @Inject
    DatastreamService datastreamService;

    private Session session;

    private DefaultIdentifierTranslator subjects;

    @Before
    public void setUp() throws RepositoryException {
        session = repo.login();
        subjects = new DefaultIdentifierTranslator();
    }

    @After
    public void tearDown() {
        session.logout();
    }

    @Test
    public void testGetRootNode() throws RepositoryException {
        final Session session = repo.login();
        final FedoraResource object = nodeService.getObject(session, "/");
        assertEquals("/", object.getPath());
        session.logout();
    }

    private Node createGraphSubjectNode(final String absPath) {
        return subjects.getSubject(absPath).asNode();
    }

    @Test
    public void testRandomNodeGraph() {
        final String testObjId = getTestObjIdentifier();

        final FedoraResource object =
            nodeService.findOrCreateObject(session, "/" + testObjId);

        logger.debug(object.getPropertiesDataset(
                new DefaultIdentifierTranslator()).toString());
        final Node s = createGraphSubjectNode("/" + testObjId);
        final Node p = createURI(REPOSITORY_NAMESPACE + "primaryType");
        final Node o = createLiteral("nt:unstructured");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph()
                .contains(ANY, s, p, o));
    }

    @Test
    public void testLastModified() throws RepositoryException {
        final String pid = getTestObjIdentifier();
        objectService.createObject(session, "/" + pid);

        session.save();
        session.logout();
        session = repo.login();

        final FedoraObject obj2 = objectService.getObject(session, "/" + pid);
        assertFalse( obj2.getLastModifiedDate().before(obj2.getCreatedDate()) );
    }

    @Test
    public void testRepositoryRootGraph() {

        final FedoraResource object = nodeService.getObject(session, "/");

        logger.debug(object.getPropertiesDataset(subjects).toString());
        final Node s = createGraphSubjectNode("/");
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
    public void testObjectGraph() throws RepositoryException {

        final String testObjId = getTestObjIdentifier();

        final FedoraResource object =
            objectService.createObject(session, "/" + testObjId);

        logger.debug(object.getPropertiesDataset(subjects).toString());

        // jcr property
        final Node s = createGraphSubjectNode("/" + testObjId);
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
    public void testObjectGraphWithCustomProperty() throws RepositoryException {

        final String testObjId = getTestObjIdentifier();

        FedoraResource object =
            objectService.createObject(session, "/" + testObjId);

        final javax.jcr.Node node = object.getNode();
        node.setProperty("dc:title", "this-is-some-title");
        node.setProperty("dc:subject", "this-is-some-subject-stored-as-a-binary", BINARY);
        node.setProperty("jcr:data", "jcr-data-should-be-ignored", BINARY);

        session.save();
        session.logout();

        session = repo.login();

        object = objectService.getObject(session, "/" + testObjId);


        logger.debug(object.getPropertiesDataset(subjects).toString());

        // jcr property
        final Node s = createGraphSubjectNode("/" + testObjId);
        Node p = DC_TITLE.asNode();
        Node o = createLiteral("this-is-some-title");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph()
                       .contains(ANY, s, p, o));

        p = createURI("http://purl.org/dc/elements/1.1/subject");
        o = createLiteral("this-is-some-subject-stored-as-a-binary");
        assertTrue(object.getPropertiesDataset(subjects).asDatasetGraph()
                       .contains(ANY, s, p, o));

        o = createLiteral("jcr-data-should-be-ignored");
        assertFalse(object.getPropertiesDataset(subjects).asDatasetGraph()
                        .contains(ANY, s, ANY, o));
    }

    @Test
    public void testRdfTypeInheritance() throws RepositoryException {
        final String testObjId = getTestObjIdentifier();

        final NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();
        //create supertype mixin
        final NodeTypeTemplate type = mgr.createNodeTypeTemplate();
        type.setName("test:aSupertype");
        type.setMixin(true);
        final NodeTypeDefinition[] nodeTypes = new NodeTypeDefinition[]{type};
        mgr.registerNodeTypes(nodeTypes, true);

        //create a type inheriting above supertype
        final NodeTypeTemplate type2 = mgr.createNodeTypeTemplate();
        type2.setName("test:testInher");
        type2.setMixin(true);
        type2.setDeclaredSuperTypeNames(new String[]{"test:aSupertype"});
        final NodeTypeDefinition[] nodeTypes2 = new NodeTypeDefinition[]{type2};
        mgr.registerNodeTypes(nodeTypes2, true);

        //create object with inheriting type
        FedoraResource object = objectService.createObject(session, "/" + testObjId);
        final javax.jcr.Node node = object.getNode();
        node.addMixin("test:testInher");

        session.save();
        session.logout();
        session = repo.login();

        object = objectService.createObject(session, "/" + testObjId);

        //test that supertype has been inherited as rdf:type
        final Node s = createGraphSubjectNode("/" + testObjId);
        final Node p = createProperty(RDF_NAMESPACE + "type").asNode();
        final Node o = createProperty("info:fedora/test/aSupertype").asNode();
        assertTrue("supertype test:aSupertype not found inherited in test:testInher!",
                   object.getPropertiesDataset(subjects).asDatasetGraph().contains(ANY, s, p, o));
    }

    @Test
    public void testDatastreamGraph() throws RepositoryException, InvalidChecksumException {

        final String testObjId = getTestObjIdentifier();

        final String testDsId = getTestDsIdentifier();

        objectService.createObject(session, "/" + testObjId);

        datastreamService.createDatastream(session, "/" + testDsId,
                "text/plain", null, new ByteArrayInputStream("123456789test123456789"
                        .getBytes()));

        final FedoraResource object =
            nodeService.getObject(session, "/" + testDsId);

        object.getNode().setProperty("fedorarelsext:isPartOf",
                session.getNode("/" + testObjId));

        final Dataset propertiesDataset = object.getPropertiesDataset(subjects);

        assertTrue(propertiesDataset.getContext().isDefined(
                Symbol.create("uri")));

        logger.debug(propertiesDataset.toString());

        // jcr property
        Node s = createGraphSubjectNode("/" + testDsId);
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
        o = createGraphSubjectNode("/" + testObjId);
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        p = createURI(REPOSITORY_NAMESPACE + "hasContent");
        o = createGraphSubjectNode("/" + testDsId + "/fcr:content");
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        // content properties
        s = createGraphSubjectNode("/" + testDsId + "/fcr:content");
        p = createURI(REPOSITORY_NAMESPACE + "mimeType");
        o = createLiteral("text/plain");
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        p = HAS_SIZE.asNode();
        o = createLiteral("22", createTypedLiteral(22L).getDatatype());
        assertTrue(datasetGraph.contains(ANY, s, p, o));
    }

    @Test
    @Ignore("Skipping until we restablish paging behavior for RDF")
    public void testObjectGraphWindow() throws RepositoryException {

        final String testObjId = getTestObjIdentifier();

        final String testObjAId = testObjId + "/" + getTestObjIdentifier();
        final String testObjBId = testObjId + "/" + getTestObjIdentifier();
        final String testObjCId = testObjId + "/" + getTestObjIdentifier();

        final FedoraResource object =
            objectService.createObject(session, "/" + testObjId);

        objectService.createObject(session, "/" + testObjAId);
        objectService.createObject(session, "/" + testObjBId);
        objectService.createObject(session, "/" + testObjCId);

        final Dataset propertiesDataset =
            object.getPropertiesDataset(subjects, 1, 1);

        logger.debug(propertiesDataset.toString());

        final DatasetGraph datasetGraph = propertiesDataset.asDatasetGraph();

        // jcr property
        Node s = createGraphSubjectNode("/" + testObjId);
        Node p = HAS_PRIMARY_IDENTIFIER.asNode();
        Node o = createLiteral(object.getNode().getIdentifier());
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        p = HAS_CHILD.asNode();
        o = createGraphSubjectNode("/" + testObjAId);
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        o = createGraphSubjectNode("/" + testObjBId);
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        o = createGraphSubjectNode("/" + testObjCId);
        assertTrue(datasetGraph.contains(ANY, s, p, o));

        s = createGraphSubjectNode("/" + testObjBId);
        p = HAS_PRIMARY_IDENTIFIER.asNode();
        assertTrue(datasetGraph.contains(ANY, s, p, ANY));

        s = createGraphSubjectNode("/" + testObjCId);
        assertFalse(datasetGraph.contains(ANY, s, p, ANY));

    }

    @Test
    public void testUpdatingObjectGraph() {

        final String testObjId = "/" + getTestObjIdentifier();

        final FedoraResource object =
            objectService.createObject(session, testObjId);

        object.updatePropertiesDataset(subjects, "INSERT { " + "<"
                + subjects.getSubject(testObjId).getURI() + "> "
                + "<info:fcrepo/zyx> \"a\" } WHERE {} ");

        // jcr property
        final Resource s = subjects.getSubject(testObjId);
        final Property p = createProperty("info:fcrepo/zyx");
        Literal o = createPlainLiteral("a");
        assertTrue(object.getPropertiesDataset(subjects).getDefaultModel()
                .contains(s, p, o));

        final String testObjIdSubjUri = createGraphSubjectNode(testObjId).getURI();

        object.updatePropertiesDataset(subjects, "DELETE { " + "<"
                + testObjIdSubjUri + "> "
                + "<info:fcrepo/zyx> ?o }\n" + "INSERT { " + "<"
                + testObjIdSubjUri + "> "
                + "<info:fcrepo/zyx> \"b\" } " + "WHERE { " + "<"
                + testObjIdSubjUri + "> "
                + "<info:fcrepo/zyx> ?o } ");

        assertFalse("found value we should have removed", object
                .getPropertiesDataset(subjects).getDefaultModel().contains(s,
                        p, o));
        o = createPlainLiteral("b");
        assertTrue("could not find new value", object.getPropertiesDataset(
                subjects).getDefaultModel().contains(s, p, o));

    }

    @Test
    public void testAddVersionLabel() throws RepositoryException {

        final String testObjId = getTestObjIdentifier();

        final FedoraResource object =
            objectService.createObject(session, "/" + testObjId);

        object.getNode().addMixin("mix:versionable");

        session.save();

        object.addVersionLabel("v0.0.1");

        session.save();

        assertTrue(asList(object.getVersionHistory().getVersionLabels())
                .contains("v0.0.1"));

    }

    @Test
    public void testGetObjectVersionGraph() throws RepositoryException {

        final String testObjId = getTestObjIdentifier();

        final FedoraResource object =
            objectService.createObject(session, "/" + testObjId);

        object.getNode().addMixin("mix:versionable");

        session.save();

        object.addVersionLabel("v0.0.1");

        session.save();

        final Model graphStore = object.getTriples(subjects, VersionsRdfContext.class).asModel();

        logger.debug(graphStore.toString());

        // go querying for the version URI
        Resource s = subjects.getSubject("/" + testObjId);
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
        final String testObjId = getTestObjIdentifier();

        final FedoraResource object =
            objectService.createObject(session, "/" + testObjId);

        final Dataset propertiesDataset =
            object.getPropertiesDataset(subjects, 0, -2);

        logger.debug(propertiesDataset.toString());

        object.updatePropertiesDataset(
                subjects,
                "PREFIX example: <http://example.org/>\n"
                        + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                        + "INSERT { <"
                        + createGraphSubjectNode("/" + testObjId).getURI()
                        + "> example:int-property \"0\"^^xsd:long } "
                        + "WHERE { }");
        assertEquals(LONG, object.getNode().getProperty("example:int-property")
                .getType());
        assertEquals(0L, object.getNode().getProperty("example:int-property")
                .getValues()[0].getLong());
    }

    @Test
    public void testUpdatingRdfType() throws RepositoryException {
        final String testObjId = getTestObjIdentifier();

        final FedoraResource object =
            objectService.createObject(session, "/" + testObjId);

        final Dataset propertiesDataset =
            object.getPropertiesDataset(subjects, 0, -2);

        logger.debug(propertiesDataset.toString());

        object.updatePropertiesDataset(subjects, "INSERT { <"
                + createGraphSubjectNode("/" + testObjId).getURI() + "> <" + RDF.type
                + "> <http://some/uri> } WHERE { }");
        assertEquals(PropertyType.URI, object.getNode().getProperty("rdf:type")
                .getType());
        assertEquals("http://some/uri", object.getNode()
                .getProperty("rdf:type").getValues()[0].getString());
    }

    @Test
    public void testEtagValue() throws RepositoryException {
        final String testObjId = getTestObjIdentifier();

        final FedoraResource object =
            objectService.createObject(session, "/" + testObjId);

        session.save();

        final String actual = object.getEtagValue();
        assertNotNull(actual);
        assertNotEquals("", actual);
    }

    @Test
    public void testGetReferences() throws RepositoryException {
        final String testObjId = getTestObjIdentifier();

        objectService.createObject(session, testObjId);
        final FedoraObject subject = objectService.createObject(session, testObjId + "/a");
        final FedoraObject object = objectService.createObject(session, testObjId + "/b");
        final Value value = session.getValueFactory().createValue(object.getNode());
        subject.getNode().setProperty("fedorarelsext:isPartOf", new Value[] { value });

        session.save();

        final Model model = object.getTriples(subjects, ReferencesRdfContext.class).asModel();

        assertTrue(
            model.contains(subjects.getSubject(subject.getPath()),
                              createProperty("http://fedora.info/definitions/v4/rels-ext#isPartOf"),
                              subjects.getSubject(object.getPath()))
        );
    }

    @Test
    public void testReplaceProperties() throws RepositoryException {
        final String testObjId = getTestObjIdentifier();
        final FedoraObject object = objectService.createObject(session, testObjId);

        final StmtIterator stmtIterator = object.getPropertiesDataset(subjects).getDefaultModel().listStatements();
        final Model model = createDefaultModel().add(stmtIterator);

        final Resource resource = model.createResource();
        final Resource subject = subjects.getSubject(object.getPath());
        final Property predicate = model.createProperty("info:xyz");
        model.add(subject, predicate, resource);
        model.add(resource, model.createProperty("http://purl.org/dc/elements/1.1/title"), "xyz");

        object.replaceProperties(subjects, model, object.getTriples(subjects, PropertiesRdfContext.class));

        final PropertyIterator properties = new PropertyIterator(object.getNode().getProperties());

        final UnmodifiableIterator<javax.jcr.Property> relation
            = filter(properties, new Predicate<javax.jcr.Property>() {
                @Override
                public boolean apply(final javax.jcr.Property property) {
                    try {
                        return property.getName().contains("xyz_ref");
                    } catch (RepositoryException e) {
                        return false;
                    }
                }
        });

        assertTrue(relation.hasNext());

        final javax.jcr.Property next = relation.next();
        final Value[] values = next.getValues();
        assertEquals(1, values.length);

        final javax.jcr.Node skolemizedNode = session.getNodeByIdentifier(values[0].getString());

        assertTrue(skolemizedNode.getPath().contains("/.well-known/genid/"));
        assertEquals("xyz", skolemizedNode.getProperty("dc:title").getValues()[0].getString());

    }

}
