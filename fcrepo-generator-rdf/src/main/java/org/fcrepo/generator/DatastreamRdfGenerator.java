
package org.fcrepo.generator;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.fcrepo.services.DatastreamService.getDatastream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.any23.extractor.ExtractionContext;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;
import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.generator.rdf.TripleSource;
import org.fcrepo.generator.rdf.TripleSource.Triple;
import org.fcrepo.generator.rdf.Utils;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.sail.memory.model.MemValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/objects/{pid}/datastreams/{dsid}/rdf")
@Produces({TEXT_XML, "text/turtle", TEXT_PLAIN})
public class DatastreamRdfGenerator extends AbstractResource {

    List<TripleSource<Datastream>> dsGenerators;

    final private static ValueFactory valFactory = new MemValueFactory();

    final private Logger logger = LoggerFactory
            .getLogger(DatastreamRdfGenerator.class);

    @GET
    @Path("/")
    @Produces({TEXT_XML, "text/turtle", TEXT_PLAIN})
    public String getRdfXml(@PathParam("pid")
    final String pid, @PathParam("dsid")
    final String dsId, @HeaderParam("Accept")
    @DefaultValue(TEXT_XML)
    final String mimeType) throws IOException, RepositoryException,
            TripleHandlerException {

        final Datastream ds = getDatastream(pid, dsId);
        final URI docURI = valFactory.createURI("info:" + pid + "/" + dsId);
        logger.debug("Using ValueFactory: " + valFactory.toString());
        final ExtractionContext context =
                new ExtractionContext("Fedora Serialization Context", docURI);
        logger.debug("Using ExtractionContext: " + context.toString());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // possible serializations
            final TripleHandler writer = Utils.selectWriter(mimeType, out);
            logger.trace("Created RDF Writer: " + writer.getClass().getName());

            writer.openContext(context);
            writer.startDocument(docURI);

            // add JCR-managed namespaces
            final NamespaceRegistry nReg =
                    ds.getNode().getSession().getWorkspace()
                            .getNamespaceRegistry();
            for (final String prefix : nReg.getPrefixes()) {
                writer.receiveNamespace(prefix, nReg.getURI(prefix), context);
            }

            // add triples from each TripleSource
            for (final TripleSource<Datastream> tripleSource : dsGenerators) {
                logger.trace("Using TripleSource: " +
                        tripleSource.getClass().getName());
                for (final Triple t : tripleSource.getTriples(ds, uriInfo)) {
                    writer.receiveTriple(valFactory.createURI(t.subject),
                            valFactory.createURI(t.predicate), valFactory
                                    .createLiteral(t.object), null, context);
                }
            }

            writer.endDocument(docURI);
            writer.close();
            logger.debug("Generated RDF: " + out.toString());
            return out.toString();
        }

    }

    public void setDsGenerators(List<TripleSource<Datastream>> dsGenerators) {
        this.dsGenerators = dsGenerators;
    }

}
