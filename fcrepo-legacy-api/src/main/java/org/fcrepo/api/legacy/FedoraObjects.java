
package org.fcrepo.api.legacy;

import static com.google.common.base.Joiner.on;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.ok;
import static org.fcrepo.api.legacy.FedoraDatastreams.getContentSize;
import static org.fcrepo.jaxb.responses.ObjectProfile.ObjectStates.A;
import static org.fcrepo.services.ObjectService.createObjectNode;
import static org.fcrepo.services.ObjectService.getObjectNode;
import static org.fcrepo.services.PathService.getObjectJcrNodePath;
import static org.fcrepo.utils.FedoraJcrTypes.DC_TITLE;
import static org.fcrepo.utils.FedoraTypesUtils.map;
import static org.fcrepo.utils.FedoraTypesUtils.nodetype2string;
import static org.fcrepo.utils.FedoraTypesUtils.value2string;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.jaxb.responses.ObjectProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/objects")
public class FedoraObjects extends AbstractResource {

    private static final Logger logger = LoggerFactory
            .getLogger(FedoraObjects.class);

    @GET
    public Response getObjects() throws RepositoryException {
        final Session session = repo.login();
        try {
            Node objects = session.getNode(getObjectJcrNodePath(""));
            StringBuffer nodes = new StringBuffer();

            for (NodeIterator i = objects.getNodes(); i.hasNext();) {
                Node n = i.nextNode();
                nodes.append("Name: " + n.getName() + ", Path:" + n.getPath() +
                        "\n");
            }
            return ok(nodes.toString()).build();
        } finally {
            session.logout();
        }

    }

    @POST
    @Path("/new")
    public Response ingestAndMint() throws RepositoryException {
        return ingest(pidMinter.mintPid());
    }

    @PUT
    @Path("/{pid}")
    @Consumes({TEXT_XML, APPLICATION_JSON})
    public Response modify(@PathParam("pid")
    final String pid) throws RepositoryException {
        /*final String objPath = "/objects/" + pid;
        final Session session = repo.login();
        try {
            final Node obj = session.getNode(objPath);
            // TODO do something with awful mess of fcrepo3 query params
            session.save();
        } finally {
            session.logout();
        }*/
        return created(uriInfo.getAbsolutePath()).build();
    }

    @POST
    @Path("/{pid}")
    public Response ingest(@PathParam("pid")
    final String pid) throws RepositoryException {

        logger.debug("Attempting to ingest with pid: " + pid);

        final Session session = repo.login();
        try {
            final Node obj = createObjectNode(session, getObjectJcrNodePath(pid));
            session.save();
            /*
             * we save before updating the repo size because the act of
             * persisting session state creates new system-curated nodes and
             * properties which contribute to the footprint of this resource
             */
            updateRepositorySize(getObjectSize(obj), session);
            // now we save again to persist the repo size
            session.save();
            logger.debug("Finished ingest with pid: " + pid);
            return created(uriInfo.getAbsolutePath()).entity(pid).build();

        } finally {
            session.logout();
        }
    }

    @GET
    @Path("/{pid}")
    @Produces({TEXT_XML, APPLICATION_JSON})
    public ObjectProfile getObject(@PathParam("pid")
    final String pid) throws RepositoryException, IOException {

        final Node obj = getObjectNode(pid);
        final ObjectProfile objectProfile = new ObjectProfile();

        objectProfile.pid = pid;
        if (obj.hasProperty(DC_TITLE)) {
            Property dcTitle = obj.getProperty(DC_TITLE);
            if (!dcTitle.isMultiple())
                objectProfile.objLabel = obj.getProperty(DC_TITLE).getString();
            else {
                objectProfile.objLabel =
                        on('/').join(map(dcTitle.getValues(), value2string));
            }
        }
        objectProfile.objOwnerId =
                obj.getProperty("fedora:ownerId").getString();
        objectProfile.objCreateDate =
                obj.getProperty("jcr:created").getString();
        objectProfile.objLastModDate =
                obj.getProperty("jcr:lastModified").getString();
        objectProfile.objSize = getObjectSize(obj);
        objectProfile.objItemIndexViewURL =
                uriInfo.getAbsolutePathBuilder().path("datastreams").build();
        objectProfile.objState = A;
        objectProfile.objModels = map(obj.getMixinNodeTypes(), nodetype2string);
        return objectProfile;

    }

    @DELETE
    @Path("/{pid}")
    public Response deleteObject(@PathParam("pid")
    final String pid) throws RepositoryException {
        final Session session = repo.login();
        final Node obj = session.getNode(getObjectJcrNodePath(pid));
        updateRepositorySize(0L - getObjectSize(obj), session);
        return deleteResource(obj);
    }

    /**
     * @param obj
     * @return object size in bytes
     * @throws RepositoryException
     */
    static Long getObjectSize(Node obj) throws RepositoryException {
        return getNodePropertySize(obj) + getObjectDSSize(obj);
    }

    /**
     * @param obj
     * @return object's datastreams' total size in bytes
     * @throws RepositoryException
     */
    private static Long getObjectDSSize(Node obj) throws RepositoryException {
        Long size = 0L;
        NodeIterator i = obj.getNodes();
        while (i.hasNext()) {
            Node ds = i.nextNode();
            size = size + getNodePropertySize(ds);
            size = size + getContentSize(ds);
        }
        return size;
    }

}
