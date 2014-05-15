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

import static javax.ws.rs.core.Response.created;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierTranslator;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.exception.InvalidChecksumException;
import org.fcrepo.serialization.SerializerUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Import serialized objects at a given endpoint
 *
 * @author ajs6f
 * @author cbeer
 */
@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:import")
public class FedoraImport extends AbstractResource {

    @InjectedSession
    protected Session session;

    @Autowired
    protected SerializerUtil serializers;

    private static final Logger LOGGER = getLogger(FedoraImport.class);

    /**
     * Deserialize a serialized object at the current path POST
     * /fcr:import?format=jcr/xml (with a JCR/XML payload)
     *
     * @param pathList
     * @param format
     * @param stream
     * @return 201 with Location header to the path of the imported resource
     * @throws IOException
     * @throws RepositoryException
     * @throws InvalidChecksumException
     * @throws URISyntaxException
     */
    @POST
    public Response importObject(@PathParam("path") final List<PathSegment> pathList,
        @QueryParam("format") @DefaultValue("jcr/xml") final String format,
        final InputStream stream)
        throws IOException, RepositoryException, InvalidChecksumException,
        URISyntaxException {

        final String path = toPath(pathList);
        LOGGER.debug("Deserializing at {}", path);

        final HttpIdentifierTranslator subjects =
            new HttpIdentifierTranslator(session, FedoraNodes.class, uriInfo);

        try {
            serializers.getSerializer(format)
                    .deserialize(session, path, stream);
            session.save();
            return created(new URI(subjects.getSubject(path).getURI())).build();
        } finally {
            session.logout();
        }
    }
}
