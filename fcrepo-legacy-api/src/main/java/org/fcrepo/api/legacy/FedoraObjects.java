
package org.fcrepo.api.legacy;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.copyOf;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static org.fcrepo.api.legacy.FedoraDatastreams.getContentSize;
import static org.fcrepo.jaxb.responses.ObjectProfile.ObjectStates.A;
import static org.fcrepo.utils.FedoraJcrTypes.DC_TITLE;

import java.io.IOException;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
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
import org.fcrepo.services.ObjectService;
import org.modeshape.common.SystemFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

@Path("/objects")
public class FedoraObjects extends AbstractResource {

    private static final Logger logger = LoggerFactory
            .getLogger(FedoraObjects.class);

    private Session readOnlySession;

    @PostConstruct
    public void loginReadOnlySession() throws LoginException,
            RepositoryException {
        readOnlySession = repo.login();
    }

    @PreDestroy
    public void logoutReadOnlySession() {
        readOnlySession.logout();
    }

    @GET
    public Response getObjects() throws RepositoryException {

        Node objects = readOnlySession.getNode("/objects");
        StringBuffer nodes = new StringBuffer();

        for (NodeIterator i = objects.getNodes(); i.hasNext();) {
            Node n = i.nextNode();
            nodes.append("Name: " + n.getName() + ", Path:" + n.getPath() +
                    "\n");
        }
        return ok(nodes.toString()).build();

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
    final String pid, final ObjectProfile objProfile)
            throws RepositoryException {

        final String objPath = "/objects/" + pid;
        final Session session = repo.login();
        try {
            if (!session.nodeExists(objPath)) {
                session.logout();
                return status(CONFLICT).entity("No such object").build();
            }
            final Node obj = session.getNode(objPath);
            obj.setProperty(DC_TITLE, objProfile.objLabel);
            if (objProfile.objModels != null)
                for (String model : objProfile.objModels) {
                    obj.addMixin(model);
                }
            session.save();
        } finally {
            session.logout();
        }
        return created(uriInfo.getAbsolutePath()).build();
    }

    @POST
    @Path("/{pid}")
    public Response ingest(@PathParam("pid")
    final String pid) throws RepositoryException {

        logger.debug("Attempting to ingest with pid: " + pid);

        final Session session = repo.login();
        try {
            final Node obj =
                    new ObjectService().createObjectNode(session, "/objects/" +
                            pid);

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
    public Response getObject(@PathParam("pid")
    final String pid) throws RepositoryException, IOException {

        final Node obj = readOnlySession.getNode("/objects/" + pid);
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
        return ok(objectProfile).build();

    }

    @DELETE
    @Path("/{pid}")
    public Response deleteObject(@PathParam("pid")
    final String pid) throws RepositoryException {
        final Session session = repo.login();
        final Node obj = session.getNode("/objects/" + pid);
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

    private Function<Value, String> value2string =
            new Function<Value, String>() {

                @Override
                public String apply(Value v) {
                    try {
                        return v.getString();
                    } catch (RepositoryException e) {
                        throw new SystemFailureException(e);
                    } catch (IllegalStateException e) {
                        throw new SystemFailureException(e);
                    }
                }
            };

    private static <From, To> Collection<To> map(From[] input,
            Function<From, To> f) {
        return transform(copyOf(input), f);
    }

    private Function<NodeType, String> nodetype2string =
            new Function<NodeType, String>() {

                @Override
                public String apply(NodeType type) {
                    return type.getName();
                }
            };
}
