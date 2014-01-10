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
package org.fcrepo.kernel;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.ImmutableSet.of;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;

import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A lexicon of the RDF properties that the fcrepo kernel (or close-to-core modules) use
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


    /**
     * REST API namespace "fedora", used for internal API links and node
     * paths.
     * Was "info:fedora/".
    **/
    public static final String RESTAPI_NAMESPACE =
            "http://fedora.info/definitions/v4/rest-api#";

    /**
     * Fedora configuration namespace "fedora-config", used for user-settable
     * configuration properties.
     **/
    public static final String FEDORA_CONFIG_NAMESPACE =
            "http://fedora.info/definitions/v4/config#";

    /**
     * REST API namespace "fedora", used for internal API links and node
     * paths.
     * Was "info:fedora/".
     **/
    public static final String PREMIS_NAMESPACE =
        "http://www.loc.gov/premis/rdf/v1#";

    /**
     * The namespaces that the repository manages internally.
     */
    public static final Set<String> managedNamespaces = of(RESTAPI_NAMESPACE,
            REPOSITORY_NAMESPACE, JCR_NAMESPACE);

    /**
     * Is this namespace one that the repository manages?
     */
    public static final Predicate<String> isManagedNamespace =
        in(managedNamespaces);

    /**
     * Relations (RELS-EXT) namespace "fedorarelsext", used for linking
     * between Fedora objects.
     * Was "info:fedora/fedora-system:def/relations-external#".
    **/
    public static final String RELATIONS_NAMESPACE =
            "http://fedora.info/definitions/v4/rels-ext#";

    // MEMBERSHIP
    public static final Property HAS_MEMBER_OF_RESULT =
            createProperty(REPOSITORY_NAMESPACE + "hasMember");
    public static final Property HAS_PARENT =
            createProperty(REPOSITORY_NAMESPACE + "hasParent");
    public static final Property HAS_CHILD =
            createProperty(REPOSITORY_NAMESPACE + "hasChild");
    public static final Property HAS_CHILD_COUNT =
            createProperty(REPOSITORY_NAMESPACE + "numberOfChildren");

    public static final Set<Property> membershipProperties = of(
            HAS_MEMBER_OF_RESULT, HAS_PARENT, HAS_CHILD, HAS_CHILD_COUNT);

    // FIXITY

    public static final Resource FIXITY_TYPE = createResource(PREMIS_NAMESPACE + "Fixity");
    public static final Property HAS_MESSAGE_DIGEST =
        createProperty(PREMIS_NAMESPACE + "hasMessageDigest");
    public static final Property HAS_SIZE =
        createProperty(PREMIS_NAMESPACE + "hasSize");
    public static final Property HAS_FIXITY_RESULT =
        createProperty(PREMIS_NAMESPACE + "hasFixity");

    public static final Property HAS_FIXITY_STATE =
            createProperty(REPOSITORY_NAMESPACE + "status");

    public static final Property HAS_FIXITY_CHECK_COUNT =
            createProperty(REPOSITORY_NAMESPACE + "numFixityChecks");
    public static final Property HAS_FIXITY_ERROR_COUNT =
            createProperty(REPOSITORY_NAMESPACE + "numFixityErrors");
    public static final Property HAS_FIXITY_REPAIRED_COUNT =
            createProperty(REPOSITORY_NAMESPACE + "numFixityRepaired");

    public static final Set<Property> fixityProperties = of(
            HAS_FIXITY_RESULT, HAS_MESSAGE_DIGEST, HAS_SIZE, HAS_FIXITY_STATE,
            HAS_FIXITY_CHECK_COUNT, HAS_FIXITY_ERROR_COUNT, HAS_FIXITY_REPAIRED_COUNT);

    // SEARCH
    public static final Property SEARCH_PAGE = createProperty("http://sindice.com/vocab/search#Page");
    public static final Property SEARCH_HAS_TOTAL_RESULTS =
            createProperty("http://sindice.com/vocab/search#totalResults");
    public static final Property SEARCH_ITEMS_PER_PAGE =
            createProperty("http://sindice.com/vocab/search#itemsPerPage");
    public static final Property SEARCH_OFFSET =
            createProperty("http://sindice.com/vocab/search#startIndex");
    public static final Property SEARCH_TERMS =
            createProperty("http://sindice.com/vocab/search#searchTerms");
    public static final Property SEARCH_HAS_MORE =
            createProperty(RESTAPI_NAMESPACE + "hasMoreResults");

    public static final Set<Property> searchProperties = of(SEARCH_PAGE,
            SEARCH_HAS_TOTAL_RESULTS, SEARCH_ITEMS_PER_PAGE, SEARCH_OFFSET,
            SEARCH_OFFSET, SEARCH_TERMS, SEARCH_HAS_MORE);

    // Linked Data Platform
    public static final String LDP_NAMESPACE = "http://www.w3.org/ns/ldp#";
    public static final Property PAGE =
        createProperty(LDP_NAMESPACE + "Page");
    public static final Property PAGE_OF =
        createProperty(LDP_NAMESPACE + "pageOf");
    public static final Property FIRST_PAGE =
        createProperty(LDP_NAMESPACE + "firstPage");
    public static final Property NEXT_PAGE =
            createProperty(LDP_NAMESPACE + "nextPage");
    public static final Property MEMBERS_INLINED =
            createProperty(LDP_NAMESPACE + "membersInlined");
    public static final Property CONTAINER =
            createProperty(LDP_NAMESPACE + "Container");
    public static final Property MEMBERSHIP_SUBJECT =
            createProperty(LDP_NAMESPACE + "membershipSubject");
    public static final Property MEMBERSHIP_PREDICATE =
            createProperty(LDP_NAMESPACE + "membershipPredicate");
    public static final Property MEMBERSHIP_OBJECT =
            createProperty(LDP_NAMESPACE + "membershipObject");
    public static final Property MEMBER_SUBJECT =
            createProperty(LDP_NAMESPACE + "MemberSubject");
    public static final Property INLINED_RESOURCE =
        createProperty(LDP_NAMESPACE + "inlinedResource");

    public static final Set<Property> ldpProperties = of(PAGE, PAGE_OF,
            FIRST_PAGE, NEXT_PAGE, MEMBERS_INLINED, CONTAINER,
            MEMBERSHIP_SUBJECT, MEMBERSHIP_PREDICATE, MEMBERSHIP_OBJECT,
            MEMBER_SUBJECT, INLINED_RESOURCE);

    // REPOSITORY INFORMATION
    public static final Property HAS_OBJECT_COUNT =
            createProperty(REPOSITORY_NAMESPACE + "objectCount");
    public static final Property HAS_OBJECT_SIZE =
            createProperty(REPOSITORY_NAMESPACE + "objectSize");
    public static final Property HAS_TRANSACTION_SERVICE =
            createProperty(RESTAPI_NAMESPACE + "hasTransactionProvider");
    public static final Property HAS_NAMESPACE_SERVICE =
            createProperty(RESTAPI_NAMESPACE + "hasNamespaces");
    public static final Property HAS_WORKSPACE_SERVICE =
            createProperty(RESTAPI_NAMESPACE + "hasWorkspaces");
    public static final Property HAS_ACCESS_ROLES_SERVICE =
            createProperty(RESTAPI_NAMESPACE + "hasAccessRoles");
    public static final Property HAS_SEARCH_SERVICE =
            createProperty("http://www.whatwg.org/specs/web-apps/current-work/"
                                   + "#link-type-search");
    public static final Property HAS_SITEMAP =
            createProperty("http://microformats.org/wiki/rel-sitemap");

    public static final Set<Property> repositoryProperties = of(
            HAS_OBJECT_COUNT, HAS_OBJECT_SIZE, HAS_TRANSACTION_SERVICE,
            HAS_NAMESPACE_SERVICE, HAS_WORKSPACE_SERVICE, HAS_SEARCH_SERVICE,
            HAS_SITEMAP);

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
            createProperty(RESTAPI_NAMESPACE + "exportsAs");
    public static final Property HAS_VERSION_HISTORY =
            createProperty(RESTAPI_NAMESPACE + "hasVersions");
    public static final Property HAS_FIXITY_SERVICE =
            createProperty(RESTAPI_NAMESPACE + "hasFixityService");
    public static final Property HAS_FEED =
            createProperty(
                    "http://www.whatwg.org/specs/web-apps/current-work/#",
                    "feed0");
    public static final Property HAS_SUBSCRIPTION_SERVICE =
            createProperty("http://microformats.org/wiki/rel-subscription");
    public static final Property NOT_IMPLEMENTED =
            createProperty(REPOSITORY_NAMESPACE + "notImplemented");
    public static final Property HAS_SPARQL_ENDPOINT =
        createProperty(RESTAPI_NAMESPACE + "sparql");

    public static final Set<Property> otherServiceProperties = of(
            HAS_SERIALIZATION, HAS_VERSION_HISTORY, HAS_FIXITY_SERVICE,
            HAS_FEED, HAS_SUBSCRIPTION_SERVICE, NOT_IMPLEMENTED);


    // CONTENT
    public static final Property HAS_CONTENT =
            createProperty(REPOSITORY_NAMESPACE + "hasContent");
    public static final Property IS_CONTENT_OF =
            createProperty(REPOSITORY_NAMESPACE + "isContentOf");
    public static final Resource CONTENT_LOCATION_TYPE =
            createResource(PREMIS_NAMESPACE + "ContentLocation");
    public static final Property HAS_CONTENT_LOCATION =
            createProperty(PREMIS_NAMESPACE + "hasContentLocation");
    public static final Property HAS_CONTENT_LOCATION_VALUE =
        createProperty(PREMIS_NAMESPACE + "hasContentLocationValue");
    public static final Property HAS_MIME_TYPE =
            createProperty(REPOSITORY_NAMESPACE + "mimeType");
    public static final Property HAS_ORIGINAL_NAME =
            createProperty(PREMIS_NAMESPACE + "hasOriginalName");

    public static final Set<Property> contentProperties = of(HAS_CONTENT,
            IS_CONTENT_OF, HAS_CONTENT_LOCATION, HAS_CONTENT_LOCATION_VALUE,
            HAS_MIME_TYPE, HAS_ORIGINAL_NAME, HAS_SIZE);


    // VERSIONING
    public static final Property HAS_VERSION =
            createProperty(REPOSITORY_NAMESPACE + "hasVersion");
    public static final Property HAS_VERSION_LABEL =
            createProperty(REPOSITORY_NAMESPACE + "hasVersionLabel");
    public static final Property VERSIONING_POLICY =
            createProperty(FEDORA_CONFIG_NAMESPACE + "versioningPolicy");

    public static final Set<Property> versioningProperties = of(HAS_VERSION,
            HAS_VERSION_LABEL);

    // RDF EXTRACTION
    public static final Property COULD_NOT_STORE_PROPERTY =
            createProperty(REPOSITORY_NAMESPACE + "couldNotStoreProperty");

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

    public static final Resource WORKSPACE_TYPE = createResource(JCR_NAMESPACE + "#Workspace");
    public static final Property HAS_WORKSPACE = createProperty(REPOSITORY_NAMESPACE + "hasWorkspace");
    public static final Property HAS_DEFAULT_WORKSPACE = createProperty(REPOSITORY_NAMESPACE + "hasDefaultWorkspace");

    public static final Set<Property> managedProperties;

    static {
        final ImmutableSet.Builder<Property> b = ImmutableSet.builder();
        b.addAll(membershipProperties).addAll(fixityProperties).addAll(
                searchProperties).addAll(ldpProperties).addAll(
                repositoryProperties).addAll(namespaceProperties).addAll(
                otherServiceProperties).addAll(contentProperties).addAll(
                versioningProperties).addAll(jcrProperties);
        managedProperties = b.build();
    }

    private static Predicate<Property> hasJcrNamespace =
        new Predicate<Property>() {

            @Override
            public boolean apply(final Property p) {
                return !p.isAnon() && p.getNameSpace().equals(JCR_NAMESPACE);

            }
        };

    private static Predicate<Property> hasFedoraNamespace =
        new Predicate<Property>() {

            @Override
            public boolean apply(final Property p) {
                return !p.isAnon()
                        && p.getNameSpace().startsWith(REPOSITORY_NAMESPACE);

            }
        };

    /**
     * Detects whether an RDF property is managed by the repository.
     */
    @SuppressWarnings("unchecked")
    public static final Predicate<Property> isManagedPredicate = or(
            in(managedProperties), hasJcrNamespace, hasFedoraNamespace);

    /**
     * Detects whether an RDF predicate URI is managed by the repository.
    **/
    public static Predicate<String> isManagedPredicateURI =
        new Predicate<String>() {
            @Override
            public boolean apply(final String uri) {
                return isManagedPredicate.apply( createProperty(uri) );
            }
        };

    private RdfLexicon() {

    }
}
