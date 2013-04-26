package org.fcrepo.api;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;


@Component
@Path("/rest/{path: .*}/fcr:new")
public class FedoraUnnamedObjects extends AbstractResource {

    private static final Logger logger = getLogger(FedoraUnnamedObjects.class);

    @Autowired
    FedoraDatastreams datastreamsResource;

    @Autowired
    FedoraObjects objectsResource;

    /**
     * Create an anonymous object with a newly minted name
     * @param pathList
     * @return 201
     */
    @POST
    public Response ingestAndMint(@PathParam("path")
    final List<PathSegment> pathList) throws RepositoryException {
        logger.debug("Creating a new unnamed object");
        final String pid = pidMinter.mintPid();
        PathSegment path = new PathSegment() {

            @Override
            public String getPath() {
                return pid;
            }

            @Override
            public MultivaluedMap<String, String> getMatrixParameters() {
                return null;
            }

        };
        ImmutableList.Builder<PathSegment> segments = ImmutableList.builder();
        segments.addAll(pathList.subList(0, pathList.size() - 1));
        segments.add(path);
        try {
            return objectsResource.createObject(
                    segments.build(), "test label",
                    FedoraJcrTypes.FEDORA_OBJECT, null, null, null, null);
        } catch (IOException e) {
            throw new RepositoryException(e.getMessage(), e);
        } catch (InvalidChecksumException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    /**
     * Create an anonymous DS with a newly minted name
     * and content from request body
     * @param pathList
     * @throws RepositoryException 
     */
    @POST
    @Path("fcr:content")
    public Response createDS(@PathParam("path")
    final List<PathSegment> pathList, @QueryParam("checksumType")
    final String checksumType, @QueryParam("checksum")
    final String checksum, @HeaderParam("Content-Type")
    final MediaType requestContentType, final InputStream requestBodyStream)
            throws IOException, InvalidChecksumException, RepositoryException {
        logger.debug("Creating a new unnamed object");
        final String dsid = pidMinter.mintPid();
        ImmutableList.Builder<PathSegment> segments = ImmutableList.builder();
        segments.addAll(pathList.subList(0, pathList.size() - 2));
        return datastreamsResource.addDatastream(
                segments.build(), checksumType,
                checksum, dsid,
                requestContentType, requestBodyStream);
    }
}
