package org.fcrepo.generator;


import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.ok;

import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.fcrepo.generator.rdf.TripleGenerator;
import org.fcrepo.services.ObjectService;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;

@Path("/objects/{pid}/rdf")
public class RdfGenerator {

    @Resource
    List<TripleGenerator> rdfgenerators;
    
    @Inject
    ObjectService objectService;

    @GET
    @Produces(TEXT_XML)
    public Response getObjectAsRdfXml(@PathParam("pid")
                                          final String pid) throws RepositoryException {

        final Node obj = objectService.getObjectNode(pid);


        final Model model = getObjectAsRdf(obj);

        RDFWriter w = model.getWriter("RDF/XML");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        w.write(model, os, "RDF/XML");

        return ok(os.toString()).build();
    }

    private Model getObjectAsRdf(final Node obj) throws RepositoryException {

        final Model model = ModelFactory.createDefaultModel();

        final com.hp.hpl.jena.rdf.model.Resource resource = model.createResource(obj.getIdentifier());
        for (TripleGenerator indexer : rdfgenerators) {
            indexer.updateResourceFromNode(resource, obj);
        }

        return model;
    }

}
