/**
 * Copyright 2015 DuraSpace, Inc.
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

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static javax.jcr.PropertyType.BINARY;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.FedoraJcrTypes.FEDORA_TOMBSTONE;
import static org.fcrepo.kernel.FedoraJcrTypes.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.RdfLexicon.RDF_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.jcr.AccessDeniedException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.jcr.version.Version;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.models.NonRdfSourceDescription;
import org.fcrepo.kernel.models.Container;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.kernel.exception.MalformedRdfException;
import org.fcrepo.kernel.impl.FedoraResourceImpl;
import org.fcrepo.kernel.impl.rdf.impl.DefaultIdentifierTranslator;
import org.fcrepo.kernel.impl.rdf.impl.PropertiesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.ReferencesRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.RootRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.TypeRdfContext;
import org.fcrepo.kernel.impl.rdf.impl.VersionsRdfContext;
import org.fcrepo.kernel.services.BinaryService;
import org.fcrepo.kernel.services.NodeService;
import org.fcrepo.kernel.services.ContainerService;
import org.fcrepo.kernel.services.VersionService;
import org.fcrepo.kernel.utils.iterators.RdfStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.security.SimplePrincipal;
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
    ContainerService containerService;

    @Inject
    BinaryService binaryService;

    @Inject
    VersionService versionService;

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
        final FedoraResource object = nodeService.find(session, "/");
        assertEquals("/", object.getPath());
        session.logout();
    }

    private Node createGraphSubjectNode(final FedoraResource obj) {
        return subjects.reverse().convert(obj).asNode();
    }

    @Test
    public void testRandomNodeGraph() {
        final FedoraResource object =
            containerService.findOrCreate(session, "/testNodeGraph");

        final Node s = subjects.reverse().convert(object).asNode();
        final Node p = createURI(REPOSITORY_NAMESPACE + "primaryType");
        final Node o = createLiteral("nt:folder");
        assertTrue(object.getTriples(subjects, PropertiesRdfContext.class).asModel().getGraph().contains(s, p, o));
    }

    @Test
    public void testLastModified() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        containerService.findOrCreate(session, "/" + pid);

        session.save();
        session.logout();
        session = repo.login();

        final Container obj2 = containerService.findOrCreate(session, "/" + pid);
        assertFalse( obj2.getLastModifiedDate().before(obj2.getCreatedDate()) );
    }

    @Test
    public void testRepositoryRootGraph() {

        final FedoraResource object = nodeService.find(session, "/");
        final Graph graph = object.getTriples(subjects, RootRdfContext.class).asModel().getGraph();

        final Node s = createGraphSubjectNode(object);

        Node p =
            createURI(REPOSITORY_NAMESPACE
                    + "repositoryJcrRepositoryVendorUrl");
        Node o = createLiteral("http://www.modeshape.org");
        assertTrue(graph.contains(s, p, o));

        p = createURI(REPOSITORY_NAMESPACE + "hasNodeType");
        o = createLiteral(FEDORA_RESOURCE);
        assertFalse(graph.contains(s, p, o));

    }

    @Test
    public void testObjectGraph() throws RepositoryException {

        final String pid = "/" + getRandomPid();
        final FedoraResource object =
            containerService.findOrCreate(session, pid);
        final Graph graph = object.getTriples(subjects, PropertiesRdfContext.class).asModel().getGraph();

        // jcr property
        Node s = createGraphSubjectNode(object);
        Node p = createURI(REPOSITORY_NAMESPACE + "uuid");
        assertFalse(graph.contains(s, p, ANY));

        // multivalued property
        s = createGraphSubjectNode(object);
        p = createURI(REPOSITORY_NAMESPACE + "mixinTypes");
        Node o = createLiteral(FEDORA_RESOURCE);
        assertTrue(graph.contains(s, p, o));

        o = createLiteral(FEDORA_CONTAINER);
        assertTrue(graph.contains(s, p, o));

    }


    @Test
    public void testObjectGraphWithCustomProperty() throws RepositoryException {

        FedoraResource object =
            containerService.findOrCreate(session, "/testObjectGraph");

        final javax.jcr.Node node = object.getNode();
        node.setProperty("dc:title", "this-is-some-title");
        node.setProperty("dc:subject", "this-is-some-subject-stored-as-a-binary", BINARY);
        node.setProperty("jcr:data", "jcr-data-should-be-ignored", BINARY);

        session.save();
        session.logout();

        session = repo.login();

        object = containerService.findOrCreate(session, "/testObjectGraph");


        final Graph graph = object.getTriples(subjects, PropertiesRdfContext.class).asModel().getGraph();

        // jcr property
        final Node s = createGraphSubjectNode(object);
        Node p = DC_TITLE.asNode();
        Node o = createLiteral("this-is-some-title");
        assertTrue(graph.contains(s, p, o));

        p = createURI("http://purl.org/dc/elements/1.1/subject");
        o = createLiteral("this-is-some-subject-stored-as-a-binary");
        assertTrue(graph.contains(s, p, o));

        p = ANY;
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
        FedoraResource object = containerService.findOrCreate(session, "/testNTTnheritanceObject");
        final javax.jcr.Node node = object.getNode();
        node.addMixin("test:testInher");

        session.save();
        session.logout();
        session = repo.login();

        object = containerService.findOrCreate(session, "/testNTTnheritanceObject");

        //test that supertype has been inherited as rdf:type
        final Node s = createGraphSubjectNode(object);
        final Node p = createProperty(RDF_NAMESPACE + "type").asNode();
        final Node o = createProperty("info:fedora/test/aSupertype").asNode();
        assertTrue("supertype test:aSupertype not found inherited in test:testInher!",
                object.getTriples(subjects, TypeRdfContext.class).asModel().getGraph().contains(s, p, o));
    }

    @Test
    public void testDatastreamGraph() throws RepositoryException, InvalidChecksumException {

        final Container parentObject = containerService.findOrCreate(session, "/testDatastreamGraphParent");

        binaryService.findOrCreate(session, "/testDatastreamGraph").setContent(
                new ByteArrayInputStream("123456789test123456789".getBytes()),
                "text/plain",
                null,
                null,
                null
        );

        final NonRdfSourceDescription object =
                binaryService.findOrCreate(session, "/testDatastreamGraph").getDescription();

        object.getNode().setProperty("fedora:isPartOf",
                session.getNode("/testDatastreamGraphParent"));

        final Graph graph = object.getTriples(subjects, PropertiesRdfContext.class).asModel().getGraph();

        // multivalued property
        final Node s = createGraphSubjectNode(object);
        Node p = createURI(REPOSITORY_NAMESPACE + "mixinTypes");
        Node o = createLiteral(FEDORA_RESOURCE);
        assertTrue(graph.contains(s, p, o));

        o = createLiteral(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
        assertTrue(graph.contains(s, p, o));

        // structure
        p = createURI(REPOSITORY_NAMESPACE + "numberOfChildren");
        //final RDFDatatype long_datatype = createTypedLiteral(0L).getDatatype();
        o = createLiteral("0");

        //TODO: re-enable number of children reporting, if practical

        //assertTrue(datasetGraph.contains(ANY, s, p, o));
        // relations
        p = createURI(REPOSITORY_NAMESPACE + "isPartOf");
        o = createGraphSubjectNode(parentObject);
        assertTrue(graph.contains(s, p, o));

    }

    @Test
    public void testUpdatingObjectGraph() throws MalformedRdfException, AccessDeniedException {

        final FedoraResource object =
            containerService.findOrCreate(session, "/testObjectGraphUpdates");

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
    public void testGetObjectVersionGraph() throws RepositoryException {

        final FedoraResource object =
            containerService.findOrCreate(session, "/testObjectVersionGraph");

        object.getNode().addMixin("mix:versionable");
        session.save();

        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session, object.getPath(), "v0.0.1");
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

    @Test(expected = MalformedRdfException.class)
    public void testAddMissingReference() throws RepositoryException, MalformedRdfException {
        final FedoraResource object =
                containerService.findOrCreate(session, "/testRefObject");

        object.updateProperties(
                subjects,
                "PREFIX example: <http://example.org/>\n"
                        + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                        + "PREFIX fedora: <" + REPOSITORY_NAMESPACE + ">\n"
                        + "INSERT { <> fedora:isPartOf <" + subjects.toDomain("/some-path") + ">}"
                        + "WHERE { }", new RdfStream());
    }

    @Test(expected = AccessDeniedException.class)
    public void testUpdateDenied() throws RepositoryException {
        final FedoraResource object =
                containerService.findOrCreate(session, "/testRefObject");
        try {
            object.updateProperties(
                    subjects,
                    "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"test-original\". }"
                            + " WHERE { }", new RdfStream());
        } catch (final AccessDeniedException e) {
            fail("Should fail at update, not create property");
        }
        final AccessControlManager acm = session.getAccessControlManager();
        final Privilege[] permissions = new Privilege[] {acm.privilegeFromName(Privilege.JCR_READ)};
        final AccessControlList acl = (AccessControlList) acm.getApplicablePolicies("/testRefObject").next();
        acl.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"), permissions);
        acm.setPolicy("/testRefObject", acl);
        session.save();

        object.updateProperties(
                subjects,
                "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"test-update\". }"
                        + " WHERE { }", new RdfStream());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testInvalidSparqlUpdateValidation() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final FedoraResource object =
                containerService.findOrCreate(session, pid);
        object.updateProperties(
                subjects,
                "INSERT { <> <http://myurl.org/title/> \"fancy title\" . \n" +
                " <> <http://myurl.org/title/> \"fancy title 2\" . } WHERE { }",
                new RdfStream());
    }

    @Test
    public void testValidSparqlUpdateValidationAltSyntax() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final FedoraResource object = containerService.findOrCreate(session, pid);
        object.updateProperties(subjects,
                "DELETE WHERE {" +
                        "<> <http://www.loc.gov/premis/rdf/v1#hasDateCreatedByApplication> ?o0 ." +
                        "}; INSERT DATA {" +
                        "<> <http://purl.org/dc/elements/1.1/title> \"Example Managed binary datastream\" ." +
                        "}",
                new RdfStream());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testInvalidSparqlUpdateValidationAltSyntax() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final FedoraResource object = containerService.findOrCreate(session, pid);
        object.updateProperties(subjects,
                "DELETE WHERE {" +
                        "<> <http://www.loc.gov/premis/rdf/v1#hasDateCreatedByApplication> ?o0 ." +
                        "}; INSERT DATA {" +
                        "<> <http://purl.org/dc/elements/1.1/title/> \"Example Managed binary datastream\" ." +
                        "}",
                new RdfStream());
    }

    @Test
    public void testValidSparqlUpdateValidation1() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final FedoraResource object =
                containerService.findOrCreate(session, pid);
        object.updateProperties(
                subjects,
                "INSERT { <> <http://myurl.org/title> \"5\" . } WHERE { }",
                new RdfStream());
    }

    @Test
    public void testValidSparqlUpdateValidation2() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final FedoraResource object =
                containerService.findOrCreate(session, pid);
        object.updateProperties(
                subjects,
                "PREFIX dsc:<http://myurl.org/title> \n" +
                        "INSERT { <> dsc:p \"ccc\" } WHERE { }",
                new RdfStream());
    }

    @Test
    public void testUpdatingRdfType() throws RepositoryException {
        final FedoraResource object =
            containerService.findOrCreate(session, "/testObjectRdfType");

        object.updateProperties(subjects, "INSERT { <"
                + createGraphSubjectNode(object).getURI() + "> <" + RDF.type
                + "> <http://some/uri> } WHERE { }", new RdfStream());
        assertTrue(object.getNode().isNodeType("{http://some/}uri"));
    }

    @Test
    public void testRemoveRdfType() throws RepositoryException {
        final FedoraResource object =
                containerService.findOrCreate(session, "/testRemoveObjectRdfType");

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
            containerService.findOrCreate(session, "/testEtagObject");

        session.save();

        final String actual = object.getEtagValue();
        assertNotNull(actual);
        assertNotEquals("", actual);
    }

    @Test
    public void testGetReferences() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        containerService.findOrCreate(session, pid);
        final Container subject = containerService.findOrCreate(session, pid + "/a");
        final Container object = containerService.findOrCreate(session, pid + "/b");
        final Value value = session.getValueFactory().createValue(object.getNode());
        subject.getNode().setProperty("fedora:isPartOf", new Value[] { value });

        session.save();

        final Model model = object.getTriples(subjects, ReferencesRdfContext.class).asModel();

        assertTrue(
            model.contains(subjects.reverse().convert(subject),
                              ResourceFactory.createProperty(REPOSITORY_NAMESPACE + "isPartOf"),
                              subjects.reverse().convert(object))
        );
    }

    @Test
    public void testReplaceProperties() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final Container object = containerService.findOrCreate(session, pid);

        final RdfStream triples = object.getTriples(subjects, PropertiesRdfContext.class);
        final Model model = triples.asModel();

        final Resource resource = model.createResource();
        final Resource subject = subjects.reverse().convert(object);
        final Property predicate = model.createProperty("info:xyz");
        model.add(subject, predicate, resource);
        model.add(resource, model.createProperty("http://purl.org/dc/elements/1.1/title"), "xyz");

        object.replaceProperties(subjects, model, object.getTriples(subjects, PropertiesRdfContext.class));

        final Iterator<javax.jcr.Property> properties = object.getNode().getProperties();

        final UnmodifiableIterator<javax.jcr.Property> relation
            = Iterators.filter(properties, new Predicate<javax.jcr.Property>() {
                @Override
                public boolean apply(final javax.jcr.Property property) {
                    try {
                        return property.getName().contains("xyz_ref");
                    } catch (final RepositoryException e) {
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
        containerService.findOrCreate(session, "/" + pid);
        session.save();

        containerService.findOrCreate(session, "/" + pid).delete();

        session.save();

        assertTrue(session.getNode("/" + pid).isNodeType(FEDORA_TOMBSTONE));
    }

    @Test
    public void testDeleteObjectWithInboundReferences() throws RepositoryException {

        final String pid = getRandomPid();
        final FedoraResource resourceA = containerService.findOrCreate(session, "/" + pid + "/a");
        final FedoraResource resourceB = containerService.findOrCreate(session, "/" + pid + "/b");

        final Value value = session.getValueFactory().createValue(resourceB.getNode());
        resourceA.getNode().setProperty("fedora:hasMember", new Value[] { value });

        session.save();
        containerService.findOrCreate(session, "/" + pid + "/a").delete();

        session.save();
        containerService.findOrCreate(session, "/" + pid + "/b").delete();

        session.save();

        assertTrue(session.getNode("/" + pid + "/b").isNodeType(FEDORA_TOMBSTONE));

    }

    @Test
    public void testGetContainer() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid + "/a");

        assertEquals(container, resource.getContainer());
    }

    @Test
    public void testGetChildren() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid + "/a");

        assertEquals(resource, container.getChildren().next());
    }

    @Test
    public void testGetChildrenWithBinary() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        final FedoraResource resource = binaryService.findOrCreate(session, "/" + pid + "/a");

        assertEquals(resource, container.getChildren().next());
    }

    @Test
    public void testGetContainerForBinary() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        final FedoraResource resource = binaryService.findOrCreate(session, "/" + pid + "/a");

        assertEquals(container, resource.getContainer());
    }

    @Test
    public void testGetContainerWithHierarchy() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid + "/a/b/c/d");

        assertEquals(container, resource.getContainer());
    }

    @Test
    public void testGetChildrenWithHierarchy() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid + "/a/b/c/d");

        assertEquals(resource, container.getChildren().next());
    }

    @Test
    public void testGetChildrenTombstonesAreHidden() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid + "/a");

        resource.delete();
        assertFalse(container.getChildren().hasNext());
    }

    @Test
    public void testGetChildrenHidesHashUris() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        containerService.findOrCreate(session, "/" + pid + "/#/a");

        assertFalse(container.getChildren().hasNext());
    }

    @Test
    public void testSetURIProperty() throws URISyntaxException, RepositoryException {
        final String pid1 = getRandomPid();
        final String pid2 = getRandomPid();
        final String prop = "premis:hasEventRelatedObject";
        final FedoraResource resource1 = containerService.findOrCreate(session, "/" + pid1);
        final FedoraResource resource2 = containerService.findOrCreate(session, "/" + pid2);
        final String uri = createGraphSubjectNode(resource2).getURI();
        resource1.setURIProperty(prop, new URI(uri));
        resource2.delete();
        session.save();

        // URI property should survive the linked resource being deleted
        assertTrue(resource1.hasProperty(prop));
        assertEquals(resource1.getProperty(prop).getString(), uri);
    }

    @Test
    public void testGetUnfrozenResource() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        object.enableVersioning();
        session.save();
        addVersionLabel("some-label", object);
        final Version version = object.getVersionHistory().getVersionByLabel("some-label");
        session.save();
        final FedoraResourceImpl frozenResource = new FedoraResourceImpl(version.getFrozenNode());

        assertEquals(object, frozenResource.getUnfrozenResource());

    }

    @Test
    public void testGetVersionedAncestor() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        object.enableVersioning();
        session.save();
        containerService.findOrCreate(session, "/" + pid + "/a/b/c");
        session.save();
        session.getWorkspace().getVersionManager().checkpoint(object.getPath());
        addVersionLabel("some-label", object);
        session.save();
        final Version version = object.getVersionHistory().getVersionByLabel("some-label");
        final FedoraResourceImpl frozenResource = new FedoraResourceImpl(version.getFrozenNode().getNode("a"));

        assertEquals(object, frozenResource.getVersionedAncestor().getUnfrozenResource());

    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testNullBaseVersion() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        session.save();
        object.getBaseVersion();
    }


    @Test (expected = RepositoryRuntimeException.class)
    public void testNullVersionHistory() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        session.save();
        object.getVersionHistory();
    }

    @Test
    public void testGetNodeVersion() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        object.enableVersioning();
        session.save();
        containerService.findOrCreate(session, "/" + pid + "/a/b/c");
        session.save();
        session.getWorkspace().getVersionManager().checkpoint(object.getPath());
        addVersionLabel("some-label", object);
        session.save();
        final Version version = object.getVersionHistory().getVersionByLabel("some-label");
        final FedoraResourceImpl frozenResource = new FedoraResourceImpl(version.getFrozenNode());
        assertNull(frozenResource.getNodeVersion("some-label"));

    }

    @Test
    public void testGetNullNodeVersion() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        object.enableVersioning();
        session.save();
        containerService.findOrCreate(session, "/" + pid + "/a/b/c");
        session.save();
        session.getWorkspace().getVersionManager().checkpoint(object.getPath());
        addVersionLabel("some-label", object);
        session.save();
        final Version version = object.getVersionHistory().getVersionByLabel("some-label");
        final FedoraResourceImpl frozenResource = new FedoraResourceImpl(version.getFrozenNode().getNode("a"));
        assertNull(frozenResource.getNodeVersion("some-label"));

    }

    @Test
    public void testNullLastModifiedDate() {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        assertFalse(object.hasProperty(JCR_LASTMODIFIED));
        assertNull(object.getLastModifiedDate());
    }

    @Test
    public void testDisableVersioning() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        object.enableVersioning();
        session.save();
        assertTrue(object.isVersioned());
        object.disableVersioning();
        assertFalse(object.isVersioned());
    }

    @Test (expected = RepositoryRuntimeException.class)
    public void testDisableVersioningException() {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        object.disableVersioning();
    }

    @Test
    public void testHash() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        object.enableVersioning();
        session.save();
        final FedoraResourceImpl frozenResource = new FedoraResourceImpl(object.getNode());
        assertFalse(frozenResource.hashCode() == 0);
    }

    @Test
    public void testDeletePartOfMultiValueProperty() throws RepositoryException {
        final String pid = UUID.randomUUID().toString();
        final String relation = "test:fakeRel";
        containerService.findOrCreate(session, pid);
        final Container subject = containerService.findOrCreate(session, pid + "/a");
        final Container referent1 = containerService.findOrCreate(session, pid + "/b");
        final Container referent2 = containerService.findOrCreate(session, pid + "/c");
        final Value[] values = new Value[2];
        values[0] = session.getValueFactory().createValue(referent1.getNode());
        values[1] = session.getValueFactory().createValue(referent2.getNode());
        subject.getNode().setProperty(relation, values);

        session.save();

        final Model model1 = referent1.getTriples(subjects, ReferencesRdfContext.class).asModel();

        assertTrue(model1.contains(subjects.reverse().convert(subject),
                createProperty("info:fedora/test/fakeRel"),
                createResource("info:fedora/" + pid + "/b")));

        assertTrue(model1.contains(subjects.reverse().convert(subject),
                createProperty("info:fedora/test/fakeRel"),
                createResource("info:fedora/" + pid + "/c")));

        // This is the test! Ensure that only the delete resource is removed from the "subject" container.
        referent2.delete();

        final Model model2 = referent1.getTriples(subjects, ReferencesRdfContext.class).asModel();

        assertTrue(model2.contains(subjects.reverse().convert(subject),
            createProperty("info:fedora/test/fakeRel"),
            createResource("info:fedora/" + pid + "/b")));

        assertFalse(model2.contains(subjects.reverse().convert(subject),
                createProperty("info:fedora/test/fakeRel"),
                createResource("info:fedora/" + pid + "/c")));
    }

    private void addVersionLabel(final String label, final FedoraResource r) throws RepositoryException {
        addVersionLabel(label, session.getWorkspace().getVersionManager().getBaseVersion(r.getPath()));
    }

    private static void addVersionLabel(final String label, final Version v) throws RepositoryException {
        v.getContainingHistory().addVersionLabel(v.getName(), label, false);
    }
}
