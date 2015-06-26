/**
 * Copyright 2015 DuraSpace, Inc.
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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static java.util.stream.Collectors.joining;
import static org.fcrepo.kernel.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.FedoraJcrTypes.FCR_METADATA;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.RdfLexicon.DCTERMS_TITLE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION_LABEL;
import static org.fcrepo.kernel.RdfLexicon.RDFS_LABEL;
import static org.fcrepo.kernel.RdfLexicon.SKOS_PREFLABEL;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.RdfLexicon.RDF_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.DC_NAMESPACE;
import static org.slf4j.LoggerFactory.getLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;

import org.fcrepo.http.commons.api.rdf.TripleOrdering;
import org.fcrepo.kernel.RdfLexicon;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * General view helpers for rendering HTML responses
 *
 * @author awoods
 */
public class ViewHelpers {

    private static final Logger LOGGER = getLogger(ViewHelpers.class);

    private static ViewHelpers instance = null;

    protected ViewHelpers() {
        // Exists only to defeat instantiation.
    }

    /**
     * ViewHelpers are singletons. Initialize or return the existing object
     * @return an instance of ViewHelpers
     */
    public static ViewHelpers getInstance() {
        if (instance == null) {
            instance = new ViewHelpers();
        }
        return instance;
    }

    /**
     * Return an iterator of Triples that match the given subject and predicate
     *
     * @param graph the graph
     * @param subject the subject
     * @param predicate the predicate
     * @return iterator
     */
    public Iterator<Triple> getObjects(final Graph graph,
        final Node subject, final Resource predicate) {
        return graph.find(subject, predicate.asNode(), ANY);
    }

    /**
     * Return an iterator of Triples for versions.
     *
     * @param graph the graph
     * @param subject the subject
     * @return iterator
     */
    public Iterator<Node> getVersions(final Graph graph,
        final Node subject) {
        return getOrderedVersions(graph, subject, HAS_VERSION);
    }

    /**
     * Return an iterator of Triples for versions in order that
     * they were created.
     *
     * @param graph the graph
     * @param subject the subject
     * @param predicate the predicate
     * @return iterator
     */
    public Iterator<Node> getOrderedVersions(final Graph graph,
        final Node subject, final Resource predicate) {
        final Iterator<Triple> versions = getObjects(graph, subject, predicate);
        final Map<String, Node> map = new TreeMap<>();
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        while (versions.hasNext()) {
            final Triple triple = versions.next();
            final String date = getVersionDate(graph, triple.getObject());
            String key = isNullOrEmpty(date) ? format.format(new Date()) : date;
            while (map.containsKey(key)) {
                key = key + "1";
            }
            map.put(key, triple.getObject());
        }
        return map.values().iterator();
    }

    /**
     * Gets the URL of the node whose version is represented by the
     * current node.  The current implementation assumes the URI
     * of that node will be the same as the breadcrumb entry that
     * precedes one with the path "fcr:versions".
     * @param uriInfo the uri info
     * @param subject the subject
     * @return the URL of the node
     */
     public String getVersionSubjectUrl(final UriInfo uriInfo, final Node subject) {
        final Map<String, String> breadcrumbs = getNodeBreadcrumbs(uriInfo, subject);
        String lastUrl = null;
        for (final Map.Entry<String, String> entry : breadcrumbs.entrySet()) {
            if (entry.getValue().equals("fcr:versions")) {
                return lastUrl;
            }
            lastUrl = entry.getKey();
        }
        return null;
     }

    /**
     * Gets a version label of a subject from the graph
     *
     * @param graph the graph
     * @param subject the subject
     * @param defaultValue a value to be returned if no label is present in the
     *                     graph
     * @return the label of the version if one has been provided; otherwise
     * the default is returned
     */
    public String getVersionLabel(final Graph graph,
                                 final Node subject, final String defaultValue) {
        final Iterator<Triple> objects = getObjects(graph, subject, HAS_VERSION_LABEL);
        if (objects.hasNext()) {
            return objects.next().getObject().getLiteralValue().toString();
        }
        return defaultValue;
    }

    /**
     * Gets a modification date of a subject from the graph
     *
     * @param graph the graph
     * @param subject the subject
     * @return the modification date or null if none exists
     */
    public String getVersionDate(final Graph graph,
                                 final Node subject) {
        final Iterator<Triple> objects = getObjects(graph, subject, CREATED_DATE);
        if (objects.hasNext()) {
            return objects.next().getObject().getLiteralValue().toString();
        }
        return "";
    }

    /**
     * Get the canonical title of a subject from the graph
     *
     * @param graph the graph
     * @param subject the subject
     * @return canonical title of the subject in the graph
     */
    public String getObjectTitle(final Graph graph, final Node subject) {

        if (subject == null) {
            return "";
        }

        final Property[] properties = new Property[] {RDFS_LABEL, DC_TITLE, DCTERMS_TITLE, SKOS_PREFLABEL};

        for (final Property p : properties) {
            final Iterator<Triple> objects = getObjects(graph, subject, p);

            if (objects.hasNext()) {
                return objects.next().getObject().getLiteral().getLexicalForm();
            }
        }

        if (subject.isURI()) {
            return subject.getURI();
        } else if (subject.isBlank()) {
            return subject.getBlankNodeLabel();
        } else {
            return subject.toString();
        }

    }

    /**
     * Take a HAS_SERIALIZATION node and find the RDFS_LABEL for the format it is associated with
     *
     * @param graph the graph
     * @param subject the subject
     * @return the label for the serialization format
     */
    public String getSerializationTitle(final Graph graph, final Node subject) {
        final Property dcFormat = createProperty(DC_NAMESPACE + "format");
        final Iterator<Triple> formatRDFs = getObjects(graph, subject, dcFormat);
        if (formatRDFs.hasNext()) {
            return getObjectTitle(graph, formatRDFs.next().getObject());
        }
        return "";
    }

    /**
     * Determines whether the subject is writable
     * true if node is writable
     * @param graph the graph
     * @param subject the subject
     * @return whether the subject is writable
     */
    public boolean isWritable(final Graph graph, final Node subject) {
        final Iterator<Triple> it = getObjects(graph, subject, RdfLexicon.WRITABLE);
        return it.hasNext() && it.next().getObject().getLiteralValue().toString().equals("true");
    }

    /**
     * Determines whether the subject is of type nt:frozenNode.
     * true if node has type nt:frozen
     * @param graph the graph
     * @param subject the subject
     * @return whether the subject is frozen node
     */
    public boolean isFrozenNode(final Graph graph, final Node subject) {
        final Iterator<Triple> objects = getObjects(graph, subject, RdfLexicon.HAS_PRIMARY_TYPE);
        return objects.hasNext()
                && objects.next().getObject()
                .getLiteralValue().toString().equals("nt:frozenNode");
    }

    /**
     * Get the string version of the object that matches the given subject and
     * predicate
     *
     * @param graph the graph
     * @param subject the subject
     * @param predicate the predicate
     * @param uriAsLink the boolean value of uri as link
     * @return string version of the object
     */
    public String getObjectsAsString(final Graph graph,
            final Node subject, final Resource predicate, final boolean uriAsLink) {
        final Iterator<Triple> iterator = getObjects(graph, subject, predicate);

        if (iterator.hasNext()) {
            final Node object = iterator.next().getObject();

            if (object.isLiteral()) {
                final String s = object.getLiteralValue().toString();
                if (s.isEmpty()) {
                    return "<empty>";
                }
                return s;
            }
            if (uriAsLink) {
                return "&lt;<a href=\"" + object.getURI() + "\">" +
                           object.getURI() + "</a>&gt;";
            }
            return object.getURI();
        }
        return "";
    }

    /**
     * Generate url to local name breadcrumbs for a given node's tree
     *
     * @param uriInfo the uri info
     * @param subject the subject
     * @return breadcrumbs
     */
    public Map<String, String> getNodeBreadcrumbs(final UriInfo uriInfo,
            final Node subject) {
        final String topic = subject.getURI();

        LOGGER.trace("Generating breadcrumbs for subject {}", subject);
        final ImmutableMap.Builder<String, String> builder =
                ImmutableMap.builder();

        final String baseUri = uriInfo.getBaseUri().toString();

        if (!topic.startsWith(baseUri)) {
            LOGGER.trace("Topic wasn't part of our base URI {}", baseUri);
            return builder.build();
        }

        final String salientPath = topic.substring(baseUri.length());

        final String[] split = salientPath.split("/");

        final StringBuilder cumulativePath = new StringBuilder();

        for (final String path : split) {

            if (path.isEmpty()) {
                continue;
            }

            cumulativePath.append(path);

            final String uri =
                    uriInfo.getBaseUriBuilder().path(cumulativePath.toString())
                            .build().toString();

            LOGGER.trace("Adding breadcrumb for path segment {} => {}", path,
                    uri);

            builder.put(uri, path);

            cumulativePath.append("/");

        }

        return builder.build();

    }

    /**
     * Sort a Iterator of Triples alphabetically by its subject, predicate, and
     * object
     *
     * @param model the model
     * @param it the iterator of triples
     * @return iterator of alphabetized triples
     */
    public List<Triple> getSortedTriples(final Model model, final Iterator<Triple> it) {
        final List<Triple> triples = newArrayList(it);
        triples.sort(new TripleOrdering(model));
        return triples;
    }

    /**
     * Get the namespace prefix (or the namespace URI itself, if no prefix is
     * available) from a prefix mapping
     *
     * @param mapping the prefix mapping
     * @param namespace the namespace
     * @param compact the boolean value of compact
     * @return namespace prefix
     */
    public String getNamespacePrefix(final PrefixMapping mapping,
            final String namespace, final boolean compact) {
        final String nsURIPrefix = mapping.getNsURIPrefix(namespace);

        if (nsURIPrefix == null) {
            if (compact) {
                final int hashIdx = namespace.lastIndexOf('#');

                final int split;

                if (hashIdx > 0) {
                    split = namespace.substring(0, hashIdx).lastIndexOf('/');
                } else {
                    split = namespace.lastIndexOf('/');
                }

                if (split > 0) {
                    return "..." + namespace.substring(split);
                }
                return namespace;
            }
            return namespace;
        }
        return nsURIPrefix + ":";
    }

    /**
     * Get a prefix preamble appropriate for a SPARQL-UPDATE query from a prefix
     * mapping object
     *
     * @param mapping the prefix mapping
     * @return prefix preamble
     */
    public String getPrefixPreamble(final PrefixMapping mapping) {
        return mapping.getNsPrefixMap().entrySet().stream()
                .map(e -> "PREFIX " + e.getKey() + ": <" + e.getValue() + ">").collect(joining("\n", "", "\n\n"));
    }

    /**
     * Determines whether the subject is kind of RDF resource
     * @param graph the graph
     * @param subject the subject
     * @param namespace the namespace
     * @param resource the resource
     * @return whether the subject is kind of RDF resource
     */
    public boolean isRdfResource(final Graph graph,
                                 final Node subject,
                                 final String namespace,
                                 final String resource) {
        return graph.find(subject, createResource(RDF_NAMESPACE + "type").asNode(),
                createResource(namespace + resource).asNode()).hasNext();
    }

    /**
     * Convert an RDF resource to an RDF node
     *
     * @param r the resource
     * @return RDF node representation of the given RDF resource
     */
    public Node asNode(final Resource r) {
        return r.asNode();
    }

    /**
     * Convert a URI string to an RDF node
     *
     * @param r the uri string
     * @return RDF node representation of the given string
     */
    public Node asLiteralStringNode(final String r) {
        return ResourceFactory.createPlainLiteral(r).asNode();
    }

    /**
     * Yes, we really did create a method to increment
     * a given int. You can't do math in a velocity template.
     *
     * @param i the given integer
     * @return maths
     */
    public int addOne(final int i) {
        return i + 1;
    }

    /**
     * Proxying access to the RDF type static property
     * @return RDF type property
     */
    public Property rdfType() {
        return RDF.type;
    }

    /**
     * Proxying access to the RDFS domain static property
     * @return RDFS domain property
     */
    public Property rdfsDomain() {
        return RDFS.domain;
    }

    /**
     * Proxying access to the RDFS class static property
     * @return RDFS class resource
     */
    public Resource rdfsClass() {
        return RDFS.Class;
    }

    /**
     * Get the content-bearing node for the given subject
     * @param subject the subject
     * @return content-bearing node for the given subject
     */
    public Node getContentNode(final Node subject) {
        return NodeFactory.createURI(subject.getURI().replace(FCR_METADATA, ""));
    }

    /**
     * Transform a source string to something appropriate for HTML ids
     * @param source the source string
     * @return transformed source string
     */
    public String parameterize(final String source) {
        return source.toLowerCase().replaceAll("[^a-z0-9\\-_]+", "_");
    }
}
