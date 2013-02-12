package org.fcrepo.merritt.api;

import org.fcrepo.AbstractResource;
import org.fcrepo.merritt.jaxb.responses.*;

import javax.jcr.*;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.ok;

@Path("/state")
public class StateService extends AbstractResource {

    @GET
    public Response getServiceState() throws RepositoryException {
        final Session session = repo.login();

        ServiceState serviceState = new ServiceState();
        serviceState.name = "fcrepo4";
        serviceState.identifier = session.getRootNode().getIdentifier();
        serviceState.serviceVersion = "0.17";
        serviceState.description = repo.getDescriptor(Repository.REP_NAME_DESC);
        serviceState.totalSize = getRepositorySize(session);
        serviceState.numObjects = session.getNode("/objects").getNodes()
                .getSize();

        return ok(serviceState).build();
    }

    @GET
    @Path("{node}")
    public Response getNodeState() throws RepositoryException {
        final Session session = repo.login();

        NodeState nodeState = new NodeState();
        nodeState.name = "fcrepo4";
        nodeState.identifier = session.getRootNode().getIdentifier();
        nodeState.nodeVersion = "0.17";
        nodeState.description = repo.getDescriptor(Repository.REP_NAME_DESC);
        nodeState.totalSize = getRepositorySize(session);
        nodeState.numObjects = session.getNode("/objects").getNodes()
                .getSize();

        return ok(nodeState).build();
    }

    @GET
    @Path("{node}/{object}")
    public Response getObjectState(@PathParam("object") final String object_id) throws RepositoryException {
        final Session session = repo.login();
        final String dsPath = "/objects/" + object_id;

        if (session.nodeExists(dsPath)) {
            final Node object = session.getNode(dsPath);

            ObjectState objectState = new ObjectState();
            objectState.identifier = object.getIdentifier();

            session.logout();
            return ok(objectState).build();
        } else {
            session.logout();
            return four04;
        }
    }

    @GET
    @Path("{node}/{object}/{version : \\d+ }")
    public Response getVersionState() {

        VersionState versionState = new VersionState();

        return ok(versionState).build();
    }

    @GET
    @Path("{node}/{object}/{file : [a-zA-Z][a-zA-Z_0-9-]+}")
    public Response getCurrentVersionFileState(@PathParam("object") final String object_id, @PathParam("file") final String file_id) throws RepositoryException {
        final Session session = repo.login();
        final String dsPath = "/objects/" + object_id + "/" + file_id;

        if (session.nodeExists(dsPath)) {
            final Node datastream = session.getNode(dsPath);

            FileState fileState = new FileState();
            fileState.identifier = datastream.getIdentifier();

            session.logout();
            return ok(fileState).build();
        } else {
            session.logout();
            return four04;
        }
    }

    @GET
    @Path("{node}/{object}/{version : \\d+ }/{file}")
    public Response getVersionFileState(@PathParam("object") final String object_id, @PathParam("file") final String file_id) throws RepositoryException {
        final Session session = repo.login();

        final String dsPath = "/objects/" + object_id + "/" + file_id;

        if (session.nodeExists(dsPath)) {
            final Node datastream = session.getNode(dsPath);

            FileState fileState = new FileState();
            fileState.identifier = datastream.getIdentifier();

            session.logout();
            return ok(fileState).build();
        } else {
            session.logout();
            return four04;
        }
    }
}
