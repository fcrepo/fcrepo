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
package org.fcrepo.kernel.api;

import static com.google.common.collect.ImmutableSet.of;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import java.net.URI;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import com.google.common.collect.ImmutableSet;

/**
 * A lexicon of the RDF properties that the fcrepo kernel (or close-to-core modules) use
 *
 * @author ajs6f
 */
public final class RdfLexicon {

    /**
     * Repository namespace "fedora"
    **/
    public static final String REPOSITORY_NAMESPACE = "http://fedora.info/definitions/v4/repository#";

    public static final String EVENT_NAMESPACE = "http://fedora.info/definitions/v4/event#";

    public static final String FCREPO_API_NAMESPACE = "http://fedora.info/definitions/fcrepo#";

    public static final String ACTIVITY_STREAMS_NAMESPACE = "https://www.w3.org/ns/activitystreams#";

    public static final String EBUCORE_NAMESPACE = "http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#";

    public static final String OA_NAMESPACE = "http://www.w3.org/ns/oa#";

    public static final String PROV_NAMESPACE = "http://www.w3.org/ns/prov#";

    public static final String PREMIS_NAMESPACE = "http://www.loc.gov/premis/rdf/v1#";

    public static final String MEMENTO_NAMESPACE = "http://mementoweb.org/ns#";

    public static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /**
     * Namespace for the W3C WebAC vocabulary.
     */
    public static final String WEBAC_NAMESPACE_VALUE = "http://www.w3.org/ns/auth/acl#";
    public static final String FEDORA_WEBAC_NAMESPACE_VALUE = "http://fedora.info/definitions/v4/webac#";
    public static final String FEDORA_WEBAC_ACL_VALUE = FEDORA_WEBAC_NAMESPACE_VALUE + "Acl";

    /**
     * Fedora configuration namespace "fedora-config", used for user-settable
     * configuration properties.
     **/
    // TODO from UCDetector: Constant "RdfLexicon.FEDORA_CONFIG_NAMESPACE" has 0 references
    // should be referenced again when versioning is back in REST api
    public static final String FEDORA_CONFIG_NAMESPACE = // NO_UCD (unused code)
            "info:fedoraconfig/";

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
     * Is this namespace one that the repository manages?
     */
    public static final Predicate<String> isManagedNamespace = p -> p.equals(REPOSITORY_NAMESPACE);

    // MEMBERSHIP
    public static final Property HAS_PARENT =
            createProperty(REPOSITORY_NAMESPACE + "hasParent");
    public static final Property HAS_CHILD =
            createProperty(REPOSITORY_NAMESPACE + "hasChild");

    public static final Set<Property> membershipProperties = of(HAS_PARENT, HAS_CHILD);

    // FIXITY

    public static final Resource FIXITY_TYPE = createResource(PREMIS_NAMESPACE + "Fixity");

    public static final Property HAS_MESSAGE_DIGEST_ALGORITHM =
            createProperty(PREMIS_NAMESPACE + "hasMessageDigestAlgorithm");

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

    // Server managed properties
    public static final Property CREATED_DATE =
            createProperty(REPOSITORY_NAMESPACE + "created");
    public static final Property CREATED_BY =
            createProperty(REPOSITORY_NAMESPACE + "createdBy");
    public static final Property LAST_MODIFIED_DATE =
            createProperty(REPOSITORY_NAMESPACE + "lastModified");
    public static final Property LAST_MODIFIED_BY =
            createProperty(REPOSITORY_NAMESPACE + "lastModifiedBy");
    public static final Set<Property> serverManagedProperties = of(
            CREATED_DATE, CREATED_BY, LAST_MODIFIED_DATE, LAST_MODIFIED_BY);

    public static final Resource FEDORA_CONTAINER =
            createResource(REPOSITORY_NAMESPACE + "Container");
    public static final Resource FEDORA_BINARY =
            createResource(REPOSITORY_NAMESPACE + "Binary");
    public static final Resource FEDORA_RESOURCE =
            createResource(REPOSITORY_NAMESPACE + "Resource");

    // Linked Data Platform
    public static final Property PAGE =
        createProperty(LDP_NAMESPACE + "Page");
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
    public static final Resource RESOURCE =
            createResource(LDP_NAMESPACE + "Resource");
    public static final Resource RDF_SOURCE =
            createResource(LDP_NAMESPACE + "RDFSource");
    public static final Resource NON_RDF_SOURCE =
        createResource(LDP_NAMESPACE + "NonRDFSource");
    public static final Property CONSTRAINED_BY =
            createProperty(LDP_NAMESPACE + "constrainedBy");
    public static final Property MEMBER_SUBJECT =
            createProperty(LDP_NAMESPACE + "MemberSubject");

    private static final Set<Property> ldpManagedProperties = of(CONTAINS);

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

    public static final Set<Property> namespaceProperties = of(
            HAS_NAMESPACE_PREFIX, HAS_NAMESPACE_URI);

    // OTHER SERVICES
    public static final Property HAS_FIXITY_SERVICE =
            createProperty(REPOSITORY_NAMESPACE + "hasFixityService");
    public static final Property HAS_SPARQL_ENDPOINT =
        createProperty(SPARQL_SD_NAMESPACE + "endpoint");

    public static final Set<Property> otherServiceProperties = of(HAS_FIXITY_SERVICE);


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

    // RDF EXTRACTION
    public static final Property COULD_NOT_STORE_PROPERTY =
            createProperty(REPOSITORY_NAMESPACE + "couldNotStoreProperty");
    public static final Property INBOUND_REFERENCES = createProperty(FCREPO_API_NAMESPACE + "PreferInboundReferences");
    public static final Property SERVER_MANAGED = createProperty(REPOSITORY_NAMESPACE + "ServerManaged");

    public static final Property EMBED_CONTAINED = createProperty(OA_NAMESPACE + "PreferContainedDescriptions");


    // WEBAC
    public static final String WEBAC_ACCESS_CONTROL_VALUE = WEBAC_NAMESPACE_VALUE + "accessControl";
    public static final URI WEBAC_ACCESS_CONTROL = URI.create(WEBAC_ACCESS_CONTROL_VALUE);


    public static final Set<Property> managedProperties;

    static {
        final ImmutableSet.Builder<Property> b = ImmutableSet.builder();
        b.addAll(membershipProperties).addAll(fixityProperties).addAll(ldpManagedProperties).addAll(
                repositoryProperties).addAll(namespaceProperties).addAll(
                otherServiceProperties).addAll(serverManagedProperties);
        managedProperties = b.build();
    }

    public static final Set<Property> relaxableProperties
            = of(LAST_MODIFIED_BY, LAST_MODIFIED_DATE, CREATED_BY, CREATED_DATE);

    public static final String SERVER_MANAGED_PROPERTIES_MODE = "fcrepo.properties.management";

    private static Predicate<Property> hasFedoraNamespace =
        p -> !p.isAnon() && p.getNameSpace().startsWith(REPOSITORY_NAMESPACE);

    public static final Predicate<Property> isRelaxed =
            p -> relaxableProperties.contains(p)
                    && ("relaxed".equals(System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)));

    /**
     * Detects whether an RDF property is managed by the repository.
     */
    public static final Predicate<Property> isManagedPredicate =
        hasFedoraNamespace.or(p -> managedProperties.contains(p));

    /**
     * Fedora defined JCR node type with supertype of nt:file with two nt:folder named fedora:timemap and
     * fedora:binaryTimemap inside.
     */
    public static final String NT_VERSION_FILE = "nt:versionFile";

    /**
     * Fedora defined JCR node type which can have no children except optionally a timemap
     */
    public static final String NT_LEAF_NODE = "nt:leafNode";

    /**
     * Fedora defined node path for a binary description.
     */
    public static final String FEDORA_DESCRIPTION = "fedora:description";

    // VERSIONING
    /**
     * Memento TimeMap type.
     */
    public static final String VERSIONING_TIMEMAP_TYPE = MEMENTO_NAMESPACE + "TimeMap";

    /**
     * Memento TimeGate type.
     */
    public static final String VERSIONING_TIMEGATE_TYPE = MEMENTO_NAMESPACE + "TimeGate";

    /**
     * Type for memento objects.
     */
    public static final String MEMENTO_TYPE = MEMENTO_NAMESPACE + "Memento";

    /**
     * This is an internal RDF type for versionable resources, this may be replaced by a Memento type.
     */
    public static final Property VERSIONED_RESOURCE =
        createProperty(MEMENTO_NAMESPACE + "OriginalResource");

    /*
     * Interaction Models.
     */
    public static final Set<String>  INTERACTION_MODELS = of(
            NON_RDF_SOURCE.getURI().replace(LDP_NAMESPACE, "ldp:"),
            BASIC_CONTAINER.getURI().replace(LDP_NAMESPACE, "ldp:"),
            DIRECT_CONTAINER.getURI().replace(LDP_NAMESPACE, "ldp:"),
            INDIRECT_CONTAINER.getURI().replace(LDP_NAMESPACE, "ldp:"));

    private RdfLexicon() {

    }
}
