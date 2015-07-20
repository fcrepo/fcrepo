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

import static java.lang.Boolean.parseBoolean;
import static javax.ws.rs.core.Response.ok;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.serialization.FedoraObjectSerializer;
import org.fcrepo.serialization.InvalidSerializationFormatException;
import org.fcrepo.serialization.SerializerUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

/**
 * Serialization for nodes
 *
 * @author awoods
 */
@Scope("request")
@Path("/{path: .*}/fcr:export")
public class FedoraExport extends FedoraBaseResource {

    @Autowired
    protected SerializerUtil serializers;

    @Inject
    protected Session session;

    private static final Logger LOGGER = getLogger(FedoraExport.class);

    /**
     * Export an object with the given format, e.g.: GET
     * /path/to/object/fcr:export?format=jcr/xml : the node as JCR/XML
     *
     * @param externalPath the external path
     * @param format the format string
     * @param skipBinary the value whether skip binary
     * @param recurse the value whether recurse
     * @return object in the given format
     */
    @GET
    public Response exportObject(
        @PathParam("path") final String externalPath,
        @QueryParam("format") @DefaultValue("jcr/xml") final String format,
        @QueryParam("skipBinary") @DefaultValue("true") final String skipBinary,
        @QueryParam("recurse") @DefaultValue("false") final String recurse) {

        final FedoraResource resource = getResourceFromPath(externalPath);

        LOGGER.debug("Requested object serialization for {} using serialization format {}", resource, format);

        final FedoraObjectSerializer serializer =
            serializers.getSerializer(format);

        return ok().type(serializer.getMediaType()).entity(
                new StreamingOutput() {

                    @Override
                    public void write(final OutputStream out)
                        throws IOException {

                        try {
                            LOGGER.debug("Selecting from serializer map: {}", serializers);
                            LOGGER.debug("Retrieved serializer for format: {}", format);
                            serializer.serialize(resource,
                                                 out,
                                                 parseBoolean(skipBinary),
                                                 parseBoolean(recurse));
                            LOGGER.info("Serialized to {}, '{}'", format, externalPath);
                        } catch (final RepositoryException e) {
                            throw new WebApplicationException(e);
                        } catch (InvalidSerializationFormatException e) {
                            throw new BadRequestException(e.getMessage());
                        }
                    }
                }).build();

    }

    @Override
    protected Session session() {
        return session;
    }
}
