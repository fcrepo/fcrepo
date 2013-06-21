package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import org.fcrepo.AbstractResource;
import org.fcrepo.Transaction;
import org.fcrepo.TxSession;
import org.fcrepo.services.TransactionService;
import org.fcrepo.session.InjectedSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:tx")
public class FedoraTransactions extends AbstractResource {

    @Autowired
    private TransactionService txService;

    @InjectedSession
    protected Session session;

    @POST
    public Response createTransaction(@PathParam("path")
                                          final List<PathSegment> pathList) throws RepositoryException {

        if (!pathList.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (session instanceof TxSession) {
            Transaction t = txService.getTransaction(((TxSession) session).getTxId());
            t.updateExpiryDate();
            return noContent().expires(t.getExpires()).build();

        } else {
            Transaction t = txService.beginTransaction(session);
    	    return created(uriInfo.getBaseUriBuilder().path(FedoraNodes.class).buildFromMap(ImmutableMap.of("path", "tx:" + t.getId()))).expires(t.getExpires()).build();
        }
    }

    @POST
    @Path("fcr:commit")
    public Response commit(@PathParam("path")
                               final List<PathSegment> pathList) throws RepositoryException {

        final String path = toPath(pathList);

        if (!path.equals("/")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (session instanceof TxSession) {
            txService.commit(((TxSession) session).getTxId());
            return noContent().build();

        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

    }

    @POST
    @Path("fcr:rollback")
    public Response rollback(@PathParam("path")
                                 final List<PathSegment> pathList) throws RepositoryException {

        final String path = toPath(pathList);

        if (!path.equals("/")) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }


        if (session instanceof TxSession) {
            txService.rollback(((TxSession) session).getTxId());
            return noContent().build();

        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

}
