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

import static com.hp.hpl.jena.graph.Node.ANY;
import static org.fcrepo.jcr.FedoraJcrTypes.FCR_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION_LABEL;
import static org.fcrepo.kernel.RdfLexicon.LAST_MODIFIED_DATE;
import static org.fcrepo.kernel.RdfLexicon.RDFS_LABEL;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.fcrepo.http.commons.api.rdf.QuadOrdering;
import org.fcrepo.kernel.RdfLexicon;
import org.slf4j.Logger;

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
        return dataset.find(ANY, subject, predicate.asNode(), ANY);
    }

    /**
     * Gets a version label of a subject from the graph
     *
     * @param dataset
     * @param subject
     * @param defaultValue a value to be returned if no label is present in the
     *                     graph
     * @return the label of the version if one has been provided; otherwise
     * the default is returned
     */
    public String getVersionLabel(final DatasetGraph dataset,
                                 final Node subject, String defaultValue) {
        final Iterator<Quad> objects = getObjects(dataset, subject,
                HAS_VERSION_LABEL);
        if (objects.hasNext()) {
            return objects.next().getObject().getLiteralValue().toString();
        }
        return defaultValue;
    }

    /**
     * Gets a modification date of a subject from the graph
     *
     * @param dataset
     * @param subject
     * @return the modification date or null if none exists
     */
    public String getVersionDate(final DatasetGraph dataset,
                                 final Node subject) {
        final Iterator<Quad>  objects = getObjects(dataset, subject,
                LAST_MODIFIED_DATE);
        if (objects.hasNext()) {
            return objects.next().getObject().getLiteralValue().toString();
        }
        return null;
    }

    /**
     * Get the canonical title of a subject from the graph
     *
     * @param dataset
     * @param subject
     * @return
     */
    public String getObjectTitle(final DatasetGraph dataset,
            final Node subject) {

        final Property[] properties = new Property[] {RDFS_LABEL, DC_TITLE};

        for (final Property p : properties) {
            final Iterator<Quad> objects = getObjects(dataset, subject, p);

            if (objects.hasNext()) {
                return objects.next().getObject().getLiteralValue().toString();
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
     * Determines whether the subject is of type nt:frozenNode.
     */
    public boolean isFrozenNode(final DatasetGraph dataset,
        final Node subject) {
        final Iterator<Quad> objects
            = getObjects(dataset, subject, RdfLexicon.HAS_PRIMARY_TYPE);
        return objects.hasNext()
                && objects.next().getObject()
                .getLiteralValue().toString().equals("nt:frozenNode");
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
            final Node subject, final Resource predicate, final boolean uriAsLink) {
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
                if (uriAsLink) {
                    return "&lt;<a href=\"" + object.getURI() + "\">" +
                               object.getURI() + "</a>&gt;";
                } else {
                    return object.getURI();
                }
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
            final String namespace, final boolean compact) {
        final String nsURIPrefix = mapping.getNsURIPrefix(namespace);

        if (nsURIPrefix == null) {
            if (compact) {
                final int hashIdx = namespace.lastIndexOf("#");

                final int split;

                if (hashIdx > 0) {
                    split = namespace.substring(0, hashIdx).lastIndexOf("/");
                } else {
                    split = namespace.lastIndexOf("/");
                }

                if (split > 0) {
                    return "..." + namespace.substring(split);
                } else {
                    return namespace;
                }

            } else {
                return namespace;
            }


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
        final StringBuilder sb = new StringBuilder();

        final Map<String, String> nsPrefixMap = mapping.getNsPrefixMap();

        for (final Map.Entry<String, String> entry : nsPrefixMap.entrySet()) {
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

    /**
     * Proxying access to the RDF type static property
     * @return
     */
    public Property rdfType() {
        return RDF.type;
    }

    /**
     * Proxying access to the RDFS domain static property
     * @return
     */
    public Property rdfsDomain() {
        return RDFS.domain;
    }

    /**
     * Proxying access to the RDFS class static property
     * @return
     */
    public Resource rdfsClass() {
        return RDFS.Class;
    }

    /**
     * Get the content-bearing node for the given subject
     * @param subject
     * @return
     */
    public Node getContentNode(final Node subject) {
        return NodeFactory.createURI(subject + "/" + FCR_CONTENT);
    }

    /**
     * Transform a source string to something appropriate for HTML ids
     * @param source
     * @return
     */
    public String parameterize(final String source) {
        return source.toLowerCase().replaceAll("[^a-z0-9\\-_]+", "_");
    }
}
