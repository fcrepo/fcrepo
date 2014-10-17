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

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static java.util.Arrays.asList;
import static javax.jcr.PropertyType.BINARY;
import static javax.jcr.PropertyType.LONG;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
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
import java.util.UUID;

import javax.inject.Inject;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraObject;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.TypeRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.VersionsRdfContext;
import org.fcrepo.kernel.services.BinaryService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ObjectService;
import org.fcrepo.kernel.utils.iterators.PropertyIterator;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
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
    BinaryService binaryService;

    private Session session;

    private DefaultIdentifierTranslator subjects;

    @Before
    public void setUp() throws RepositoryException {
        session = repo.login();
        subjects = new DefaultIdentifierTranslator(session);
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

    private Node createGraphSubjectNode(final FedoraResource obj) {
        return subjects.reverse().convert(obj).asNode();
    }

    @Test
    public void testRandomNodeGraph() {
        final FedoraResource object =
            objectService.findOrCreateObject(session, "/testNodeGraph");

        final Node s = subjects.reverse().convert(object).asNode();
        final Node p = createURI(REPOSITORY_NAMESPACE + "primaryType");
        final Node o = createLiteral("nt:folder");
        assertTrue(object.getTriples(subjects, PropertiesRdfContext.class).asModel().getGraph().contains(s, p, o));
    }

    @Test
    public void testLastModified() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        objectService.findOrCreateObject(session, "/" + pid);

        session.save();
        session.logout();
        session = repo.login();

        final FedoraObject obj2 = objectService.findOrCreateObject(session, "/" + pid);
        assertFalse( obj2.getLastModifiedDate().before(obj2.getCreatedDate()) );
    }

    @Test
    public void testRepositoryRootGraph() {

        final FedoraResource object = nodeService.getObject(session, "/");
        final Graph graph = object.getTriples(subjects, PropertiesRdfContext.class).asModel().getGraph();

        final Node s = createGraphSubjectNode(object);
        Node p = createURI(REPOSITORY_NAMESPACE + "primaryType");
        Node o = createLiteral("mode:root");
        assertTrue(graph.contains(s, p, o));

        p =
            createURI(REPOSITORY_NAMESPACE
                    + "repository/jcr.repository.vendor.url");
        o = createLiteral("http://www.modeshape.org");
        assertTrue(graph.contains(s, p, o));

        p = createURI(REPOSITORY_NAMESPACE + "hasNodeType");
        o = createLiteral("fedora:resource");
        assertTrue(graph.contains(s, p, o));

    }

    @Test
    public void testObjectGraph() throws RepositoryException {

        final String pid = "/" + getRandomPid();
        final FedoraResource object =
            objectService.findOrCreateObject(session, pid);
        final Graph graph = object.getTriples(subjects, PropertiesRdfContext.class).asModel().getGraph();

        // jcr property
        final Node s = createGraphSubjectNode(object);
        Node p = createURI(REPOSITORY_NAMESPACE + "uuid");
        Node o = createLiteral(object.getNode().getIdentifier());
        assertTrue(graph.contains(s, p, o));

        // multivalued property
        p = createURI(REPOSITORY_NAMESPACE + "mixinTypes");
        o = createLiteral("fedora:resource");
        assertTrue(graph.contains(s, p, o));

        o = createLiteral("fedora:object");
        assertTrue(graph.contains(s, p, o));

    }


    @Test
    public void testObjectGraphWithCustomProperty() throws RepositoryException {

        FedoraResource object =
            objectService.findOrCreateObject(session, "/testObjectGraph");

        final javax.jcr.Node node = object.getNode();
        node.setProperty("dc:title", "this-is-some-title");
        node.setProperty("dc:subject", "this-is-some-subject-stored-as-a-binary", BINARY);
        node.setProperty("jcr:data", "jcr-data-should-be-ignored", BINARY);

        session.save();
        session.logout();

        session = repo.login();

        object = objectService.findOrCreateObject(session, "/testObjectGraph");


        final Graph graph = object.getTriples(subjects, PropertiesRdfContext.class).asModel().getGraph();

        // jcr property
        final Node s = createGraphSubjectNode(object);
        Node p = DC_TITLE.asNode();
        Node o = createLiteral("this-is-some-title");
        assertTrue(graph.contains(s, p, o));

        p = createURI("http://purl.org/dc/elements/1.1/subject");
        o = createLiteral("this-is-some-subject-stored-as-a-binary");
        assertTrue(graph.contains(s, p, o));

        p = Node.ANY;
        o = createLiteral("jcr-data-should-be-ignored");
        assertFalse(graph.contains(s, p, o));


    }

    @Test
    public void testRdfTypeInheritance() throws RepositoryException {
        logger.info("in testRdfTypeInheritance...");
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
        FedoraResource object = objectService.findOrCreateObject(session, "/testNTTnheritanceObject");
        final javax.jcr.Node node = object.getNode();
        node.addMixin("test:testInher");

        session.save();
        session.logout();
        session = repo.login();

        object = objectService.findOrCreateObject(session, "/testNTTnheritanceObject");

        //test that supertype has been inherited as rdf:type
        final Node s = createGraphSubjectNode(object);
        final Node p = createProperty(RDF_NAMESPACE + "type").asNode();
        final Node o = createProperty("info:fedora/test/aSupertype").asNode();
        assertTrue("supertype test:aSupertype not found inherited in test:testInher!",
                object.getTriples(subjects, TypeRdfContext.class).asModel().getGraph().contains(s, p, o));
    }

    @Test
    public void testDatastreamGraph() throws RepositoryException, InvalidChecksumException {

        final FedoraObject parentObject = objectService.findOrCreateObject(session, "/testDatastreamGraphParent");

        binaryService.findOrCreateBinary(session, "/testDatastreamGraph").setContent(
                new ByteArrayInputStream("123456789test123456789".getBytes()),
                "text/plain",
                null,
                null,
                null
        );

        final Datastream object =
                binaryService.findOrCreateBinary(session, "/testDatastreamGraph").getDescription();

        object.getNode().setProperty("fedorarelsext:isPartOf",
                session.getNode("/testDatastreamGraphParent"));

        final Graph graph = object.getTriples(subjects, PropertiesRdfContext.class).asModel().getGraph();


        // jcr property
        Node s = createGraphSubjectNode(object);
        Node p = createURI(REPOSITORY_NAMESPACE + "uuid");
        Node o = createLiteral(object.getNode().getIdentifier());

        assertTrue(graph.contains(s, p, o));

        // multivalued property
        p = createURI(REPOSITORY_NAMESPACE + "mixinTypes");
        o = createLiteral("fedora:resource");
        assertTrue(graph.contains(s, p, o));

        o = createLiteral("fedora:datastream");
        assertTrue(graph.contains(s, p, o));

        // structure
        p = createURI(REPOSITORY_NAMESPACE + "numberOfChildren");
        //final RDFDatatype long_datatype = createTypedLiteral(0L).getDatatype();
        o = createLiteral("0");

        //TODO: re-enable number of children reporting, if practical

        //assertTrue(datasetGraph.contains(ANY, s, p, o));
        // relations
        p = createURI(RELATIONS_NAMESPACE + "isPartOf");
        o = createGraphSubjectNode(parentObject);
        assertTrue(graph.contains(s, p, o));

        p = createURI(REPOSITORY_NAMESPACE + "hasContent");
        o = createGraphSubjectNode(object.getBinary());
        assertTrue(graph.contains(s, p, o));

        // content properties
        s = createGraphSubjectNode(object.getBinary());
        p = createURI(REPOSITORY_NAMESPACE + "mimeType");
        o = createLiteral("text/plain");
        assertTrue(graph.contains(s, p, o));

        p = HAS_SIZE.asNode();
        o = createLiteral("22", createTypedLiteral(22L).getDatatype());
        assertTrue(graph.contains(s, p, o));
    }

    @Test
    public void testUpdatingObjectGraph() {

        final FedoraResource object =
            objectService.findOrCreateObject(session, "/testObjectGraphUpdates");

        object.updateProperties(subjects, "INSERT { " + "<"
                + createGraphSubjectNode(object).getURI() + "> "
                + "<info:fcrepo/zyx> \"a\" } WHERE {} ", new RdfStream());

        // jcr property
        final Resource s = createResource(createGraphSubjectNode(object).getURI());
        final Property p = createProperty("info:fcrepo/zyx");
        Literal o = createPlainLiteral("a");
        Model model = object.getTriples(subjects, PropertiesRdfContext.class).asModel();
        assertTrue(model.contains(s, p, o));

        object.updateProperties(subjects, "DELETE { " + "<"
                + createGraphSubjectNode(object).getURI() + "> "
                + "<info:fcrepo/zyx> ?o }\n" + "INSERT { " + "<"
                + createGraphSubjectNode(object).getURI() + "> "
                + "<info:fcrepo/zyx> \"b\" } " + "WHERE { " + "<"
                + createGraphSubjectNode(object).getURI() + "> "
                + "<info:fcrepo/zyx> ?o } ", RdfStream.fromModel(model));

        model = object.getTriples(subjects, PropertiesRdfContext.class).asModel();

        assertFalse("found value we should have removed", model.contains(s, p, o));
        o = createPlainLiteral("b");
        assertTrue("could not find new value", model.contains(s, p, o));

    }

    @Test
    public void testAddVersionLabel() throws RepositoryException {

        final FedoraResource object =
            objectService.findOrCreateObject(session, "/testObjectVersionLabel");

        object.getNode().addMixin("mix:versionable");

        session.save();

        object.addVersionLabel("v0.0.1");

        session.save();

        assertTrue(asList(object.getVersionHistory().getVersionLabels())
                .contains("v0.0.1"));

    }

    @Test
    public void testGetObjectVersionGraph() throws RepositoryException {

        final FedoraResource object =
            objectService.findOrCreateObject(session, "/testObjectVersionGraph");

        object.getNode().addMixin("mix:versionable");

        session.save();

        object.addVersionLabel("v0.0.1");

        session.save();

        final Model graphStore = object.getTriples(subjects, VersionsRdfContext.class).asModel();

        logger.debug(graphStore.toString());

        // go querying for the version URI
        Resource s = createResource(createGraphSubjectNode(object).getURI());
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
            objectService.findOrCreateObject(session, "/testObjectRdfType");

        object.updateProperties(
                subjects,
                "PREFIX example: <http://example.org/>\n"
                        + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                        + "INSERT { <"
                        + createGraphSubjectNode(object).getURI()
                        + "> example:int-property \"0\"^^xsd:long } "
                        + "WHERE { }", new RdfStream());
        assertEquals(LONG, object.getNode().getProperty("example:int-property")
                .getType());
        assertEquals(0L, object.getNode().getProperty("example:int-property")
                .getValues()[0].getLong());
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testAddMissingReference() throws RepositoryException {
        final FedoraResource object =
                objectService.findOrCreateObject(session, "/testRefObject");

        object.updateProperties(
                subjects,
                "PREFIX example: <http://example.org/>\n"
                        + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                        + "PREFIX fedorarelsext: <http://fedora.info/definitions/v4/rels-ext#>\n"
                        + "INSERT { <> fedorarelsext:isPartOf <" + subjects.toDomain("/some-path") + ">}"
                        + "WHERE { }", new RdfStream());
    }

    @Test
    public void testUpdatingRdfType() throws RepositoryException {
        final FedoraResource object =
            objectService.findOrCreateObject(session, "/testObjectRdfType");

        object.updateProperties(subjects, "INSERT { <"
                + createGraphSubjectNode(object).getURI() + "> <" + RDF.type
                + "> <http://some/uri> } WHERE { }", new RdfStream());
        assertTrue(object.getNode().isNodeType("{http://some/}uri"));
    }

    @Test
    public void testRemoveRdfType() throws RepositoryException {
        final FedoraResource object =
                objectService.findOrCreateObject(session, "/testRemoveObjectRdfType");

        object.updateProperties(subjects, "INSERT { <"
                + createGraphSubjectNode(object).getURI() + "> <" + RDF.type
                + "> <http://some/uri> } WHERE { }", object.getTriples(subjects, TypeRdfContext.class));
        assertTrue(object.getNode().isNodeType("{http://some/}uri"));

        object.updateProperties(subjects, "DELETE { <"
                + createGraphSubjectNode(object).getURI() + "> <" + RDF.type
                + "> <http://some/uri> } WHERE { }", object.getTriples(subjects, TypeRdfContext.class));
        assertFalse(object.getNode().isNodeType("{http://some/}uri"));
    }

    @Test
    public void testEtagValue() throws RepositoryException {
        final FedoraResource object =
            objectService.findOrCreateObject(session, "/testEtagObject");

        session.save();

        final String actual = object.getEtagValue();
        assertNotNull(actual);
        assertNotEquals("", actual);
    }

    @Test
    public void testGetReferences() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        objectService.findOrCreateObject(session, pid);
        final FedoraObject subject = objectService.findOrCreateObject(session, pid + "/a");
        final FedoraObject object = objectService.findOrCreateObject(session, pid + "/b");
        final Value value = session.getValueFactory().createValue(object.getNode());
        subject.getNode().setProperty("fedorarelsext:isPartOf", new Value[] { value });

        session.save();

        final Model model = object.getTriples(subjects, ReferencesRdfContext.class).asModel();

        assertTrue(
            model.contains(subjects.reverse().convert(subject),
                              ResourceFactory.createProperty("http://fedora.info/definitions/v4/rels-ext#isPartOf"),
                              subjects.reverse().convert(object))
        );
    }

    @Test
    public void testReplaceProperties() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final FedoraObject object = objectService.findOrCreateObject(session, pid);

        final RdfStream triples = object.getTriples(subjects, PropertiesRdfContext.class);
        final Model model = triples.asModel();

        final Resource resource = model.createResource();
        final Resource subject = subjects.reverse().convert(object);
        final Property predicate = model.createProperty("info:xyz");
        model.add(subject, predicate, resource);
        model.add(resource, model.createProperty("http://purl.org/dc/elements/1.1/title"), "xyz");

        object.replaceProperties(subjects, model, object.getTriples(subjects, PropertiesRdfContext.class));

        final PropertyIterator properties = new PropertyIterator(object.getNode().getProperties());

        final UnmodifiableIterator<javax.jcr.Property> relation
            = Iterators.filter(properties, new Predicate<javax.jcr.Property>() {
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


    @Test
    public void testDeleteObject() throws RepositoryException {
        final String pid = getRandomPid();
        objectService.findOrCreateObject(session, "/" + pid);
        session.save();

        objectService.findOrCreateObject(session, "/" + pid).delete();

        session.save();

        assertFalse(session.nodeExists("/" + pid));
    }

    @Test
    public void testDeleteObjectWithInboundReferences() throws RepositoryException {

        final String pid = getRandomPid();
        final FedoraResource resourceA = objectService.findOrCreateObject(session, "/" + pid + "/a");
        final FedoraResource resourceB = objectService.findOrCreateObject(session, "/" + pid + "/b");

        final Value value = session.getValueFactory().createValue(resourceB.getNode());
        resourceA.getNode().setProperty("fedorarelsext:hasMember", new Value[] { value });

        session.save();
        objectService.findOrCreateObject(session, "/" + pid + "/a").delete();

        session.save();
        objectService.findOrCreateObject(session, "/" + pid + "/b").delete();

        session.save();

        assertFalse(session.nodeExists("/" + pid + "/b"));

    }

    @Test
    public void testGetContainer() throws RepositoryException {
        final String pid = getRandomPid();
        final FedoraObject container = objectService.findOrCreateObject(session, "/" + pid);
        final FedoraResource resource = objectService.findOrCreateObject(session, "/" + pid + "/a");

        assertEquals(container, resource.getContainer());
    }

    @Test
    public void testGetChildren() throws RepositoryException {
        final String pid = getRandomPid();
        final FedoraObject container = objectService.findOrCreateObject(session, "/" + pid);
        final FedoraResource resource = objectService.findOrCreateObject(session, "/" + pid + "/a");

        assertEquals(resource, container.getChildren().next());
    }

    @Test
    public void testGetChildrenWithBinary() throws RepositoryException {
        final String pid = getRandomPid();
        final FedoraObject container = objectService.findOrCreateObject(session, "/" + pid);
        final FedoraResource resource = binaryService.findOrCreateBinary(session, "/" + pid + "/a");

        assertEquals(resource, container.getChildren().next());
    }

    @Test
    public void testGetContainerForBinary() throws RepositoryException {
        final String pid = getRandomPid();
        final FedoraObject container = objectService.findOrCreateObject(session, "/" + pid);
        final FedoraResource resource = binaryService.findOrCreateBinary(session, "/" + pid + "/a");

        assertEquals(container, resource.getContainer());
    }

    @Test
    public void testGetContainerWithHierarchy() throws RepositoryException {
        final String pid = getRandomPid();
        final FedoraObject container = objectService.findOrCreateObject(session, "/" + pid);
        final FedoraResource resource = objectService.findOrCreateObject(session, "/" + pid + "/a/b/c/d");

        assertEquals(container, resource.getContainer());
    }

    @Test
    public void testGetChildrenWithHierarchy() throws RepositoryException {
        final String pid = getRandomPid();
        final FedoraObject container = objectService.findOrCreateObject(session, "/" + pid);
        final FedoraResource resource = objectService.findOrCreateObject(session, "/" + pid + "/a/b/c/d");

        assertEquals(resource, container.getChildren().next());
    }
}
