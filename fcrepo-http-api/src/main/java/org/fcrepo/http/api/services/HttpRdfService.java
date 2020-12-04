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

package org.fcrepo.http.api.services;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.vocabulary.RDF.Init.type;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.fcrepo.kernel.api.RdfLexicon.restrictedType;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.JsonParseException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.MultipleConstraintViolationException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.ServerManagedPropertyException;
import org.fcrepo.kernel.api.exception.ServerManagedTypeException;
import org.fcrepo.kernel.api.exception.UnsupportedMediaTypeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * A service that will translate the resourceURI to Fedora ID in the Rdf InputStream
 *
 * @author bseeger
 * @author bbpennel
 * @since 2019-11-07
 */
@Component
public class HttpRdfService {

    private static final Logger log = getLogger(HttpRdfService.class);

    /**
     * Convert internal IDs to external URIs
     * @param extResourceId The external URI of the resource.
     * @param stream The RDF stream to be translated.
     * @param idTranslator The identifier converter.
     * @return a converted RDF Stream.
     */
    public RdfStream bodyToExternalStream(final String extResourceId, final RdfStream stream,
                                     final HttpIdentifierConverter idTranslator) {
        return new DefaultRdfStream(NodeFactory.createURI(extResourceId), stream.map(t -> {
            final Node subject = makeExternalNode(t.getSubject(), idTranslator);
            final Node object = makeExternalNode(t.getObject(), idTranslator);
            return new Triple(subject, t.getPredicate(), object);
        }));
    }

    /**
     * Return a converted or original resource.
     * @param resource The Node to be checked.
     * @param identifierConverter A identifier converter.
     * @return The resulting node.
     */
    private Node makeExternalNode(final Node resource, final HttpIdentifierConverter identifierConverter) {
        if (resource.isURI() && identifierConverter.inInternalDomain(resource.toString())) {
            return NodeFactory.createURI(identifierConverter.toExternalId(resource.toString()));
        } else {
            return resource;
        }
    }

    /**
     * Parse the request body to a Model, with the URI to Fedora ID translations done.
     *
     * @param extResourceId the external ID of the Fedora resource
     * @param stream the input stream containing the RDF
     * @param contentType the media type of the RDF
     * @param idTranslator the identifier convert
     * @param lenientHandling whether the request included a handling=lenient prefer header.
     * @return RdfStream containing triples from request body, with Fedora IDs in them
     * @throws MalformedRdfException in case rdf json cannot be parsed
     * @throws BadRequestException in the case where the RDF syntax is bad
     */
    public Model bodyToInternalModel(final FedoraId extResourceId, final InputStream stream,
                                     final MediaType contentType, final HttpIdentifierConverter idTranslator,
                                     final boolean lenientHandling)
                                     throws RepositoryRuntimeException, BadRequestException {
        final List<ConstraintViolationException> exceptions = new ArrayList<>();
        final Model model = parseBodyAsModel(stream, contentType, extResourceId.getEncodedFullId());
        final List<Statement> insertStatements = new ArrayList<>();
        final StmtIterator stmtIterator = model.listStatements();

        while (stmtIterator.hasNext()) {
            final Statement stmt = stmtIterator.nextStatement();
            if (lenientHandling && stmtIsServerManaged(stmt)) {
                // Remove any statement that touches a server managed property or namespace.
                stmtIterator.remove();
            } else {
                try {
                    checkForDisallowedRdf(stmt);
                    if (stmt.getSubject().isURIResource()) {
                        final String originalSubj = stmt.getSubject().getURI();
                        final String subj = idTranslator.inExternalDomain(originalSubj) ?
                                idTranslator.toInternalId(originalSubj) : originalSubj;

                        RDFNode obj = stmt.getObject();
                        if (stmt.getObject().isURIResource()) {
                            final String objString = stmt.getObject().asResource().getURI();
                            if (idTranslator.inExternalDomain(objString)) {
                                obj = model.getResource(idTranslator.toInternalId(objString));
                            }
                        }

                        if (!subj.equals(originalSubj) || !obj.equals(stmt.getObject())) {
                            insertStatements.add(new StatementImpl(model.getResource(subj), stmt.getPredicate(), obj));

                            stmtIterator.remove();
                        }
                    } else {
                        log.debug("Subject {} is not a URI resource, skipping", stmt.getSubject());
                    }
                } catch (final ServerManagedPropertyException | ServerManagedTypeException exc) {
                    exceptions.add(exc);
                }
            }
        }

        if (!exceptions.isEmpty()) {
            throw new MultipleConstraintViolationException(exceptions);
        }

        model.add(insertStatements);

        log.debug("Model: {}", model);
        return model;
    }

    /**
     * Takes a PATCH request body and translates any subjects and objects that are in the domain of the repository
     * to use internal IDs.
     * @param resourceId the internal ID of the current resource.
     * @param requestBody the request body.
     * @param idTranslator an ID converter for the current context.
     * @return the converted PATCH request.
     */
    public String patchRequestToInternalString(final FedoraId resourceId, final String requestBody,
                                               final HttpIdentifierConverter idTranslator) {
        final UpdateRequest request = UpdateFactory.create(requestBody, resourceId.getEncodedFullId());
        final List<Update> updates = request.getOperations();
        final SparqlTranslateVisitor visitor = new SparqlTranslateVisitor(idTranslator);
        for (final Update update : updates) {
            update.visit(visitor);
        }
        return visitor.getTranslatedRequest().toString();
    }

    /**
     * Parse the request body as a Model.
     *
     * @param requestBodyStream rdf request body
     * @param contentType content type of body
     * @param extResourceId the external ID of the Fedora resource
     * @return Model containing triples from request body
     * @throws MalformedRdfException in case rdf json cannot be parsed
     * @throws BadRequestException in the case where the RDF syntax is bad
     */
    protected static Model parseBodyAsModel(final InputStream requestBodyStream,
                                         final MediaType contentType,
                                         final String extResourceId) throws BadRequestException,
                                         RepositoryRuntimeException {

        if (requestBodyStream == null) {
            return null;
        }

        // The 'contentTypeToLang()' method will not accept 'charset' parameters
        String contentTypeWithoutCharset = contentType.toString();
        if (contentType.getParameters().containsKey("charset")) {
            contentTypeWithoutCharset = contentType.getType() + "/" + contentType.getSubtype();
        }

        final Lang format = contentTypeToLang(contentTypeWithoutCharset);
        if (format == null) {
            // No valid RDF format for the mimeType.
            throw new UnsupportedMediaTypeException("Media type " + contentTypeWithoutCharset + " is not a valid RDF " +
                    "format");
        }
        try {
            final Model inputModel = createDefaultModel();
            inputModel.read(requestBodyStream, extResourceId, format.getName().toUpperCase());
            return inputModel;
        } catch (final RiotException e) {
            throw new BadRequestException("RDF was not parsable: " + e.getMessage(), e);

        } catch (final RuntimeIOException e) {
            if (e.getCause() instanceof JsonParseException) {
                final var cause = e.getCause();
                throw new MalformedRdfException(cause.getMessage(), cause);
            }
            throw new RepositoryRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Checks if the RDF contains any disallowed statements.
     * @param statement a statement from the incoming RDF.
     */
    private static void checkForDisallowedRdf(final Statement statement) {
        checkTripleForDisallowed(statement.asTriple());
    }

    /**
     * Several tests for invalid or disallowed RDF statements.
     * @param triple the triple to check.
     */
    public static void checkTripleForDisallowed(final Triple triple) {
        if (triple.getPredicate().equals(type().asNode()) && !triple.getObject().isURI()) {
            // The object of a rdf:type triple is not a URI.
            throw new MalformedRdfException(
                    String.format("Invalid rdf:type: %s", triple.getObject()));
        } else if (restrictedType.test(triple)) {
            // The object of a rdf:type triple has a restricted namespace.
            throw new ServerManagedTypeException(
                    String.format("The server managed type (%s) cannot be modified by the client.",
                            triple.getObject()));
        } else if (isManagedPredicate.test(createProperty(triple.getPredicate().getURI()))) {
            // The predicate is server managed.
            throw new ServerManagedPropertyException(
                    String.format("The server managed predicate (%s) cannot be modified by the client.",
                            triple.getPredicate()));
        }
    }

    /**
     * Does the statement's triple touch any server managed properties / namespaces.
     * ie.
     * - has a rdf:type with an object which is in a managed namespace
     * - has a predicate which is in a managed namespace.
     *
     * @param statement the statement to check
     * @return Return true if this does touch a server managed property or namespace.
     */
    private boolean stmtIsServerManaged(final Statement statement) {
        final Triple triple = statement.asTriple();
        return restrictedType.test(triple) || isManagedPredicate.test(createProperty(triple.getPredicate().getURI()));
    }
}
