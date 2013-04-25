
package org.fcrepo.generator;

import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.ok;

import java.io.InputStream;
import java.util.List;

import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.generator.dublincore.DCGenerator;
import org.fcrepo.services.ObjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/oai/{path: .*(?!(oai_dc))}/oai_dc")
public class DublinCoreGenerator extends AbstractResource {

    @Resource
    List<DCGenerator> dcgenerators;

    @Autowired
    ObjectService objectService;

    @GET
    @Produces(TEXT_XML)
    public Response getObjectAsDublinCore(@PathParam("path")
    final List<PathSegment> pathList) throws RepositoryException {
        
        final String path = toPath(pathList);
        final Node obj = objectService.getObjectNode(path);

        for (final DCGenerator indexer : dcgenerators) {
            final InputStream inputStream = indexer.getStream(obj);

            if (inputStream != null) {
                return ok(inputStream).build();
            }
        }
        // no indexers = no path for DC
        throw new PathNotFoundException();

    }

}
