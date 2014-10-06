/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ContiguousSet.create;
import static com.google.common.collect.DiscreteDomain.integers;
import static com.google.common.collect.Range.closed;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.fcrepo.http.commons.domain.RDFMediaType.JSON_LD;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3;
import static org.fcrepo.http.commons.domain.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.commons.domain.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.commons.domain.RDFMediaType.RDF_XML;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE;
import static org.fcrepo.http.commons.domain.RDFMediaType.TURTLE_X;
import static org.fcrepo.kernel.RdfLexicon.HAS_MEMBER_OF_RESULT;

import java.util.Collection;

import javax.inject.Inject;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.springframework.context.annotation.Scope;

import com.google.common.base.Function;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * JAX-RS Resource offering identifier creation.
 *
 * @author ajs6f
 * @author cbeer
 */
@Scope("prototype")
@Path("/{path: .*}/fcr:identifier")
public class FedoraIdentifiers extends FedoraBaseResource {

    @Inject
    protected Session session;

    /**
     * Mint identifiers (without creating the objects)
     *
     * POST /path/to/mint/from/fcr:identifier?count=15
     *
     * @param count number of PIDs to return
     * @return HTTP 200 with block of PIDs
     */
    @POST
    @Produces({TURTLE, N3, N3_ALT2, RDF_XML, NTRIPLES, APPLICATION_XML, TEXT_PLAIN, TURTLE_X,
                      TEXT_HTML, APPLICATION_XHTML_XML, JSON_LD})
    public RdfStream getNextPid(@PathParam("path")
            final String externalPath,
            @QueryParam("count")
            @DefaultValue("1")
            final Integer count,
            @Context
            final UriInfo uriInfo) {


        final String path = toPath(translator(), externalPath);

        final Node pidsResult = createURI(uriInfo.getAbsolutePath().toASCIIString());

        final Collection<String> identifiers = transform(create(closed(1, count), integers()), pidMinter.makePid());

        return new RdfStream(transform(transform(identifiers, absolutize(path)),
                identifier2triple(translator(), pidsResult))).topic(pidsResult).session(session);

    }

    private static Function<String, String> absolutize(final String path) {
        return new Function<String, String>() {

            @Override
            public String apply(final String identifier) {
                return path.equals("/") ? "/" + identifier : path + "/"
                        + identifier;
            }
        };
    }

    private static Function<String, Triple> identifier2triple(
        final IdentifierConverter<Resource, javax.jcr.Node> subjects, final Node pidsResult) {
        return new Function<String, Triple>() {

            @Override
            public Triple apply(final String identifier) {

                final Node s = subjects.toDomain(identifier).asNode();
                return Triple.create(pidsResult, HAS_MEMBER_OF_RESULT
                        .asNode(), s);

            }
        };
    }

    @Override
    protected Session session() {
        return session;
    }

}
