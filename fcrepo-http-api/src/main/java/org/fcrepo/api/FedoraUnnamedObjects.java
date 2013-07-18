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

import static javax.ws.rs.core.Response.created;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraResource;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.session.InjectedSession;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Utility for creating new objects at automatically
 * generated path
 *
 * @author ajs6f
 * @author cbeer
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:new")
public class FedoraUnnamedObjects extends AbstractResource {

    @InjectedSession
    protected Session session;

    private static final Logger logger = getLogger(FedoraUnnamedObjects.class);

    /**
     * Create an anonymous object with a newly minted name
     * 
     * @param pathList
     * @return 201
     */
    @POST
    public Response ingestAndMint(
            @PathParam("path")
            final List<PathSegment> pathList,
            @QueryParam("mixin")
            @DefaultValue(FedoraJcrTypes.FEDORA_OBJECT)
            final String mixin,
            @QueryParam("checksum")
            final String checksum,
            @HeaderParam("Content-Type")
            final MediaType requestContentType,
            final InputStream requestBodyStream,
            @Context
            final UriInfo uriInfo) throws RepositoryException, IOException,
        InvalidChecksumException, URISyntaxException {
        final String pid = pidMinter.mintPid();

        final String path = toPath(pathList) + "/" + pid;

        logger.debug("Attempting to ingest with path: {}", path);

        try {
            if (nodeService.exists(session, path)) {
                return Response.status(SC_CONFLICT).entity(
                        path + " is an existing resource").build();
            }

            final URI checksumURI;

            if (checksum != null && !checksum.equals("")) {
                checksumURI = new URI(checksum);
            } else {
                checksumURI = null;
            }


            final FedoraResource resource =
                    createObjectOrDatastreamFromRequestContent(
                            FedoraNodes.class, session, path, mixin, uriInfo,
                            requestBodyStream, requestContentType,
                            checksumURI);

            session.save();
            logger.debug("Finished creating {} with path: {}", mixin, path);

            final HttpGraphSubjects subjects =
                    new HttpGraphSubjects(FedoraNodes.class, uriInfo);

            return created(
                    new URI(subjects.getGraphSubject(resource.getNode())
                            .getURI())).entity(path).build();

        } finally {
            session.logout();
        }
    }
}
