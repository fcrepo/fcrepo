/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.kernel.modeshape;

import static java.net.URI.create;
import static java.util.Collections.emptySet;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.vocabulary.RDF.type;
import static javax.jcr.PropertyType.BINARY;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_REPOSITORY_ROOT;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_TOMBSTONE;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MIXIN_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_NODE_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_PRIMARY_IDENTIFIER;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_VERSION_LABEL;
import static org.fcrepo.kernel.api.RdfLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.JCR_NT_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MIX_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MODE_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RequiredRdfContext.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.RequiredRdfContext.SERVER_MANAGED;
import static org.fcrepo.kernel.api.RequiredRdfContext.VERSIONS;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.ROOT;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.UncheckedPredicate.uncheck;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
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

import com.google.common.collect.Iterators;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.InvalidPrefixException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.NonRdfSourceDescriptionImpl;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;

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
        assertTrue(object.hasType(ROOT));
        assertTrue(object.hasType(FEDORA_REPOSITORY_ROOT));
        session.logout();
    }

    private Node createGraphSubjectNode(final FedoraResource obj) {
        return subjects.reverse().convert(obj).asNode();
    }

    @Test
    public void testRandomNodeGraph() {
        final FedoraResource object = containerService.findOrCreate(session, "/testNodeGraph");
        final Node s = subjects.reverse().convert(object).asNode();
        final Model rdf = object.getTriples(subjects, PROPERTIES).collect(toModel());

        assertFalse(rdf.getGraph().contains(s, HAS_PRIMARY_IDENTIFIER.asNode(), ANY));
        assertFalse(rdf.getGraph().contains(s, HAS_PRIMARY_TYPE.asNode(), ANY));
        assertFalse(rdf.getGraph().contains(s, HAS_NODE_TYPE.asNode(), ANY));
        assertFalse(rdf.getGraph().contains(s, HAS_MIXIN_TYPE.asNode(), ANY));
    }

    @Test
    public void testLastModified() throws RepositoryException {
        final String pid = getRandomPid();
        containerService.findOrCreate(session, "/" + pid);

        try {
            session.save();
        } finally {
            session.logout();
        }

        session = repo.login();

        final Container obj2 = containerService.findOrCreate(session, "/" + pid);
        final Date created = roundDate(obj2.getCreatedDate());
        final Date modified = roundDate(obj2.getLastModifiedDate());
        assertFalse(modified + " should not be before " + created, modified.before(created));

        final Graph graph = obj2.getTriples(subjects, PROPERTIES).collect(toModel()).getGraph();
        final Node s = createGraphSubjectNode(obj2);
        final ExtendedIterator<Triple> iter = graph.find(s, LAST_MODIFIED_DATE.asNode(), ANY);
        assertEquals("Should have one lastModified triple", 1, iter.toList().size());
    }

    @Test
    public void testTouch() throws RepositoryException {
        final String pid = getRandomPid();
        containerService.findOrCreate(session, "/" + pid);

        try {
            session.save();
        } finally {
            session.logout();
        }

        session = repo.login();

        final Container obj2 = containerService.findOrCreate(session, "/" + pid);
        final FedoraResourceImpl impl = new FedoraResourceImpl(getJcrNode(obj2));
        final Date oldMod = impl.getLastModifiedDate();
        impl.touch();
        assertTrue( oldMod.before(obj2.getLastModifiedDate()) );
    }

    @Test
    public void testRepositoryRootGraph() {

        final FedoraResource object = nodeService.find(session, "/");
        final Graph graph = object.getTriples(subjects, SERVER_MANAGED).collect(toModel()).getGraph();

        final Node s = createGraphSubjectNode(object);

        Node p =
            createURI(REPOSITORY_NAMESPACE
                    + "repositoryJcrRepositoryVendorUrl");
        Node o = createLiteral("http://www.modeshape.org");
        assertFalse(graph.contains(s, p, o));

        p = HAS_NODE_TYPE.asNode();
        o = createLiteral(FEDORA_RESOURCE);
        assertFalse(graph.contains(s, p, o));

        assertTrue(graph.contains(s, type.asNode(), createURI(REPOSITORY_NAMESPACE + "Resource")));
        assertTrue(graph.contains(s, type.asNode(), createURI(REPOSITORY_NAMESPACE + "RepositoryRoot")));
        assertTrue(graph.contains(s, type.asNode(), createURI(REPOSITORY_NAMESPACE + "Container")));

    }

    @Test
    public void testObjectGraph() {

        final String pid = "/" + getRandomPid();
        final FedoraResource object =
            containerService.findOrCreate(session, pid);
        final Graph graph = object.getTriples(subjects, SERVER_MANAGED).collect(toModel()).getGraph();

        // jcr property
        Node s = createGraphSubjectNode(object);
        Node p = HAS_PRIMARY_IDENTIFIER.asNode();
        assertFalse(graph.contains(s, p, ANY));

        // multivalued property
        s = createGraphSubjectNode(object);
        p = HAS_MIXIN_TYPE.asNode();
        Node o = createLiteral(FEDORA_RESOURCE);
        assertFalse(graph.contains(s, p, o));

        o = createLiteral(FEDORA_CONTAINER);
        assertFalse(graph.contains(s, p, o));

    }


    @Test
    public void testObjectGraphWithCustomProperty() throws RepositoryException {

        FedoraResource object =
            containerService.findOrCreate(session, "/testObjectGraph");

        final javax.jcr.Node node = getJcrNode(object);
        node.setProperty("dc:title", "this-is-some-title");
        node.setProperty("dc:subject", "this-is-some-subject-stored-as-a-binary", BINARY);
        node.setProperty("jcr:data", "jcr-data-should-be-ignored", BINARY);

        session.save();
        session.logout();

        session = repo.login();

        object = containerService.findOrCreate(session, "/testObjectGraph");


        final Graph graph = object.getTriples(subjects, PROPERTIES).collect(toModel()).getGraph();

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
        final javax.jcr.Node node = getJcrNode(object);
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
                object.getTriples(subjects, PROPERTIES).collect(toModel()).getGraph().contains(s, p, o));
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

        final FedoraResource object = binaryService.findOrCreate(session, "/testDatastreamGraph").getDescription();

        getJcrNode(object).setProperty("fedora:isPartOf",
                session.getNode("/testDatastreamGraphParent"));

        final Graph graph = object.getTriples(subjects, PROPERTIES).collect(toModel()).getGraph();

        // multivalued property
        final Node s = createGraphSubjectNode(object);
        Node p = HAS_MIXIN_TYPE.asNode();
        Node o = createLiteral(FEDORA_RESOURCE);
        assertFalse(graph.contains(s, p, o));

        o = createLiteral(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
        assertFalse(graph.contains(s, p, o));

        // structure
        //TODO: re-enable number of children reporting, if practical

        //assertTrue(datasetGraph.contains(ANY, s, p, o));
        // relations
        p = createURI(REPOSITORY_NAMESPACE + "isPartOf");
        o = createGraphSubjectNode(parentObject);
        assertTrue(graph.contains(s, p, o));

    }

    @Test
    public void testUpdatingObjectGraph() {

        final Node subject = createURI("info:fedora/testObjectGraphUpdates");
        final FedoraResource object =
            containerService.findOrCreate(session, "/testObjectGraphUpdates");

        object.updateProperties(subjects, "INSERT { " + "<"
                + createGraphSubjectNode(object).getURI() + "> "
                + "<info:fcrepo/zyx> \"a\" } WHERE {} ", object.getTriples(subjects, emptySet()));

        // jcr property
        final Resource s = createResource(createGraphSubjectNode(object).getURI());
        final Property p = createProperty("info:fcrepo/zyx");
        Literal o = createPlainLiteral("a");
        Model model = object.getTriples(subjects, PROPERTIES).collect(toModel());
        assertTrue(model.contains(s, p, o));

        object.updateProperties(subjects, "DELETE { " + "<"
                + createGraphSubjectNode(object).getURI() + "> "
                + "<info:fcrepo/zyx> ?o }\n" + "INSERT { " + "<"
                + createGraphSubjectNode(object).getURI() + "> "
                + "<info:fcrepo/zyx> \"b\" } " + "WHERE { " + "<"
                + createGraphSubjectNode(object).getURI() + "> "
                + "<info:fcrepo/zyx> ?o } ", DefaultRdfStream.fromModel(subject, model));

        model = object.getTriples(subjects, PROPERTIES).collect(toModel());

        assertFalse("found value we should have removed", model.contains(s, p, o));
        o = createPlainLiteral("b");
        assertTrue("could not find new value", model.contains(s, p, o));

    }

    @Test
    public void testGetRootObjectTypes() {

        final FedoraResource object = nodeService.find(session, "/");

        final List<URI> types = object.getTypes();

        assertFalse(types.stream()
            .map(x -> x.toString())
            .anyMatch(x -> x.startsWith(JCR_NAMESPACE) || x.startsWith(MIX_NAMESPACE) ||
                    x.startsWith(MODE_NAMESPACE) || x.startsWith(JCR_NT_NAMESPACE)));
    }

    @Test
    public void testGetObjectTypes() {

        final FedoraResource object =
            containerService.findOrCreate(session, "/testObjectVersionGraph");

        final List<URI> types = object.getTypes();

        assertTrue(types.contains(create(REPOSITORY_NAMESPACE + "Container")));
        assertTrue(types.contains(create(REPOSITORY_NAMESPACE + "Resource")));
        assertFalse(types.stream()
            .map(x -> x.toString())
            .anyMatch(x -> x.startsWith(JCR_NAMESPACE) || x.startsWith(MIX_NAMESPACE) ||
                    x.startsWith(MODE_NAMESPACE) || x.startsWith(JCR_NT_NAMESPACE)));
    }

    @Test
    public void testGetObjectVersionGraph() throws RepositoryException {

        final FedoraResource object =
            containerService.findOrCreate(session, "/testObjectVersionGraph");

        getJcrNode(object).addMixin("mix:versionable");
        session.save();

        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session, object.getPath(), "v0.0.1");
        session.save();

        final Model graphStore = object.getTriples(subjects, VERSIONS).collect(toModel());

        logger.debug(graphStore.toString());

        // go querying for the version URI
        Resource s = createResource(createGraphSubjectNode(object).getURI());
        final ExtendedIterator<Statement> triples = graphStore.listStatements(s,HAS_VERSION, (RDFNode)null);

        final List<Statement> list = triples.toList();
        assertEquals(1, list.size());

        // make sure the URI is derived from the label
        s = list.get(0).getObject().asResource();
        assertEquals("URI should be derived from label.", s.getURI(), createGraphSubjectNode(object).getURI()
                + "/" + FCR_VERSIONS + "/v0.0.1");

        // make sure the label is listed
        assertTrue(graphStore.contains(s, HAS_VERSION_LABEL, createPlainLiteral("v0.0.1")));
    }

    @Test(expected = MalformedRdfException.class)
    public void testAddMissingReference() throws MalformedRdfException {
        final FedoraResource object =
                containerService.findOrCreate(session, "/testRefObject");

        object.updateProperties(
                subjects,
                "PREFIX example: <http://example.org/>\n"
                        + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                        + "PREFIX fedora: <" + REPOSITORY_NAMESPACE + ">\n"
                        + "INSERT { <> fedora:isPartOf <" + subjects.toDomain("/some-path") + ">}"
                        + "WHERE { }", object.getTriples(subjects, emptySet()));
    }

    @Test(expected = AccessDeniedException.class)
    public void testUpdateDenied() throws RepositoryException {
        final FedoraResource object =
                containerService.findOrCreate(session, "/testRefObject");
        try {
            object.updateProperties(
                    subjects,
                    "INSERT { <> <http://purl.org/dc/elements/1.1/title> \"test-original\". }"
                            + " WHERE { }", object.getTriples(subjects, emptySet()));
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
                        + " WHERE { }", object.getTriples(subjects, emptySet()));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testInvalidSparqlUpdateValidation() {
        final String pid = getRandomPid();
        final FedoraResource object =
                containerService.findOrCreate(session, pid);
        object.updateProperties(
                subjects,
                "INSERT { <> <http://myurl.org/title/> \"fancy title\" . \n" +
                " <> <http://myurl.org/title/> \"fancy title 2\" . } WHERE { }",
                object.getTriples(subjects, emptySet()));
    }

    @Test (expected = InvalidPrefixException.class)
    public void testInvalidPrefixSparqlUpdateValidation() {
        final String pid = getRandomPid();
        final FedoraResource object =
                containerService.findOrCreate(session, pid);
        object.updateProperties(
                subjects,
                "PREFIX pcdm: <http://pcdm.org/models#>\n"
                        + "INSERT { <> a pcdm:Object}\n"
                        + "WHERE { }", object.getTriples(subjects, emptySet()));
        object.updateProperties(
                subjects,
                "PREFIX pcdm: <http://garbage.org/models#>\n"
                        + "INSERT { <> a pcdm:Garbage}\n"
                        + "WHERE { }", object.getTriples(subjects, emptySet()));
    }

    @Test
    public void testValidSparqlUpdateWithLiteralTrailingSlash() {
        final String pid = getRandomPid();
        final FedoraResource object = containerService.findOrCreate(session, pid);
        object.updateProperties(
                subjects,
                "INSERT { <> <http://myurl.org/title> \"fancy title/\" . \n" +
                " <> <http://myurl.org/title> \"fancy title 2<br/>\" . } WHERE { }",
                object.getTriples(subjects, emptySet()));
    }

    @Test
    public void testValidSparqlUpdateValidationAltSyntax() {
        final String pid = getRandomPid();
        final FedoraResource object = containerService.findOrCreate(session, pid);
        object.updateProperties(subjects,
                "DELETE WHERE {" +
                        "<> <http://www.loc.gov/premis/rdf/v1#hasDateCreatedByApplication> ?o0 ." +
                        "}; INSERT DATA {" +
                        "<> <http://purl.org/dc/elements/1.1/title> \"Example Managed binary datastream\" ." +
                        "}",
                object.getTriples(subjects, emptySet()));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testInvalidSparqlUpdateValidationAltSyntax() {
        final String pid = getRandomPid();
        final FedoraResource object = containerService.findOrCreate(session, pid);
        object.updateProperties(subjects,
                "DELETE WHERE {" +
                        "<> <http://www.loc.gov/premis/rdf/v1#hasDateCreatedByApplication> ?o0 ." +
                        "}; INSERT DATA {" +
                        "<> <http://purl.org/dc/elements/1.1/title/> \"Example Managed binary datastream\" ." +
                        "}",
                object.getTriples(subjects, emptySet()));
    }

    @Test
    public void testValidSparqlUpdateValidation1() {
        final String pid = getRandomPid();
        final FedoraResource object =
                containerService.findOrCreate(session, pid);
        object.updateProperties(
                subjects,
                "INSERT { <> <http://myurl.org/title> \"5\" . } WHERE { }",
                object.getTriples(subjects, emptySet()));
    }

    @Test
    public void testValidSparqlUpdateValidation2() {
        final String pid = getRandomPid();
        final FedoraResource object =
                containerService.findOrCreate(session, pid);
        object.updateProperties(
                subjects,
                "PREFIX dsc:<http://myurl.org/title> \n" +
                        "INSERT { <> dsc:p \"ccc\" } WHERE { }",
                object.getTriples(subjects, emptySet()));
    }

    @Test
    public void testUpdatingRdfType() throws RepositoryException {
        final FedoraResource object =
            containerService.findOrCreate(session, "/testObjectRdfType");

        object.updateProperties(subjects, "INSERT { <"
                + createGraphSubjectNode(object).getURI() + "> <" + RDF.type
                + "> <http://some/uri> } WHERE { }", object.getTriples(subjects, emptySet() ));
        assertTrue(getJcrNode(object).isNodeType("{http://some/}uri"));
    }

    @Test
    public void testRemoveRdfType() throws RepositoryException {
        final FedoraResource object =
                containerService.findOrCreate(session, "/testRemoveObjectRdfType");

        object.updateProperties(subjects, "INSERT { <"
                + createGraphSubjectNode(object).getURI() + "> <" + RDF.type
                + "> <http://some/uri> } WHERE { }", object.getTriples(subjects, PROPERTIES));
        assertTrue(getJcrNode(object).isNodeType("{http://some/}uri"));

        object.updateProperties(subjects, "DELETE { <"
                + createGraphSubjectNode(object).getURI() + "> <" + RDF.type
                + "> <http://some/uri> } WHERE { }", object.getTriples(subjects, PROPERTIES));
        assertFalse(getJcrNode(object).isNodeType("{http://some/}uri"));
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
        final String pid = getRandomPid();
        containerService.findOrCreate(session, pid);
        final Container subject = containerService.findOrCreate(session, pid + "/a");
        final Container object = containerService.findOrCreate(session, pid + "/b");
        final Value value = session.getValueFactory().createValue(getJcrNode(object));
        getJcrNode(subject).setProperty("fedora:isPartOf", new Value[] { value });

        session.save();

        final Model model = object.getTriples(subjects, INBOUND_REFERENCES).collect(toModel());

        assertTrue(
            model.contains(subjects.reverse().convert(subject),
                              ResourceFactory.createProperty(REPOSITORY_NAMESPACE + "isPartOf"),
                              subjects.reverse().convert(object))
        );
    }

    @Test
    public void testReplaceProperties() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, pid);

        try (final RdfStream triples = object.getTriples(subjects, PROPERTIES)) {
            final Model model = triples.collect(toModel());

            final Resource resource = model.createResource();
            final Resource subject = subjects.reverse().convert(object);
            final Property predicate = model.createProperty("info:xyz");
            model.add(subject, predicate, resource);
            model.add(resource, model.createProperty("http://purl.org/dc/elements/1.1/title"), "xyz");

            object.replaceProperties(subjects, model, object.getTriples(subjects, PROPERTIES));

            @SuppressWarnings("unchecked")
            final Iterator<javax.jcr.Property> properties = getJcrNode(object).getProperties();

            final Iterator<javax.jcr.Property> relation = Iterators.filter(properties, uncheck(
                    (final javax.jcr.Property p) -> p.getName().contains("xyz_ref"))::test);

            assertTrue(relation.hasNext());

            final javax.jcr.Property next = relation.next();
            final Value[] values = next.getValues();
            assertEquals(1, values.length);

            final javax.jcr.Node skolemizedNode = session.getNodeByIdentifier(values[0].getString());

            assertTrue(skolemizedNode.getPath().contains("/.well-known/genid/"));
            assertEquals("xyz", skolemizedNode.getProperty("dc:title").getValues()[0].getString());
        }
    }

    @Test
    public void testReplacePropertiesHashURIs() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, pid);
        final Model model = object.getTriples(subjects, PROPERTIES).collect(toModel());

        final Resource hashResource = createResource(createGraphSubjectNode(object).getURI() + "#creator");
        final Property foafName = model.createProperty("http://xmlns.com/foaf/0.1/name");
        final Literal nameValue = model.createLiteral("xyz");
        final Resource foafPerson = createResource("http://xmlns.com/foaf/0.1/Person");

        model.add(hashResource, foafName, nameValue);
        model.add(hashResource, type, foafPerson);

        final Resource subject = subjects.reverse().convert(object);
        final Property dcCreator = model.createProperty("http://purl.org/dc/elements/1.1/creator");

        model.add(subject, dcCreator, hashResource);

        object.replaceProperties(subjects, model, object.getTriples(subjects, PROPERTIES));
        assertEquals(1, getJcrNode(object).getNode("#").getNodes().getSize());

        final Model updatedModel = object.getTriples(subjects, PROPERTIES).collect(toModel());

        updatedModel.remove(hashResource, foafName, nameValue);
        object.replaceProperties(subjects, updatedModel, object.getTriples(subjects, PROPERTIES));
        assertEquals(1, getJcrNode(object).getNode("#").getNodes().getSize());

        final Model updatedModel2 = object.getTriples(subjects, PROPERTIES).collect(toModel());

        updatedModel2.remove(hashResource, type, foafPerson);
        object.replaceProperties(subjects, updatedModel2, object.getTriples(subjects, PROPERTIES));
        assertEquals(1, getJcrNode(object).getNode("#").getNodes().getSize());

        final Model updatedModel3 = object.getTriples(subjects, PROPERTIES).collect(toModel());

        updatedModel3.remove(subject, dcCreator, hashResource);
        object.replaceProperties(subjects, updatedModel3, object.getTriples(subjects, PROPERTIES));
        assertEquals(0, getJcrNode(object).getNode("#").getNodes().getSize());
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

        final Value value = session.getValueFactory().createValue(getJcrNode(resourceB));
        getJcrNode(resourceA).setProperty("fedora:hasMember", new Value[] { value });

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

        assertEquals(resource, container.getChildren().findFirst().get());
    }

    @Test
    public void testGetChildrenRecursively() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        containerService.findOrCreate(session, "/" + pid + "/a");
        containerService.findOrCreate(session, "/" + pid + "/a/b");
        containerService.findOrCreate(session, "/" + pid + "/a/b/c");
        containerService.findOrCreate(session, "/" + pid + "/a/c/d");
        containerService.findOrCreate(session, "/" + pid + "/a/c/e");

        assertEquals(5, container.getChildren(true).count());
        assertEquals(1, container.getChildren(false).count());
    }

    @Test
    public void testGetChildrenWithBinary() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        final FedoraResource resource = binaryService.findOrCreate(session, "/" + pid + "/a");

        assertEquals(resource, container.getChildren().findFirst().get());
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

        assertEquals(resource, container.getChildren().findFirst().get());
    }

    @Test
    public void testGetChildrenTombstonesAreHidden() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid + "/a");

        resource.delete();
        assertFalse(container.getChildren().findFirst().isPresent());
    }

    @Test
    public void testGetChildrenHidesHashUris() {
        final String pid = getRandomPid();
        final Container container = containerService.findOrCreate(session, "/" + pid);
        containerService.findOrCreate(session, "/" + pid + "/#/a");

        assertFalse(container.getChildren().findFirst().isPresent());
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

    @Test
    public void testVersionedChild() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        object.enableVersioning();
        session.save();

        final Container child = containerService.findOrCreate(session, "/" + pid + "/child");
        child.enableVersioning();
        session.save();

        session.getWorkspace().getVersionManager().checkpoint(object.getPath());
        addVersionLabel("object-v0", object);
        session.save();

        session.getWorkspace().getVersionManager().checkpoint(child.getPath());
        addVersionLabel("child-v0", child);
        session.save();

        final Version version = object.getVersionHistory().getVersionByLabel("object-v0");
        final FedoraResourceImpl frozenResource = new FedoraResourceImpl(version.getFrozenNode().getNode("child"));

        assertEquals(child, frozenResource.getVersionedAncestor().getUnfrozenResource());
    }

    @Test
    public void testVersionedChildDeleted() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        object.enableVersioning();
        session.save();

        final Container child = containerService.findOrCreate(session, "/" + pid + "/child");
        getJcrNode(child).setProperty("dc:title", "this-is-some-title");
        session.save();

        session.getWorkspace().getVersionManager().checkpoint(object.getPath());
        addVersionLabel("object-v0", object);
        session.save();

        // Delete the child!
        child.delete();
        session.save();

        final Version version = object.getVersionHistory().getVersionByLabel("object-v0");
        final FedoraResourceImpl frozenResource = new FedoraResourceImpl(version.getFrozenNode());

        // Parent version is correct
        assertEquals(object, frozenResource.getVersionedAncestor().getUnfrozenResource());

        // Versioned child still exists
        final FedoraResourceImpl frozenChild = new FedoraResourceImpl(version.getFrozenNode().getNode("child"));
        final javax.jcr.Property property = frozenChild.getNode().getProperty("dc:title");
        assertNotNull(property);
        assertEquals("this-is-some-title", property.getString());
    }

    @Test
    public void testVersionedChildBinaryDeleted() throws RepositoryException, InvalidChecksumException, IOException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        object.enableVersioning();
        session.save();

        final FedoraBinary child = binaryService.findOrCreate(session, "/" + pid + "/child");
        final String content = "123456789test123456789";
        child.setContent(new ByteArrayInputStream(content.getBytes()), "text/plain", null, null, null);
        session.save();

        session.getWorkspace().getVersionManager().checkpoint(object.getPath());
        addVersionLabel("object-v0", object);
        session.save();

        // Delete the child!
        child.delete();
        session.save();

        final Version version = object.getVersionHistory().getVersionByLabel("object-v0");
        final FedoraResourceImpl frozenResource = new FedoraResourceImpl(version.getFrozenNode());

        // Parent version is correct
        assertEquals(object, frozenResource.getVersionedAncestor().getUnfrozenResource());

        // Versioned child still exists
        final NonRdfSourceDescription frozenChild =
                new NonRdfSourceDescriptionImpl(version.getFrozenNode().getNode("child"));
        try (final InputStream contentStream = ((FedoraBinary) frozenChild.getDescribedResource()).getContent()) {
            assertNotNull(contentStream);
            assertEquals(content, IOUtils.toString(contentStream));
        }
    }

    @Test
    public void testDeleteLinkedVersionedResources() throws RepositoryException {
        final Container object1 = containerService.findOrCreate(session, "/" + getRandomPid());
        final Container object2 = containerService.findOrCreate(session, "/" + getRandomPid());

        // Create a link between objects 1 and 2
        object2.updateProperties(subjects, "PREFIX example: <http://example.org/>\n" +
                "INSERT { <> <example:link> " + "<" + createGraphSubjectNode(object1).getURI() + ">" +
                " } WHERE {} ",
                object2.getTriples(subjects, emptySet()));

        // Create version of object2
        versionService.createVersion(session, object2.getPath(), "obj2-v0");

        // Verify that the objects exist
        assertTrue("object1 should exist!", exists(object1));
        assertTrue("object2 should exist!", exists(object2));

        // This is the test: verify successful deletion of the objects
        object2.delete();
        session.save();

        object1.delete();
        session.save();

        // Double-verify that the objects are gone
        assertFalse("/object2 should NOT exist!", exists(object2));
        assertFalse("/object1 should NOT exist!", exists(object1));
    }

    private boolean exists(final Container resource) {
        try {
            resource.getPath();
            return true;
        } catch (RepositoryRuntimeException e) {
            return false;
        }
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
        assertNull(frozenResource.getVersion("some-label"));

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
        assertNull(frozenResource.getVersion("some-label"));

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
        final FedoraResourceImpl frozenResource = new FedoraResourceImpl(getJcrNode(object));
        assertFalse(frozenResource.hashCode() == 0);
    }

    @Test
    public void testDeletePartOfMultiValueProperty() throws RepositoryException {
        final String pid = getRandomPid();
        final String relation = "test:fakeRel";
        containerService.findOrCreate(session, pid);
        final Container subject = containerService.findOrCreate(session, pid + "/a");
        final Container referent1 = containerService.findOrCreate(session, pid + "/b");
        final Container referent2 = containerService.findOrCreate(session, pid + "/c");
        final Value[] values = new Value[2];
        values[0] = session.getValueFactory().createValue(getJcrNode(referent1));
        values[1] = session.getValueFactory().createValue(getJcrNode(referent2));
        getJcrNode(subject).setProperty(relation, values);

        session.save();

        final Model model1 = referent1.getTriples(subjects, INBOUND_REFERENCES).collect(toModel());

        assertTrue(model1.contains(subjects.reverse().convert(subject),
                createProperty("info:fedora/test/fakeRel"),
                createResource("info:fedora/" + pid + "/b")));

        assertTrue(model1.contains(subjects.reverse().convert(subject),
                createProperty("info:fedora/test/fakeRel"),
                createResource("info:fedora/" + pid + "/c")));

        // This is the test! Ensure that only the delete resource is removed from the "subject" container.
        referent2.delete();

        final Model model2 = referent1.getTriples(subjects, INBOUND_REFERENCES).collect(toModel());

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

    private static Date roundDate(final Date date) {
        return new Date(date.getTime() - date.getTime() % 1000);
    }
}
