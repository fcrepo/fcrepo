
package org.fcrepo.responses;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.ImmutableMap.builder;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static org.fcrepo.responses.RdfSerializationUtils.getDatasetSubject;
import static org.fcrepo.responses.RdfSerializationUtils.getFirstValueForPredicate;
import static org.fcrepo.responses.RdfSerializationUtils.primaryTypePredicate;
import static org.fcrepo.responses.RdfSerializationUtils.setCachingHeaders;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.generic.FieldTool;
import org.fcrepo.RdfLexicon;
import org.fcrepo.session.SessionFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap.Builder;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;

/**
 * A simple JAX-RS Entity Provider that can accept RDF datasets
 * that represent Fedora resources and merge them into templates
 * chosen based on the primary node type of the backing JCR node
 * for that Fedora resource.
 *
 * @author ajs6f
 * @date May 19, 2013
 */
@Provider
@Component
public class HtmlProvider implements MessageBodyWriter<Dataset> {

    @Autowired
    SessionFactory sessionFactory;

    @javax.ws.rs.core.Context
    UriInfo uriInfo;


    protected VelocityEngine velocity = new VelocityEngine();

    /**
     * Location in the classpath where Velocity templates are to be found.
     */
    public static final String templatesLocation = "/views";

    /**
     * A map from String names for primary node types to the
     * Velocity templates that should be used for those node types.
     */
    protected Map<String, Template> templatesMap;

    public static final String templateFilenameExtension = ".vsl";

    private static final Logger LOGGER = getLogger(HtmlProvider.class);

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
        final Session session = sessionFactory.getSession();
        try {
            // we search all of the possible node primary types
            for (final NodeTypeIterator primaryNodeTypes =
                    session.getWorkspace().getNodeTypeManager()
                            .getPrimaryNodeTypes(); primaryNodeTypes.hasNext();) {
                final String primaryNodeTypeName =
                        primaryNodeTypes.nextNodeType().getName();
                // for each node primary type, we try to find a template
                final String templateLocation =
                        templatesLocation + "/" + primaryNodeTypeName.replace(':', '-') +
                                templateFilenameExtension;
                try {
                    final Template template =
                            velocity.getTemplate(templateLocation);
                    template.setName(templateLocation);
                    LOGGER.debug("Found template: {}", templateLocation);
                    templatesMapBuilder.put(primaryNodeTypeName, template);
                    LOGGER.debug(
                                        "which we will use for nodes with primary type: {}",
                                        primaryNodeTypeName);
                } catch (final ResourceNotFoundException e) {
                    LOGGER.debug(
                                        "Didn't find template for nodes with primary type: {} in location: {}",
                                        primaryNodeTypeName, templateLocation);
                    /*
                     * we don't care-- just means we don't have an HTML
                     * representation
                     * available for that kind of node
                     */
                }

            }

            List<String> otherTemplates = ImmutableList.of("search:results", "jcr:namespaces");

            for ( String key : otherTemplates) {
                final Template template = velocity.getTemplate(templatesLocation + "/" + key.replace(':', '-') + templateFilenameExtension);
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
            final OutputStream entityStream) throws IOException,
            WebApplicationException {

        LOGGER.debug("Writing an HTML response for: {}", rdf);
        LOGGER.trace("Attempting to discover our subject");
		Node subject = getDatasetSubject(rdf);

        // add standard headers
        httpHeaders.put("Content-type", of((Object) TEXT_HTML));
        setCachingHeaders(httpHeaders, rdf);

        final Template nodeTypeTemplate = getTemplate(rdf, subject, annotations);

        final FieldTool fieldTool = new FieldTool();

        final Context context = new VelocityContext();
        context.put("rdfLexicon", fieldTool.in(RdfLexicon.class));
        context.put("helpers", ViewHelpers.getInstance());
        context.put("rdf", rdf.asDatasetGraph());
        context.put("subjects", rdf.getDefaultModel().listSubjects());
        context.put("nodeany", Node.ANY);
        context.put("topic", subject);
        context.put("uriInfo", uriInfo);

        // the contract of MessageBodyWriter<T> is _not_ to close the stream
        // after writing to it
        final Writer outWriter = new OutputStreamWriter(entityStream);
        nodeTypeTemplate.merge(context, outWriter);
        outWriter.flush();

    }

    private Template getTemplate(final Dataset rdf, final Node subject, final Annotation[] annotations) {
        Template template = null;

        for (Annotation a : annotations) {
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
                    getFirstValueForPredicate(rdf, subject, primaryTypePredicate);

            LOGGER.debug("Found primary node type: {}", nodeType);
            template = templatesMap.get(nodeType);
        }

        LOGGER.debug("Choosing template: {}", template.getName());
        return template;
    }

    public void setTemplatesMap(final Map<String, Template> templatesMap) {
        this.templatesMap = templatesMap;
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType) {
        return mediaType.equals(TEXT_HTML_TYPE) &&
                (Dataset.class.isAssignableFrom(type) || Dataset.class
                        .isAssignableFrom(genericType.getClass()));
    }

    @Override
    public long getSize(final Dataset t, final Class<?> type,
            final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        // we don't know in advance how large the result might be
        return -1;
    }

}
