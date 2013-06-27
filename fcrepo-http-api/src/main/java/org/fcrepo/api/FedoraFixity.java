/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.RDFMediaType.RDF_XML;
import static org.fcrepo.http.RDFMediaType.TURTLE;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.session.InjectedSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.query.Dataset;

/**
 * @author ajs6f
 * @date Jun 12, 2013
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:fixity")
public class FedoraFixity extends AbstractResource {

    @InjectedSession
    protected Session session;

    @GET
    @Timed
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES,
            TEXT_HTML})
    public Dataset getDatastreamFixity(@PathParam("path")
    final List<PathSegment> pathList, @Context
    final Request request, @Context
    final UriInfo uriInfo) throws RepositoryException {

        try {
            final String path = toPath(pathList);

            final Datastream ds =
                    datastreamService.getDatastream(session, path);

            return datastreamService.getFixityResultsModel(
                    new HttpGraphSubjects(FedoraNodes.class, uriInfo, session),
                    ds);
        } finally {
            session.logout();
        }
    }
}
