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
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.Lang.TTL;
import static org.fcrepo.auth.webac.URIConstants.FOAF_AGENT_VALUE;
import static org.fcrepo.auth.webac.URIConstants.FOAF_GROUP;
import static org.fcrepo.auth.webac.URIConstants.FOAF_MEMBER_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_ACCESS_CONTROL_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_ACCESSTO_CLASS_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_ACCESSTO_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_AGENT_CLASS_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_AGENT_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_AUTHORIZATION;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_MODE_VALUE;
import static org.fcrepo.auth.webac.URIConstants.WEBAC_NAMESPACE_VALUE;
import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.FedoraSessionImpl.getJcrSession;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isNonRdfSourceDescription;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
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
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.fcrepo.auth.roles.common.AccessRolesProvider;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.modeshape.FedoraSessionImpl;
import org.fcrepo.kernel.modeshape.rdf.impl.DefaultIdentifierTranslator;

import org.modeshape.jcr.value.Path;
import org.slf4j.Logger;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.JenaException;

/**
 * @author acoburn
 * @since 9/3/15
 */
public class WebACRolesProvider implements AccessRolesProvider {

    public static final String ROOT_AUTHORIZATION_PROPERTY = "fcrepo.auth.webac.authorization";

    public static final String GROUP_AGENT_BASE_URI_PROPERTY = "fcrepo.auth.webac.groupAgent.baseUri";

    public static final String USER_AGENT_BASE_URI_PROPERTY = "fcrepo.auth.webac.userAgent.baseUri";

    private static final Logger LOGGER = getLogger(WebACRolesProvider.class);

    private static final String FEDORA_INTERNAL_PREFIX = "info:fedora";

    private static final String ROOT_AUTHORIZATION_LOCATION = "/root-authorization.ttl";

    private static final String JCR_VERSIONABLE_UUID_PROPERTY = "jcr:versionableUuid";

    @Inject
    private NodeService nodeService;

    @Inject
    private SessionFactory sessionFactory;

    @Override
    public void postRoles(final Node node, final Map<String, Set<String>> data) throws RepositoryException {
        throw new UnsupportedOperationException("postRoles() is not implemented");
    }

    @Override
    public void deleteRoles(final Node node) throws RepositoryException {
        throw new UnsupportedOperationException("deleteRoles() is not implemented");
    }

    @Override
    public Map<String, Collection<String>> findRolesForPath(final Path absPath, final Session session)
            throws RepositoryException {
        return getAgentRoles(locateResource(absPath, new FedoraSessionImpl(session)));
    }

    private FedoraResource locateResource(final Path path, final FedoraSession session) {
        try {
            if (getJcrSession(session).nodeExists(path.toString()) || path.isRoot()) {
                LOGGER.debug("findRolesForPath: {}", path.getString());
                final FedoraResource resource = nodeService.find(session, path.toString());

                if (resource.hasType("nt:version")) {
                    LOGGER.debug("{} is a version, getting the baseVersion", resource);
                    return getBaseVersion(resource);
                }
                return resource;
            }
        } catch (final RepositoryException ex) {
            throw new RepositoryRuntimeException(ex);
        }
        LOGGER.trace("Path: {} does not exist, checking parent", path.getString());
        return locateResource(path.getParent(), session);
    }

    /**
     * Get the versionable FedoraResource for this version resource
     *
     * @param resource the Version resource
     * @return the base versionable resource or the version if not found.
     */
    private FedoraResource getBaseVersion(final FedoraResource resource) {
        final FedoraSession internalSession = sessionFactory.getInternalSession();

        try {
            final VersionHistory base = ((Version) getJcrNode(resource)).getContainingHistory();
            if (base.hasProperty(JCR_VERSIONABLE_UUID_PROPERTY)) {
                final String versionUuid = base.getProperty(JCR_VERSIONABLE_UUID_PROPERTY).getValue().getString();
                LOGGER.debug("versionableUuid : {}", versionUuid);
                return nodeService.find(internalSession,
                        getJcrSession(internalSession).getNodeByIdentifier(versionUuid).getPath());
            }
        } catch (final ItemNotFoundException e) {
            LOGGER.error("Node with jcr:versionableUuid not found : {}", e.getMessage());
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        return resource;
    }

    @Override
    public Map<String, Collection<String>> getRoles(final Node node, final boolean effective) {
        try {
            return getAgentRoles(nodeService.find(new FedoraSessionImpl(node.getSession()), node.getPath()));
        } catch (final RepositoryException ex) {
            throw new RepositoryRuntimeException(ex);
        }
    }

    /**
     *  For a given FedoraResource, get a mapping of acl:agent values to acl:mode values.
     */
    private Map<String, Collection<String>> getAgentRoles(final FedoraResource resource) {
        LOGGER.debug("Getting agent roles for: {}", resource.getPath());

        // Get the effective ACL by searching the target node and any ancestors.
        final Optional<ACLHandle> effectiveAcl = getEffectiveAcl(
                isNonRdfSourceDescription.test(getJcrNode(resource)) ?
                    ((NonRdfSourceDescription)nodeConverter.convert(getJcrNode(resource))).getDescribedResource() :
                    resource);

        // Construct a list of acceptable acl:accessTo values for the target resource.
        final List<String> resourcePaths = new ArrayList<>();
        resourcePaths.add(FEDORA_INTERNAL_PREFIX + resource.getPath());

        // Construct a list of acceptable acl:accessToClass values for the target resource.
        final List<URI> rdfTypes = resource.getTypes();

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
                .map(auth -> getAuthorizations(auth.uri.toString()))
                .orElseGet(() -> getDefaultAuthorizations());

        // Filter the acl:Authorization statements so that they correspond only to statements that apply to
        // the target (or acl-bearing ancestor) resource path or rdf:type.
        // Then, assign all acceptable acl:mode values to the relevant acl:agent values: this creates a UNION
        // of acl:modes for each particular acl:agent.
        final Map<String, Collection<String>> effectiveRoles = new HashMap<>();
        authorizations.stream()
            .filter(checkAccessTo.or(checkAccessToClass))
            .forEach(auth -> {
                concat(auth.getAgents().stream(), dereferenceAgentClasses(auth.getAgentClasses()).stream())
                    .forEach(agent -> {
                        effectiveRoles.computeIfAbsent(agent, key -> new HashSet<>())
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
     *  This maps a Collection of acl:agentClass values to a List of agents.
     *  Any out-of-domain URIs are silently ignored.
     */
    private List<String> dereferenceAgentClasses(final Collection<String> agentClasses) {
        final FedoraSession internalSession = sessionFactory.getInternalSession();
        final IdentifierConverter<Resource, FedoraResource> translator =
                new DefaultIdentifierTranslator(getJcrSession(internalSession));

        final List<String> members = agentClasses.stream().flatMap(agentClass -> {
            if (agentClass.startsWith(FEDORA_INTERNAL_PREFIX)) {
                final FedoraResource resource = nodeService.find(
                    internalSession, agentClass.substring(FEDORA_INTERNAL_PREFIX.length()));
                return getAgentMembers(translator, resource);
            } else if (agentClass.equals(FOAF_AGENT_VALUE)) {
                return of(agentClass);
            } else {
                LOGGER.info("Ignoring agentClass: {}", agentClass);
                return empty();
            }
        }).collect(toList());

        if (LOGGER.isDebugEnabled() && !agentClasses.isEmpty()) {
            LOGGER.debug("Found {} members in {} agentClass resources", members.size(), agentClasses.size());
        }

        return members;
    }

    /**
     *  Given a FedoraResource, return a list of agents.
     */
    private static Stream<String> getAgentMembers(final IdentifierConverter<Resource, FedoraResource> translator,
            final FedoraResource resource) {
        return resource.getTriples(translator, PROPERTIES).filter(memberTestFromTypes.apply(resource.getTypes()))
            .map(Triple::getObject).flatMap(WebACRolesProvider::nodeToStringStream);
    }

    /**
     * Map a Jena Node to a Stream of Strings. Any non-URI, non-Literals map to an empty Stream,
     * making this suitable to use with flatMap.
     */
    private static final Stream<String> nodeToStringStream(final org.apache.jena.graph.Node object) {
        if (object.isURI()) {
            return of(object.getURI());
        } else if (object.isLiteral()) {
            return of(object.getLiteralValue().toString());
        } else {
            return empty();
        }
    }

    /**
     *  A simple predicate for filtering out any non-foaf:member properties
     */
    private static final Function<List<URI>, Predicate<Triple>> memberTestFromTypes = types -> triple ->
        types.contains(FOAF_GROUP) && triple.predicateMatches(createURI(FOAF_MEMBER_VALUE));

    /**
     *  A simple predicate for filtering out any non-acl triples.
     */
    private static final Predicate<Triple> hasAclPredicate = triple ->
        triple.getPredicate().getNameSpace().equals(WEBAC_NAMESPACE_VALUE);

    /**
     *  This function reads a Fedora ACL resource and all of its acl:Authorization children.
     *  The RDF from each child resource is put into a WebACAuthorization object, and the
     *  full list is returned.
     *
     *  @param location the location of the ACL resource
     *  @return a list of acl:Authorization objects
     */
    private List<WebACAuthorization> getAuthorizations(final String location) {

        final FedoraSession internalSession = sessionFactory.getInternalSession();
        final List<WebACAuthorization> authorizations = new ArrayList<>();
        final IdentifierConverter<Resource, FedoraResource> translator =
                new DefaultIdentifierTranslator(getJcrSession(internalSession));

        LOGGER.debug("Effective ACL: {}", location);

        // Find the specified ACL resource

        if (location.startsWith(FEDORA_INTERNAL_PREFIX)) {

            final FedoraResource resource = nodeService.find(internalSession,
                    location.substring(FEDORA_INTERNAL_PREFIX.length()));

            // Read each child resource, filtering on acl:Authorization type, keeping only acl-prefixed triples.
            resource.getChildren().forEach(child -> {
                if (child.getTypes().contains(WEBAC_AUTHORIZATION)) {
                    final Map<String, List<String>> aclTriples = new HashMap<>();
                    child.getTriples(translator, PROPERTIES).filter(hasAclPredicate)
                        .forEach(triple -> {
                            final String predicate = triple.getPredicate().getURI();
                            final List<String> values = aclTriples.computeIfAbsent(predicate,
                                key -> new ArrayList<>());
                            nodeToStringStream(triple.getObject()).forEach(values::add);
                            if (predicate.equals(WEBAC_AGENT_VALUE)) {
                                additionalAgentValues(triple.getObject()).forEach(values::add);
                            }
                        });
                    // Create a WebACAuthorization object from the provided triples.
                    LOGGER.debug("Adding acl:Authorization from {}", child.getPath());
                    authorizations.add(createAuthorizationFromMap(aclTriples));
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
                    data.getOrDefault(WEBAC_ACCESSTO_CLASS_VALUE, emptyList()));
    }

    /**
     * Recursively find the effective ACL as a URI along with the FedoraResource that points to it.
     * This way, if the effective ACL is pointed to from a parent resource, the child will inherit
     * any permissions that correspond to access to that parent. This ACL resource may or may not exist,
     * and it may be external to the fedora repository.
     */
    static Optional<ACLHandle> getEffectiveAcl(final FedoraResource resource) {
        try {
            final IdentifierConverter<Resource, FedoraResource> translator =
                new DefaultIdentifierTranslator(getJcrNode(resource).getSession());
            final List<String> acls = resource.getTriples(translator, PROPERTIES)
                    .filter(triple -> triple.getPredicate().equals(createURI(WEBAC_ACCESS_CONTROL_VALUE)))
                    .map(triple -> {
                        if (triple.getObject().isURI()) {
                            return triple.getObject().getURI();
                        }
                        final String error = String.format("The value %s of the %s on this resource must be a URI",
                                triple.getObject(), WEBAC_ACCESS_CONTROL_VALUE);
                        LOGGER.error(error);
                        throw new MalformedRdfException(error);
                    }).collect(toList());

            if (!acls.isEmpty()) {
                if (acls.size() > 1) {
                    LOGGER.warn("Found multiple ACLs defined for this node. Using: {}", acls.get(0));
                }
                return Optional.of(new ACLHandle(URI.create(acls.get(0)), resource));
            } else if (getJcrNode(resource).getDepth() == 0) {
                LOGGER.debug("No ACLs defined on this node or in parent hierarchy");
                return Optional.empty();
            } else {
                LOGGER.trace("Checking parent resource for ACL. No ACL found at {}", resource.getPath());
                return getEffectiveAcl(resource.getContainer());
            }
        } catch (final RepositoryException ex) {
            LOGGER.debug("Exception finding effective ACL: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static List<WebACAuthorization> getDefaultAuthorizations() {
        final Map<String, List<String>> aclTriples = new HashMap<>();
        final List<WebACAuthorization> authorizations = new ArrayList<>();

        getDefaultAcl().listStatements().mapWith(Statement::asTriple).forEachRemaining(triple -> {
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

    private static Model getDefaultAcl() {
        final String rootAcl = System.getProperty(ROOT_AUTHORIZATION_PROPERTY);
        final Model model = createDefaultModel();

        if (rootAcl != null && new File(rootAcl).isFile()) {
            try {
                LOGGER.debug("Getting root authorization from file: {}", rootAcl);
                return model.read(rootAcl);
            } catch (final JenaException ex) {
                LOGGER.error("Error parsing root authorization file: {}", ex.getMessage());
            }
        }
        try (final InputStream is = WebACRolesProvider.class.getResourceAsStream(ROOT_AUTHORIZATION_LOCATION)) {
            LOGGER.debug("Getting root authorization from classpath: {}", ROOT_AUTHORIZATION_LOCATION);
            return model.read(is, null, TTL.getName());
        } catch (final IOException ex) {
            LOGGER.error("Error reading root authorization file: {}", ex.getMessage());
        } catch (final JenaException ex) {
            LOGGER.error("Error parsing root authorization file: {}", ex.getMessage());
        }
        return createDefaultModel();
    }
}
