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
package org.fcrepo.kernel.impl.services;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.ACLAuthorizationConstraintViolationException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RequestWithAclLinkHeaderException;
import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.RdfLexicon.DEFAULT_INTERACTION_MODEL;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.INTERACTION_MODELS_FULL;
import static org.fcrepo.kernel.api.RdfLexicon.IS_MEMBER_OF_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.MEMBERSHIP_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.WEBAC_ACCESS_TO;
import static org.fcrepo.kernel.api.RdfLexicon.WEBAC_ACCESS_TO_CLASS;
import static org.fcrepo.kernel.api.RdfLexicon.WEBAC_ACCESS_TO_PROPERTY;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Abstract service for interacting with a kernel service
 *
 * @author whikloj
 * @author bseeger
 */

public abstract class AbstractService {

    private static final Logger log = getLogger(ReplacePropertiesServiceImpl.class);

    private static final Node WEBAC_ACCESS_TO_URI = createURI(WEBAC_ACCESS_TO);

    private static final Node WEBAC_ACCESS_TO_CLASS_URI = createURI(WEBAC_ACCESS_TO_CLASS);

    protected final List<Triple> serverManagedProperties = new ArrayList<>();

    @Autowired
    @Qualifier("containmentIndex")
    protected ContainmentIndex containmentIndex;

    @Inject
    private EventAccumulator eventAccumulator;

    @Autowired
    @Qualifier("referenceService")
    protected ReferenceService referenceService;

    @Inject
    protected MembershipService membershipService;

    /**
     * Utility to determine the correct interaction model from elements of a request.
     *
     * @param linkTypes         Link headers with rel="type"
     * @param isRdfContentType  Is the Content-type a known RDF type?
     * @param contentPresent    Is there content present on the request body?
     * @param isExternalContent Is there Link headers that define external content?
     * @return The determined or default interaction model.
     */
    protected String determineInteractionModel(final List<String> linkTypes,
                                               final boolean isRdfContentType, final boolean contentPresent,
                                               final boolean isExternalContent) {
        final String interactionModel = linkTypes == null ? null :
                linkTypes.stream().filter(INTERACTION_MODELS_FULL::contains).findFirst().orElse(null);

        // If you define a valid interaction model, we try to use it.
        if (interactionModel != null) {
            return interactionModel;
        }
        if (isExternalContent || (contentPresent && !isRdfContentType)) {
            return NON_RDF_SOURCE.toString();
        } else {
            return DEFAULT_INTERACTION_MODEL.toString();
        }
    }

    /**
     * Check that we don't try to provide an ACL Link header.
     *
     * @param links list of the link headers provided.
     * @throws RequestWithAclLinkHeaderException If we provide an rel="acl" link header.
     */
    protected void checkAclLinkHeader(final List<String> links) throws RequestWithAclLinkHeaderException {
        final var matcher = Pattern.compile("rel=[\"']?acl[\"']?").asPredicate();
        if (links != null && links.stream().anyMatch(matcher)) {
            throw new RequestWithAclLinkHeaderException(
                    "Unable to handle request with the specified LDP-RS as the ACL.");
        }
    }

    /**
     * Verifies that DirectContainer properties are valid, throwing exceptions if the triples
     * do not meet LDP requirements or a server managed property is specified as a membership relation.
     * If no membershipResource or membership relation are specified, defaults will be populated.
     * @param fedoraId id of the resource described
     * @param interactionModel interaction model of the resource
     * @param model model to check
     */
    protected void ensureValidDirectContainer(final FedoraId fedoraId, final String interactionModel,
            final Model model) {
        if (!(RdfLexicon.DIRECT_CONTAINER.getURI().equals(interactionModel)
                || RdfLexicon.INDIRECT_CONTAINER.getURI().equals(interactionModel))) {
            return;
        }
        final var dcResc = model.getResource(fedoraId.getFullId());
        final AtomicBoolean hasMembershipResc = new AtomicBoolean(false);
        final AtomicBoolean hasRelation = new AtomicBoolean(false);

        dcResc.listProperties().forEachRemaining(stmt -> {
            final var predicate = stmt.getPredicate();

            if (MEMBERSHIP_RESOURCE.equals(predicate)) {
                if (hasMembershipResc.get()) {
                    throw new MalformedRdfException("Direct and Indirect containers must specify"
                            + " exactly one ldp:membershipResource property, multiple are present");
                }

                if (stmt.getObject().isURIResource()) {
                    hasMembershipResc.set(true);
                } else {
                    throw new MalformedRdfException("Direct and Indirect containers must specify"
                            + " a ldp:membershipResource property with a resource as the object");
                }
            } else if (HAS_MEMBER_RELATION.equals(predicate) || IS_MEMBER_OF_RELATION.equals(predicate)) {
                if (hasRelation.get()) {
                    throw new MalformedRdfException("Direct and Indirect containers must specify exactly one"
                            + " ldp:hasMemberRelation or ldp:isMemberOfRelation property, but multiple were present");
                }

                final RDFNode obj = stmt.getObject();
                if (obj.isURIResource()) {
                    final String uri = obj.asResource().getURI();

                    // Throw exception if object is a server-managed property
                    if (isManagedPredicate.test(createProperty(uri))) {
                        throw new ServerManagedPropertyException(String.format(
                                "%s cannot take a server managed property as an object: property value = %s.",
                                predicate.getLocalName(), uri));
                    }
                    hasRelation.set(true);
                } else {
                    throw new MalformedRdfException("Direct and Indirect containers must specify either"
                            + " ldp:hasMemberRelation or ldp:isMemberOfRelation properties,"
                            + " with a predicate as the object");
                }
            }
        });

        if (!hasMembershipResc.get()) {
            dcResc.addProperty(MEMBERSHIP_RESOURCE, dcResc);
        }
        if (!hasRelation.get()) {
            dcResc.addProperty(HAS_MEMBER_RELATION, RdfLexicon.LDP_MEMBER);
        }
    }

    /**
     * This method does two things:
     * - Throws an exception if an authorization has both accessTo and accessToClass
     * - Adds a default accessTo target if an authorization has neither accessTo nor accessToClass
     *
     * @param inputModel to be checked and updated
     */
    protected void ensureValidACLAuthorization(final Model inputModel) {

        // TODO -- check ACL first

        final Set<Node> uniqueAuthSubjects = new HashSet<>();
        inputModel.listStatements().forEachRemaining((final Statement s) -> {
            log.debug("statement: s={}, p={}, o={}", s.getSubject(), s.getPredicate(), s.getObject());
            final Node subject = s.getSubject().asNode();
            // If subject is Authorization Hash Resource, add it to the map with its accessTo/accessToClass status.
            if (subject.toString().contains("/" + FCR_ACL + "#")) {
                uniqueAuthSubjects.add(subject);
            }
        });
        final Graph graph = inputModel.getGraph();
        uniqueAuthSubjects.forEach((final Node subject) -> {
            if (graph.contains(subject, WEBAC_ACCESS_TO_URI, Node.ANY) &&
                    graph.contains(subject, WEBAC_ACCESS_TO_CLASS_URI, Node.ANY)) {
                throw new ACLAuthorizationConstraintViolationException(
                        String.format(
                                "Using both accessTo and accessToClass within " +
                                        "a single Authorization is not allowed: %s.",
                                subject.toString().substring(subject.toString().lastIndexOf("#"))));
            } else if (!(graph.contains(subject, WEBAC_ACCESS_TO_URI, Node.ANY) ||
                    graph.contains(subject, WEBAC_ACCESS_TO_CLASS_URI, Node.ANY))) {
                inputModel.add(createDefaultAccessToStatement(subject.toString()));
            }
        });
    }

    protected void recordEvent(final String transactionId, final FedoraId fedoraId, final ResourceOperation operation) {
        this.eventAccumulator.recordEventForOperation(transactionId, fedoraId, operation);
    }

    /**
     * Wrapper to call the referenceService updateReference method
     * @param transactionId the transaction ID.
     * @param resourceId the resource's ID.
     * @param model the model of the request body.
     */
    protected void updateReferences(final String transactionId, final FedoraId resourceId, final String user,
                                    final Model model) {
        referenceService.updateReferences(transactionId, resourceId, user,
                fromModel(model.getResource(resourceId.getFullId()).asNode(), model));
    }

    protected void lockArchivalGroupResource(final Transaction tx,
                                             final PersistentStorageSession pSession,
                                             final FedoraId fedoraId) {
        final var headers = pSession.getHeaders(fedoraId, null);
        if (headers.getArchivalGroupId() != null) {
            tx.lockResource(headers.getArchivalGroupId());
        }
    }

    protected void lockArchivalGroupResourceFromParent(final Transaction tx,
                                                       final PersistentStorageSession pSession,
                                                       final FedoraId parentId) {
        if (parentId != null && !parentId.isRepositoryRoot()) {
            final var parentHeaders = pSession.getHeaders(parentId, null);
            if (parentHeaders.isArchivalGroup()) {
                tx.lockResource(parentId);
            } else if (parentHeaders.getArchivalGroupId() != null) {
                tx.lockResource(parentHeaders.getArchivalGroupId());
            }
        }
    }

    /**
     * Returns a Statement with the resource containing the acl to be the accessTo target for the given auth subject.
     *
     * @param authSubject - acl authorization subject uri string
     * @return acl statement
     */
    private Statement createDefaultAccessToStatement(final String authSubject) {
        final String currentResourcePath = authSubject.substring(0, authSubject.indexOf("/" + FCR_ACL));
        return createStatement(
                createResource(authSubject),
                WEBAC_ACCESS_TO_PROPERTY,
                createResource(currentResourcePath));
    }
}

