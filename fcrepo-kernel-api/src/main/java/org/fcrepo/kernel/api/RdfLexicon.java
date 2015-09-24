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
package org.fcrepo.kernel.api;

import static com.google.common.collect.ImmutableSet.of;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;

import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A lexicon of the RDF properties that the fcrepo kernel (or close-to-core modules) use
 *
 * @author ajs6f
 */
public final class RdfLexicon {

    /**
     * Repository namespace "fcrepo", used for JCR properties exposed
     * publicly.
     * Was "info:fedora/fedora-system:def/internal#".
    **/
    public static final String REPOSITORY_NAMESPACE =
            "http://fedora.info/definitions/v4/repository#";

    /**
     *  The core JCR namespace.
     */
    public static final String JCR_NAMESPACE = "http://www.jcp.org/jcr/1.0";

    public static final String MODE_NAMESPACE = "http://www.modeshape.org/1.0";

    public static final String MIX_NAMESPACE = "http://www.jcp.org/jcr/mix/1.0";

    public static final String JCR_NT_NAMESPACE = "http://www.jcp.org/jcr/nt/1.0";

    public static final String EBUCORE_NAMESPACE = "http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#";

    /**
     * Fedora configuration namespace "fedora-config", used for user-settable
     * configuration properties.
     **/
    // TODO from UCDetector: Constant "RdfLexicon.FEDORA_CONFIG_NAMESPACE" has 0 references
    // should be referenced again when versioning is back in REST api
    public static final String FEDORA_CONFIG_NAMESPACE = // NO_UCD (unused code)
            "http://fedora.info/definitions/v4/config#";

    /**
     * REST API namespace "fedora", used for internal API links and node
     * paths.
     * Was "info:fedora/".
     **/
    public static final String PREMIS_NAMESPACE =
        "http://www.loc.gov/premis/rdf/v1#";

    /**
     * Linked Data Platform namespace.
     */
    public static final String LDP_NAMESPACE = "http://www.w3.org/ns/ldp#";

    /**
     * SPARQL service description namespace.
     */
    public static final String SPARQL_SD_NAMESPACE =
            "http://www.w3.org/ns/sparql-service-description#";

    /**
     * The namespaces that the repository manages internally.
     */
    public static final Set<String> managedNamespaces = of(
            REPOSITORY_NAMESPACE, JCR_NAMESPACE,
            MIX_NAMESPACE, JCR_NT_NAMESPACE, MODE_NAMESPACE);

    /**
     * Is this namespace one that the repository manages?
     */
    public static final Predicate<String> isManagedNamespace =
        p -> managedNamespaces.contains(p);

    public static final String RDF_NAMESPACE =
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static final String INDEXING_NAMESPACE =
            "http://fedora.info/definitions/v4/indexing#";

    public static final String DC_NAMESPACE =
            "http://purl.org/dc/elements/1.1/";

    // MEMBERSHIP
    public static final Property HAS_PARENT =
            createProperty(REPOSITORY_NAMESPACE + "hasParent");
    public static final Property HAS_CHILD =
            createProperty(REPOSITORY_NAMESPACE + "hasChild");
    public static final Property HAS_CHILD_COUNT =
            createProperty(REPOSITORY_NAMESPACE + "numberOfChildren");

    public static final Set<Property> membershipProperties = of(HAS_PARENT, HAS_CHILD, HAS_CHILD_COUNT);

    // FIXITY

    public static final Resource FIXITY_TYPE = createResource(PREMIS_NAMESPACE + "Fixity");
    public static final Property HAS_MESSAGE_DIGEST =
        createProperty(PREMIS_NAMESPACE + "hasMessageDigest");
    public static final Property HAS_SIZE =
        createProperty(PREMIS_NAMESPACE + "hasSize");
    public static final Property HAS_FIXITY_RESULT =
        createProperty(PREMIS_NAMESPACE + "hasFixity");

    public static final Property HAS_FIXITY_CHECK_COUNT =
            createProperty(REPOSITORY_NAMESPACE + "numFixityChecks");
    public static final Property HAS_FIXITY_ERROR_COUNT =
            createProperty(REPOSITORY_NAMESPACE + "numFixityErrors");
    public static final Property HAS_FIXITY_REPAIRED_COUNT =
            createProperty(REPOSITORY_NAMESPACE + "numFixityRepaired");

    public static final Set<Property> fixityProperties = of(
            HAS_FIXITY_RESULT, HAS_MESSAGE_DIGEST, HAS_SIZE,
            HAS_FIXITY_CHECK_COUNT, HAS_FIXITY_ERROR_COUNT, HAS_FIXITY_REPAIRED_COUNT);

    public static final Resource EVENT_OUTCOME_INFORMATION = createResource(PREMIS_NAMESPACE + "EventOutcomeDetail");

    public static final Property HAS_FIXITY_STATE =
            createProperty(PREMIS_NAMESPACE + "hasEventOutcome");

    public static final Property WRITABLE =
            createProperty(REPOSITORY_NAMESPACE + "writable");

    // Linked Data Platform
    public static final Property PAGE =
        createProperty(LDP_NAMESPACE + "Page");
    public static final Property PAGE_OF =
        createProperty(LDP_NAMESPACE + "pageOf");
    public static final Property FIRST_PAGE =
        createProperty(LDP_NAMESPACE + "firstPage");
    public static final Property NEXT_PAGE =
            createProperty(LDP_NAMESPACE + "nextPage");
    public static final Resource CONTAINER =
            createResource(LDP_NAMESPACE + "Container");
    public static final Resource BASIC_CONTAINER =
            createResource(LDP_NAMESPACE + "BasicContainer");
    public static final Resource DIRECT_CONTAINER =
            createResource(LDP_NAMESPACE + "DirectContainer");
    public static final Resource INDIRECT_CONTAINER =
            createResource(LDP_NAMESPACE + "IndirectContainer");
    public static final Property MEMBERSHIP_RESOURCE =
            createProperty(LDP_NAMESPACE + "membershipResource");
    public static final Property HAS_MEMBER_RELATION =
            createProperty(LDP_NAMESPACE + "hasMemberRelation");
    public static final Property CONTAINS =
        createProperty(LDP_NAMESPACE + "contains");
    public static final Property LDP_MEMBER =
            createProperty(LDP_NAMESPACE + "member");
    public static final Property RDF_SOURCE =
            createProperty(LDP_NAMESPACE + "RDFSource");
    public static final Property NON_RDF_SOURCE =
        createProperty(LDP_NAMESPACE + "NonRDFSource");
    public static final Property CONSTRAINED_BY =
            createProperty(LDP_NAMESPACE + "constrainedBy");
    public static final Property MEMBER_SUBJECT =
            createProperty(LDP_NAMESPACE + "MemberSubject");

    public static final Set<Property> ldpProperties = of(PAGE, PAGE_OF,
            FIRST_PAGE, NEXT_PAGE, CONTAINS, LDP_MEMBER);
    public static final Set<Resource> ldpResources = of(CONTAINER,
            DIRECT_CONTAINER);

    // REPOSITORY INFORMATION
    public static final Property HAS_OBJECT_COUNT =
            createProperty(REPOSITORY_NAMESPACE + "objectCount");
    public static final Property HAS_OBJECT_SIZE =
            createProperty(REPOSITORY_NAMESPACE + "objectSize");
    public static final Property HAS_TRANSACTION_SERVICE =
            createProperty(REPOSITORY_NAMESPACE + "hasTransactionProvider");
    public static final Property HAS_ACCESS_ROLES_SERVICE =
            createProperty(REPOSITORY_NAMESPACE + "hasAccessRoles");

    public static final Set<Property> repositoryProperties = of(
            HAS_OBJECT_COUNT, HAS_OBJECT_SIZE, HAS_TRANSACTION_SERVICE);

    // NAMESPACES
    public static final Property HAS_NAMESPACE_PREFIX =
            createProperty("http://purl.org/vocab/vann/preferredNamespacePrefix");
    public static final Property HAS_NAMESPACE_URI =
            createProperty("http://purl.org/vocab/vann/preferredNamespaceUri");
    public static final Property VOAF_VOCABULARY = createProperty("http://purl.org/vocommons/voaf#Vocabulary");

    public static final Set<Property> namespaceProperties = of(
            HAS_NAMESPACE_PREFIX, HAS_NAMESPACE_URI, VOAF_VOCABULARY);

    // OTHER SERVICES
    public static final Property HAS_SERIALIZATION =
            createProperty(REPOSITORY_NAMESPACE + "exportsAs");
    public static final Property HAS_VERSION_HISTORY =
            createProperty(REPOSITORY_NAMESPACE + "hasVersions");
    public static final Property HAS_FIXITY_SERVICE =
            createProperty(REPOSITORY_NAMESPACE + "hasFixityService");
    public static final Property HAS_FEED =
            createProperty(
                    "http://www.whatwg.org/specs/web-apps/current-work/#",
                    "feed0");
    public static final Property HAS_SUBSCRIPTION_SERVICE =
            createProperty("http://microformats.org/wiki/rel-subscription");
    public static final Property HAS_SPARQL_ENDPOINT =
        createProperty(SPARQL_SD_NAMESPACE + "endpoint");

    public static final Set<Property> otherServiceProperties = of(
            HAS_SERIALIZATION, HAS_VERSION_HISTORY, HAS_FIXITY_SERVICE,
            HAS_FEED, HAS_SUBSCRIPTION_SERVICE);


    // BINARY DESCRIPTIONS
    public static final Property DESCRIBES =
            createProperty("http://www.iana.org/assignments/relation/describes");
    public static final Property DESCRIBED_BY =
            createProperty("http://www.iana.org/assignments/relation/describedby");

    public static final Set<Property> structProperties = of(DESCRIBES, DESCRIBED_BY);

    // CONTENT
    public static final Resource CONTENT_LOCATION_TYPE =
            createResource(PREMIS_NAMESPACE + "ContentLocation");
    public static final Resource INACCESSIBLE_RESOURCE =
            createResource(REPOSITORY_NAMESPACE + "inaccessibleResource");
    public static final Property HAS_CONTENT_LOCATION =
            createProperty(PREMIS_NAMESPACE + "hasContentLocation");
    public static final Property HAS_CONTENT_LOCATION_VALUE =
        createProperty(PREMIS_NAMESPACE + "hasContentLocationValue");
    public static final Property HAS_MIME_TYPE =
            createProperty(EBUCORE_NAMESPACE + "hasMimeType");
    public static final Property HAS_ORIGINAL_NAME =
            createProperty(EBUCORE_NAMESPACE + "filename");

    public static final Set<Property> contentProperties = of(HAS_CONTENT_LOCATION, HAS_CONTENT_LOCATION_VALUE,
            HAS_SIZE);


    // VERSIONING
    public static final Property HAS_VERSION =
            createProperty(REPOSITORY_NAMESPACE + "hasVersion");
    public static final Property HAS_VERSION_LABEL =
            createProperty(REPOSITORY_NAMESPACE + "hasVersionLabel");

    public static final Set<Property> versioningProperties = of(HAS_VERSION,
            HAS_VERSION_LABEL);

    // RDF EXTRACTION
    public static final Property COULD_NOT_STORE_PROPERTY =
            createProperty(REPOSITORY_NAMESPACE + "couldNotStoreProperty");
    public static final Property INBOUND_REFERENCES = createProperty(REPOSITORY_NAMESPACE + "InboundReferences");
    public static final Property EMBED_CONTAINS = createProperty(REPOSITORY_NAMESPACE + "EmbedResources");
    public static final Property SERVER_MANAGED = createProperty(REPOSITORY_NAMESPACE + "ServerManaged");

    // IMPORTANT JCR PROPERTIES
    public static final Property HAS_PRIMARY_IDENTIFIER =
            createProperty(REPOSITORY_NAMESPACE + "uuid");
    public static final Property HAS_PRIMARY_TYPE =
            createProperty(REPOSITORY_NAMESPACE + "primaryType");
    public static final Property HAS_NODE_TYPE =
            createProperty(REPOSITORY_NAMESPACE + "hasNodeType");
    public static final Property HAS_MIXIN_TYPE =
            createProperty(REPOSITORY_NAMESPACE + "mixinTypes");
    public static final Property CREATED_DATE =
            createProperty(REPOSITORY_NAMESPACE + "created");
    public static final Property CREATED_BY =
            createProperty(REPOSITORY_NAMESPACE + "createdBy");
    public static final Property LAST_MODIFIED_DATE =
            createProperty(REPOSITORY_NAMESPACE + "lastModified");
    public static final Property LAST_MODIFIED_BY =
            createProperty(REPOSITORY_NAMESPACE + "lastModifiedBy");

    public static final Set<Property> jcrProperties = of(
            HAS_PRIMARY_IDENTIFIER, HAS_PRIMARY_TYPE, HAS_NODE_TYPE,
            HAS_MIXIN_TYPE, CREATED_DATE, CREATED_BY, LAST_MODIFIED_DATE,
            LAST_MODIFIED_BY);

    public static final Property RDFS_LABEL =
            createProperty("http://www.w3.org/2000/01/rdf-schema#label");
    public static final Property DC_TITLE =
            createProperty("http://purl.org/dc/elements/1.1/title");
    public static final Property DCTERMS_TITLE =
            createProperty("http://purl.org/dc/terms/title");
    public static final Property SKOS_PREFLABEL =
            createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");

    public static final Set<Property> managedProperties;

    static {
        final ImmutableSet.Builder<Property> b = ImmutableSet.builder();
        b.addAll(membershipProperties).addAll(fixityProperties).addAll(ldpProperties).addAll(
                repositoryProperties).addAll(namespaceProperties).addAll(
                otherServiceProperties).addAll(structProperties).addAll(contentProperties).addAll(
                versioningProperties).addAll(jcrProperties);
        managedProperties = b.build();
    }

    private static Predicate<Property> hasJcrNamespace =
        p -> !p.isAnon() && p.getNameSpace().equals(JCR_NAMESPACE);

    private static Predicate<Property> hasFedoraNamespace =
        p -> !p.isAnon() && p.getNameSpace().startsWith(REPOSITORY_NAMESPACE);

    /**
     * Detects whether an RDF property is managed by the repository.
     */
    @SuppressWarnings("unchecked")
    public static final Predicate<Property> isManagedPredicate =
        hasJcrNamespace.or(hasFedoraNamespace).or(p -> managedProperties.contains(p));

    /**
     * Detects whether an RDF predicate URI is managed by the repository.
    **/
    public static Predicate<String> isManagedPredicateURI =
        p -> isManagedPredicate.test(createProperty(p));

    private RdfLexicon() {

    }
}
