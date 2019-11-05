
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
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createStatement;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.fcrepo.kernel.api.RdfLexicon.WEBAC_ACCESS_TO;
import static org.fcrepo.kernel.api.RdfLexicon.WEBAC_ACCESS_TO_CLASS;
import static org.fcrepo.kernel.api.RdfLexicon.WEBAC_ACCESS_TO_PROPERTY;
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonParseException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.ACLAuthorizationConstraintViolationException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.services.ReplacePropertiesService;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.slf4j.Logger;

/**
 * This class mediates update operations between the kernel and persistent storage layers
 * @author bseeger
 */

public class ReplacePropertiesServiceImpl implements ReplacePropertiesService {

    private static final Logger LOGGER = getLogger(ReplacePropertiesServiceImpl.class);

    private static final Node WEBAC_ACCESS_TO_URI = createURI(WEBAC_ACCESS_TO);

    private static final Node WEBAC_ACCESS_TO_CLASS_URI = createURI(WEBAC_ACCESS_TO_CLASS);

    @Inject
    private RdfSourceOperationFactory factory;

    @Inject
    private PersistentStorageSessionManager psManager;

    @Override
    public void replaceProperties(final Transaction tx,
                           final FedoraResource fedoraResource,
                           final InputStream requestBodyStream,
                           final String contentType) throws MalformedRdfException {
        try {
            final PersistentStorageSession pSession = this.psManager.getSession(tx.getId());

            final Model inputModel = parseBodyAsModel(requestBodyStream, contentType, fedoraResource);

            ensureValidMemberRelation(inputModel);

            ensureValidACLAuthorization(fedoraResource, inputModel);

            final ResourceOperation updateOp = factory.updateBuilder(fedoraResource.getId())
                .triples(fromModel(createURI(fedoraResource.getId()), inputModel))
                .build();

            pSession.persist(updateOp);
        } catch (final PersistentStorageException ex) {
            throw new RepositoryRuntimeException(String.format("failed to replace resource %s",
                  fedoraResource.getId()), ex);
        }


    }
    /**
     * Parse the request body as a Model.
     *
     * @param requestBodyStream rdf request body
     * @param contentType content type of body
     * @param resource the fedora resource
     * @return Model containing triples from request body
     * @throws MalformedRdfException in case rdf json cannot be parsed
     */
    private Model parseBodyAsModel(final InputStream requestBodyStream,
            final String contentType, final FedoraResource resource) throws MalformedRdfException {
        final Lang format = contentTypeToLang(contentType);

        final Model inputModel;
        try {
            inputModel = createDefaultModel();
            inputModel.read(requestBodyStream, "www.example.com", format.getName().toUpperCase());
            return inputModel;
        } catch (final RiotException e) {
            throw new MalformedRdfException("RDF was not parsable: " + e.getMessage(), e);

        } catch (final RuntimeIOException e) {
            if (e.getCause() instanceof JsonParseException) {
                throw new MalformedRdfException(e.getCause());
            }
            throw new RepositoryRuntimeException(e);
        }
    }

    /**
     * This method throws an exception if the arg model contains a triple with 'ldp:hasMemberRelation' as a predicate
     *   and a server-managed property as the object.
     *
     * @param inputModel to be checked
     * @throws ServerManagedPropertyException on error
     */
    private void ensureValidMemberRelation(final Model inputModel) {
        // check that ldp:hasMemberRelation value is not server managed predicate.
        inputModel.listStatements().forEachRemaining((final Statement s) -> {
            LOGGER.debug("statement: s={}, p={}, o={}", s.getSubject(), s.getPredicate(), s.getObject());

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
     * @param resource the fedora resource
     * @param inputModel to be checked and updated
     */
    private void ensureValidACLAuthorization(final FedoraResource resource, final Model inputModel) {
        if (resource.isAcl()) {
            final Set<Node> uniqueAuthSubjects = new HashSet<>();
            inputModel.listStatements().forEachRemaining((final Statement s) -> {
                LOGGER.debug("statement: s={}, p={}, o={}", s.getSubject(), s.getPredicate(), s.getObject());
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
                                        "a single Authorization is not allowed: {0}.",
                                subject.toString().substring(subject.toString().lastIndexOf("#"))));
                } else if (!(graph.contains(subject, WEBAC_ACCESS_TO_URI, Node.ANY) ||
                        graph.contains(subject, WEBAC_ACCESS_TO_CLASS_URI, Node.ANY))) {
                    inputModel.add(createDefaultAccessToStatement(subject.toString()));
                }
            });
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