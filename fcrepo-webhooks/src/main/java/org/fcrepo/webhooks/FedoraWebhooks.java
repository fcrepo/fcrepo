
package org.fcrepo.webhooks;

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.fcrepo.AbstractResource;
import org.fcrepo.messaging.legacy.LegacyMethod;
import org.fcrepo.observer.FedoraEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@Path("/fcr:webhooks")
@Component
public class FedoraWebhooks extends AbstractResource {
    
    public static final String WEBHOOK_SEARCH = "webhook:*";
    
    public static final String WEBHOOK_CALLBACK_PROPERTY = "webhook:callbackUrl";
    
    public static final String WEBHOOK_JCR_TYPE = "webhook:callback";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(FedoraWebhooks.class);

    protected static final PoolingClientConnectionManager connectionManager =
            new PoolingClientConnectionManager();

    protected static HttpClient client;

    /**
     * For use with non-mutating methods.
     */
    private static Session readOnlySession;

    static {
        connectionManager.setMaxTotal(Integer.MAX_VALUE);
        connectionManager.setDefaultMaxPerRoute(5);
        connectionManager.closeIdleConnections(3, TimeUnit.SECONDS);
        client = new DefaultHttpClient(connectionManager);
    }

    @Override
    @PostConstruct
    public void initialize() throws
            RepositoryException {

        eventBus.register(this);

        final Session session = sessions.getSession();
        jcrTools.registerNodeTypes(session, "webhooks.cnd");
        session.save();
        session.logout();
    }

    /**
     * Trigger all the registered webhook callbacks for a resource when
     * the event is received
     *
     * @param resource
     * @param event
     * @throws RepositoryException
     */
    public static void runHooks(final Node resource, final FedoraEvent event)
            throws RepositoryException {
        if (resource == null) {
            LOGGER.warn("resource node is null; event path is {}", event.getPath());
            return;
        }

        final NodeIterator webhooksIterator =
                resource.getSession().getRootNode().getNodes(WEBHOOK_SEARCH);

        while (webhooksIterator.hasNext()) {
            final Node hook = webhooksIterator.nextNode();
            final String callbackUrl =
                    hook.getProperty(WEBHOOK_CALLBACK_PROPERTY).getString();
            final HttpPost method = new HttpPost(callbackUrl);
            final LegacyMethod eventSerialization =
                    new LegacyMethod(event, resource);
            final StringWriter writer = new StringWriter();

            try {
                eventSerialization.writeTo(writer);
                method.setEntity(new StringEntity(writer.toString()));
            } catch (final IOException e) {
                LOGGER.warn("Got exception generating webhook body: {}", e);
            }

            try {
                LOGGER.debug("Firing callback for" + hook.getName());
                client.execute(method);
            } catch (final IOException e) {
                LOGGER.warn("Got exception running webhook callback for {}: {}", hook.getName(), e);
            }

        }
    }

    @GET
    public Response showWebhooks() throws RepositoryException {

        final NodeIterator webhooksIterator =
                readOnlySession.getRootNode().getNodes("webhook:*");
        final StringBuilder str = new StringBuilder();

        while (webhooksIterator.hasNext()) {
            final Node hook = webhooksIterator.nextNode();
            final String callbackUrl =
                    hook.getProperty("webhook:callbackUrl").getString();
            str.append(hook.getIdentifier() + ": " + callbackUrl + ", ");
        }

        return ok(str.toString()).build();
    }

    @POST
    @Path("{id}")
    public Response registerWebhook(@PathParam("id")
    final String id, @FormParam("callbackUrl")
    final String callbackUrl) throws RepositoryException {

        final Session session = getAuthenticatedSession();

        final Node n =
                jcrTools.findOrCreateChild(session.getRootNode(), "webhook:" +
                        id, "webhook:callback");

        n.setProperty("webhook:callbackUrl", callbackUrl);

        session.save();
        session.logout();

        return created(uriInfo.getAbsolutePath()).build();
    }

    @DELETE
    @Path("{id}")
    public Response registerWebhook(@PathParam("id")
    final String id) throws RepositoryException {

        final Session session = getAuthenticatedSession();

        final Node n =
                jcrTools.findOrCreateChild(session.getRootNode(), "webhook:" +
                        id, WEBHOOK_JCR_TYPE);
        n.remove();

        session.save();
        session.logout();

        return noContent().build();
    }

    /**
     * Listen to the EventBus and trigger webhooks callbacks (see .runHooks)
     * @param event
     */
    @Subscribe
    public void onEvent(final FedoraEvent event) {
        try {
            LOGGER.debug("Webhooks received event: {}", event);
            final Node resource =
                    jcrTools.findOrCreateNode(readOnlySession.getRepository().login(), event.getPath());

            runHooks(resource, event);
        } catch (final RepositoryException e) {
            LOGGER.error("Got a repository exception handling message: {}", e);
        }
    }

    @PostConstruct
    public final void getSession() {
        try {
            readOnlySession = sessions.getSession();
        } catch (final RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    @PreDestroy
    public final void logoutSession() {
        readOnlySession.logout();
    }
}