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
package org.fcrepo;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;

import com.hp.hpl.jena.rdf.model.Property;

/**
 * A lexicon of the RDF properties that the fcrepo kernel (or close-to-core modules) use
 */
public final class RdfLexicon {

    public static final String INTERNAL_NAMESPACE =
            "info:fedora/fedora-system:def/internal#";

    // MEMBERSHIP
    public static final Property HAS_MEMBER_OF_RESULT =
            createProperty(INTERNAL_NAMESPACE + "hasMember");
    public static final Property HAS_PARENT =
            createProperty(INTERNAL_NAMESPACE + "hasParent");
    public static final Property HAS_CHILD =
            createProperty(INTERNAL_NAMESPACE + "hasChild");
    public static final Property HAS_CHILD_COUNT =
            createProperty(INTERNAL_NAMESPACE + "numberOfChildren");

    // FIXITY
    public static final Property IS_FIXITY_RESULT_OF =
            createProperty(INTERNAL_NAMESPACE + "isFixityResultOf");
    public static final Property HAS_FIXITY_RESULT =
            createProperty(INTERNAL_NAMESPACE + "hasFixityResult");
    public static final Property HAS_FIXITY_STATE =
            createProperty(INTERNAL_NAMESPACE + "status");
    public static final Property HAS_COMPUTED_CHECKSUM =
            createProperty(INTERNAL_NAMESPACE + "computedChecksum");
    public static final Property HAS_COMPUTED_SIZE =
            createProperty(INTERNAL_NAMESPACE + "computedSize");

    public static final Property HAS_FIXITY_CHECK_COUNT =
            createProperty(INTERNAL_NAMESPACE + "numFixityChecks");
    public static final Property HAS_FIXITY_ERROR_COUNT =
            createProperty(INTERNAL_NAMESPACE + "numFixityErrors");
    public static final Property HAS_FIXITY_REPAIRED_COUNT =
            createProperty(INTERNAL_NAMESPACE + "numFixityRepaired");


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
            createProperty("info:fedora/search/hasMoreResults");

    public static final Property PAGE =
        createProperty("http://www.w3.org/ns/ldp#Page");
    public static final Property PAGE_OF =
        createProperty("http://www.w3.org/ns/ldp#pageOf");
    public static final Property NEXT_PAGE =
            createProperty("http://www.w3.org/ns/ldp#nextPage");

    // REPOSITORY INFORMATION
    public static final Property HAS_OBJECT_COUNT =
            createProperty(INTERNAL_NAMESPACE + "objectCount");
    public static final Property HAS_OBJECT_SIZE =
            createProperty(INTERNAL_NAMESPACE + "objectSize");
    public static final Property HAS_TRANSACTION_SERVICE =
            createProperty("info:fedora/hasTransactionProvider");
    public static final Property HAS_NAMESPACE_SERVICE =
            createProperty("info:fedora/hasNamespaces");
    public static final Property HAS_SEARCH_SERVICE =
            createProperty("http://www.whatwg.org/specs/web-apps/current-work/"
                                   + "#link-type-search");
    public static final Property HAS_SITEMAP =
            createProperty("http://microformats.org/wiki/rel-sitemap");

    // NAMESPACES
    public static final Property HAS_NAMESPACE_PREFIX =
            createProperty("http://purl.org/vocab/vann/preferredNamespacePrefix");
    public static final Property HAS_NAMESPACE_URI =
            createProperty("http://purl.org/vocab/vann/preferredNamespaceUri");
    public static final Property VOAF_VOCABULARY = createProperty("http://purl.org/vocommons/voaf#Vocabulary");


    // OTHER SERVICES
    public static final Property HAS_SERIALIZATION =
            createProperty("info:fedora/exportsAs");
    public static final Property HAS_VERSION_HISTORY =
            createProperty("info:fedora/hasVersions");
    public static final Property HAS_FIXITY_SERVICE =
            createProperty("info:fedora/hasFixity");
    public static final Property HAS_FEED =
            createProperty(
                    "http://www.whatwg.org/specs/web-apps/current-work/#",
                    "feed0");
    public static final Property HAS_SUBSCRIPTION_SERVICE =
            createProperty("http://microformats.org/wiki/rel-subscription");

    // CONTENT
    public static final Property HAS_CONTENT =
            createProperty(INTERNAL_NAMESPACE + "hasContent");
    public static final Property IS_CONTENT_OF =
            createProperty(INTERNAL_NAMESPACE + "isContentOf");
    public static final Property HAS_LOCATION =
            createProperty(INTERNAL_NAMESPACE + "hasLocation");
    public static final Property HAS_MIME_TYPE =
            createProperty(INTERNAL_NAMESPACE + "mimeType");
    public static final Property HAS_SIZE =
            createProperty(INTERNAL_NAMESPACE + "hasSize");

    // VERSIONING
    public static final Property HAS_VERSION =
            createProperty(INTERNAL_NAMESPACE + "hasVersion");
    public static final Property HAS_VERSION_LABEL =
            createProperty(INTERNAL_NAMESPACE + "hasVersionLabel");

    // RDF EXTRACTION
    public static final Property COULD_NOT_STORE_PROPERTY =
            createProperty(INTERNAL_NAMESPACE + "couldNotStoreProperty");

    // IMPORTANT JCR PROPERTIES
    public static final Property HAS_PRIMARY_IDENTIFIER =
            createProperty(INTERNAL_NAMESPACE + "uuid");
    public static final Property HAS_PRIMARY_TYPE =
            createProperty(INTERNAL_NAMESPACE + "primaryType");
    public static final Property HAS_NODE_TYPE =
            createProperty(INTERNAL_NAMESPACE + "hasNodeType");
    public static final Property HAS_MIXIN_TYPE =
            createProperty(INTERNAL_NAMESPACE + "mixinTypes");

    public static final Property CREATED_DATE =
            createProperty(INTERNAL_NAMESPACE + "created");
    public static final Property CREATED_BY =
            createProperty(INTERNAL_NAMESPACE + "createdBy");


    public static final Property LAST_MODIFIED_DATE =
            createProperty(INTERNAL_NAMESPACE + "lastModified");
    public static final Property LAST_MODIFIED_BY =
            createProperty(INTERNAL_NAMESPACE + "lastModifiedBy");


    public static final Property RDFS_LABEL =
            createProperty("http://www.w3.org/2000/01/rdf-schema#label");
    public static final Property DC_TITLE =
            createProperty("http://purl.org/dc/terms/title");

    private RdfLexicon() {

    }
}
