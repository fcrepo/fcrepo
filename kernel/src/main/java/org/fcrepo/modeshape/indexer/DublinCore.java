package org.fcrepo.modeshape.indexer;

import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.ok;

import java.io.InputStream;
import java.util.List;

import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.fcrepo.modeshape.AbstractResource;
import org.fcrepo.modeshape.indexer.dublincore.AbstractIndexer;

@Path("/objects/{pid}")
public class DublinCore extends AbstractResource {

	@Resource
	List<AbstractIndexer> indexers;

	@GET
	@Produces(TEXT_XML)
	@Path("/oai_dc")
	public Response getObjectAsDublinCore(@PathParam("pid") final String pid)
			throws RepositoryException {
		final Session session = repo.login();

		try {
			if (session.nodeExists("/objects/" + pid)) {
				final Node obj = session.getNode("/objects/" + pid);

				for (AbstractIndexer indexer : indexers) {
					InputStream inputStream = indexer.getStream(obj);

					if (inputStream != null) {
						return ok(inputStream).build();
					}
				}

				return four04;
			} else {
				return four04;
			}

		} finally {
			session.logout();
		}

	}

}
