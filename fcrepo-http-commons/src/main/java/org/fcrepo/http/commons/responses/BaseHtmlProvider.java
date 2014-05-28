/**
 * Copyright 2014 DuraSpace, Inc.
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
import static com.google.common.collect.ImmutableMap.builder;
import static com.hp.hpl.jena.graph.Node.ANY;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static org.fcrepo.kernel.impl.rdf.SerializationUtils.getDatasetSubject;
import static org.fcrepo.kernel.impl.rdf.SerializationUtils.unifyDatasetModel;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.getFirstValueForPredicate;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.primaryTypePredicate;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.setCachingHeaders;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.FieldTool;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.RdfLexicon;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap.Builder;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * A simple JAX-RS Entity Provider that can accept RDF datasets that represent
 * Fedora resources and merge them into templates chosen based on the primary
 * node type of the backing JCR node for that Fedora resource.
 *
 * @author ajs6f
 * @since May 19, 2013
 */
@Provider
@Produces({TEXT_HTML, APPLICATION_XHTML_XML})
public class BaseHtmlProvider implements MessageBodyWriter<Dataset> {

    @Autowired
    SessionFactory sessionFactory;

    @javax.ws.rs.core.Context
    UriInfo uriInfo;

    private static EscapeTool escapeTool = new EscapeTool();

    protected VelocityEngine velocity = new VelocityEngine();

    /**
     * Location in the classpath where Velocity templates are to be found.
     */
    public static final String templatesLocation = "/views";

    /**
     * A map from String names for primary node types to the Velocity templates
     * that should be used for those node types.
     */
    protected Map<String, Template> templatesMap;

    public static final String templateFilenameExtension = ".vsl";

    private static final Logger LOGGER = getLogger(BaseHtmlProvider.class);

    public static final String velocityPropertiesLocation =
            "/velocity.properties";

    @PostConstruct
    void init() throws IOException, RepositoryException {

        LOGGER.trace("Velocity engine initializing...");
        final Properties properties = new Properties();
        final URL propertiesUrl =
                getClass().getResource(velocityPropertiesLocation);
        LOGGER.debug("Using Velocity configuration from {}", propertiesUrl);
        try (final InputStream propertiesStream = propertiesUrl.openStream()) {
            properties.load(propertiesStream);
        }
        velocity.init(properties);
        LOGGER.trace("Velocity engine initialized.");

        LOGGER.trace("Assembling a map of node primary types -> templates...");
        final Builder<String, Template> templatesMapBuilder = builder();
        final Session session = sessionFactory.getInternalSession();
        try {
            // we search all of the possible node primary types
            for (final NodeTypeIterator primaryNodeTypes =
                    session.getWorkspace().getNodeTypeManager()
                            .getPrimaryNodeTypes(); primaryNodeTypes.hasNext();) {
                final String primaryNodeTypeName =
                        primaryNodeTypes.nextNodeType().getName();
                // for each node primary type, we try to find a template
                final String templateLocation =
                        templatesLocation + "/" +
                                primaryNodeTypeName.replace(':', '-') +
                                templateFilenameExtension;
                if (velocity.resourceExists(templateLocation)) {
                    final Template template =
                        velocity.getTemplate(templateLocation);
                    template.setName(templateLocation);
                    LOGGER.debug("Found template: {}", templateLocation);
                    templatesMapBuilder.put(primaryNodeTypeName, template);
                    LOGGER.debug("which we will use for nodes with primary type: {}",
                                 primaryNodeTypeName);
                } else {
                    // No HTML representation available for that kind of node
                    LOGGER.debug("Didn't find template for nodes with primary type: {} in location: {}",
                                 primaryNodeTypeName, templateLocation);
                }
            }

            final List<String> otherTemplates =
                    ImmutableList.of("search:results", "jcr:namespaces",
                                     "jcr:workspaces", "jcr:nodetypes",
                                     "node", "fcr:versions", "fcr:lock");

            for (final String key : otherTemplates) {
                final Template template =
                        velocity.getTemplate(templatesLocation + "/" +
                                key.replace(':', '-') +
                                templateFilenameExtension);
                templatesMapBuilder.put(key, template);
            }

            templatesMap = templatesMapBuilder.build();

        } finally {
            session.logout();
        }
        LOGGER.trace("Assembled template map.");
        LOGGER.trace("HtmlProvider initialization complete.");
    }

    @Override
    public void writeTo(final Dataset rdf, final Class<?> type,
            final Type genericType, final Annotation[] annotations,
            final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream) throws IOException {

        LOGGER.debug("Writing an HTML response for: {}", rdf);
        LOGGER.trace("Attempting to discover our subject");
        final Node subject = getDatasetSubject(rdf);

        // add standard headers
        httpHeaders.put("Content-type", of((Object) TEXT_HTML));
        setCachingHeaders(httpHeaders, rdf);

        final Template nodeTypeTemplate =
                getTemplate(rdf, subject, annotations);

        final Context context = getContext(rdf, subject);

        // the contract of MessageBodyWriter<T> is _not_ to close the stream
        // after writing to it
        final Writer outWriter = new OutputStreamWriter(entityStream);
        nodeTypeTemplate.merge(context, outWriter);
        outWriter.flush();

    }

    protected Context getContext(final Dataset rdf, final Node subject) {
        final FieldTool fieldTool = new FieldTool();

        final Context context = new VelocityContext();
        context.put("rdfLexicon", fieldTool.in(RdfLexicon.class));
        context.put("helpers", ViewHelpers.getInstance());
        context.put("esc", escapeTool);
        context.put("rdf", rdf.asDatasetGraph());

        final Model model = unifyDatasetModel(rdf);

        context.put("model", model);
        context.put("subjects", model.listSubjects());
        context.put("nodeany", ANY);
        context.put("topic", subject);
        context.put("uriInfo", uriInfo);
        return context;
    }

    private Template getTemplate(final Dataset rdf, final Node subject,
            final Annotation[] annotations) {
        Template template = null;

        for (final Annotation a : annotations) {
            if (a instanceof HtmlTemplate) {
                final String value = ((HtmlTemplate) a).value();
                LOGGER.debug("Found an HtmlTemplate annotation {}", value);
                template = templatesMap.get(value);
                break;
            }
        }

        if (template == null) {
            LOGGER.trace("Attempting to discover the primary type of the node for the resource in question...");
            final String nodeType =
                    getFirstValueForPredicate(rdf, subject,
                            primaryTypePredicate);

            LOGGER.debug("Found primary node type: {}", nodeType);
            template = templatesMap.get(nodeType);
        }

        if (template == null) {
            LOGGER.debug("Falling back on default node template");
            template = templatesMap.get("node");
        }

        LOGGER.debug("Choosing template: {}", template.getName());
        return template;
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {
        LOGGER.debug(
                "Checking to see if type: {} is serializable to mimeType: {}",
                type.getName(), mediaType);
        return (mediaType.equals(TEXT_HTML_TYPE) || mediaType
                .equals(APPLICATION_XHTML_XML_TYPE))
                && Dataset.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(final Dataset t, final Class<?> type,
            final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        // we don't know in advance how large the result might be
        return -1;
    }

}
