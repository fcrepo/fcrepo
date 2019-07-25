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
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.NodeService;
import org.slf4j.Logger;

/**
 * @author acoburn
 * @since 9/3/15
 */
public class WebACRolesProvider {

    public static final String GROUP_AGENT_BASE_URI_PROPERTY = "fcrepo.auth.webac.groupAgent.baseUri";

    public static final String USER_AGENT_BASE_URI_PROPERTY = "fcrepo.auth.webac.userAgent.baseUri";

    private static final Logger LOGGER = getLogger(WebACRolesProvider.class);

    private static final String FEDORA_INTERNAL_PREFIX = "info:fedora";

    private static final org.apache.jena.graph.Node RDF_TYPE_NODE = createURI(RDF_NAMESPACE + "type");
    private static final org.apache.jena.graph.Node VCARD_GROUP_NODE = createURI(VCARD_GROUP_VALUE);
    private static final org.apache.jena.graph.Node VCARD_MEMBER_NODE = createURI(VCARD_MEMBER_VALUE);

    @Inject
    private NodeService nodeService;

    @Inject
    private SessionFactory sessionFactory;

    /**
     * Get the roles assigned to this Node.
     *
     * @param resource the subject resource
     * @return a set of roles for each principal
     */
    public Map<String, Collection<String>> getRoles(final FedoraResource resource) {
        return getAgentRoles(resource);
    }

    /**
     *  For a given FedoraResource, get a mapping of acl:agent values to acl:mode values and
     *  for foaf:Agent and acl:AuthenticatedAgent include the acl:agentClass value to acl:mode.
     */
    private Map<String, Collection<String>> getAgentRoles(final FedoraResource resource) {
        LOGGER.debug("Getting agent roles for: {}", resource.getPath());

        // Get the effective ACL by searching the target node and any ancestors.
        final Optional<ACLHandle> effectiveAcl = getEffectiveAcl(resource, false, sessionFactory);

        // Construct a list of acceptable acl:accessTo values for the target resource.
        final List<String> resourcePaths = new ArrayList<>();
        resourcePaths.add(FEDORA_INTERNAL_PREFIX + resource.getDescribedResource().getPath());

        // Construct a list of acceptable acl:accessToClass values for the target resource.
        final List<URI> rdfTypes = resource.getDescription().getTypes();

        // Add the resource location and types of the ACL-bearing parent,
        // if present and if different than the target resource.
        effectiveAcl
            .map(aclHandle -> aclHandle.resource)
            .filter(effectiveResource -> !effectiveResource.getPath().equals(resource.getPath()))
            .ifPresent(effectiveResource -> {
                resourcePaths.add(FEDORA_INTERNAL_PREFIX + effectiveResource.getPath());
                rdfTypes.addAll(effectiveResource.getTypes());
            });

        // If we fall through to the system/classpath-based Authorization and it
        // contains any acl:accessTo properties, it is necessary to add each ancestor
        // path up the node hierarchy, starting at the resource location up to the
        // root location. This way, the checkAccessTo predicate (below) can be properly
        // created to match any acl:accessTo values that are part of the getDefaultAuthorization.
        // This is not relevant if an effectiveAcl is present.
        if (!effectiveAcl.isPresent()) {
            resourcePaths.addAll(getAllPathAncestors(resource.getPath()));
        }

        // Create a function to check acl:accessTo, scoped to the given resourcePaths
        final Predicate<WebACAuthorization> checkAccessTo = accessTo.apply(resourcePaths);

        // Create a function to check acl:accessToClass, scoped to the given rdf:type values,
        // but transform the URIs to Strings first.
        final Predicate<WebACAuthorization> checkAccessToClass =
            accessToClass.apply(rdfTypes.stream().map(URI::toString).collect(toList()));

        // Read the effective Acl and return a list of acl:Authorization statements
        final List<WebACAuthorization> authorizations = effectiveAcl
                .map(auth -> auth.authorizations)
                .orElseGet(() -> getDefaultAuthorizations());

        // Filter the acl:Authorization statements so that they correspond only to statements that apply to
        // the target (or acl-bearing ancestor) resource path or rdf:type.
        // Then, assign all acceptable acl:mode values to the relevant acl:agent values: this creates a UNION
        // of acl:modes for each particular acl:agent.
        final Map<String, Collection<String>> effectiveRoles = new HashMap<>();
        authorizations.stream()
                      .filter(checkAccessTo.or(checkAccessToClass))
                      .forEach(auth -> {
                          concat(auth.getAgents().stream(), dereferenceAgentGroups(auth.getAgentGroups()).stream())
                              .filter(agent -> !agent.equals(FOAF_AGENT_VALUE) &&
                                               !agent.equals(WEBAC_AUTHENTICATED_AGENT_VALUE))
                              .forEach(agent -> {
                                  effectiveRoles.computeIfAbsent(agent, key -> new HashSet<>())
                                                .addAll(auth.getModes().stream().map(URI::toString).collect(toSet()));
                              });
                          auth.getAgentClasses().stream().filter(agentClass -> agentClass.equals(FOAF_AGENT_VALUE) ||
                                                                               agentClass.equals(
                                                                                   WEBAC_AUTHENTICATED_AGENT_VALUE))
                              .forEach(agentClass -> {
                                  effectiveRoles.computeIfAbsent(agentClass, key -> new HashSet<>())
                                                .addAll(auth.getModes().stream().map(URI::toString).collect(toSet()));
                              });
                      });

        LOGGER.debug("Unfiltered ACL: {}", effectiveRoles);

        return effectiveRoles;
    }

    /**
     * Given a path (e.g. /a/b/c/d) retrieve a list of all ancestor paths.
     * In this case, that would be a list of "/a/b/c", "/a/b", "/a" and "/".
     */
    private static List<String> getAllPathAncestors(final String path) {
        final List<String> segments = asList(path.split("/"));
        return range(1, segments.size())
                .mapToObj(frameSize -> FEDORA_INTERNAL_PREFIX + "/" + String.join("/", segments.subList(1, frameSize)))
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
    private List<String> dereferenceAgentGroups(final Collection<String> agentGroups) {
        final FedoraSession internalSession = sessionFactory.getInternalSession();
        //TODO figure out where the translator should be coming from.
        final IdentifierConverter<Resource, FedoraResource> translator = null;

        final List<String> members = agentGroups.stream().flatMap(agentGroup -> {
            if (agentGroup.startsWith(FEDORA_INTERNAL_PREFIX)) {
                //strip off trailing hash.
                final int hashIndex = agentGroup.indexOf("#");
                final String agentGroupNoHash = hashIndex > 0 ?
                                         agentGroup.substring(0, hashIndex) :
                                         agentGroup;
                final String hashedSuffix = hashIndex > 0 ? agentGroup.substring(hashIndex) : null;
                final FedoraResource resource = nodeService.find(
                    internalSession, agentGroupNoHash.substring(FEDORA_INTERNAL_PREFIX.length()));
                return getAgentMembers(translator, resource, hashedSuffix);
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
    private static Stream<String> getAgentMembers(final IdentifierConverter<Resource, FedoraResource> translator,
                                                  final FedoraResource resource, final String hashPortion) {

        //resolve list of triples, accounting for hash-uris.
        final List<Triple> triples = resource.getTriples(translator, PROPERTIES).filter(
            triple -> hashPortion == null || triple.getSubject().getURI().endsWith(hashPortion)).collect(toList());
        //determine if there is a rdf:type vcard:Group
        final boolean hasVcardGroup = triples.stream().anyMatch(
            triple -> triple.matches(triple.getSubject(), RDF_TYPE_NODE, VCARD_GROUP_NODE));
        //return members only if there is an associated vcard:Group
        if (hasVcardGroup) {
            return triples.stream()
                          .filter(triple -> triple.predicateMatches(VCARD_MEMBER_NODE))
                          .map(Triple::getObject).flatMap(WebACRolesProvider::nodeToStringStream)
                                                 .map(WebACRolesProvider::stripUserAgentBaseURI);
        } else {
            return empty();
        }
    }

    private static String stripUserAgentBaseURI(final String object) {
        final String userBaseUri = System.getProperty(USER_AGENT_BASE_URI_PROPERTY);
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
     * @param sessionFactory the session factory
     * @return a list of acl:Authorization objects
     */
    private static List<WebACAuthorization> getAuthorizations(final FedoraResource aclResource,
                                                              final boolean ancestorAcl,
                                                              final SessionFactory sessionFactory) {

        final FedoraSession internalSession = sessionFactory.getInternalSession();
        final List<WebACAuthorization> authorizations = new ArrayList<>();
        //TODO figure out where the translator should be coming from
        final IdentifierConverter<Resource, FedoraResource> translator = null;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ACL: {}", aclResource.getPath());
        }

        if (aclResource.isAcl()) {
            //resolve set of subjects that are of type acl:authorization
            final List<Triple> triples = aclResource.getTriples(translator, PROPERTIES).collect(toList());

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
                LOGGER.debug("Adding acl:Authorization from {}", aclResource.getPath());
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
        return new WebACAuthorization(
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
     * @param sessionFactory session factory
     */
    static Optional<ACLHandle> getEffectiveAcl(final FedoraResource resource, final boolean ancestorAcl,
                                                final SessionFactory sessionFactory) {
        try {

            final FedoraResource aclResource = resource.getAcl();

            if (aclResource != null) {
                final List<WebACAuthorization> authorizations =
                    getAuthorizations(aclResource, ancestorAcl, sessionFactory);
                if (authorizations.size() > 0) {
                    return Optional.of(
                        new ACLHandle(resource, authorizations));
                }
            }

            if (resource.getAcl() == null) {
                LOGGER.debug("No ACLs defined on this node or in parent hierarchy");
                return Optional.empty();
            } else {
                LOGGER.trace("Checking parent resource for ACL. No ACL found at {}", resource.getPath());
                return getEffectiveAcl(resource.getContainer(), true, sessionFactory);
            }
        } catch (final RepositoryException ex) {
            LOGGER.debug("Exception finding effective ACL: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static List<WebACAuthorization> getDefaultAuthorizations() {
        final Map<String, List<String>> aclTriples = new HashMap<>();
        final List<WebACAuthorization> authorizations = new ArrayList<>();

        getDefaultAcl(null).listStatements().mapWith(Statement::asTriple).forEachRemaining(triple -> {
            if (hasAclPredicate.test(triple)) {
                final String predicate = triple.getPredicate().getURI();
                final List<String> values = aclTriples.computeIfAbsent(predicate,
                    key -> new ArrayList<>());
                nodeToStringStream(triple.getObject()).forEach(values::add);
                if (predicate.equals(WEBAC_AGENT_VALUE)) {
                    additionalAgentValues(triple.getObject()).forEach(values::add);
                }
            }
        });

        authorizations.add(createAuthorizationFromMap(aclTriples));
        return authorizations;
    }

    private static Stream<String> additionalAgentValues(final org.apache.jena.graph.Node object) {
        final String groupBaseUri = System.getProperty(GROUP_AGENT_BASE_URI_PROPERTY);
        final String userBaseUri = System.getProperty(USER_AGENT_BASE_URI_PROPERTY);

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
}
