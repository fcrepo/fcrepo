/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api.responses;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.generic.EscapeTool;

import org.fcrepo.config.FedoraPropsConfig;
import org.fcrepo.config.SystemInfoConfig;
import org.fcrepo.http.api.FedoraSearch;
import org.fcrepo.http.commons.responses.ViewHelpers;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.SearchResult;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;

import javax.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_HTML_WITH_CHARSET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * HTML writer for search results
 *
 * @author awoods
 * @since 2020-08-04
 */
@Provider
@Produces({TEXT_HTML_WITH_CHARSET})
public class SearchResultProvider implements MessageBodyWriter<SearchResult> {

    @Inject
    UriInfo uriInfo;

    @Inject
    HttpServletRequest request;

    @Inject
    private FedoraPropsConfig fedoraPropsConfig;

    @Inject
    private SystemInfoConfig systemInfoConfig;

    private static final EscapeTool escapeTool = new EscapeTool();

    private final VelocityEngine velocity = new VelocityEngine();

    // Location in the classpath where Velocity templates are to be found.
    private static final String templatesLocation = "/views";

    private static final String templateFilenameExtension = ".vsl";

    private static final String velocityPropertiesLocation = "/velocity.properties";

    private static final ViewHelpers VIEW_HELPERS = ViewHelpers.getInstance();

    private static final Logger LOGGER = getLogger(SearchResultProvider.class);

    @PostConstruct
    void init() throws IOException {
        LOGGER.trace("Velocity engine initializing...");
        final Properties properties = new Properties();
        final var velocityLog = fedoraPropsConfig.getVelocityLog().toString();
        LOGGER.debug("Setting Velocity runtime log: {}", velocityLog);
        properties.setProperty("runtime.log", velocityLog);

        final URL propertiesUrl = getClass().getResource(velocityPropertiesLocation);

        LOGGER.debug("Using Velocity configuration from {}", propertiesUrl);
        try (final InputStream propertiesStream = propertiesUrl.openStream()) {
            properties.load(propertiesStream);
        }
        velocity.init(properties);
        LOGGER.trace("Velocity engine initialized.");
    }

    @Override
    public boolean isWriteable(final Class<?> type,
                               final Type genericType,
                               final Annotation[] annotations,
                               final MediaType mediaType) {
        return SearchResult.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(final SearchResult result,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final SearchResult result,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream)
            throws WebApplicationException {

        final Template template = velocity.getTemplate(getTemplateLocation("search"));

        final Context context;
        context = getContext();
        context.put("searchResults", result);

        // The contract of MessageBodyWriter<T> is _not_ to close the stream after writing to it
        final PrintWriter writer = new PrintWriter(entityStream, false, StandardCharsets.UTF_8);
        template.merge(context, writer);
        writer.flush();
    }

    private Context getContext() {

        final Context context = new VelocityContext();
        final String[] baseUrl = uriInfo.getBaseUri().getPath().split("/");
        if (baseUrl.length > 0) {
            final String staticBaseUrl = String.join("/", Arrays.copyOf(baseUrl, baseUrl.length - 1));
            context.put("staticBaseUrl", staticBaseUrl);
        } else {
            context.put("staticBaseUrl", "/");
        }
        final var searchPage = uriInfo.getBaseUriBuilder().clone().path(FedoraSearch.class).toString();
        context.put("searchPage", searchPage);
        context.put("fields",
                Arrays.stream(Condition.Field.values()).map(Condition.Field::toString).toArray(String[]::new));
        context.put("operators", Arrays.stream(Condition.Operator.values()).map(Condition.Operator::getStringValue)
                .toArray(String[]::new));
        context.put("isOriginalResource", null);
        context.put("helpers", VIEW_HELPERS);
        context.put("esc", escapeTool);
        context.put("uriInfo", uriInfo);
        context.put("fedoraProps", fedoraPropsConfig);
        context.put("fedoraCommit", systemInfoConfig.getGitCommit());
        context.put("fedoraVersion", systemInfoConfig.getImplementationVersion());
        return context;
    }

    private static String getTemplateLocation(final String templateName) {
        return templatesLocation + "/" + templateName.replace(':', '-') + templateFilenameExtension;
    }

}
