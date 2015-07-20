/**
 * Copyright 2015 DuraSpace, Inc.
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
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.fcrepo.http.commons.domain.ContentLocation;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.serialization.InvalidSerializationFormatException;
import org.fcrepo.serialization.SerializerUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

/**
 * Import serialized objects at a given endpoint
 *
 * @author ajs6f
 * @author cbeer
 */
@Scope("prototype")
@Path("/{path: .*}/fcr:import")
public class FedoraImport extends FedoraBaseResource {

    @Inject
    protected Session session;

    @Autowired
    protected SerializerUtil serializers;

    private static final Logger LOGGER = getLogger(FedoraImport.class);

    /**
     * Deserialize a serialized object at the current path POST
     * /fcr:import?format=jcr/xml (with a JCR/XML payload)
     *
     * @param externalPath the external path
     * @param format the format
     * @param requestBodyStream the request body stream
     * @return 201 with Location header to the path of the imported resource
     * @throws IOException if IO exception occurred
     * @throws InvalidChecksumException if invalid checksum exception occurred
     * @throws URISyntaxException if uri syntax exception
     */
    @POST
    public Response importObject(@PathParam("path") final String externalPath,
        @QueryParam("format") @DefaultValue("jcr/xml") final String format,
        @ContentLocation final InputStream requestBodyStream)
        throws IOException, InvalidChecksumException, URISyntaxException {

        final String path = toPath(translator(), externalPath);
        LOGGER.info("Deserializing to {}, '{}'", format, path);

        try {
            serializers.getSerializer(format)
                    .deserialize(session, path, requestBodyStream);
            session.save();
            return created(new URI(path)).build();
        } catch ( ItemExistsException ex ) {
            return status(CONFLICT).entity("Item already exists").build();
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        } catch (InvalidSerializationFormatException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    protected Session session() {
        return session;
    }
}
