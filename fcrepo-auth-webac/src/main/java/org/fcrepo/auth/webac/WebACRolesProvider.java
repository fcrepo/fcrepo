/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.webac;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.fcrepo.auth.webac.URIConstants.FOAF_AGENT_VALUE;
import static org.fcrepo.auth.webac.URIConstants.VCARD_GROUP_VALUE;
import static org.fcrepo.auth.webac.URIConstants.VCARD_MEMBER_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_ACCESSTO_CLASS_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_ACCESSTO_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_AGENT_CLASS_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_AGENT_GROUP_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_AGENT_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_AUTHENTICATED_AGENT_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_AUTHORIZATION_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_DEFAULT_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_NAMESPACE_VALUE;
import static org.fcrepo.http.api.FedoraAcl.getDefaultAcl;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.fcrepo.config.AuthPropsConfig;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.auth.ACLHandle;
import org.fcrepo.kernel.api.auth.WebACAuthorization;
import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.exception.PathNotFoundRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.models.WebacAcl;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;


/**
 * @author acoburn
 * @since 9/3/15
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class WebACRolesProvider {

    private static final Logger LOGGER = getLogger(WebACRolesProvider.class);

    private static final org.apache.jena.graph.Node RDF_TYPE_NODE = createURI(RDF_NAMESPACE + "type");
    private static final org.apache.jena.graph.Node VCARD_GROUP_NODE = createURI(VCARD_GROUP_VALUE);
    private static final org.apache.jena.graph.Node VCARD_MEMBER_NODE = createURI(VCARD_MEMBER_VALUE);

    @Inject
    private AuthPropsConfig authPropsConfig;

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private Cache<String, Optional<ACLHandle>> authHandleCache;

    /**
     * Retrieve an effective ACL using the cache, but do not negatively-cache empty results.
     *
     * Caffeine will cache whatever value is returned by the mapping function, including Optional.empty().
     * If ACL resolution transiently fails (e.g., during concurrent writes/index lag), caching an empty
     * value can cause authorization to incorrectly fall back to the classpath/root ACL until the
     * entry expires or the process restarts.
     */
    private Optional<ACLHandle> getEffectiveAclCached(final FedoraResource resource) {
        final var key = resource.getId();
        LOGGER.info("Getting ACL for {}", key);
        final var cached = authHandleCache.getIfPresent(key);
        LOGGER.info("Cached: {}", cached);
        // If present in cache and non-empty, use it.
        if (cached != null && cached.isPresent()) {
            return cached;
        }

        // Compute fresh.
        final var computed = getEffectiveAcl(resource, false);

        // Only cache positive results.
        if (computed.isPresent()) {
            authHandleCache.put(key, computed);
        } else {
            // Ensure we don't keep a stale negative cache entry.
            authHandleCache.invalidate(key);
        }

        return computed;
    }

    private String userBaseUri;
    private String groupBaseUri;

    @PostConstruct
    public void setup() {
        this.userBaseUri = authPropsConfig.getUserAgentBaseUri();
        this.groupBaseUri = authPropsConfig.getGroupAgentBaseUri();
    }

    /**
     * Get the roles assigned to a FedoraId using the default authorization. This allows non-resource transactions to
     * retrieve an ACL without the need for a stub resource.
     *
     * @param id the subject id
     * @param fedoraResource the parent resource of the id, most likely info:fedora
     * @param transaction the transaction being acted upon
     * @return a mapping of each principal to a set of its roles
     */
    public Map<String, Collection<String>> getRoles(final FedoraId id,
                                                    final FedoraResource fedoraResource,
                                                    final Transaction transaction) {
        LOGGER.debug("Getting agent roles for id: {}", id);
        // Construct a list of acceptable acl:accessTo values for the target resource.
        final List<String> resourcePaths = new ArrayList<>();

        // See if the root acl has been updated
        final var effectiveAcl = getEffectiveAclCached(fedoraResource);
        effectiveAcl.map(ACLHandle::getResource)
            .filter(effectiveResource -> !effectiveResource.getId().equals(id.getResourceId()))
            .ifPresent(effectiveResource -> {
                resourcePaths.add(effectiveResource.getId());
            });


        // Add the tx + ancestor paths
        resourcePaths.add(id.getResourceId());
        resourcePaths.addAll(getAllPathAncestors(id.getResourceId()));

        // Create a function to check acl:accessTo, scoped to the given resourcePaths
        final Predicate<WebACAuthorization> checkAccessTo = accessTo.apply(resourcePaths);

        final var authorizations = effectiveAcl.map(ACLHandle::getAuthorizations)
                                               .orElseGet(this::getDefaultAuthorizations);
        final var effectiveRoles = getEffectiveRoles(authorizations, checkAccessTo, transaction);

        LOGGER.debug("Unfiltered ACL: {}", effectiveRoles);
        return effectiveRoles;
    }

    /**
     * Get the roles assigned to this Node.
     *
     * @param resource the subject resource
     * @param transaction the transaction being acted upon
     * @return a set of roles for each principal
     */
    public Map<String, Collection<String>> getRoles(final FedoraResource resource, final Transaction transaction) {
        LOGGER.debug("Getting agent roles for resource: {}", resource.getId());

        // Get the effective ACL by searching the target node and any ancestors.
        final Optional<ACLHandle> effectiveAcl = getEffectiveAclCached(resource);

        // Construct a list of acceptable acl:accessTo values for the target resource.
        final List<String> resourcePaths = new ArrayList<>();
        if (resource instanceof WebacAcl) {
            // ACLs don't describe their resource, but we still want the container which is the resource.
            resourcePaths.add(resource.getContainer().getId());
        } else {
            resourcePaths.add(resource.getDescribedResource().getId());
        }

        // Construct a list of acceptable acl:accessToClass values for the target resource.
        final List<URI> rdfTypes = resource.getDescription().getTypes();

        // Add the resource location and types of the ACL-bearing parent,
        // if present and if different than the target resource.
        effectiveAcl
            .map(ACLHandle::getResource)
            .filter(effectiveResource -> !effectiveResource.getId().equals(resource.getId()))
            .ifPresent(effectiveResource -> {
                resourcePaths.add(effectiveResource.getId());
                rdfTypes.addAll(effectiveResource.getTypes());
            });

        // If we fall through to the system/classpath-based Authorization and it
        // contains any acl:accessTo properties, it is necessary to add each ancestor
        // path up the node hierarchy, starting at the resource location up to the
        // root location. This way, the checkAccessTo predicate (below) can be properly
        // created to match any acl:accessTo values that are part of the getDefaultAuthorization.
        // This is not relevant if an effectiveAcl is present.
        if (effectiveAcl.isEmpty()) {
            resourcePaths.addAll(getAllPathAncestors(resource.getId()));
        }

        // Create a function to check acl:accessTo, scoped to the given resourcePaths
        final Predicate<WebACAuthorization> checkAccessTo = accessTo.apply(resourcePaths);

        // Create a function to check acl:accessToClass, scoped to the given rdf:type values,
        // but transform the URIs to Strings first.
        final Predicate<WebACAuthorization> checkAccessToClass =
            accessToClass.apply(rdfTypes.stream().map(URI::toString).collect(toList()));

        // Read the effective Acl and return a list of acl:Authorization statements
        final List<WebACAuthorization> authorizations = effectiveAcl
                .map(ACLHandle::getAuthorizations)
                .orElseGet(this::getDefaultAuthorizations);

        // Filter the acl:Authorization statements so that they correspond only to statements that apply to
        // the target (or acl-bearing ancestor) resource path or rdf:type.
        // Then, assign all acceptable acl:mode values to the relevant acl:agent values: this creates a UNION
        // of acl:modes for each particular acl:agent.
        final Map<String, Collection<String>> effectiveRoles =
            getEffectiveRoles(authorizations, checkAccessTo.or(checkAccessToClass), transaction);

        LOGGER.debug("Unfiltered ACL: {}", effectiveRoles);

        return effectiveRoles;
    }

    /**
     * Get the effective roles for a list of authorizations
     *
     * @param authorizations The authorizations to get roles for
     * @param authorizationFilter The filter to apply on the list of roles
     * @param transaction the transaction being acted upon
     * @return a mapping of each principal to a set of its roles
     */
    private Map<String, Collection<String>> getEffectiveRoles(final List<WebACAuthorization> authorizations,
                                                              final Predicate<WebACAuthorization> authorizationFilter,
                                                              final Transaction transaction) {
        final Predicate<String> isFoafOrAuthenticated = (agentClass) ->
            agentClass.equals(FOAF_AGENT_VALUE) || agentClass.equals(WEBAC_AUTHENTICATED_AGENT_VALUE);

        final Map<String, Collection<String>> effectiveRoles = new HashMap<>();
        authorizations.stream()
                      .filter(authorizationFilter)
                      .forEach(auth -> {
                          final var modes = auth.getModes().stream().map(URI::toString).collect(toSet());
                          concat(auth.getAgents().stream(),
                                 dereferenceAgentGroups(transaction, auth.getAgentGroups()).stream())
                              .filter(Predicate.not(isFoafOrAuthenticated))
                              .forEach(agent -> {
                                  effectiveRoles.computeIfAbsent(agent, key -> new HashSet<>())
                                                .addAll(modes);
                              });
                          auth.getAgentClasses()
                              .stream()
                              .filter(isFoafOrAuthenticated)
                              .forEach(agentClass -> {
                                  effectiveRoles.computeIfAbsent(agentClass, key -> new HashSet<>())
                                                .addAll(modes);
                              });
                      });
        return effectiveRoles;
    }

    /**
     * Given a path (e.g. /a/b/c/d) retrieve a list of all ancestor paths.
     * In this case, that would be a list of "/a/b/c", "/a/b", "/a" and "/".
     */
    private static List<String> getAllPathAncestors(final String path) {
        final List<String> segments = asList(path.replace(FEDORA_ID_PREFIX, "").split("/"));
        return range(1, segments.size())
                .mapToObj(frameSize -> {
                    final var subpath = String.join("/", segments.subList(1, frameSize));
                    return FEDORA_ID_PREFIX + (!subpath.isBlank() ? "/" : "") + subpath;
                })
                .collect(toList());
    }

    /**
     *  This is a function for generating a Predicate that filters WebACAuthorizations according
     *  to whether the given acl:accessToClass values contain any of the rdf:type values provided
     *  when creating the predicate.
     */
    private static final Function<List<String>, Predicate<WebACAuthorization>> accessToClass = uris -> auth ->
        uris.stream().anyMatch(uri -> auth.getAccessToClassURIs().contains(uri));

    /**
     *  This is a function for generating a Predicate that filters WebACAuthorizations according
     *  to whether the given acl:accessTo values contain any of the target resource values provided
     *  when creating the predicate.
     */
    private static final Function<List<String>, Predicate<WebACAuthorization>> accessTo = uris -> auth ->
        uris.stream().anyMatch(uri -> auth.getAccessToURIs().contains(uri));

    /**
     *  This maps a Collection of acl:agentGroup values to a List of agents.
     *  Any out-of-domain URIs are silently ignored.
     */
    private List<String> dereferenceAgentGroups(final Transaction transaction, final Collection<String> agentGroups) {
        final List<String> members = agentGroups.stream().flatMap(agentGroup -> {
            if (agentGroup.startsWith(FEDORA_ID_PREFIX)) {
                //strip off trailing hash.
                final int hashIndex = agentGroup.indexOf("#");
                final String agentGroupNoHash = hashIndex > 0 ?
                                         agentGroup.substring(0, hashIndex) :
                                         agentGroup;
                final String hashedSuffix = hashIndex > 0 ? agentGroup.substring(hashIndex) : null;
                try {
                    final FedoraId fedoraId = FedoraId.create(agentGroupNoHash);
                    final FedoraResource resource = resourceFactory.getResource(transaction, fedoraId);
                    return getAgentMembers(resource, hashedSuffix);
                } catch (final PathNotFoundException e) {
                    throw new PathNotFoundRuntimeException(e.getMessage(), e);
                }
            } else if (agentGroup.equals(FOAF_AGENT_VALUE)) {
                return of(agentGroup);
            } else {
                LOGGER.info("Ignoring agentGroup: {}", agentGroup);
                return empty();
            }
        }).collect(toList());

        if (LOGGER.isDebugEnabled() && !agentGroups.isEmpty()) {
            LOGGER.debug("Found {} members in {} agentGroups resources", members.size(), agentGroups.size());
        }

        return members;
    }

    /**
     * Given a FedoraResource, return a list of agents.
     */
    private Stream<String> getAgentMembers(final FedoraResource resource, final String hashPortion) {
        //resolve list of triples, accounting for hash-uris.
        final List<Triple> triples = resource.getTriples().filter(
            triple -> hashPortion == null || triple.getSubject().getURI().endsWith(hashPortion)).collect(toList());
        //determine if there is a rdf:type vcard:Group
        final boolean hasVcardGroup = triples.stream().anyMatch(
            triple -> triple.matches(triple.getSubject(), RDF_TYPE_NODE, VCARD_GROUP_NODE));
        //return members only if there is an associated vcard:Group
        if (hasVcardGroup) {
            return triples.stream()
                          .filter(triple -> triple.predicateMatches(VCARD_MEMBER_NODE))
                          .map(Triple::getObject).flatMap(WebACRolesProvider::nodeToStringStream)
                                                 .map(this::stripUserAgentBaseURI);
        } else {
            return empty();
        }
    }

    private String stripUserAgentBaseURI(final String object) {
        if (userBaseUri != null && object.startsWith(userBaseUri)) {
            return object.substring(userBaseUri.length());
        }
        return object;
    }

    /**
     * Map a Jena Node to a Stream of Strings. Any non-URI, non-Literals map to an empty Stream,
     * making this suitable to use with flatMap.
     */
    private static Stream<String> nodeToStringStream(final org.apache.jena.graph.Node object) {
        if (object.isURI()) {
            return of(object.getURI());
        } else if (object.isLiteral()) {
            return of(object.getLiteralValue().toString());
        } else {
            return empty();
        }
    }


    /**
     *  A simple predicate for filtering out any non-acl triples.
     */
    private static final Predicate<Triple> hasAclPredicate = triple ->
        triple.getPredicate().getNameSpace().equals(WEBAC_NAMESPACE_VALUE);

    /**
     * This function reads a Fedora ACL resource and all of its acl:Authorization children.
     * The RDF from each child resource is put into a WebACAuthorization object, and the
     * full list is returned.
     *
     * @param aclResource the ACL resource
     * @param ancestorAcl flag indicating whether or not the ACL resource associated with an ancestor of the target
     *                    resource
     * @return a list of acl:Authorization objects
     */
    private List<WebACAuthorization> getAuthorizations(final FedoraResource aclResource,
                                                       final boolean ancestorAcl) {

        final List<WebACAuthorization> authorizations = new ArrayList<>();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ACL: {}", aclResource.getId());
        }

        if (aclResource.isAcl()) {
            //resolve set of subjects that are of type acl:authorization
            final List<Triple> triples = aclResource.getTriples().collect(toList());

            final Set<org.apache.jena.graph.Node> authSubjects = triples.stream().filter(t -> {
                return t.getPredicate().getURI().equals(RDF_NAMESPACE + "type") &&
                       t.getObject().getURI().equals(WEBAC_AUTHORIZATION_VALUE);
            }).map(t -> t.getSubject()).collect(Collectors.toSet());

            // Read resource, keeping only acl-prefixed triples.
            final Map<String, Map<String, List<String>>> authMap = new HashMap<>();
            triples.stream().filter(hasAclPredicate)
                    .forEach(triple -> {
                        if (authSubjects.contains(triple.getSubject())) {
                            final Map<String, List<String>> aclTriples =
                                authMap.computeIfAbsent(triple.getSubject().getURI(), key -> new HashMap<>());

                            final String predicate = triple.getPredicate().getURI();
                            final List<String> values = aclTriples.computeIfAbsent(predicate,
                                                                                   key -> new ArrayList<>());
                            nodeToStringStream(triple.getObject()).forEach(values::add);
                            if (predicate.equals(WEBAC_AGENT_VALUE)) {
                                additionalAgentValues(triple.getObject()).forEach(values::add);
                            }
                        }
                    });
            // Create a WebACAuthorization object from the provided triples.
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Adding acl:Authorization from {}", aclResource.getId());
            }
            authMap.values().forEach(aclTriples -> {
                final WebACAuthorization authorization = createAuthorizationFromMap(aclTriples);
                //only include authorizations if the acl resource is not an ancestor acl
                //or the authorization has at least one acl:default
                if (!ancestorAcl || authorization.getDefaults().size() > 0) {
                    authorizations.add(authorization);
                }
            });
        }

        return authorizations;
    }

    private static WebACAuthorization createAuthorizationFromMap(final Map<String, List<String>> data) {
        return new WebACAuthorizationImpl(
                data.getOrDefault(WEBAC_AGENT_VALUE, emptyList()),
                data.getOrDefault(WEBAC_AGENT_CLASS_VALUE, emptyList()),
                data.getOrDefault(WEBAC_MODE_VALUE, emptyList()).stream()
                .map(URI::create).collect(toList()),
                data.getOrDefault(WEBAC_ACCESSTO_VALUE, emptyList()),
                data.getOrDefault(WEBAC_ACCESSTO_CLASS_VALUE, emptyList()),
                data.getOrDefault(WEBAC_AGENT_GROUP_VALUE, emptyList()),
                data.getOrDefault(WEBAC_DEFAULT_VALUE, emptyList()));
    }

    /**
     * Recursively find the effective ACL as a URI along with the FedoraResource that points to it.
     * This way, if the effective ACL is pointed to from a parent resource, the child will inherit
     * any permissions that correspond to access to that parent. This ACL resource may or may not exist,
     * and it may be external to the fedora repository.
     * @param resource the Fedora resource
     * @param ancestorAcl the flag for looking up ACL from ancestor hierarchy resources
     */
    Optional<ACLHandle> getEffectiveAcl(final FedoraResource resource, final boolean ancestorAcl) {

        final FedoraResource aclResource = resource.getAcl();

        if (aclResource != null) {
            final List<WebACAuthorization> authorizations =
                getAuthorizations(aclResource, ancestorAcl);
            if (authorizations.size() > 0) {
                return Optional.of(
                    new ACLHandleImpl(resource, authorizations));
            }
        }

        FedoraResource container = resource.getContainer();
        // The resource is not ldp:contained by anything, so checked its described resource.
        if (container == null && (resource instanceof NonRdfSourceDescription || resource instanceof TimeMap)) {
            final var described = resource.getDescribedResource();
            if (!Objects.equals(resource, described)) {
                container = described;
            }
        }
        if (container == null) {
            LOGGER.debug("No ACLs defined on this node or in parent hierarchy");
            return Optional.empty();
        } else {
            LOGGER.trace("Checking parent resource for ACL. No ACL found at {}", resource.getId());
            return getEffectiveAcl(container, true);
        }
    }

    private List<WebACAuthorization> getDefaultAuthorizations() {
        final List<WebACAuthorization> authorizations = new ArrayList<>();

        final var defaultAcls = getDefaultAcl(null, authPropsConfig.getRootAuthAclPath());
        final var aclSubjects = defaultAcls.listSubjects();

        aclSubjects.forEach(aclResource -> {
            final Map<String, List<String>> aclTriples = new HashMap<>();
            aclResource.listProperties().mapWith(Statement::asTriple).forEach(aclTriple -> {
                if (hasAclPredicate.test(aclTriple)) {
                    final String predicate = aclTriple.getPredicate().getURI();
                    final List<String> values = aclTriples.computeIfAbsent(predicate, key -> new ArrayList<>());
                    nodeToStringStream(aclTriple.getObject()).forEach(values::add);
                    if (predicate.equals(WEBAC_AGENT_VALUE)) {
                        additionalAgentValues(aclTriple.getObject()).forEach(values::add);
                    }
                }
            });

            if (!aclTriples.isEmpty()) {
                authorizations.add(createAuthorizationFromMap(aclTriples));
            }
        });

        return authorizations;
    }

    private Stream<String> additionalAgentValues(final org.apache.jena.graph.Node object) {
        if (object.isURI()) {
            final String uri = object.getURI();
            if (userBaseUri != null && uri.startsWith(userBaseUri)) {
                return of(uri.substring(userBaseUri.length()));
            } else if (groupBaseUri != null && uri.startsWith(groupBaseUri)) {
                return of(uri.substring(groupBaseUri.length()));
            }
        }
        return empty();
    }

    /*
     * The below two methods are ONLY used by tests and so invalidating the cache should not have any impact.
     */

    /**
     * @param userBaseUri the user base uri
     */
    public void setUserBaseUri(final String userBaseUri) {
        this.userBaseUri = userBaseUri;
        authHandleCache.invalidateAll();
    }

    /**
     * @param groupBaseUri the group base uri
     */
    public void setGroupBaseUri(final String groupBaseUri) {
        this.groupBaseUri = groupBaseUri;
        authHandleCache.invalidateAll();
    }
}
