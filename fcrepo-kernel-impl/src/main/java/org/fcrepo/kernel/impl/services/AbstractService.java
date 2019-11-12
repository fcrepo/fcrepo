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

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_LASTMODIFIED;
import static org.fcrepo.kernel.api.RdfLexicon.DEFAULT_INTERACTION_MODEL;
import static org.fcrepo.kernel.api.RdfLexicon.INTERACTION_MODELS_FULL;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.RDF_NAMESPACE;

import com.fasterxml.jackson.core.JsonParseException;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.MalformedRdfException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.RequestWithAclLinkHeaderException;
import org.fcrepo.kernel.api.exception.ServerManagedTypeException;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;

import java.io.InputStream;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.Link;

public class AbstractService {

    static String rdfType = RDF_NAMESPACE + "type";

    List<Triple> serverManagedProperties = new ArrayList<>();

    /**
     * Utility to determine the correct interaction model from elements of a request.
     *
     * @param linkTypes Link headers with rel="type"
     * @param isRdfContentType Is the Content-type a known RDF type?
     * @param contentPresent Is there content present on the request body?
     * @param isExternalContent Is there Link headers that define external content?
     * @return The determined or default interaction model.
     */
    String determineInteractionModel(final List<String> linkTypes,
                                                   final boolean isRdfContentType, final boolean contentPresent,
                                                   final boolean isExternalContent) {
        final String interactionModel = linkTypes == null ? null : linkTypes.stream().filter(INTERACTION_MODELS_FULL::contains).findFirst()
                .orElse(null);

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
     * @param links list of the link headers provided.
     * @throws RequestWithAclLinkHeaderException If we provide an rel="acl" link header.
     */
    void checkAclLinkHeader(final List<String> links) throws RequestWithAclLinkHeaderException {
        final Predicate matcher = Pattern.compile("rel=[\"']?acl[\"']?").asPredicate();
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
    void hasRestrictedPath(final String externalPath) {
        final String[] pathSegments = externalPath.split("/");
        if (Arrays.stream(pathSegments).anyMatch(p -> p.startsWith("fedora:"))) {
            throw new ServerManagedTypeException("Path cannot contain a fedora: prefixed segment.");
        }
    }

    /**
     * Parse the request body as a Model.
     * TODO: Replace this with HttpRdfService.parseBodyAsModel once https://github.com/fcrepo4/fcrepo4/pull/1575 lands.
     *
     * @param requestBodyStream rdf request body
     * @param contentType content type of body
     * @param fedoraId the fedora resource identifier
     *
     * @return Model containing triples from request body
     *
     * @throws MalformedRdfException in case rdf cannot be parsed
     */
    Model parseBodyAsModel(final InputStream requestBodyStream,
                                   final String contentType, final String fedoraId) throws MalformedRdfException {
        if (requestBodyStream == null) {
            return null;
        }

        final Lang format = contentTypeToLang(contentType);
        final Model inputModel;
        try {
            inputModel = createDefaultModel();
            inputModel.read(requestBodyStream, fedoraId, format.getName().toUpperCase());
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
     * Looks through an RdfStream for rdf:types in the LDP namespace or server managed predicates.
     *
     * @param model The RDF model.
     */
    void checkForSmtsLdpTypes(final Model model) {
        final StmtIterator it = model.listStatements();
        while (it.hasNext()) {
            final Statement st = it.next();
            if (st.getPredicate().hasURI(rdfType) && st.getObject().isURIResource() &&
                    st.getObject().toString().startsWith(LDP_NAMESPACE)) {
                throw new MalformedRdfException("RDF contains a server managed triple or restricted rdf:type");
            }
        }
    }

    /**
     * Execute the populateServerManagedTriples and return the stream.
     * @param fedoraId The current resource ID.
     * @return The RDF stream of server managed properties.
     */
    RdfStream getServerManagedStream(final String fedoraId) {
        populateServerManagedTriples(fedoraId);
        return new DefaultRdfStream(asNode(fedoraId), serverManagedProperties.stream());
    }

    /**
     * Populate server managed properties.
     * Override in subclasses to add additional triples, always call super() first;
     */
    void populateServerManagedTriples(final String fedoraId) {
        final ZonedDateTime now = ZonedDateTime.now();
        serverManagedProperties.add(new Triple(
                asNode(fedoraId),
                asNode(FEDORA_LASTMODIFIED),
                asLiteral(now.format(DateTimeFormatter.RFC_1123_DATE_TIME), XSDDatatype.XSDdateTime))
        );
        // TODO: get current user.
        // this.serverManagedProperties.add(new Triple(
        //      asNode(fedoraId),
        //      asNode(FEDORA_LASTMODIFIEDBY),
        //      asLiteral(user))
        // );
    }

    /**
     * Utility to turn a resource string to a Node.
     * @param uri the resource.
     * @return the resource as a Node.
     */
    Node asNode(final String uri) {
        return ResourceFactory.createResource(uri).asNode();
    }

    /**
     * Utility to turn a typed literal into a Node.
     * @param literal The literal value.
     * @param type The datatype.
     * @return The literal as a node.
     */
    Node asLiteral(final String literal, final RDFDatatype type) {
        return ResourceFactory.createTypedLiteral(literal, type).asNode();
    }

    /**
     * Get the rel="type" link headers from a list of them.
     * @param headers a list of string LINK headers.
     * @return a list of LINK headers with rel="type"
     */
    protected List<String> getTypes(final List<String> headers) {
        final List<String> types = getLinkHeaders(headers) == null ? null : getLinkHeaders(headers).stream()
                .filter(p -> p.getRel().equalsIgnoreCase("type")).map(Link::getUri)
                .map(URI::toString).collect(Collectors.toList());
        return types;
    }

    /**
     * Converts a list of string LINK headers to actual LINK objects.
     * @param headers the list of string link headers.
     * @return the list of LINK headers.
     */
    protected List<Link> getLinkHeaders(final List<String> headers) {
        return headers == null ? null : headers.stream().map(p -> Link.fromUri(p).build()).collect(Collectors.toList());
    }
}
