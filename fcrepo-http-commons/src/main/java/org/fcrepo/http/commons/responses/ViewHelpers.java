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
import static java.time.Instant.now;
import static java.time.ZoneId.of;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.RdfLexicon.CONTAINS;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.WRITABLE;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicate;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.format.DateTimeFormatter;
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

    private static final String DEFAULT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(of("GMT")).format(now());

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
        LOGGER.warn("May Need Update with Mementos!");
        return null;
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
        vs.sort((v1, v2) -> getVersionDate(g, v1).orElse(DEFAULT).compareTo(getVersionDate(g, v2).orElse(DEFAULT)));
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
     * Gets a modification date of a subject from the graph
     *
     * @param graph the graph
     * @param subject the subject
     * @return the modification date if it exists
     */
    public Optional<String> getVersionDate(final Graph graph, final Node subject) {
        return getValue(graph, subject, CREATED_DATE.asNode());
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
        return getValue(graph, subject, WRITABLE.asNode()).filter("true"::equals).isPresent();
    }

    /**
     * Determines whether the subject is of type fedora:Version.
     * true if node has type fedora:Version
     * @param graph the graph
     * @param subject the subject
     * @return whether the subject is a versioned node
     */
    public boolean isVersionedNode(final Graph graph, final Node subject) {
        return listObjects(graph, subject, RDF.type.asNode()).toList().stream().map(Node::getURI)
            .anyMatch((REPOSITORY_NAMESPACE + "Version")::equals);
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
}
