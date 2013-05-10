package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.ok;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.services.ObjectService;
import org.fcrepo.utils.FedoraJcrTypes;
import org.modeshape.jcr.api.JcrConstants;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;


@Component
@Path("/rest/{path: .*}/fcr:children")
public class FedoraChildren extends AbstractResource {

	private static final Logger logger = getLogger(FedoraChildren.class);

	/**
	 * Returns a list of the first-generation
	 * descendants of an object, filtered by
	 * an optional mixin parameter
	 *
	 * @param pathList
	 * @param mixin
	 * @return 200
	 * @throws javax.jcr.RepositoryException
	 * @throws java.io.IOException
	 */
	@GET
	@Timed
	@Produces({TEXT_XML, APPLICATION_JSON, TEXT_HTML})
	public Response getObjects(
									  @PathParam("path") final List<PathSegment> pathList,
									  @QueryParam("mixin") @DefaultValue("") final String limitByMixinValue
	) throws RepositoryException, IOException {


		final Session session = getAuthenticatedSession();

        final String mixin;
		try {
			final String path = toPath(pathList);
			logger.info("getting children of {}", path);

            if (limitByMixinValue != null) {
                switch (limitByMixinValue) {
                    case "":
                        mixin = null;
                        break;
                    case FedoraJcrTypes.FEDORA_OBJECT:
                        mixin = JcrConstants.NT_FOLDER;
                        break;
                    case FedoraJcrTypes.FEDORA_DATASTREAM:
                        mixin = JcrConstants.NT_FILE;
                        break;
                    default:
                        mixin = limitByMixinValue;
                        break;
                }
            } else {
                mixin = null;
            }

			return ok(nodeService.getObjectNames(session, path, mixin).toString()).build();
		} finally {
			session.logout();
		}
	}


	public ObjectService getObjectService() {
		return objectService;
	}


	public void setObjectService(ObjectService objectService) {
		this.objectService = objectService;
	}

}
