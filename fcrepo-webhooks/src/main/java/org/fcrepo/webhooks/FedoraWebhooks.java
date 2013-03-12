package org.fcrepo.webhooks;


import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.fcrepo.AbstractResource;
import org.fcrepo.messaging.legacy.LegacyMethod;
import org.fcrepo.observer.FedoraEvent;
import org.fcrepo.utils.FedoraTypesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.ok;

@Path("/webhooks")
public class FedoraWebhooks extends AbstractResource {

    static final private Logger logger = LoggerFactory
            .getLogger(FedoraWebhooks.class);


    protected static final PoolingClientConnectionManager connectionManager =
            new PoolingClientConnectionManager();

    protected static HttpClient client;


    @Inject
    EventBus eventBus;

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

    @PostConstruct
    public void initialize() throws LoginException, NoSuchWorkspaceException,
            RepositoryException {

        eventBus.register(this);

        final Session session = repo.login();
        jcrTools.registerNodeTypes(session, "webhooks.cnd");
        session.save();
        session.logout();
    }

    public static void runHooks(final Node resource, final FedoraEvent event) throws RepositoryException {
        final NodeIterator webhooksIterator = resource.getSession().getRootNode().getNodes("webhook:*");

        while(webhooksIterator.hasNext()) {
            final Node hook = webhooksIterator.nextNode();
            final String callbackUrl = hook.getProperty("webhook:callbackUrl").getString();
            HttpPost method = new HttpPost(callbackUrl);
            LegacyMethod eventSerialization = new LegacyMethod(event, resource);
            StringWriter writer = new StringWriter();

            try {
                eventSerialization.writeTo(writer);
                method.setEntity(new StringEntity(writer.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                logger.debug("Firing callback for" + hook.getName());
                client.execute(method);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @GET
    public Response showWebhooks() throws RepositoryException {

        final NodeIterator webhooksIterator = readOnlySession.getRootNode().getNodes("webhook:*");
        StringBuilder str = new StringBuilder();

        while(webhooksIterator.hasNext()) {
            final Node hook = webhooksIterator.nextNode();
            final String callbackUrl = hook.getProperty("webhook:callbackUrl").getString();
            str.append(callbackUrl + ", ");
        }

        return ok(str.toString()).build();
    }

    @POST
    @Path("{id}")
    public Response registerWebhook(@PathParam("id") final String id, @QueryParam("callbackUrl") final String callbackUrl) throws RepositoryException {

        final Session session = repo.login();

        Node n = jcrTools.findOrCreateChild(session.getRootNode(), "webhook:" + id, "webhook:callback");

        n.setProperty("webhook:callbackUrl", callbackUrl);

        session.save();
        session.logout();

        return created(uriInfo.getAbsolutePath()).build();
    }



    @Subscribe
    public void newEvent(Event event) {
        try {
            final Node resource = jcrTools.findOrCreateNode(readOnlySession, event.getPath());
            final boolean isDatastreamNode =
                    FedoraTypesUtils.isFedoraDatastream.apply(resource);
            final boolean isObjectNode =
                    FedoraTypesUtils.isFedoraObject.apply(resource) &&
                            !isDatastreamNode;

            if(isDatastreamNode || isObjectNode) {
                runHooks(resource, new FedoraEvent(event));
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    @PostConstruct
    public final void getSession() {
        try {
            readOnlySession = repo.login();
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    @PreDestroy
    public final void logoutSession() {
        readOnlySession.logout();
    }
}