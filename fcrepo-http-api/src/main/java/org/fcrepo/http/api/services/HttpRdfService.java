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
import static org.fcrepo.kernel.api.rdf.DefaultRdfStream.fromModel;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.JsonParseException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.RdfStream;
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

    // TODO: Should eventually `Inject`
    private HttpIdentifierConverter idTranslator;

    /**
     * Parse the request body to a Model, with the URI to Fedora ID translations done.
     *
     * @param extResourceId the external ID of the Fedora resource
     * @param stream the input stream containing the RDF
     * @param contentType the media type of the RDF
     * @return RdfStream containing triples from request body, with Fedora IDs in them
     * @throws MalformedRdfException in case rdf json cannot be parsed
     * @throws BadRequestException in the case where the RDF syntax is bad
     */
    public Model bodyToInternalModel(final String extResourceId, final InputStream stream,
                                     final MediaType contentType)
                                     throws RepositoryRuntimeException, BadRequestException {

        final Model model = parseBodyAsModel(stream, contentType, extResourceId);
        final List<Statement> insertStatements = new ArrayList<>();
        final StmtIterator stmtIterator = model.listStatements();

        while (stmtIterator.hasNext()) {
            final Statement stmt = stmtIterator.nextStatement();
            final String originalSubj = stmt.getSubject().getURI();
            final String subj = idTranslator.inExternalDomain(originalSubj) ?
                idTranslator.toInternalId(originalSubj) : originalSubj;

            RDFNode obj = stmt.getObject();
            if (stmt.getObject().isResource()) {
                final String objString = stmt.getObject().asResource().getURI();
                if (idTranslator.inExternalDomain(objString)) {
                    obj = model.getResource(idTranslator.toInternalId(objString));
                }
            }

            if (!subj.equals(originalSubj) || !obj.equals(stmt.getObject())) {
                insertStatements.add(new StatementImpl(model.getResource(subj),
                    stmt.getPredicate(), obj));

                stmtIterator.remove();
            }
        }

        model.add(insertStatements);

        log.debug("Model: {}", model);
        return model;
    }

     /**
     * Parse the request body to a RdfStream, with the URI to Fedora ID translations done.
     *
     * @param extResourceId the external ID of the Fedora resource
     * @param stream the input stream containing the RDF
     * @param contentType the media type of the RDF
     * @return RdfStream containing triples from request body, with Fedora IDs in them
     * @throws MalformedRdfException in case rdf json cannot be parsed
     * @throws BadRequestException in the case where the RDF syntax is bad
     */
    public RdfStream bodyToInternalStream(final String extResourceId, final InputStream stream,
                                          final MediaType contentType)
                                          throws RepositoryRuntimeException, BadRequestException {
        final Model model = bodyToInternalModel(extResourceId, stream, contentType);

        return fromModel(model.getResource(idTranslator.toInternalId(extResourceId)).asNode(), model);
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

        final Lang format = contentTypeToLang(contentType.toString());
        try {
            final Model inputModel = createDefaultModel();
            inputModel.read(requestBodyStream, extResourceId, format.getName().toUpperCase());
            return inputModel;
        } catch (final RiotException e) {
            throw new BadRequestException("RDF was not parsable: " + e.getMessage(), e);

        } catch (final RuntimeIOException e) {
            if (e.getCause() instanceof JsonParseException) {
                throw new MalformedRdfException(e.getCause());
            }
            throw new RepositoryRuntimeException(e);
        }
    }

}
