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

package org.fcrepo.responses;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Quad;
import org.fcrepo.RdfLexicon;
import org.fcrepo.api.rdf.QuadOrdering;
import org.slf4j.Logger;

import javax.ws.rs.core.UriInfo;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * General view helpers for rendering HTML responses
 */
public class ViewHelpers {

    private final Logger LOGGER = getLogger(ViewHelpers.class);

    private static ViewHelpers instance = null;

    protected ViewHelpers() {
        // Exists only to defeat instantiation.
    }

    /**
     * ViewHelpers are singletons. Initialize or return the existing object
     * @return
     */
    public static ViewHelpers getInstance() {
        if (instance == null) {
            instance = new ViewHelpers();
        }
        return instance;
    }

    /**
     * Return an iterator of Quads that match the given subject and predicate
     * 
     * @param dataset
     * @param subject
     * @param predicate
     * @return
     */
    public Iterator<Quad> getObjects(final DatasetGraph dataset,
            final Node subject, final Resource predicate) {
        return dataset.find(Node.ANY, subject, predicate.asNode(), Node.ANY);
    }

    /**
     * Get the canonical title of a subject from the graph
     * 
     * @param dataset
     * @param subject
     * @return
     */
    public String
            getObjectTitle(final DatasetGraph dataset, final Node subject) {

        Property[] properties =
                new Property[] {RdfLexicon.RDFS_LABEL, RdfLexicon.DC_TITLE};

        for (Property p : properties) {
            final Iterator<Quad> objects = getObjects(dataset, subject, p);

            if (objects.hasNext()) {
                return objects.next().getObject().getLiteralValue().toString();
            }
        }

        return subject.getURI();
    }

    /**
     * Get the string version of the object that matches the given subject and
     * predicate
     * 
     * @param dataset
     * @param subject
     * @param predicate
     * @return
     */
    public String getObjectsAsString(final DatasetGraph dataset,
            final Node subject, final Resource predicate) {
        final Iterator<Quad> iterator = getObjects(dataset, subject, predicate);

        if (iterator.hasNext()) {
            final Node object = iterator.next().getObject();

            if (object.isLiteral()) {
                final String s = object.getLiteralValue().toString();

                if (s.isEmpty()) {
                    return "<empty>";
                } else {
                    return s;
                }
            } else {
                return "&lt;<a href=\"" + object.getURI() + "\">" +
                        object.getURI() + "</a>&gt;";
            }
        } else {
            return "";
        }
    }

    /**
     * Generate url -> local name breadcrumbs for a given node's tree
     * 
     * @param uriInfo
     * @param subject
     * @return
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

        StringBuilder cumulativePath = new StringBuilder();

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
     * Sort a Iterator of Quads alphabetically by its subject, predicate, and
     * object
     * 
     * @param model
     * @param it
     * @return
     */
    public List<Quad> getSortedTriples(final Model model,
            final Iterator<Quad> it) {
        return Ordering.from(new QuadOrdering(model)).sortedCopy(
                ImmutableList.copyOf(it));
    }

    /**
     * Get the namespace prefix (or the namespace URI itself, if no prefix is
     * available) from a prefix mapping
     * 
     * @param mapping
     * @param namespace
     * @return
     */
    public String getNamespacePrefix(final PrefixMapping mapping,
            final String namespace) {
        final String nsURIPrefix = mapping.getNsURIPrefix(namespace);

        if (nsURIPrefix == null) {
            return namespace;
        } else {
            return nsURIPrefix + ":";
        }
    }

    /**
     * Get a prefix preamble appropriate for a SPARQL-UPDATE query from a prefix
     * mapping object
     * 
     * @param mapping
     * @return
     */
    public String getPrefixPreamble(final PrefixMapping mapping) {
        StringBuilder sb = new StringBuilder();

        final Map<String, String> nsPrefixMap = mapping.getNsPrefixMap();

        for (Map.Entry<String, String> entry : nsPrefixMap.entrySet()) {
            sb.append("PREFIX " + entry.getKey() + ": <" + entry.getValue() +
                    ">\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Convert an RDF resource to an RDF node
     *
     * @param r
     * @return
     */
    public Node asNode(final Resource r) {
        return r.asNode();
    }

    /**
     * Yes, we really did create a method to increment
     * a given int. You can't do math in a velocity template.
     *
     * @param i
     * @return
     */
    public int addOne(final int i) {
        return i + 1;
    }
}
