
package org.fcrepo.generator;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.PathSegment;

import org.apache.any23.extractor.ExtractionContext;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;
import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.FedoraObject;
import org.fcrepo.generator.rdf.TripleSource;
import org.fcrepo.generator.rdf.TripleSource.Triple;
import org.fcrepo.generator.rdf.Utils;
import org.fcrepo.services.DatastreamService;
import org.fcrepo.utils.NamespaceTools;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.sail.memory.model.MemValueFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
@Path("/rest/{path: .*}/fcr:rdf")
@Produces({TEXT_XML, "text/turtle", TEXT_PLAIN})
public class FedoraRdfGenerator extends AbstractResource {

    private static final ValueFactory valFactory = new MemValueFactory();

    private static final Logger logger = getLogger(FedoraRdfGenerator.class);
    
    private List<TripleSource<FedoraObject>> objectGenerators;
    
    private List<TripleSource<Datastream>> datastreamGenerators;

    @GET
    @Produces({TEXT_XML, "text/turtle", TEXT_PLAIN})
    public String getRdfXml(
            @PathParam("path") final List<PathSegment> pathList,
            @HeaderParam("Accept") @DefaultValue(TEXT_XML) final String mimeType
            ) throws IOException, RepositoryException, TripleHandlerException {

		final Session session = getAuthenticatedSession();
        final String path = toPath(pathList);
        final java.net.URI itemUri = uriInfo.getBaseUriBuilder().build("/rest" + path);
        final URI docURI = valFactory.createURI(itemUri.toString());
        logger.debug("Using ValueFactory: " + valFactory.toString());
        final ExtractionContext context =
                new ExtractionContext("Fedora Serialization Context", docURI);
        logger.debug("Using ExtractionContext: {}", context.toString());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // possible serializations
            final TripleHandler writer = Utils.selectWriter(mimeType, out);
            logger.trace("Created RDF Writer: {}", writer.getClass().getName());

            writer.openContext(context);
            writer.startDocument(docURI);

            if (objectService.isFile(session, path)) {
                writeDatastreamTriples(path, writer, context);
            } else {
                writeObjectTriples(path, writer, context);
            }


            writer.endDocument(docURI);
            writer.close();
            logger.debug("Generated RDF: {}", out.toString());
            return out.toString();
        }

    }
    
    // add JCR-managed namespaces
    private void writeNamespaces(Node node, TripleHandler writer, ExtractionContext context)
            throws TripleHandlerException, RepositoryException {
        final NamespaceRegistry nReg =
                NamespaceTools.getNamespaceRegistry(node);
        for (final String prefix : nReg.getPrefixes()) {
            final String nsURI = nReg.getURI(prefix);
            if (nsURI != null && !nsURI.equals("") &&
                    !prefix.equals("xmlns")) {
                writer.receiveNamespace(prefix, nsURI, context);
            }
        }
    }
    
    private void writeDatastreamTriples(String path, TripleHandler writer, ExtractionContext context)
            throws TripleHandlerException, RepositoryException {

		final Session session = getAuthenticatedSession();

        final Datastream obj = datastreamService.getDatastream(session, path);
        // add namespaces
        writeNamespaces(obj.getNode(), writer, context);
        // add triples from each TripleSource
        for (final TripleSource<Datastream> tripleSource : datastreamGenerators) {
            logger.trace("Using TripleSource: {}",
                    tripleSource.getClass().getName());
            for (final Triple t : tripleSource.getTriples(obj, uriInfo)) {
                writer.receiveTriple(valFactory.createURI(t.subject),
                        valFactory.createURI(t.predicate), valFactory
                                .createLiteral(t.object), null, context);
            }
        }
    }

    private void writeObjectTriples(String path, TripleHandler writer, ExtractionContext context)
            throws TripleHandlerException, RepositoryException {

		final Session session = getAuthenticatedSession();

        final FedoraObject obj = objectService.getObject(session, path);
        // add namespaces
        writeNamespaces(obj.getNode(), writer, context);
        // add triples from each TripleSource
        for (final TripleSource<FedoraObject> tripleSource : objectGenerators) {
            logger.trace("Using TripleSource: {}",
                    tripleSource.getClass().getName());
            for (final Triple t : tripleSource.getTriples(obj, uriInfo)) {
                writer.receiveTriple(valFactory.createURI(t.subject),
                        valFactory.createURI(t.predicate), valFactory
                                .createLiteral(t.object), null, context);
            }
        }        
    }

    public void setObjectGenerators(
            final List<TripleSource<FedoraObject>> objectGenerators) {
        this.objectGenerators = objectGenerators;
    }

    public void setDatastreamGenerators(
            final List<TripleSource<Datastream>> dsGenerators) {
        this.datastreamGenerators = dsGenerators;
    }

    void setDatastreamService(DatastreamService datastreamService) {
        this.datastreamService = datastreamService;
    }

}
