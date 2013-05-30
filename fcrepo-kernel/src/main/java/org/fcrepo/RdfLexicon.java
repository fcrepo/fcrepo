package org.fcrepo;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public final class RdfLexicon {

    public static final String INTERNAL_NAMESPACE = "info:fedora/fedora-system:def/internal#";

    // MEMBERSHIP
    public static final Property HAS_MEMBER_OF_RESULT = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#hasMember");
    public static final Property HAS_PARENT =  ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#hasParent");
    public static final Property HAS_CHILD =  ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#hasChild");
    public static final Property HAS_CHILD_COUNT =  ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#numberOfChildren");

    // FIXITY
    public static final Property IS_FIXITY_RESULT_OF = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#isFixityResultOf");
    public static final Property HAS_FIXITY_RESULT = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#hasFixityResult");
    public static final Property HAS_FIXITY_STATE = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#status");
    public static final Property HAS_COMPUTED_CHECKSUM = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#computedChecksum");
    public static final Property HAS_COMPUTED_SIZE = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#computedChecksum");

    // SEARCH
    public static final Property SEARCH_HAS_TOTAL_RESULTS = ResourceFactory.createProperty("http://a9.com/-/spec/opensearch/1.1/totalResults");
    public static final Property SEARCH_ITEMS_PER_PAGE = ResourceFactory.createProperty("http://a9.com/-/spec/opensearch/1.1/itemsPerPage");
    public static final Property SEARCH_OFFSET = ResourceFactory.createProperty("http://a9.com/-/spec/opensearch/1.1/startIndex");
    public static final Property SEARCH_TERMS = ResourceFactory.createProperty("http://a9.com/-/spec/opensearch/1.1/Query#searchTerms");
    public static final Property SEARCH_HAS_MORE = ResourceFactory.createProperty("info:fedora/search/hasMoreResults");
    public static final Property SEARCH_NEXT_PAGE = ResourceFactory.createProperty("info:fedora/search/next");

    // REPOSITORY INFORMATION
    public static final Property HAS_OBJECT_COUNT = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#objectCount");
    public static final Property HAS_OBJECT_SIZE = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#objectSize");
    public static final Property HAS_TRANSACTION_SERVICE = ResourceFactory.createProperty("info:fedora/hasTransactionProvider");
    public static final Property HAS_NAMESPACE_SERVICE = ResourceFactory.createProperty("info:fedora/hasNamespaces");
    public static final Property HAS_SEARCH_SERVICE = ResourceFactory.createProperty("http://www.whatwg.org/specs/web-apps/current-work/multipage/links.html#link-type-search");
    public static final Property HAS_SITEMAP = ResourceFactory.createProperty("http://microformats.org/wiki/rel-sitemap");

    // NAMESPACES
    public static final Property HAS_NAMESPACE_PREFIX = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#hasNamespace");


    // OTHER SERVICES
    public static final Property HAS_SERIALIZATION = ResourceFactory.createProperty("info:fedora/exportsAs");
    public static final Property HAS_VERSION_HISTORY = ResourceFactory.createProperty("info:fedora/hasVersions");
    public static final Property HAS_FIXITY_SERVICE = ResourceFactory.createProperty("info:fedora/hasFixity");
    public static final Property HAS_FEED = ResourceFactory.createProperty("http://www.whatwg.org/specs/web-apps/current-work/multipage/history.html#link-type-feed");
    public static final Property HAS_SUBSCRIPTION_SERVICE = ResourceFactory.createProperty("http://microformats.org/wiki/rel-subscription");

    // CONTENT
    public static final Property HAS_CONTENT = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#hasContent");
    public static final Property IS_CONTENT_OF = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#isContentOf");
    public static final Property HAS_LOCATION = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#hasLocation");
    public static final Property HAS_MIME_TYPE = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#mimeType");
    public static final Property HAS_SIZE = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#hasSize");

    // VERSIONING
    public static final Property HAS_VERSION = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#hasVersion");
    public static final Property HAS_VERSION_LABEL = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#hasVersionLabel");

    // IMPORTANT JCR PROPERTIES
    public static final Property HAS_PRIMARY_IDENTIFIER = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#uuid");
    public static final Property HAS_PRIMARY_TYPE = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#primaryType");
    public static final Property HAS_NODE_TYPE = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#hasNodeType");
    public static final Property HAS_MIXIN_TYPE = ResourceFactory.createProperty("info:fedora/fedora-system:def/internal#mixinTypes");



    private RdfLexicon() {

    }
}
