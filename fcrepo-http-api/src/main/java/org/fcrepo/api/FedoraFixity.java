package org.fcrepo.api;

import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.RDFMediaType.RDF_XML;
import static org.fcrepo.http.RDFMediaType.TURTLE;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import com.hp.hpl.jena.query.Dataset;
import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.http.RDFMediaType;
import org.fcrepo.services.DatastreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;

@Component
@Path("/rest/{path: .*}/fcr:fixity")
public class FedoraFixity extends AbstractResource {

	@Autowired
	private DatastreamService datastreamService;

	@GET
	@Timed
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES})
	public Dataset getDatastreamFixity(@PathParam("path") List<PathSegment> pathList,
                                       @Context final Request request,
                                       @Context final UriInfo uriInfo) throws RepositoryException {

		final Session session = getAuthenticatedSession();

		try {
			final String path = toPath(pathList);

            /* select the best response type */
            final Variant bestPossibleResponse =
                    request.selectVariant(RDFMediaType.POSSIBLE_RDF_VARIANTS);

			final Datastream ds = datastreamService.getDatastream(session, path);

            return datastreamService.getFixityResultsModel(new HttpGraphSubjects(FedoraNodes.class, uriInfo), ds);
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

}
