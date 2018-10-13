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
package org.fcrepo.http.api.responses;

import static java.lang.System.getProperty;
import static java.util.stream.Stream.of;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static com.google.common.collect.ImmutableMap.builder;
import static org.apache.jena.graph.Node.ANY;
import static org.apache.jena.sparql.util.graph.GraphUtils.multiValueURI;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_HTML_WITH_CHARSET;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.google.common.collect.ImmutableMap;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.FieldTool;
import org.fcrepo.http.api.FedoraLdp;
import org.fcrepo.http.commons.api.rdf.HttpResourceConverter;
import org.fcrepo.http.commons.responses.HtmlTemplate;
import org.fcrepo.http.commons.responses.RdfNamespacedStream;
import org.fcrepo.http.commons.responses.ViewHelpers;
import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.http.commons.session.SessionFactory;
import org.fcrepo.kernel.api.RdfLexicon;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.glassfish.jersey.uri.UriTemplate;
import org.slf4j.Logger;

/**
 * Simple HTML provider for RdfNamespacedStreams
 *
 * @author ajs6f
 * @author whikloj
 * @since Nov 19, 2013
 */
@Provider
@Produces({TEXT_HTML_WITH_CHARSET})
public class StreamingBaseHtmlProvider implements MessageBodyWriter<RdfNamespacedStream> {


    @javax.ws.rs.core.Context
    UriInfo uriInfo;

    @javax.ws.rs.core.Context
    SessionFactory sessionFactory;

    @javax.ws.rs.core.Context
    HttpServletRequest request;

    private HttpSession session() {
        return sessionFactory.getSession(request);
    }

    private IdentifierConverter<Resource, FedoraResource> translator() {
        return new HttpResourceConverter(session(),
                uriInfo.getBaseUriBuilder().clone().path(FedoraLdp.class));
    }

    private static final EscapeTool escapeTool = new EscapeTool();

    private final VelocityEngine velocity = new VelocityEngine();

    /**
     * Location in the classpath where Velocity templates are to be found.
     */
    private static final String templatesLocation = "/views";

    /**
     * A map from String names for primary node types to the Velocity templates
     * that should be used for those node types.
     */
    private Map<String, Template> templatesMap;

    private static final String templateFilenameExtension = ".vsl";

    private static final String velocityPropertiesLocation =
            "/velocity.properties";

    private static final ViewHelpers VIEW_HELPERS = ViewHelpers.getInstance();

    private static final Logger LOGGER =
        getLogger(StreamingBaseHtmlProvider.class);

    @PostConstruct
    void init() throws IOException {
        LOGGER.trace("Velocity engine initializing...");
        final Properties properties = new Properties();
        final String fcrepoHome = getProperty("fcrepo.home");
        final String velocityLog = getProperty("fcrepo.velocity.runtime.log");
        if (velocityLog != null) {
            LOGGER.debug("Setting Velocity runtime log: {}", velocityLog);
            properties.setProperty("runtime.log", velocityLog);
        } else if (fcrepoHome != null) {
            LOGGER.debug("Using fcrepo.home directory for the velocity log");
            properties.setProperty("runtime.log", fcrepoHome + getProperty("file.separator") + "velocity.log");
        }
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

        of("fcr:versions", "fcr:fixity", "default")
            .forEach(key -> templatesMapBuilder.put(key, velocity.getTemplate(getTemplateLocation(key))));

        templatesMap = templatesMapBuilder
            .put(REPOSITORY_NAMESPACE + "RepositoryRoot", velocity.getTemplate(getTemplateLocation("root")))
            .put(REPOSITORY_NAMESPACE + "Binary", velocity.getTemplate(getTemplateLocation("binary")))
            .put(REPOSITORY_NAMESPACE + "Version", velocity.getTemplate(getTemplateLocation("resource")))
            .put(REPOSITORY_NAMESPACE + "Pairtree", velocity.getTemplate(getTemplateLocation("resource")))
            .put(REPOSITORY_NAMESPACE + "Container", velocity.getTemplate(getTemplateLocation("resource")))
            .put(LDP_NAMESPACE + "NonRdfSource", velocity.getTemplate(getTemplateLocation("binary")))
            .put(LDP_NAMESPACE + "RdfSource", velocity.getTemplate(getTemplateLocation("resource"))).build();

        LOGGER.trace("Assembled template map.");
        LOGGER.trace("HtmlProvider initialization complete.");
    }

    @Override
    public void writeTo(final RdfNamespacedStream nsStream, final Class<?> type,
                        final Type genericType, final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException {

        final Node subject = ViewHelpers.getContentNode(nsStream.stream.topic());

        final Model model = nsStream.stream.collect(toModel());
        model.setNsPrefixes(nsStream.namespaces);

        final Template nodeTypeTemplate = getTemplate(model, subject, Arrays.asList(annotations));

        final Context context = getContext(model, subject);

        final FedoraResource resource = getResourceFromSubject(subject.toString());
        context.put("isOriginalResource", (resource != null && resource.isOriginalResource()));
        context.put("isVersion", (resource != null && resource.isMemento()));
        context.put("isLDPNR", (resource != null &&
                (resource instanceof FedoraBinary || !resource.getDescribedResource().equals(resource))));

        // the contract of MessageBodyWriter<T> is _not_ to close the stream
        // after writing to it
        final Writer outWriter = new OutputStreamWriter(entityStream);
        nodeTypeTemplate.merge(context, outWriter);
        outWriter.flush();
    }

    /**
     * Get a FedoraResource for the subject of the graph, if it exists.
     *
     * @param subjectUri the uri of the subject
     * @return FedoraResource if exists or null
     */
    private FedoraResource getResourceFromSubject(final String subjectUri) {
        final UriTemplate uriTemplate =
            new UriTemplate(uriInfo.getBaseUriBuilder().clone().path(FedoraLdp.class).toTemplate());
        final Map<String, String> values = new HashMap<>();
        uriTemplate.match(subjectUri, values);
        if (values.containsKey("path")) {
            return translator().convert(translator().toDomain(values.get("path")));
        }
        return null;
    }

    private Context getContext(final Model model, final Node subject) {
        final FieldTool fieldTool = new FieldTool();

        final Context context = new VelocityContext();
        final String[] baseUrl = uriInfo.getBaseUri().getPath().split("/");
        if (baseUrl.length > 0) {
            final String staticBaseUrl = String.join("/", Arrays.copyOf(baseUrl, baseUrl.length - 1));
            context.put("staticBaseUrl", staticBaseUrl);
        } else {
            context.put("staticBaseUrl", "/");
        }
        context.put("rdfLexicon", fieldTool.in(RdfLexicon.class));
        context.put("helpers", VIEW_HELPERS);
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
                                 final List<Annotation> annotations) {

        final String tplName = annotations.stream().filter(x -> x instanceof HtmlTemplate)
            .map(x -> ((HtmlTemplate) x).value()).filter(templatesMap::containsKey).findFirst()
            .orElseGet(() -> {
                final List<String> types = multiValueURI(rdf.getResource(subject.getURI()), type);
                if (types.contains(REPOSITORY_NAMESPACE + "RepositoryRoot")) {
                    return REPOSITORY_NAMESPACE + "RepositoryRoot";
                }
                return types.stream().filter(templatesMap::containsKey).findFirst().orElse("default");
            });
        LOGGER.debug("Using template: {}", tplName);
        return templatesMap.get(tplName);
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType,
                               final Annotation[] annotations, final MediaType mediaType) {
        LOGGER.debug(
                "Checking to see if type: {} is serializable to mimeType: {}",
                type.getName(), mediaType);
        return isTextHtml(mediaType) && RdfNamespacedStream.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(final RdfNamespacedStream t, final Class<?> type,
                        final Type genericType, final Annotation[] annotations,
                        final MediaType mediaType) {
        // we don't know in advance how large the result might be
        return -1;
    }

    private static String getTemplateLocation(final String nodeTypeName) {
        return templatesLocation + "/" +
            nodeTypeName.replace(':', '-') + templateFilenameExtension;
    }

    private static boolean isTextHtml(final MediaType mediaType) {
        return mediaType.getType().equals(TEXT_HTML_TYPE.getType()) &&
            mediaType.getSubtype().equals(TEXT_HTML_TYPE.getSubtype());
    }

}
