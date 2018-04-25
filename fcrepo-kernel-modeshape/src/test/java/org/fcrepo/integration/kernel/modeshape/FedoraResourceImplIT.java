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
import static javax.jcr.PropertyType.BINARY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.jcr.version.Version;

import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.AccessDeniedException;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.InvalidPrefixException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.Container;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.FedoraTimeMap;
import org.fcrepo.kernel.api.models.FedoraWebacAcl;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.services.VersionService;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.jcr.security.SimplePrincipal;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.collect.Iterators;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_CREATEDBY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_LASTMODIFIEDBY;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_NON_RDF_SOURCE_DESCRIPTION;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_REPOSITORY_ROOT;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_RESOURCE;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_TIME_MAP;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_TOMBSTONE;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_WEBAC_ACL;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RequiredRdfContext.INBOUND_REFERENCES;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.api.RequiredRdfContext.SERVER_MANAGED;
import static org.fcrepo.kernel.api.RequiredRdfContext.VERSIONS;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FIELD_DELIMITER;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.ROOT;
import static org.fcrepo.kernel.modeshape.FedoraResourceImpl.LDPCV_TIME_MAP;
import static org.fcrepo.kernel.modeshape.FedoraResourceImpl.CONTAINER_WEBAC_ACL;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.HAS_MIXIN_TYPE;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.HAS_NODE_TYPE;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.HAS_PRIMARY_IDENTIFIER;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.JCR_NAMESPACE;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.JCR_NT_NAMESPACE;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.MIX_NAMESPACE;
import static org.fcrepo.kernel.modeshape.RdfJcrLexicon.MODE_NAMESPACE;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.UncheckedPredicate.uncheck;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDstring;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ResourceFactory.createPlainLiteral;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DC_11.title;
import static org.apache.jena.vocabulary.RDF.type;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * <p>FedoraResourceImplIT class.</p>
 *
 * @author ajs6f
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class FedoraResourceImplIT extends AbstractIT {

    @Inject
    FedoraRepository repo;

    @Inject
    NodeService nodeService;

    @Inject
    ContainerService containerService;

    @Inject
    BinaryService binaryService;

    @Inject
    VersionService versionService;

    private FedoraSession session;

    private DefaultIdentifierTranslator subjects;

    @Before
    public void setUp() throws RepositoryException {
        session = repo.login();
        subjects = new DefaultIdentifierTranslator(getJcrSession(session));
    }

    @After
    public void tearDown() {
        session.expire();
    }

    @Test
    public void testGetRootNode() throws RepositoryException {
        final FedoraSession session = repo.login();
        final FedoraResource object = nodeService.find(session, "/");
        assertEquals("/", object.getPath());
        assertTrue(object.hasType(ROOT));
        assertTrue(object.hasType(FEDORA_REPOSITORY_ROOT));
        session.expire();
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
            session.commit();
        } finally {
            session.expire();
        }

        session = repo.login();

        final Container obj2 = containerService.findOrCreate(session, "/" + pid);
        final Instant created = roundDate(obj2.getCreatedDate());
        final Instant modified = roundDate(obj2.getLastModifiedDate());
        assertFalse(modified + " should not be before " + created, modified.isBefore(created));

        final Graph graph = obj2.getTriples(subjects, PROPERTIES).collect(toModel()).getGraph();
        final Node s = createGraphSubjectNode(obj2);
        final ExtendedIterator<Triple> iter = graph.find(s, LAST_MODIFIED_DATE.asNode(), ANY);
        assertEquals("Should have one lastModified triple", 1, iter.toList().size());
    }

    @Test
    public void testImplicitTouch() throws RepositoryException {
        final String pid = getRandomPid();
        containerService.findOrCreate(session, "/" + pid);

        try {
            session.commit();
        } finally {
            session.expire();
        }

        session = repo.login();

        final Container obj2 = containerService.findOrCreate(session, "/" + pid);
        final FedoraResourceImpl impl = new FedoraResourceImpl(getJcrNode(obj2));
        final Instant oldMod = impl.getLastModifiedDate();
        impl.touch(false, null, null, null, null);
        assertTrue(oldMod.isBefore(obj2.getLastModifiedDate()));
    }

    @Test
    public void testTouch() throws RepositoryException {
        final Calendar specified = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        specified.add(Calendar.YEAR, 1);

        final String pid = getRandomPid();
        containerService.findOrCreate(session, "/" + pid);

        try {
            session.commit();
        } finally {
            session.expire();
        }

        session = repo.login();

        final Container obj2 = containerService.findOrCreate(session, "/" + pid);
        final FedoraResourceImpl impl = new FedoraResourceImpl(getJcrNode(obj2));

        final String specifiedUser = "me";
        specified.add(Calendar.YEAR, 1);

        impl.touch(false, specified, specifiedUser, specified, specifiedUser);

        assertEquals(specifiedUser, impl.getNode().getProperty(FEDORA_LASTMODIFIEDBY).getString());
        assertEquals(specifiedUser, impl.getNode().getProperty(FEDORA_CREATEDBY).getString());
        assertEquals(specified.getTime(), Date.from(obj2.getLastModifiedDate()));
        assertEquals(specified.getTime(), Date.from(obj2.getCreatedDate()));
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

        session.commit();
        session.expire();

        session = repo.login();

        object = containerService.findOrCreate(session, "/testObjectGraph");


        final Graph graph = object.getTriples(subjects, PROPERTIES).collect(toModel()).getGraph();

        // jcr property
        final Node s = createGraphSubjectNode(object);
        Node p = title.asNode();
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
        final Session jcrSession = getJcrSession(session);
        final NodeTypeManager mgr = jcrSession.getWorkspace().getNodeTypeManager();
        //create supertype mixin
        final NodeTypeTemplate type1 = mgr.createNodeTypeTemplate();
        type1.setName("test:aSupertype");
        type1.setMixin(true);
        final NodeTypeDefinition[] nodeTypes = new NodeTypeDefinition[]{type1};
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

        session.commit();
        session.expire();
        session = repo.login();

        object = containerService.findOrCreate(session, "/testNTTnheritanceObject");

        //test that supertype has been inherited as rdf:type
        final Node s = createGraphSubjectNode(object);
        final Node p = type.asNode();
        final Node o = createProperty("info:fedora/test/aSupertype").asNode();
        assertTrue("supertype test:aSupertype not found inherited in test:testInher!",
                object.getTriples(subjects, PROPERTIES).collect(toModel()).getGraph().contains(s, p, o));
    }

    @Test
    public void testDatastreamGraph() throws RepositoryException, InvalidChecksumException {

        final Container parentObject = containerService.findOrCreate(session, "/testDatastreamGraphParent");
        final Session jcrSession = getJcrSession(session);

        binaryService.findOrCreate(session, "/testDatastreamGraph").setContent(
                new ByteArrayInputStream("123456789test123456789".getBytes()),
                "text/plain",
                null,
                null,
                null
        );

        final FedoraResource object = binaryService.findOrCreate(session, "/testDatastreamGraph").getDescription();

        getJcrNode(object).setProperty("fedora:isPartOf",
                jcrSession.getNode("/testDatastreamGraphParent"));

        final Graph graph = object.getTriples(subjects, PROPERTIES).collect(toModel()).getGraph();

        // multivalued property
        final Node s = createGraphSubjectNode(object.getDescribedResource());
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

    /**
     * Test technically correct DELETE/WHERE with multiple patterns on the same subject and predicate.
     * See also FCREPO-2391
     */
    @Test
    public void testUpdatesWithMultiplePredicateMatches() {
        final Node subject = createURI("info:fedora/testUpdatesWithMultiplePredicateMatches");
        final FedoraResource object =
            containerService.findOrCreate(session, "/testUpdatesWithMultiplePredicateMatches");
        final Function<String, String> read = i -> {
            try {
                return IOUtils.toString(getClass().getResource(i), Charset.forName("UTF8"));
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        };
        final Function<FedoraResource, Model> modelOf = o -> {
            return o.getTriples(subjects, PROPERTIES).collect(toModel());
        };
        final BiFunction<FedoraResource, String, FedoraResource> update = (o, s) -> {
            o.updateProperties(subjects, read.apply(s), DefaultRdfStream.fromModel(subject, modelOf.apply(o)));
            return o;
        };
        update.apply(update.apply(object, "/patch-test/insert-data.txt"), "/patch-test/delete-where.txt");
        final Resource s = createResource(createGraphSubjectNode(object).getURI());
        final Property pid = createProperty("info:fedora/fedora-system:def/model#PID");
        final Literal o = createPlainLiteral("cdc:17256");
        final Model model = modelOf.apply(object);
        assertTrue(model.contains(s, pid, o));
        final Property dcSubject = createProperty("http://purl.org/dc/elements/1.1/subject");
        assertFalse(model.contains(s, dcSubject, (RDFNode)null));
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
    @Ignore("Until implemented with Memento")
    public void testGetObjectVersionGraph() throws RepositoryException {

        final FedoraResource object =
                containerService.findOrCreate(session, "/testObjectVersionGraph");

        getJcrNode(object).addMixin("mix:versionable");
        session.commit();

        final Instant theDate = Instant.now();
        // create a version and make sure there are 2 versions (root + created)
        versionService.createVersion(session, object, subjects, theDate);
        session.commit();

        final Model graphStore = object.getTriples(subjects, VERSIONS).collect(toModel());

        logger.debug(graphStore.toString());

        // go querying for the version URI
        final Resource s = createResource(createGraphSubjectNode(object).getURI());
//        final ExtendedIterator<Statement> triples = graphStore.listStatements(s,HAS_VERSION, (RDFNode)null);

//        final List<Statement> list = triples.toList();
//        assertEquals(1, list.size());

        // make sure the URI is derived from the label
//        s = list.get(0).getObject().asResource();
//        assertEquals("URI should be derived from label.", s.getURI(), createGraphSubjectNode(object).getURI()
//                + "/" + FCR_VERSIONS + "/v0.0.1");

        // make sure the label is listed
//        assertTrue(graphStore.contains(s, HAS_VERSION_LABEL, createPlainLiteral("v0.0.1")));
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
        final Session jcrSession = getJcrSession(session);
        final AccessControlManager acm = jcrSession.getAccessControlManager();
        final Privilege[] permissions = new Privilege[] {acm.privilegeFromName(Privilege.JCR_READ)};
        final AccessControlList acl = (AccessControlList) acm.getApplicablePolicies("/testRefObject").next();
        acl.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"), permissions);
        acm.setPolicy("/testRefObject", acl);
        session.commit();

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

        session.commit();

        final String actual = object.getEtagValue();
        assertNotNull(actual);
        assertNotEquals("", actual);
    }

    @Test
    public void testGetReferences() throws RepositoryException {
        final String pid = getRandomPid();
        final Session jcrSession = getJcrSession(session);
        containerService.findOrCreate(session, pid);
        final Container subject = containerService.findOrCreate(session, pid + "/a");
        final Container object = containerService.findOrCreate(session, pid + "/b");
        final Value value = jcrSession.getValueFactory().createValue(getJcrNode(object));
        getJcrNode(subject).setProperty("fedora:isPartOf", new Value[] { value });

        session.commit();

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
        final Session jcrSession = getJcrSession(session);

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

            final javax.jcr.Node skolemizedNode = jcrSession.getNodeByIdentifier(values[0].getString());

            assertTrue(skolemizedNode.getPath().contains("/#/"));
            assertEquals("xyz" + FIELD_DELIMITER + XSDstring.getURI(),
                    skolemizedNode.getProperty("dc:title").getValues()[0].getString());
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

    @Test (expected = ConstraintViolationException.class)
    public void testReplacePropertyBadMimeType() {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, pid);

        try (final RdfStream triples = object.getTriples(subjects, PROPERTIES)) {
            final Model model = triples.collect(toModel());

            final Resource resource = model.createResource();
            final Resource subject = subjects.reverse().convert(object);
            final Property predicate = model.createProperty(
                    "http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#hasMimeType");
            model.add(subject, predicate, "--Total Junk Mime Type--");
            model.add(resource, model.createProperty("http://purl.org/dc/elements/1.1/title"), "xyz");

            object.replaceProperties(subjects, model, object.getTriples(subjects, PROPERTIES));
        }
    }

    @Test (expected = IllegalArgumentException.class)
    public void testUpdatePropertyBadMimeType() {
        final String pid = getRandomPid();
        final FedoraResource object = containerService.findOrCreate(session, pid);
        object.updateProperties(subjects,
                "PREFIX ebucore: <http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#>\n" +
                "INSERT { <> ebucore:hasMimeType \"-- Complete Junk --\"" + " . } WHERE { }",
                object.getTriples(subjects, emptySet()));
    }
    @Test
    public void testDeleteObject() throws RepositoryException {
        final String pid = getRandomPid();
        final Session jcrSession = getJcrSession(session);
        containerService.findOrCreate(session, "/" + pid);
        session.commit();

        containerService.findOrCreate(session, "/" + pid).delete();

        session.commit();

        assertTrue(jcrSession.getNode("/" + pid).isNodeType(FEDORA_TOMBSTONE));
    }

    @Test
    public void testDeleteObjectWithInboundReferences() throws RepositoryException {

        final String pid = getRandomPid();
        final FedoraResource resourceA = containerService.findOrCreate(session, "/" + pid + "/a");
        final FedoraResource resourceB = containerService.findOrCreate(session, "/" + pid + "/b");
        final Session jcrSession = getJcrSession(session);

        final Value value = jcrSession.getValueFactory().createValue(getJcrNode(resourceB));
        getJcrNode(resourceA).setProperty("fedora:hasMember", new Value[] { value });

        session.commit();
        containerService.findOrCreate(session, "/" + pid + "/a").delete();

        session.commit();
        containerService.findOrCreate(session, "/" + pid + "/b").delete();

        session.commit();

        assertTrue(jcrSession.getNode("/" + pid + "/b").isNodeType(FEDORA_TOMBSTONE));

    }

    @Test
    public void testDeleteObjectWithInboundReferencesToChildren() throws RepositoryException {
        // Set up resources
        final String pid = getRandomPid();
        final FedoraResource resourceA = containerService.findOrCreate(session, "/" + pid + "/a");
        containerService.findOrCreate(session, "/" + pid + "/b");
        final FedoraResource resourceX = containerService.findOrCreate(session, "/" + pid + "/b/x");
        final Session jcrSession = getJcrSession(session);

        // Create a Weak reference
        final Value value = jcrSession.getValueFactory().createValue(getJcrNode(resourceX), true);
        getJcrNode(resourceA).setProperty("fedora:hasMember", new Value[] { value });

        session.commit();

        // Verify that relationship exists
        final Node s = subjects.reverse().convert(resourceA).asNode();
        final Node hasMember = createProperty(REPOSITORY_NAMESPACE, "hasMember").asNode();

        final Model rdf = resourceA.getTriples(subjects, PROPERTIES).collect(toModel());
        assertTrue(rdf.toString(), rdf.getGraph().contains(s, hasMember, ANY));

        // Delete parent of reference target
        containerService.findOrCreate(session, "/" + pid + "/b").delete();

        session.commit();

        // Verify that relationship does NOT exist, and that the resource successfully loads.
        containerService.find(session, "/" + pid + "/a");

        final Model rdfAfter = resourceA.getTriples(subjects, PROPERTIES).collect(toModel());
        assertFalse(rdfAfter.getGraph().contains(s, hasMember, ANY));
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
    // @Ignore ("Until implemented with Memento")
    public void testDeleteLinkedVersionedResources() throws RepositoryException {
        final Container object1 = containerService.findOrCreate(session, "/" + getRandomPid());
        final Container object2 = containerService.findOrCreate(session, "/" + getRandomPid());
        object2.enableVersioning();
        session.commit();

        // Create a link between objects 1 and 2
        object2.updateProperties(subjects, "PREFIX example: <http://example.org/>\n" +
                        "INSERT { <> <example:link> " + "<" + createGraphSubjectNode(object1).getURI() + ">" +
                        " } WHERE {} ",
                object2.getTriples(subjects, emptySet()));

        final Instant theDate = Instant.now();
        // Create version of object2
        versionService.createVersion(session, object2, subjects, theDate);

        // Verify that the objects exist
        assertTrue("object1 should exist!", exists(object1));
        assertTrue("object2 should exist!", exists(object2));

        // This is the test: verify successful deletion of the objects
        object2.delete();
        session.commit();

        object1.delete();
        session.commit();

        // Double-verify that the objects are gone
        assertFalse("/object2 should NOT exist!", exists(object2));
        assertFalse("/object1 should NOT exist!", exists(object1));
    }

    private boolean exists(final Container resource) {
        try {
            resource.getPath();
            return true;
        } catch (final RepositoryRuntimeException e) {
            return false;
        }
    }

    @Test
    @Ignore ("Until implemented with Memento")
    public void testDisableVersioning() throws RepositoryException {
        final String pid = getRandomPid();
        final Container object = containerService.findOrCreate(session, "/" + pid);
        object.enableVersioning();
        session.commit();
        assertTrue(object.isVersioned());
        object.disableVersioning();
        assertFalse(object.isVersioned());
    }

    @Test (expected = RepositoryRuntimeException.class)
    @Ignore ("Until implemented with Memento")
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
        session.commit();
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
        final Session jcrSession = getJcrSession(session);
        final Value[] values = new Value[2];
        values[0] = jcrSession.getValueFactory().createValue(getJcrNode(referent1));
        values[1] = jcrSession.getValueFactory().createValue(getJcrNode(referent2));
        getJcrNode(subject).setProperty(relation, values);

        session.commit();

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

    @Test
    public void testFindOrCreateTimeMapLDPCv() throws RepositoryException {
        final String pid = getRandomPid();
        final Session jcrSession = getJcrSession(session);
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();

        // Create TimeMap (LDPCv)
        final FedoraResource ldpcvResource = resource.findOrCreateTimeMap();

        assertNotNull(ldpcvResource);
        assertEquals("/" + pid + "/" + LDPCV_TIME_MAP, ldpcvResource.getPath());

        session.commit();

        final javax.jcr.Node timeMapNode = jcrSession.getNode("/" + pid).getNode(LDPCV_TIME_MAP);
        assertTrue(timeMapNode.isNodeType(FEDORA_TIME_MAP));

        final FedoraResource timeMap = resource.getTimeMap();
        assertTrue(timeMap instanceof FedoraTimeMap);
        assertEquals(timeMapNode, ((FedoraResourceImpl)timeMap).getNode());
    }

    @Test
    public void testGetMementoByDatetime() throws RepositoryException {
        final FedoraResource object1 = containerService.findOrCreate(session, "/" + getRandomPid());
        object1.enableVersioning();

        final DateTimeFormatter FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

        final Instant time1 = Instant.from(FMT.parse("2018-01-01T20:15:00"));
        final FedoraResource memento1 = versionService.createVersion(session, object1, subjects, time1);

        final Instant time2 = Instant.from(FMT.parse("2018-01-01T10:15:00"));
        final FedoraResource memento2 = versionService.createVersion(session, object1, subjects, time2);

        final Instant time3 = Instant.from(FMT.parse("2017-12-31T08:00:00"));
        final FedoraResource memento3 = versionService.createVersion(session, object1, subjects, time3);
        session.commit();

        final Instant afterLast = Instant.from(FMT.parse("2018-02-01T10:00:00"));
        assertEquals("Did not get expected Memento for Datetime", memento1,
            object1.findMementoByDatetime(afterLast));

        final Instant betweenLastAndMiddle =
            Instant.from(FMT.parse("2018-01-01T15:00:00"));

        assertEquals("Did not get expected Memento for Datetime", memento2,
            object1.findMementoByDatetime(betweenLastAndMiddle));

        final Instant betweenMiddleAndFirst =
            Instant.from(FMT.parse("2018-01-01T08:00:00"));
        assertEquals("Did not get expected Memento for Datetime", memento3,
            object1.findMementoByDatetime(betweenMiddleAndFirst));

        // Assert exact matches
        assertEquals("Did not get expected Memento for Datetime", memento1,
            object1.findMementoByDatetime(time1));
        assertEquals("Did not get expected Memento for Datetime", memento2,
            object1.findMementoByDatetime(time2));
        assertEquals("Did not get expected Memento for Datetime", memento3,
            object1.findMementoByDatetime(time3));

        final Instant beforeFirst = Instant.from(FMT.parse("2016-01-01T00:00:00"));
        assertEquals("Did not get expected Memento for Datetime", memento3,
            object1.findMementoByDatetime(beforeFirst));

    }

    @Test
    public void testGetMementoByDatetimeEmpty() {
        final FedoraResource object1 = containerService.findOrCreate(session, "/" + getRandomPid());
        object1.enableVersioning();

        final DateTimeFormatter FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

        final Instant time = Instant.from(FMT.parse("2016-04-21T09:43:00"));

        assertNull("Expected the null back because 0 Mementos.",
            object1.findMementoByDatetime(time));
    }

    @Test
    public void testGetAcl() throws RepositoryException {
        final String pid = getRandomPid();
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();

        // Retrieve ACL for the resource created
        final FedoraResource nullAclResource = resource.getAcl();
        assertNull(nullAclResource);

        // Create ACL for the resource
        final FedoraResource aclResource = resource.findOrCreateAcl();
        session.commit();

        final FedoraResource aclResourceFound = resource.getAcl();
        assertNotNull(aclResourceFound);
        assertTrue(aclResourceFound instanceof FedoraWebacAcl);
        assertEquals(aclResource, aclResourceFound);
    }

    @Test
    public void testFindOrCreateAcl() throws RepositoryException {
        final String pid = getRandomPid();
        final Session jcrSession = getJcrSession(session);
        final FedoraResource resource = containerService.findOrCreate(session, "/" + pid);
        session.commit();

        // Create ACL for the resource
        final FedoraResource aclResource = resource.findOrCreateAcl();
        assertNotNull(aclResource);
        assertTrue(aclResource instanceof FedoraWebacAcl);
        assertEquals("/" + pid + "/" + CONTAINER_WEBAC_ACL, aclResource.getPath());
        session.commit();

        final javax.jcr.Node aclNode = jcrSession.getNode("/" + pid).getNode(CONTAINER_WEBAC_ACL);
        assertTrue(aclNode.isNodeType(FEDORA_WEBAC_ACL));
    }

    @Test
    public void testFindOrCreateBinaryAcl() throws RepositoryException, InvalidChecksumException {
        final String pid = getRandomPid();
        final Session jcrSession = getJcrSession(session);

        binaryService.findOrCreate(session, "/" + pid).setContent(
                new ByteArrayInputStream("binary content".getBytes()),
                "text/plain",
                null,
                null,
                null
        );

        // Retrieve the binary resource and create ACL
        final FedoraResource binary = binaryService.findOrCreate(session, "/" + pid);
        final FedoraResource binaryAclResource = binary.findOrCreateAcl();

        assertNotNull(binaryAclResource);
        assertTrue(binaryAclResource instanceof FedoraWebacAcl);
        assertEquals("/" + pid + "/" + CONTAINER_WEBAC_ACL, binaryAclResource.getPath());
        session.commit();

        final javax.jcr.Node aclNode = jcrSession.getNode("/" + pid).getNode(CONTAINER_WEBAC_ACL);
        assertTrue(aclNode.isNodeType(FEDORA_WEBAC_ACL));
    }

    private void addVersionLabel(final String label, final FedoraResource r) throws RepositoryException {
        final Session jcrSession = getJcrSession(session);
        addVersionLabel(label, jcrSession.getWorkspace().getVersionManager().getBaseVersion(r.getPath()));
    }

    private static void addVersionLabel(final String label, final Version v) throws RepositoryException {
        v.getContainingHistory().addVersionLabel(v.getName(), label, false);
    }

    private static Instant roundDate(final Instant date) {
        return date.minusMillis(date.toEpochMilli() % 1000);
    }
}
