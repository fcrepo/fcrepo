
package org.fcrepo.generator;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.fcrepo.services.ObjectService.getObject;

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
import org.apache.any23.writer.NTriplesWriter;
import org.apache.any23.writer.RDFXMLWriter;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;
import org.apache.any23.writer.TurtleWriter;
import org.fcrepo.AbstractResource;
import org.fcrepo.FedoraObject;
import org.fcrepo.generator.rdf.TripleSource;
import org.fcrepo.generator.rdf.TripleSource.Triple;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.sail.memory.model.MemValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/objects/{pid}/rdf")
@Produces({TEXT_XML, TEXT_PLAIN})
public class RdfGenerator extends AbstractResource {

    List<TripleSource<FedoraObject>> objectGenerators;

    final private static ValueFactory valFactory = new MemValueFactory();

    final private Logger logger = LoggerFactory.getLogger(RdfGenerator.class);

    @GET
    @Path("/")
    @Produces({TEXT_XML, "text/turtle", TEXT_PLAIN})
    public String getRdfXml(@PathParam("pid")
    final String pid, @HeaderParam("Accept")
    @DefaultValue(TEXT_XML)
    final String mimeType) throws IOException, RepositoryException,
            TripleHandlerException {

        final FedoraObject obj = getObject(pid);
        final URI docURI = valFactory.createURI("info:" + pid);
        logger.debug("Using ValueFactory: " + valFactory.toString());
        final ExtractionContext context =
                new ExtractionContext("Fedora Serialization Context", docURI);
        logger.debug("Using ExtractionContext: " + context.toString());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // possible serializations
            TripleHandler writer;
            switch (mimeType) {
                case "text/turtle":
                    writer = new TurtleWriter(out);
                    break;
                case TEXT_PLAIN:
                    writer = new NTriplesWriter(out);
                default:
                    writer = new RDFXMLWriter(out);
                    break;
            }

            logger.trace("Created RDF Writer: " + writer.getClass().getName());

            writer.openContext(context);
            writer.startDocument(docURI);
            // add JCR-managed namespaces
            final NamespaceRegistry nReg =
                    obj.getNode().getSession().getWorkspace()
                            .getNamespaceRegistry();
            for (final String prefix : nReg.getPrefixes()) {
                writer.receiveNamespace(prefix, nReg.getURI(prefix), context);
            }

            for (final TripleSource<FedoraObject> tripleSource : objectGenerators) {
                logger.trace("Using TripleSource: " +
                        tripleSource.getClass().getName());
                for (final Triple t : tripleSource.getTriples(obj, uriInfo)) {
                    logger.debug("TripleSource generated: " + t.toString());
                    final URI subject = valFactory.createURI(t.subject);
                    logger.trace("Created final triple subject: " +
                            subject.toString());
                    final URI predicate = valFactory.createURI(t.predicate);
                    logger.trace("Created final triple predicate: " +
                            predicate.toString());
                    final Literal object = valFactory.createLiteral(t.object);
                    logger.trace("Created final triple object: " +
                            object.toString());
                    writer.receiveTriple(subject, predicate, object, null,
                            context);
                }
            }

            writer.endDocument(docURI);
            writer.close();
            logger.debug("Generated RDF: " + out.toString());
            return out.toString();
        }

    }

    public void setObjectGenerators(
            List<TripleSource<FedoraObject>> objectGenerators) {
        this.objectGenerators = objectGenerators;
    }

}
