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
package org.fcrepo.http.api.responses;

import static com.hp.hpl.jena.graph.Node.ANY;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.getAllValuesForPredicate;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.getFirstValueForPredicate;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.mixinTypesPredicate;
import static org.fcrepo.http.commons.responses.RdfSerializationUtils.primaryTypePredicate;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableMap.builder;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.FieldTool;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.responses.ViewHelpers;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.impl.rdf.impl.NamespaceRdfContext;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple HTML provider for RdfStreams
 *
 * @author ajs6f
 * @since Nov 19, 2013
 */
@Provider
@Produces({TEXT_HTML, APPLICATION_XHTML_XML})
public class StreamingBaseHtmlProvider implements MessageBodyWriter<RdfStream> {


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
     * Location in the classpath where the common css file is to be found.
     */
    public static final String commonCssLocation = "/views/common.css";

    /**
     * Location in the classpath where the common javascript file is to be found.
     */
    public static final String commonJsLocation = "/views/common.js";

    /**
     * A map from String names for primary node types to the Velocity templates
     * that should be used for those node types.
     */
    protected Map<String, Template> templatesMap;

    public static final String templateFilenameExtension = ".vsl";

    public static final String velocityPropertiesLocation =
            "/velocity.properties";

    private static final Logger LOGGER =
        getLogger(StreamingBaseHtmlProvider.class);

    private final Predicate<NodeType> acceptWhenTemplateExists = new Predicate<NodeType>() {
        @Override
        public boolean apply(final NodeType nodeType) {
            final String nodeTypeName = nodeType.getName();
            if (isBlank(nodeTypeName)) {
                return false;
            }
            return velocity.resourceExists(getTemplateLocation(nodeTypeName));
        }
    };

    private final Predicate<String> acceptWhenTemplateMapContainsKey = new Predicate<String>() {
        @Override
        public boolean apply(final String key) {
            if (isBlank(key)) {
                return false;
            }
            return templatesMap.containsKey(key);
        }
    };

    @PostConstruct
    void init() throws IOException {

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
        final ImmutableMap.Builder<String, Template> templatesMapBuilder = builder();
        final Session session = sessionFactory.getInternalSession();
        try {
            // we search all of the possible node primary types and mixins
            for (final NodeTypeIterator primaryNodeTypes =
                         session.getWorkspace().getNodeTypeManager()
                                 .getPrimaryNodeTypes(); primaryNodeTypes.hasNext();) {
                final NodeType primaryNodeType =
                    primaryNodeTypes.nextNodeType();
                final String primaryNodeTypeName =
                    primaryNodeType.getName();

                // Create a list of the primary type and all its parents
                final List<NodeType> nodeTypesList = new ArrayList<>();
                nodeTypesList.add(primaryNodeType);
                nodeTypesList.addAll(Arrays.asList(primaryNodeType.getSupertypes()));

                // Find a template that matches the primary type or one of its parents
                final NodeType templateMatch = Iterables.find(nodeTypesList, acceptWhenTemplateExists, null);
                if (templateMatch != null) {
                    addTemplate(primaryNodeTypeName, templateMatch.getName(), templatesMapBuilder);
                } else {
                    // No HTML representation available for that kind of node
                    LOGGER.debug("Didn't find template for nodes with primary type or its parents: {} in location: {}",
                            primaryNodeTypeName, templatesLocation);
                }
            }

            final List<String> otherTemplates =
                    ImmutableList.of("jcr:nodetypes", "node", "fcr:versions", "fcr:fixity");

            for (final String key : otherTemplates) {
                final Template template =
                    velocity.getTemplate(getTemplateLocation(key));
                templatesMapBuilder.put(key, template);
            }

            templatesMap = templatesMapBuilder.build();

        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        LOGGER.trace("Assembled template map.");
        LOGGER.trace("HtmlProvider initialization complete.");
    }

    @Override
    public void writeTo(final RdfStream rdfStream, final Class<?> type,
                        final Type genericType, final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException {

        try {
            final RdfStream nsRdfStream = new NamespaceRdfContext(rdfStream.session());

            rdfStream.namespaces(nsRdfStream.namespaces());

            final Node subject = rdfStream.topic();

            final Model model = rdfStream.asModel();

            final Template nodeTypeTemplate = getTemplate(model, subject, annotations);

            final Context context = getContext(model, subject);

            // the contract of MessageBodyWriter<T> is _not_ to close the stream
            // after writing to it
            final Writer outWriter = new OutputStreamWriter(entityStream);
            nodeTypeTemplate.merge(context, outWriter);
            outWriter.flush();

        } catch (final RepositoryException e) {
            throw new WebApplicationException(e);
        }

    }

    protected Context getContext(final Model model, final Node subject) {
        final FieldTool fieldTool = new FieldTool();

        final Context context = new VelocityContext();
        context.put("rdfLexicon", fieldTool.in(RdfLexicon.class));
        context.put("helpers", ViewHelpers.getInstance());
        context.put("esc", escapeTool);
        context.put("rdf", model.getGraph());

        context.put("model", model);
        context.put("subjects", model.listSubjects());
        context.put("nodeany", ANY);
        context.put("topic", subject);
        context.put("uriInfo", uriInfo);
        return context;
    }

    private Template getTemplate(final Model rdf, final Node subject,
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
            LOGGER.trace("Attempting to discover the mixin types of the node for the resource in question...");
            final Iterator<String> mixinTypes =
                getAllValuesForPredicate(rdf, subject,
                                         mixinTypesPredicate);

            LOGGER.debug("Found mixins: {}", mixinTypes);
            if (mixinTypes != null) {
                final ImmutableList<String> copy = copyOf(mixinTypes);
                final String mixin = Iterables.find(copy, acceptWhenTemplateMapContainsKey, null);
                if (mixin != null) {
                    LOGGER.debug("Matched mixin type: {}", mixin);
                    template = templatesMap.get(mixin);
                }
            }
        }

        if (template == null) {
            LOGGER.trace("Attempting to discover the primary type of the node for the resource in question...");
            final String nodeType =
                    getFirstValueForPredicate(rdf, subject, primaryTypePredicate);

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
                && RdfStream.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(final RdfStream t, final Class<?> type,
                        final Type genericType, final Annotation[] annotations,
                        final MediaType mediaType) {
        // we don't know in advance how large the result might be
        return -1;
    }

    private void addTemplate(final String primaryNodeTypeName, final String templateNodeTypeName,
                             final ImmutableMap.Builder<String, Template> templatesMapBuilder) {
        final String templateLocation = getTemplateLocation(templateNodeTypeName);
        final Template template =
            velocity.getTemplate(templateLocation);
        template.setName(templateLocation);
        LOGGER.debug("Found template: {}", templateLocation);
        templatesMapBuilder.put(primaryNodeTypeName, template);
        LOGGER.debug("which we will use for nodes with primary type: {}",
                     primaryNodeTypeName);
    }

    private static String getTemplateLocation(final String nodeTypeName) {
        return templatesLocation + "/" +
            nodeTypeName.replace(':', '-') + templateFilenameExtension;
    }
}
