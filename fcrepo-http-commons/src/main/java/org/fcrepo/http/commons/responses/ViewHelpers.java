/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.jena.atlas.iterator.Iter.asStream;
import static org.apache.jena.graph.GraphUtil.listObjects;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.vocabulary.DC.title;
import static org.apache.jena.vocabulary.RDF.type;
import static org.apache.jena.vocabulary.RDFS.label;
import static org.apache.jena.vocabulary.SKOS.prefLabel;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.MEMENTO_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_ROOT;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_LABEL_FORMATTER;
import static org.fcrepo.kernel.api.services.VersionService.MEMENTO_RFC_1123_FORMATTER;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Instant;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import javax.ws.rs.core.UriInfo;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.vocabulary.DCTerms;

import org.fcrepo.http.commons.api.rdf.TripleOrdering;
import org.slf4j.Logger;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * General view helpers for rendering HTML responses
 *
 * @author awoods
 * @author ajs6f
 */
public class ViewHelpers {

    private static final Logger LOGGER = getLogger(ViewHelpers.class);

    private static ViewHelpers instance = null;

    private static final List<Property>  TITLE_PROPERTIES = asList(label, title, DCTerms.title, prefLabel);

    private ViewHelpers() {
        // Exists only to defeat instantiation.
    }

    /**
     * ViewHelpers is a singleton. Initialize or return the existing object
     * @return an instance of ViewHelpers
     */
    public static ViewHelpers getInstance() {
        return instance == null ? instance = new ViewHelpers() : instance;
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
        // Mementos should be ordered by date so use the getOrderedVersions.
        return getOrderedVersions(graph, subject, CONTAINS.asResource());
    }

    /**
     * Return an iterator of Triples for versions in order that
     * they were created.
     *
     * @param g the graph
     * @param subject the subject
     * @param predicate the predicate
     * @return iterator
     */
    public Iterator<Node> getOrderedVersions(final Graph g, final Node subject, final Resource predicate) {
        final List<Node> vs = listObjects(g, subject, predicate.asNode()).toList();
        vs.sort(Comparator.comparing(v -> getVersionDate(g, v)));
        return vs.iterator();
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
     * Get the date time as the version label.
     *
     * @param graph the graph
     * @param subject the subject
     * @return the datetime in RFC 1123 format.
     */
    public String getVersionLabel(final Graph graph, final Node subject) {
        final Instant datetime = getVersionDate(graph, subject);
        return MEMENTO_RFC_1123_FORMATTER.format(datetime);
    }

    /**
     * Gets a modification date of a subject from the graph
     *
     * @param graph the graph
     * @param subject the subject
     * @return the modification date if it exists
     */
    public Instant getVersionDate(final Graph graph, final Node subject) {
        final String[] pathParts = subject.getURI().split("/");
        return MEMENTO_LABEL_FORMATTER.parse(pathParts[pathParts.length - 1], Instant::from);
    }

    private static Optional<String> getValue(final Graph graph, final Node subject, final Node predicate) {
        final Iterator<Node> objects = listObjects(graph, subject, predicate);
        return Optional.ofNullable(objects.hasNext() ? objects.next().getLiteralValue().toString() : null);
    }

    /**
     * Get the canonical title of a subject from the graph
     *
     * @param graph the graph
     * @param sub the subject
     * @return canonical title of the subject in the graph
     */
    public String getObjectTitle(final Graph graph, final Node sub) {
        if (sub == null) {
            return "";
        }
        final Optional<String> title = TITLE_PROPERTIES.stream().map(Property::asNode).flatMap(p -> listObjects(
                graph, sub, p).toList().stream()).filter(Node::isLiteral).map(Node::getLiteral).map(
                        LiteralLabel::toString).findFirst();
        return title.orElse(sub.isURI() ? sub.getURI() : sub.isBlank() ? sub.getBlankNodeLabel() : sub.toString());
    }

    /**
     * Determines whether the subject is writable
     * true if node is writable
     * @param graph the graph
     * @param subject the subject
     * @return whether the subject is writable
     */
    public boolean isWritable(final Graph graph, final Node subject) {
        // XXX: always return true until we can determine a better way to control the HTML UI
        return true;
    }

    /**
     * Determines whether the subject is of type memento:Memento.
     *
     * @param graph the graph
     * @param subject the subject
     * @return whether the subject is a versioned node
     */
    public boolean isVersionedNode(final Graph graph, final Node subject) {
        return listObjects(graph, subject, RDF.type.asNode()).toList().stream().map(Node::getURI)
            .anyMatch((MEMENTO_TYPE)::equals);
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
        LOGGER.trace("Getting Objects as String: s:{}, p:{}, g:{}", subject, predicate, graph);
        final Iterator<Node> iterator = listObjects(graph, subject, predicate.asNode());
        if (iterator.hasNext()) {
            final Node obj = iterator.next();
            if (obj.isLiteral()) {
                final String lit = obj.getLiteralValue().toString();
                return lit.isEmpty() ? "<empty>" : lit;
            }
            return uriAsLink ? "&lt;<a href=\"" + obj.getURI() + "\">" + obj.getURI() + "</a>&gt;" : obj.getURI();
        }
        return "";
    }

    /**
     * Returns the original resource as a URI Node if
     * the subject represents a memento uri; otherwise it
     * returns the subject parameter.
     * @param subject the subject
     * @return a URI node of the original resource.
     */
    public Node getOriginalResource(final Node subject) {
        if (!subject.isURI()) {
            return subject;
        }

        final String subjectUri = subject.getURI();
        final int index = subjectUri.indexOf(FCR_VERSIONS);
        if (index > 0) {
            return NodeFactory.createURI(subjectUri.substring(0, index - 1));
        } else {
            return subject;
        }
    }

    /**
     * Same as above but takes a string.
     * NB: This method is currently used in fcrepo-http-api/src/main/resources/views/default.vsl
     * @param subjectUri the URI
     * @return a node
     */
    public Node getOriginalResource(final String subjectUri) {
        return getOriginalResource(createURI(subjectUri));
    }

    /**
     * Get the number of child resources associated with the arg 'subject' as specified by the triple found in the arg
     * 'graph' with the predicate RdfLexicon.HAS_CHILD_COUNT.
     *
     * @param graph   of triples
     * @param subject for which child resources is sought
     * @return number of child resources
     */
    public int getNumChildren(final Graph graph, final Node subject) {
        LOGGER.trace("Getting number of children: s:{}, g:{}", subject, graph);
        return (int) asStream(listObjects(graph, subject, CONTAINS.asNode())).count();
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
        final String baseUri = uriInfo.getBaseUri().toString();

        if (!topic.startsWith(baseUri)) {
            LOGGER.trace("Topic wasn't part of our base URI {}", baseUri);
            return emptyMap();
        }

        final String salientPath = topic.substring(baseUri.length());
        final StringJoiner cumulativePath = new StringJoiner("/");
        return stream(salientPath.split("/")).filter(seg -> !seg.isEmpty()).collect(toMap(seg -> uriInfo
                .getBaseUriBuilder().path(cumulativePath.add(seg).toString())
                .build().toString(), seg -> seg, (u, v) -> null, LinkedHashMap::new));
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
     * @param ns the namespace
     * @param compact the boolean value of compact
     * @return namespace prefix
     */
    public String getNamespacePrefix(final PrefixMapping mapping,
            final String ns, final boolean compact) {
        final String nsURIPrefix = mapping.getNsURIPrefix(ns);
        if (nsURIPrefix == null) {
            if (compact) {
                final int hashIdx = ns.lastIndexOf('#');
                final int split = hashIdx > 0 ? ns.substring(0, hashIdx).lastIndexOf('/') : ns.lastIndexOf('/');
                return split > 0 ? "..." + ns.substring(split) : ns;
            }
            return ns;
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
        LOGGER.trace("Is RDF Resource? s:{}, ns:{}, r:{}, g:{}", subject, namespace, resource, graph);
        return graph.find(subject, type.asNode(),
                createResource(namespace + resource).asNode()).hasNext();
    }

    /**
     * Is the subject the repository root resource.
     *
     * @param graph The graph
     * @param subject The current subject
     * @return true if has rdf:type http://fedora.info/definitions/v4/repository#RepositoryRoot
     */
    public boolean isRootResource(final Graph graph, final Node subject) {
        return graph.contains(subject, rdfType().asNode(), REPOSITORY_ROOT.asNode());
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
    public static Node getContentNode(final Node subject) {
        return subject == null ? null : NodeFactory.createURI(subject.getURI().replace("/" + FCR_METADATA, ""));
    }

    /**
     * Create a URI Node from the provided String
     *
     * @param uri from which a URI Node will be created
     * @return URI Node
     */
    public static Node createURI(final String uri) {
        return NodeFactory.createURI(uri);
    }

    /**
     * Transform a source string to something appropriate for HTML ids
     * @param source the source string
     * @return transformed source string
     */
    public static String parameterize(final String source) {
        return source.toLowerCase().replaceAll("[^a-z0-9\\-_]+", "_");
    }

    /**
     * Test if a Predicate is managed
     * @param property the property
     * @return whether the property is managed
     */
    public static boolean isManagedProperty(final Node property) {
        return property.isURI() && isManagedPredicate.test(createProperty(property.getURI()));
    }

    /**
     * Find a key in a map and format it as a string
     * @param input map of objects.
     * @param key the key to locate in the map.
     * @return the result string.
     */
    public static String getString(final Map<String, Object> input, final String key) {
        if (input.get(key) == null) {
            return "";
        }
        final var value = input.get(key);
        final var clazz = value.getClass();
        final String output;
        if (clazz == String.class) {
            output = formatAsString((String) value);
        } else if (clazz == String[].class) {
            output = formatAsString((String[]) value);
        } else if (clazz == Long.class) {
            output = formatAsString((Long) value);
        } else {
            output = "";
        }
        return output;
    }

    /**
     * Format to a string and check for null values
     * @param input a string array or null
     * @return a string.
     */
    private static String formatAsString(final String[] input) {
        return (input == null || input.length == 0 ? "" :  String.join(", ", input));
    }

    /**
     * Format a string to check for null values
     * @param input a string or null
     * @return a string.
     */
    private static String formatAsString(final String input) {
        return (input == null ? "" : input);
    }

    private static String formatAsString(final Long input) {
        return (input == null ? "" : input.toString());
    }
}
