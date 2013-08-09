/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.http.commons.responses;

import static com.google.common.collect.ImmutableList.of;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static org.fcrepo.kernel.utils.JcrRdfTools.getRDFNamespaceForJcrNamespace;
import static org.joda.time.format.DateTimeFormat.forPattern;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.Locale;

import javax.ws.rs.core.MultivaluedMap;

import org.fcrepo.kernel.rdf.GraphProperties;
import org.fcrepo.kernel.rdf.SerializationUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.util.Context;

/**
 * Utilities to help with serializing a graph to an HTTP resource
 */
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

    /**
     * DateTimeFormatter for RFC2822 (used in HTTP headers), e.g.:
     *    Mon, 01 Jul 2013 07:51:23Z
     */
    protected static DateTimeFormatter RFC2822DATEFORMAT =
            forPattern("EEE, dd MMM yyyy HH:mm:ss Z").withLocale(Locale.US)
                    .withZone(DateTimeZone.forID("GMT"));

    /**
     * Get the very first value for a predicate as a string, or null if the
     * predicate is not used
     * 
     * @param rdf
     * @param subject
     * @param predicate
     * @return
     */
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

    /**
     * Get the subject of the dataset, given by the context's "uri"
     * 
     * @param rdf
     * @return
     */
    static Node getDatasetSubject(final Dataset rdf) {
        Context context = rdf.getContext();
        String uri = context.getAsString(GraphProperties.URI_SYMBOL);
        logger.debug("uri from context: {}", uri);
        if (uri != null) {
            return createURI(uri);
        } else {
            return null;
        }
    }

    /**
     * Set the cache control and last modified HTTP headers from data in the
     * graph
     * 
     * @param httpHeaders
     * @param rdf
     */
    static void setCachingHeaders(final MultivaluedMap<String,
            Object> httpHeaders, final Dataset rdf) {
        httpHeaders.put("Cache-Control", of((Object) "max-age=0"));
        httpHeaders.put("Cache-Control", of((Object) "must-revalidate"));

        logger.trace("Attempting to discover the last-modified date of the node for the resource in question...");
        final Iterator<Quad> iterator =
                rdf.asDatasetGraph().find(ANY, SerializationUtils.getDatasetSubject(rdf),
                        lastModifiedPredicate, ANY);

        if (!iterator.hasNext()) {
            return;
        }

        final Object dateObject = iterator.next().getObject().getLiteralValue();

        if (!(dateObject instanceof XSDDateTime)) {
            logger.debug("Found last-modified date, but it was"
                    + "not an XSDDateTime: {}", dateObject);

            return;
        }

        final XSDDateTime lastModified = (XSDDateTime) dateObject;
        logger.debug("Found last-modified date: {}", lastModified);
        final String lastModifiedAsRdf2822 =
                RFC2822DATEFORMAT
                        .print(new DateTime(lastModified.asCalendar()));
        httpHeaders.put("Last-Modified", of((Object) lastModifiedAsRdf2822));
    }

}
