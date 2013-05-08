package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_XML;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.PathSegment;

import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.jaxb.responses.management.DatastreamFixity;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.services.LowLevelStorageService;
import org.fcrepo.utils.FixityResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Path("/rest/{path: .*}/fcr:fixity")
public class FedoraFixity extends AbstractResource {

	@Autowired
	private DatastreamService datastreamService;

	@Autowired
	private LowLevelStorageService llStoreService;

	@GET
	@Timed
	@Produces({TEXT_XML, APPLICATION_JSON})
	public DatastreamFixity getDatastreamFixity(@PathParam("path")
												List<PathSegment> pathList) throws RepositoryException {

		final Session session = getAuthenticatedSession();

		try {
			final String path = toPath(pathList);

			final Datastream ds = datastreamService.getDatastream(session, path);

			final DatastreamFixity dsf = new DatastreamFixity();
			dsf.path = path;
			dsf.timestamp = new Date();

			final Collection<FixityResult> blobs =
					llStoreService.runFixityAndFixProblems(ds);
			dsf.statuses = new ArrayList<FixityResult>(blobs);
			return dsf;
		} finally {
			session.logout();
		}
	}

	public DatastreamService getDatastreamService() {
		return datastreamService;
	}

	public void setDatastreamService(final DatastreamService datastreamService) {
		this.datastreamService = datastreamService;
	}

	public LowLevelStorageService getLlStoreService() {
		return llStoreService;
	}

	public void setLlStoreService(final LowLevelStorageService llStoreService) {
		this.llStoreService = llStoreService;
	}

}
