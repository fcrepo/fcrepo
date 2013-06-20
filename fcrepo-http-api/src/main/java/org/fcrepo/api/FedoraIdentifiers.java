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

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ContiguousSet.create;
import static com.google.common.collect.DiscreteDomain.integers;
import static com.google.common.collect.Range.closed;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.update.GraphStoreFactory.create;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.fcrepo.RdfLexicon.HAS_MEMBER_OF_RESULT;
import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.RDFMediaType.RDF_XML;
import static org.fcrepo.http.RDFMediaType.TURTLE;

import java.util.Collection;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.AbstractResource;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.session.InjectedSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * JAX-RS Resource offering PID creation.
 *
 * @author ajs6f
 * @author cbeer
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:pid")
public class FedoraIdentifiers extends AbstractResource {

    @InjectedSession
    protected Session session;

    /**
     * @param numPids number of PIDs to return
     * @return HTTP 200 with block of PIDs
     */
    @POST
    @Timed
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES,
            TEXT_HTML})
    public Dataset getNextPid(@PathParam("path")
    final List<PathSegment> pathList, @QueryParam("numPids")
    @DefaultValue("1")
    final Integer numPids, @Context
    final UriInfo uriInfo) throws RepositoryException {

        final String path = toPath(pathList);

        final Model model = createDefaultModel();

        final Resource pidsResult =
                createResource(uriInfo.getAbsolutePath().toASCIIString());

        final Collection<String> identifiers =
                transform(create(closed(1, numPids), integers()), pidMinter
                        .makePid());

        final HttpGraphSubjects subjects =
                new HttpGraphSubjects(FedoraNodes.class, uriInfo, session);

        for (final String identifier : identifiers) {

            final String absPath;
            if (path.equals("/")) {
                absPath = "/" + identifier;
            } else {
                absPath = path + "/" + identifier;
            }

            final Resource s = subjects.getGraphSubject(absPath);

            model.add(pidsResult, HAS_MEMBER_OF_RESULT, s);
        }

        return create(model).toDataset();

    }
}
