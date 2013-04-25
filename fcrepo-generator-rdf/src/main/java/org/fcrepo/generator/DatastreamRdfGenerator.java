
package org.fcrepo.generator;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;

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
import javax.ws.rs.core.PathSegment;

import org.apache.any23.extractor.ExtractionContext;
import org.apache.any23.writer.TripleHandler;
import org.apache.any23.writer.TripleHandlerException;
import org.fcrepo.AbstractResource;
import org.fcrepo.Datastream;
import org.fcrepo.generator.rdf.TripleSource;
import org.fcrepo.generator.rdf.TripleSource.Triple;
import org.fcrepo.generator.rdf.Utils;
import org.fcrepo.services.DatastreamService;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.sail.memory.model.MemValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Path("/rest/{path: .*(?!(fcr\\:))}/fcr:datastreams/{dsid}/fcr:rdf")
@Produces({TEXT_XML, "text/turtle", TEXT_PLAIN})
public class DatastreamRdfGenerator extends AbstractResource {

    private List<TripleSource<Datastream>> dsGenerators;

    private static final ValueFactory valFactory = new MemValueFactory();

    private final Logger logger = LoggerFactory
            .getLogger(DatastreamRdfGenerator.class);

    @GET
    @Path("/")
    @Produces({TEXT_XML, "text/turtle", TEXT_PLAIN})
    public String getRdfXml(@PathParam("path")
    final List<PathSegment> pathList, @PathParam("dsid")
    final String dsId, @HeaderParam("Accept")
    @DefaultValue(TEXT_XML)
    final String mimeType) throws IOException, RepositoryException,
            TripleHandlerException {

        final String path = toPath(pathList);
        final Datastream ds = datastreamService.getDatastream(path, dsId);
        final URI docURI = valFactory.createURI("info:" + path + "/" + dsId);
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
                final String nsURI = nReg.getURI(prefix);
                if (nsURI != null && !nsURI.equals("") &&
                        !prefix.equals("xmlns")) {
                    writer.receiveNamespace(prefix, nsURI, context);
                }
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

    public void setDsGenerators(
            final List<TripleSource<Datastream>> dsGenerators) {
        this.dsGenerators = dsGenerators;
    }

}
