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

import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.RdfLexicon.DEFAULT_INTERACTION_MODEL;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.INTERACTION_MODELS_FULL;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.WEBAC_ACCESS_TO;
import static org.fcrepo.kernel.api.RdfLexicon.WEBAC_ACCESS_TO_CLASS;
import static org.fcrepo.kernel.api.RdfLexicon.WEBAC_ACCESS_TO_PROPERTY;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.exception.ACLAuthorizationConstraintViolationException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RequestWithAclLinkHeaderException;
import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;
import org.fcrepo.kernel.api.exception.ServerManagedTypeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.observer.EventAccumulator;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.ReferenceService;
import org.slf4j.Logger;


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

    @Inject
    protected ContainmentIndex containmentIndex;

    @Inject
    private EventAccumulator eventAccumulator;

    @Inject
    protected ReferenceService referenceService;

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
     * Check if a path has a segment prefixed with fedora:
     *
     * @param externalPath the path.
     */
    protected void hasRestrictedPath(final String externalPath) {
        final String[] pathSegments = externalPath.split("/");
        if (Arrays.stream(pathSegments).anyMatch(p -> p.startsWith("fedora:"))) {
            throw new ServerManagedTypeException("Path cannot contain a fedora: prefixed segment.");
        }
    }

    /**
     * Looks through an RdfStream for rdf:types in the LDP namespace or server managed predicates.
     *
     * @param model The RDF model.
     */
    protected void checkForSmtsLdpTypes(final Model model) {
        final StmtIterator it = model.listStatements();
        while (it.hasNext()) {
            final Statement st = it.next();
            if ((st.getPredicate().equals(RDF.type) && st.getObject().isURIResource() &&
                    st.getObject().toString().startsWith(LDP_NAMESPACE)) ||
                    isManagedPredicate.test(st.getPredicate())) {
                throw new MalformedRdfException("RDF contains a server managed triple or restricted rdf:type");
            }
        }
    }

    /*
     * This method throws an exception if the arg model contains a triple with 'ldp:hasMemberRelation' as a predicate
     *   and a server-managed property as the object.
     *
     * @param inputModel to be checked
     * @throws ServerManagedPropertyException on error
     */
    protected void ensureValidMemberRelation(final Model inputModel) {
        // check that ldp:hasMemberRelation value is not server managed predicate.
        inputModel.listStatements().forEachRemaining((final Statement s) -> {
            log.debug("statement: s={}, p={}, o={}", s.getSubject(), s.getPredicate(), s.getObject());

            if (s.getPredicate().equals(HAS_MEMBER_RELATION)) {
                final RDFNode obj = s.getObject();
                if (obj.isURIResource()) {
                    final String uri = obj.asResource().getURI();

                    // Throw exception if object is a server-managed property
                    if (isManagedPredicate.test(createProperty(uri))) {
                        throw new ServerManagedPropertyException(
                                String.format(
                                        "{0} cannot take a server managed property " +
                                                "as an object: property value = {1}.",
                                        HAS_MEMBER_RELATION, uri));
                    }
                }
            }
        });
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

