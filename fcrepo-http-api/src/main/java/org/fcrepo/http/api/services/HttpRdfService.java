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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.slf4j.Logger;

/**
 * A service that will translate the resourceURI to fedora ID in the Rdf InputStream
 *
 * @author bseeger
 * @author bbpennel
 * @since 2019-11-07
 */

public class HttpRdfService {

    private static final Logger LOGGER = getLogger(HttpRdfService.class);

    /**
     * Parse the request body to a Model, with the URI to Fedora ID translations done.
     *
     * @param resource the fedora resource
     * @param stream the input stream containing the RDF
     * @param contentType the media type of the RDF
     * @param idTranslator the uri to fedora resource ID translator
     * @return RdfStream containing triples from request body, with fedora IDs in them
     * @throws MalformedRdfException in case rdf json cannot be parsed
     * @throws BadRequestException in the case where the RDF syntax is bad
     */
    public Model bodyToInternalModel(final FedoraResource resource, final InputStream stream,
                                     final MediaType contentType, final HttpIdentifierConverter idTranslator)
                                     throws RepositoryRuntimeException, BadRequestException {

        final Model model = parseBodyAsModel(stream, contentType, resource);

        final StmtIterator stmtIterator = model.listStatements();
        while (stmtIterator.hasNext()) {
            final Statement stmt = stmtIterator.next();

            String subj = stmt.getSubject().getURI().toString();
            subj = subj.replace(subj, idTranslator.convert(subj));
            String obj = stmt.getObject().asLiteral().toString();
            if (stmt.getObject().isURIResource()) {
                obj = stmt.getObject().asLiteral().toString().replace(obj, idTranslator.convert(obj));
            }

            model.getResource(subj).addProperty(stmt.getPredicate(), obj);
            stmtIterator.remove();
        }

        LOGGER.debug("Model: {}", model);
        return model;
    }

     /**
     * Parse the request body to a RdfStream, with the URI to Fedora ID translations done.
     *
     * @param resource the fedora resource
     * @param stream the input stream containing the RDF
     * @param contentType the media type of the RDF
     * @param idTranslator the uri to fedora resource ID translator
     * @return RdfStream containing triples from request body, with fedora IDs in them
     * @throws MalformedRdfException in case rdf json cannot be parsed
     * @throws BadRequestException in the case where the RDF syntax is bad
     */
    public RdfStream bodyToInternalStream(final FedoraResource resource, final InputStream stream,
                                          final MediaType contentType, final HttpIdentifierConverter idTranslator)
                                          throws RepositoryRuntimeException, BadRequestException {
        final Model model = bodyToInternalModel(resource, stream, contentType, idTranslator);

        return fromModel(model.getResource(resource.getId()).asNode(), model);
    }

    /**
     * Parse the request body as a Model.
     *
     * @param requestBodyStream rdf request body
     * @param contentType content type of body
     * @param resource the fedora resource
     * @return Model containing triples from request body
     * @throws MalformedRdfException in case rdf json cannot be parsed
     * @throws BadRequestException in the case where the RDF syntax is bad
     */
    public static Model parseBodyAsModel(final InputStream requestBodyStream,
                                         final MediaType contentType,
                                         final FedoraResource resource) throws BadRequestException,
                                         RepositoryRuntimeException {
        final Lang format = contentTypeToLang(contentType.toString());

        final Model inputModel;
        try {
            inputModel = createDefaultModel();
            inputModel.read(requestBodyStream, resource.getId(), format.getName().toUpperCase());
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
