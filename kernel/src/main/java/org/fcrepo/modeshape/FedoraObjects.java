package org.fcrepo.modeshape;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.copyOf;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.ok;
import static org.fcrepo.modeshape.FedoraDatastreams.getContentSize;
import static org.fcrepo.modeshape.jaxb.responses.ObjectProfile.ObjectStates.A;

import java.io.IOException;
import java.util.Calendar;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.fcrepo.modeshape.jaxb.responses.ObjectProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

@Path("/objects")
public class FedoraObjects extends AbstractResource {

	private static final Logger logger = LoggerFactory
			.getLogger(FedoraObjects.class);

	@GET
	public Response getObjects() throws RepositoryException {
		final Session session = repo.login();
		Node objects = session.getNode("/objects");
		StringBuffer nodes = new StringBuffer();

		for (NodeIterator i = objects.getNodes(); i.hasNext();) {
			Node n = i.nextNode();
			nodes.append("Name: " + n.getName() + ", Path:" + n.getPath()
					+ "\n");
		}
		session.logout();
		return ok(nodes.toString()).build();

	}

	@POST
	@Path("/new")
	public Response ingestAndMint() throws RepositoryException {
		return ingest(pidMinter.mintPid());
	}

	@POST
	@Path("/{pid}")
	public Response ingest(@PathParam("pid") final String pid)
			throws RepositoryException {

		logger.debug("Attempting to ingest with pid: " + pid);

		final Session session = repo.login();

		if (session.hasPermission("/objects/" + pid, "add_node")) {
			final Node obj = jcrTools.findOrCreateNode(session, "/objects/"
					+ pid, "nt:folder");
			obj.addMixin("fedora:object");
			obj.addMixin("fedora:owned");
			obj.setProperty("fedora:ownerId", "Fedo Radmin");
			obj.setProperty("jcr:lastModified", Calendar.getInstance());
            obj.setProperty("dc:identifier", new String[] { obj.getIdentifier(), pid });
			session.save();
			/*
			 * we save before updating the repo size because the act of
			 * persisting session state creates new system-curated nodes and
			 * properties which contribute to the footprint of this resource
			 */
			updateRepositorySize(getObjectSize(obj), session);
			// now we save again to persist the repo size
			session.save();
			session.logout();
			logger.debug("Finished ingest with pid: " + pid);
			return created(uriInfo.getAbsolutePath()).build();
		} else {
			session.logout();
			return four03;
		}
	}

	@GET
	@Path("/{pid}")
	@Produces({ TEXT_XML, APPLICATION_JSON })
	public Response getObject(@PathParam("pid") final String pid)
			throws RepositoryException, IOException {

		final Session session = repo.login();

		if (session.nodeExists("/objects/" + pid)) {

			final Node obj = session.getNode("/objects/" + pid);
			final ObjectProfile objectProfile = new ObjectProfile();

			objectProfile.pid = pid;
			objectProfile.objLabel = obj.getName();
			objectProfile.objOwnerId = obj.getProperty("fedora:ownerId")
					.getString();
			objectProfile.objCreateDate = obj.getProperty("jcr:created")
					.getString();
			objectProfile.objLastModDate = obj.getProperty("jcr:lastModified")
					.getString();
			objectProfile.objSize = getObjectSize(obj);
			objectProfile.objItemIndexViewURL = uriInfo
					.getAbsolutePathBuilder().path("datastreams").build();
			objectProfile.objState = A;
			objectProfile.objModels = transform(
					copyOf(obj.getMixinNodeTypes()),
					new Function<NodeType, String>() {
						@Override
						public String apply(NodeType type) {
							return type.getName();
						}
					});

			session.logout();
			return ok(objectProfile).build();
		} else {
			session.logout();
			return four04;
		}
	}

	@DELETE
	@Path("/{pid}")
	public Response deleteObject(@PathParam("pid") final String pid)
			throws RepositoryException {
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

}
