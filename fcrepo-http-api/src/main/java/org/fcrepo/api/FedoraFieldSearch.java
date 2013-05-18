
package org.fcrepo.api;

import static com.google.common.collect.ImmutableList.builder;
import static java.lang.Integer.parseInt;
import static javax.jcr.query.Query.JCR_SQL2;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.fcrepo.http.RDFMediaType.N3;
import static org.fcrepo.http.RDFMediaType.N3_ALT1;
import static org.fcrepo.http.RDFMediaType.N3_ALT2;
import static org.fcrepo.http.RDFMediaType.NTRIPLES;
import static org.fcrepo.http.RDFMediaType.RDF_JSON;
import static org.fcrepo.http.RDFMediaType.RDF_XML;
import static org.fcrepo.http.RDFMediaType.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.update.GraphStore;
import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraResource;
import org.fcrepo.api.rdf.HttpGraphSubjects;
import org.fcrepo.http.RDFMediaType;
import org.fcrepo.jaxb.search.FieldSearchResult;
import org.fcrepo.jaxb.search.ObjectFields;
import org.fcrepo.provider.GraphStreamingOutput;
import org.fcrepo.provider.ModelStreamingOutput;
import org.fcrepo.provider.VelocityViewer;
import org.fcrepo.utils.FedoraJcrTypes;
import org.fcrepo.utils.JcrRdfTools;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Vincent Nguyen
 */

@Component
@Path("/rest/fcr:search")
public class FedoraFieldSearch extends AbstractResource implements
        FedoraJcrTypes {

    private static final Logger logger = getLogger(FedoraFieldSearch.class);


    @GET
    @Timed
    @Produces({N3, N3_ALT1, N3_ALT2, TURTLE, RDF_XML, RDF_JSON, NTRIPLES})
    public GraphStreamingOutput searchSubmitRdf(@QueryParam("q") final String terms,
            @QueryParam("offset") @DefaultValue("0") final long offset,
            @QueryParam("limit") final int limit, @Context final Request request)
                    throws RepositoryException{

        final Session session = getAuthenticatedSession();
        try{
            /* select the best response type */
            final Variant bestPossibleResponse = request.selectVariant(RDFMediaType.POSSIBLE_RDF_VARIANTS);


            final Resource searchResult = ResourceFactory.createResource(uriInfo.getRequestUri().toASCIIString());
            final GraphStore graphStore = nodeService.searchRepository(new HttpGraphSubjects(FedoraNodes.class, uriInfo), searchResult, session, terms, limit, offset);

            return new GraphStreamingOutput(graphStore, bestPossibleResponse.getMediaType());

        }finally{
            session.logout();
        }
    }

}
