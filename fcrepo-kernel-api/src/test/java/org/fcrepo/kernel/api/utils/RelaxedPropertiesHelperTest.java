/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.utils;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RelaxableServerManagedPropertyException;
import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;
import org.fcrepo.kernel.api.exception.ServerManagedTypeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RelaxedPropertiesHelper}
 *
 * @author whikloj
 */
public class RelaxedPropertiesHelperTest {

    private Resource fedoraResource;

    private Calendar expectedCreatedDate;

    private Calendar expectedLastModifiedDate;

    private Node subject;

    @BeforeEach
    public void setUp() {
        final ModelCom model = new ModelCom(createDefaultModel().getGraph());
        fedoraResource = new ResourceImpl(FedoraId.create("test-subject").getFullId(), model);
        // Calendar months are 0-based, so October is 9
        expectedCreatedDate = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("GMT"))
                .setDate(2023, 9, 1).setTimeOfDay(0, 0, 0, 0)
                .build();
        final var createdDate = ResourceFactory.createTypedLiteral("2023-10-01T00:00:00Z",
                XSDDatatype.XSDdateTime);
        final var createdBy = ResourceFactory.createPlainLiteral("test-user");
        // Calendar months are 0-based, so November is 10
        expectedLastModifiedDate = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("GMT"))
                .setDate(2025, 10, 1).setTimeOfDay(0, 0, 0, 0)
                .build();
        final var lastModifiedDate = ResourceFactory.createTypedLiteral("2025-11-01T00:00:00Z",
                XSDDatatype.XSDdateTime);
        final var lastModifiedBy = ResourceFactory.createPlainLiteral("other-user");
        fedoraResource.addProperty(CREATED_BY, createdBy);
        fedoraResource.addProperty(CREATED_DATE, createdDate);
        fedoraResource.addProperty(LAST_MODIFIED_BY, lastModifiedBy);
        fedoraResource.addProperty(LAST_MODIFIED_DATE, lastModifiedDate);
        subject = NodeFactory.createURI("http://example.com/subject");
    }

    @Test
    public void testGetCreatedDate() {
        final var createdDate = RelaxedPropertiesHelper.getCreatedDate(fedoraResource);
        final var compareTo = expectedCreatedDate.compareTo(createdDate);
        assertEquals(0, compareTo);
    }

    @Test
    public void testGetCreatedBy() {
        final var createdBy = RelaxedPropertiesHelper.getCreatedBy(fedoraResource);
        assertEquals("test-user", createdBy);
    }

    @Test
    public void testGetModifiedDate() {
        final var modifiedDate = RelaxedPropertiesHelper.getModifiedDate(fedoraResource);
        assertEquals(expectedLastModifiedDate, modifiedDate);
    }

    @Test
    public void testGetModifiedBy() {
        final var modifiedBy = RelaxedPropertiesHelper.getModifiedBy(fedoraResource);
        assertEquals("other-user", modifiedBy);
    }

    /**
     * Test that you can't set a rdf:type to a literal.
     */
    @Test
    public void testCheckForDisallowedType() {
        final var triple = Triple.create(
                subject,
                NodeFactory.createURI(RDF.type.getURI()),
                NodeFactory.createLiteral("some-type")
        );
        assertThrows(MalformedRdfException.class, () -> {
            RelaxedPropertiesHelper.checkTripleForDisallowed(triple);
        });
        // URI is allowed
        final var triple2 = Triple.create(
                subject,
                NodeFactory.createURI(RDF.type.getURI()),
                NodeFactory.createURI("http://example.com/allowed-type")
        );
        RelaxedPropertiesHelper.checkTripleForDisallowed(triple2);
        assertTrue(true);
        // Variable is allowed
        final var triple3 = Triple.create(
                subject,
                NodeFactory.createURI(RDF.type.getURI()),
                NodeFactory.createVariable("some-variable")
        );
        RelaxedPropertiesHelper.checkTripleForDisallowed(triple3);
        assertTrue(true);
    }

    /**
     * Test that you can't set a type to a URI in a restricted namespace.
     * These are <a href="http://fedora.info/definitions/v4/repository#">FEDORA</a>,
     * <a href="http://www.w3.org/ns/ldp#">LDP</a> and <a href="http://mementoweb.org/ns#">MEMENTO</a>.
     */
    @Test
    public void testCheckForRestrictedNamespace() {
        final List<Triple> triples = List.of(
            Triple.create(
                subject,
                NodeFactory.createURI(RDF.type.getURI()),
                NodeFactory.createURI(REPOSITORY_NAMESPACE + "my-type")
            ),
            Triple.create(
                subject,
                NodeFactory.createURI(RDF.type.getURI()),
                NodeFactory.createURI(LDP_NAMESPACE + "my-type")
            ),
            Triple.create(
                subject,
                NodeFactory.createURI(RDF.type.getURI()),
                NodeFactory.createURI(MEMENTO_NAMESPACE + "my-type")
            )
        );
        for (final var triple : triples) {
            assertThrows(ServerManagedTypeException.class,
                    () -> RelaxedPropertiesHelper.checkTripleForDisallowed(triple));
        }
    }

    /**
     * Test that you can't use a predicate from the <a href="http://fedora.info/definitions/v4/repository#">FEDORA</a>,
     * or <a href="http://mementoweb.org/ns#">MEMENTO</a> namespaces, or one of
     * - <a href="http://www.loc.gov/premis/rdf/v1#hasFixity">hasFixity</a>
     * - <a href="http://www.loc.gov/premis/rdf/v1#hasMessageDigest">hasMessageDigest</a>
     * - <a href="http://www.w3.org/ns/ldp#contains">contains</a>
     *
     * Of items in the <a href="http://fedora.info/definitions/v4/repository#">FEDORA</a> namespace some are relaxable
     * and are tested in the {@link #testRelaxablePredicate} method.
     */
    @Test
    public void testIsServerManaged() {
        final List<Triple> triples = List.of(
            Triple.create(
                subject,
                NodeFactory.createURI(REPOSITORY_NAMESPACE + "my-predicate"),
                NodeFactory.createURI("http://example.com/allowed-type")
            ),
            Triple.create(
                subject,
                NodeFactory.createURI(MEMENTO_NAMESPACE + "my-predicate"),
                NodeFactory.createURI("http://example.com/allowed-type")
            ),
            Triple.create(
                subject,
                HAS_FIXITY_RESULT.asNode(),
                NodeFactory.createLiteral("some-value")
            ),
            Triple.create(
                subject,
                HAS_MESSAGE_DIGEST.asNode(),
                NodeFactory.createLiteral("some-value")
            ),
            Triple.create(
                subject,
                CONTAINS.asNode(),
                NodeFactory.createLiteral("some-value")
            )
        );
        for (final var triple : triples) {
            assertThrows(ServerManagedPropertyException.class,
                    () -> RelaxedPropertiesHelper.checkTripleForDisallowed(triple));
        }
    }

    /**
     * Test certain predicates that are relaxable. These are
     * - <a href="http://fedora.info/definitions/v4/repository#createdBy">createdBy</a>
     * - <a href="http://fedora.info/definitions/v4/repository#createdDate">createdDate</a>
     * - <a href="http://fedora.info/definitions/v4/repository#lastModifiedBy">lastModifiedBy</a>
     * - <a href="http://fedora.info/definitions/v4/repository#lastModifiedDate">lastModifiedDate</a>
     *
     */
    @Test
    public void testRelaxablePredicate() {
        final List<Triple> triples = List.of(
            Triple.create(
                subject,
                CREATED_BY.asNode(),
                NodeFactory.createLiteral("some-value")
            ),
            Triple.create(
                subject,
                CREATED_DATE.asNode(),
                NodeFactory.createLiteral("some-value")
            ),
            Triple.create(
                subject,
                LAST_MODIFIED_BY.asNode(),
                NodeFactory.createLiteral("some-value")
            ),
            Triple.create(
                subject,
                LAST_MODIFIED_DATE.asNode(),
                NodeFactory.createLiteral("some-value")
            )
        );
        for (final var triple : triples) {
            assertThrows(RelaxableServerManagedPropertyException.class,
                    () -> RelaxedPropertiesHelper.checkTripleForDisallowed(triple));
        }
    }

    /**
     * Test that mime-types sent in RDF with a ebucore:hasMimeType predicate must be valid or a variable.
     * @see <a href="https://tools.ietf.org/html/rfc6838">RFC 6838</a>
     */
    @Test
    public void testValidMimeType() {
        final List<Triple> valid_triples = List.of(
                Triple.create(
                        subject,
                        HAS_MIME_TYPE.asNode(),
                        NodeFactory.createLiteral("text/plain")
                ),
                Triple.create(
                        subject,
                        HAS_MIME_TYPE.asNode(),
                        NodeFactory.createLiteral("application/json")
                ),
                Triple.create(
                        subject,
                        HAS_MIME_TYPE.asNode(),
                        NodeFactory.createLiteral("application/n-triples")
                ),
                Triple.create(
                        subject,
                        HAS_MIME_TYPE.asNode(),
                        NodeFactory.createVariable("some-variable")
                )
        );
        for (final var triple : valid_triples) {
            RelaxedPropertiesHelper.checkTripleForDisallowed(triple);
        }
        // Invalid mime types
        final Triple invalidTriple = Triple.create(
                subject,
                HAS_MIME_TYPE.asNode(),
                NodeFactory.createLiteral("video")
        );
        assertThrows(MalformedRdfException.class,
                () -> RelaxedPropertiesHelper.checkTripleForDisallowed(invalidTriple));
    }
}
