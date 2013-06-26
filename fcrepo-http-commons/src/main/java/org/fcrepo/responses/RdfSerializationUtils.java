
package org.fcrepo.responses;

import static com.google.common.collect.ImmutableList.of;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static org.fcrepo.utils.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.joda.time.format.DateTimeFormat.forPattern;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.ws.rs.core.MultivaluedMap;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.Symbol;

public class RdfSerializationUtils {

    private static final Logger logger = getLogger(RdfSerializationUtils.class);

    /**
     * The RDF predicate that will indicate the primary node type.
     */
    public static Node primaryTypePredicate =
            createURI(getRDFNamespaceForJcrNamespace("http://www.jcp.org/jcr/1.0") +
                    "primaryType");

    /**
     * The RDF predicate that will indicate the last-modified date of the node.
     */
    public static Node lastModifiedPredicate =
            createURI(getRDFNamespaceForJcrNamespace("http://www.jcp.org/jcr/1.0") +
                    "lastModified");

    public static DateTimeFormatter RFC2822DATEFORMAT =
            forPattern("EEE, dd MMM yyyy HH:mm:ss Z");

    static String getFirstValueForPredicate(final Dataset rdf,
            final Node subject, final Node predicate) {
        final Iterator<Quad> statements =
                rdf.asDatasetGraph().find(ANY, subject, predicate, ANY);
        // we'll take the first one we get
        if (statements.hasNext()) {
            final Quad statement = statements.next();
            logger.trace("Checking statement: {}", statement);
            return statement.asTriple().getObject().getLiteral()
                    .getLexicalForm();
        } else {
            logger.trace("No value found for predicate: {}", predicate);
            return null;
        }
    }

    static Node getDatasetSubject(final Dataset rdf) {
        Context context = rdf.getContext();
        String uri = context.getAsString(Symbol.create("uri"));
        logger.debug("uri from context: {}", uri);
        if (uri != null) {
            return createURI(uri);
        } else {
            return null;
        }
    }

    static void
            setCachingHeaders(final MultivaluedMap<String, Object> httpHeaders,
                    final Dataset rdf) {
        httpHeaders.put("Cache-Control", of((Object) "max-age=0"));
        httpHeaders.put("Cache-Control", of((Object) "must-revalidate"));

        logger.trace("Attempting to discover the last-modified date of the node for the resource in question...");
        final String lastModifiedinXSDStyle =
                getFirstValueForPredicate(rdf, getDatasetSubject(rdf),
                        lastModifiedPredicate);
        if (lastModifiedinXSDStyle != null) {
            logger.debug("Found last-modified date: {}", lastModifiedinXSDStyle);
            final String lastModified =
                    RFC2822DATEFORMAT.print(ISODateTimeFormat.dateTime()
                            .withOffsetParsed().parseDateTime(
                                    lastModifiedinXSDStyle));
            httpHeaders.put("Last-Modified", of((Object) lastModified));
        }
    }

    static Model unifyDatasetModel(final Dataset dataset) {
        final Iterator<String> iterator = dataset.listNames();
        Model model = ModelFactory.createDefaultModel();

        model = model.union(dataset.getDefaultModel());

        while (iterator.hasNext()) {
            final String modelName = iterator.next();
            logger.debug("Serializing model {}", modelName);
            model = model.union(dataset.getNamedModel(modelName));
        }

        model.setNsPrefixes(dataset.getDefaultModel().getNsPrefixMap());
        return model;
    }
}
