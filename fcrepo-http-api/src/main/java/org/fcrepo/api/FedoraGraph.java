package org.fcrepo.api;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.UpdateAction;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.jena.riot.WebContent;
import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.utils.FedoraJcrTypes;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Variant;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.ok;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Path("/rest/{path: .*}/fcr:graph")
public class FedoraGraph extends AbstractResource {

	private static final Logger logger = getLogger(FedoraGraph.class);

	@GET
	@Produces({WebContent.contentTypeN3,
					  WebContent.contentTypeN3Alt1,
					  WebContent.contentTypeN3Alt2,
					  WebContent.contentTypeTurtle,
					  WebContent.contentTypeRDFXML,
					  WebContent.contentTypeRDFJSON,
					  WebContent.contentTypeNTriples})
	public StreamingOutput describeRdf(@PathParam("path") final List<PathSegment> pathList, @Context Request request) throws RepositoryException, IOException {

		final String path = toPath(pathList);
		logger.trace("getting profile for {}", path);



		List<Variant> possibleResponseVariants =
				Variant.mediaTypes(new MediaType(WebContent.contentTypeN3.split("/")[0], WebContent.contentTypeN3.split("/")[1]),
										  new MediaType(WebContent.contentTypeN3Alt1.split("/")[0], WebContent.contentTypeN3Alt1.split("/")[1]),
										  new MediaType(WebContent.contentTypeN3Alt2.split("/")[0], WebContent.contentTypeN3Alt2.split("/")[1]),
										  new MediaType(WebContent.contentTypeTurtle.split("/")[0], WebContent.contentTypeTurtle.split("/")[1]),
										  new MediaType(WebContent.contentTypeRDFXML.split("/")[0], WebContent.contentTypeRDFXML.split("/")[1]),
										  new MediaType(WebContent.contentTypeRDFJSON.split("/")[0], WebContent.contentTypeRDFJSON.split("/")[1]),
										  new MediaType(WebContent.contentTypeNTriples.split("/")[0], WebContent.contentTypeNTriples.split("/")[1]),
										  new MediaType(WebContent.contentTypeTriG.split("/")[0], WebContent.contentTypeTriG.split("/")[1]),
										  new MediaType(WebContent.contentTypeNQuads.split("/")[0], WebContent.contentTypeNQuads.split("/")[1])
				)
						.add().build();
		Variant bestPossibleResponse = request.selectVariant(possibleResponseVariants);


		final String rdfWriterFormat = WebContent.contentTypeToLang(bestPossibleResponse.getMediaType().toString()).getName().toUpperCase();

		return new StreamingOutput() {
			@Override
			public void write(final OutputStream out) throws IOException {

				final Session session = getAuthenticatedSession();
				try {
					Node node = session.getNode(path);

					final FedoraObject object = objectService.getObject(node.getSession(), path);
					final GraphStore graphStore = object.getGraphStore();

					graphStore.toDataset().getDefaultModel().write(out, rdfWriterFormat);
				} catch (final RepositoryException e) {
					throw new WebApplicationException(e);
				} finally {
					session.logout();
				}
			}

		};

	}

	/**
	 * Creates a new object.
	 *
	 * @param pathList
	 * @return 201
	 * @throws RepositoryException
	 * @throws org.fcrepo.exception.InvalidChecksumException
	 * @throws IOException
	 */
	@POST
	@Consumes({WebContent.contentTypeSPARQLUpdate})
	@Timed
	public Response updateSparql(
										@PathParam("path") final List<PathSegment> pathList,
										final InputStream requestBodyStream
	) throws RepositoryException, IOException, InvalidChecksumException {

		String path = toPath(pathList);
		logger.debug("Attempting to ingest with path: {}", path);

		final Session session = getAuthenticatedSession();

		try {
			if (objectService.exists(session, path)) {

				if(requestBodyStream != null) {

					final FedoraObject result = objectService.getObject(session, path);

					UpdateAction.parseExecute(IOUtils.toString(requestBodyStream), result.getGraphStore());

					session.save();

					return ok().build();
				} else {
					return Response.status(HttpStatus.SC_CONFLICT).entity(path + " is an existing resource").build();
				}
			} else {
				return Response.status(HttpStatus.SC_NOT_FOUND).entity(path + " must be an existing resource").build();
			}

		} finally {
			session.logout();
		}
	}
}
