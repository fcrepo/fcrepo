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
import static org.apache.jena.vocabulary.RDF.type;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.jena.graph.Triple;
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

    public static final String REPOSITORY_WEBAC_NAMESPACE = "http://fedora.info/definitions/v4/webac#";

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

    /**
     * Linked Data Platform namespace.
     */
    public static final String LDP_NAMESPACE = "http://www.w3.org/ns/ldp#";

    /**
     * Is this namespace one that the repository manages?
     */
    public static final Predicate<String> isManagedNamespace = p -> p.equals(REPOSITORY_NAMESPACE) ||
            p.equals(LDP_NAMESPACE) || p.equals(MEMENTO_NAMESPACE);

    /**
     * Tests if the triple has a predicate of rdf:type and an object with a managed namespace.
     * @see RdfLexicon#isManagedNamespace
     */
    public static final Predicate<Triple> restrictedType = s -> s.getPredicate().equals(type.asNode()) &&
            (s.getObject().isURI() && isManagedNamespace.test(s.getObject().getNameSpace()));

    // FIXITY
    public static final Resource PREMIS_FIXITY =
            createResource(PREMIS_NAMESPACE + "Fixity");
    public static final Resource PREMIS_EVENT_OUTCOME_DETAIL =
            createResource(PREMIS_NAMESPACE + "EventOutcomeDetail");

    public static final Property HAS_MESSAGE_DIGEST =
            createProperty(PREMIS_NAMESPACE + "hasMessageDigest");

    public static final Property HAS_SIZE =
        createProperty(PREMIS_NAMESPACE + "hasSize");
    public static final Property HAS_FIXITY_RESULT =
        createProperty(PREMIS_NAMESPACE + "hasFixity");

    private static final Set<Property> fixityProperties = of(
            HAS_FIXITY_RESULT, HAS_MESSAGE_DIGEST);

    public static final Property HAS_FIXITY_STATE =
            createProperty(PREMIS_NAMESPACE + "hasEventOutcome");

    public static final Property WRITABLE =
            createProperty(REPOSITORY_NAMESPACE + "writable");

    public static final String FEDORA_NON_RDF_SOURCE_DESCRIPTION_URI = REPOSITORY_NAMESPACE +
            "NonRdfSourceDescription";

    // Server managed properties
    public static final Property CREATED_DATE =
            createProperty(REPOSITORY_NAMESPACE + "created");
    public static final Property CREATED_BY =
            createProperty(REPOSITORY_NAMESPACE + "createdBy");
    public static final Property LAST_MODIFIED_DATE =
            createProperty(REPOSITORY_NAMESPACE + "lastModified");
    public static final Property LAST_MODIFIED_BY =
            createProperty(REPOSITORY_NAMESPACE + "lastModifiedBy");

    public static final Resource FEDORA_CONTAINER =
            createResource(REPOSITORY_NAMESPACE + "Container");
    public static final Resource FEDORA_BINARY =
            createResource(REPOSITORY_NAMESPACE + "Binary");
    public static final Resource FEDORA_RESOURCE =
            createResource(REPOSITORY_NAMESPACE + "Resource");
    public static final Resource FEDORA_PAIR_TREE =
            createResource(REPOSITORY_NAMESPACE + "Pairtree");
    public static final Resource ARCHIVAL_GROUP =
            createResource(REPOSITORY_NAMESPACE + "ArchivalGroup");

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
    public static final Property INSERTED_CONTENT_RELATION =
            createProperty(LDP_NAMESPACE + "insertedContentRelation");
    public static final Property IS_MEMBER_OF_RELATION =
            createProperty(LDP_NAMESPACE + "isMemberOfRelation");
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
    public static final Property HAS_TRANSACTION_SERVICE =
            createProperty(REPOSITORY_NAMESPACE + "hasTransactionProvider");
    public static final Resource REPOSITORY_ROOT =
            createResource(REPOSITORY_NAMESPACE + "RepositoryRoot");

    // OTHER SERVICES
    public static final Property HAS_FIXITY_SERVICE =
            createProperty(REPOSITORY_NAMESPACE + "hasFixityService");

    public static final Property HAS_MIME_TYPE =
            createProperty(EBUCORE_NAMESPACE + "hasMimeType");
    public static final Property HAS_ORIGINAL_NAME =
            createProperty(EBUCORE_NAMESPACE + "filename");

    // EXTERNAL CONTENT
    public static final Property EXTERNAL_CONTENT = createProperty(FCREPO_API_NAMESPACE + "ExternalContent");
    public static final Property PROXY_FOR = createProperty(REPOSITORY_NAMESPACE + "proxyFor");
    public static final Property REDIRECTS_TO = createProperty(REPOSITORY_NAMESPACE + "redirectsTo");

    // RDF EXTRACTION
    public static final Property INBOUND_REFERENCES = createProperty(FCREPO_API_NAMESPACE + "PreferInboundReferences");
    public static final Property PREFER_SERVER_MANAGED = createProperty(REPOSITORY_NAMESPACE + "ServerManaged");
    public static final Property PREFER_MINIMAL_CONTAINER = createProperty(LDP_NAMESPACE +
            "PreferMinimalContainer");
    public static final Property PREFER_CONTAINMENT = createProperty(LDP_NAMESPACE + "PreferContainment");
    public static final Property PREFER_MEMBERSHIP = createProperty(LDP_NAMESPACE + "PreferMembership");


    public static final Property EMBED_CONTAINED = createProperty(OA_NAMESPACE + "PreferContainedDescriptions");


    // WEBAC
    public static final String WEBAC_ACCESS_CONTROL_VALUE = WEBAC_NAMESPACE_VALUE + "accessControl";

    public static final String SERVER_MANAGED_PROPERTIES_MODE = "fcrepo.properties.management";

    public static final String WEBAC_ACCESS_TO = WEBAC_NAMESPACE_VALUE + "accessTo";

    public static final String WEBAC_ACCESS_TO_CLASS = WEBAC_NAMESPACE_VALUE + "accessToClass";

    public static final Property WEBAC_ACCESS_TO_PROPERTY = createProperty(WEBAC_ACCESS_TO);

    public static final String FEDORA_WEBAC_ACL_URI = REPOSITORY_WEBAC_NAMESPACE + "Acl";

    // Properties which are managed by the server but are not from managed namespaces
    private static final Set<Property> serverManagedProperties;
    static {
        final ImmutableSet.Builder<Property> b = ImmutableSet.builder();
        b.addAll(fixityProperties).addAll(ldpManagedProperties);
        serverManagedProperties = b.build();
    }

    private static final Predicate<Property> hasFedoraNamespace =
        p -> !p.isAnon() && p.getNameSpace().startsWith(REPOSITORY_NAMESPACE);

    private static Predicate<Property> hasMementoNamespace =
        p -> !p.isAnon() && p.getNameSpace().startsWith(MEMENTO_NAMESPACE);

    // Server managed properties which may be overridden by clients when the server is in "relaxed" mode
    private static final Set<Property> relaxableProperties = of(LAST_MODIFIED_BY, LAST_MODIFIED_DATE, CREATED_BY,
            CREATED_DATE);

    // Detects if a server managed property is allowed to be updated in "relaxed" mode
    public static final Predicate<Property> isRelaxed =
            p -> relaxableProperties.contains(p)
                    && ("relaxed".equals(System.getProperty(SERVER_MANAGED_PROPERTIES_MODE)));

    /**
     * Detects whether an RDF property is managed by the repository.
     */
    public static final Predicate<Property> isManagedPredicate =
            hasFedoraNamespace.or(hasMementoNamespace).or(p -> serverManagedProperties.contains(p));

    // VERSIONING
    /**
     * Memento TimeMap type.
     */
    public static final String VERSIONING_TIMEMAP_TYPE = MEMENTO_NAMESPACE + "TimeMap";
    public static final Resource VERSIONING_TIMEMAP = createResource(VERSIONING_TIMEMAP_TYPE);

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
    public static final Resource VERSIONED_RESOURCE =
        createResource(MEMENTO_NAMESPACE + "OriginalResource");

    public static final Property MEMENTO_ORIGINAL_RESOURCE =
        createProperty(MEMENTO_NAMESPACE + "original");

    /*
     * Interaction Models.
     */
    public static final Set<Resource> INTERACTION_MODEL_RESOURCES = of(
            BASIC_CONTAINER, INDIRECT_CONTAINER, DIRECT_CONTAINER, NON_RDF_SOURCE);

    /**
     * String set of valid interaction models with full LDP URI.
     */
    public static final Set<String> INTERACTION_MODELS_FULL = INTERACTION_MODEL_RESOURCES.stream().map(Resource::getURI)
            .collect(Collectors.toSet());

    /**
     * This defines what we assume if you don't specify.
     */
    public static final Resource DEFAULT_INTERACTION_MODEL = BASIC_CONTAINER;

    private RdfLexicon() {
        // This constructor left intentionally blank.
    }
}
