package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;

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

import org.fcrepo.AbstractResource;
import org.fcrepo.Transaction;
import org.fcrepo.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/fcr:tx")
public class FedoraTransactions extends AbstractResource {

    @Autowired
    private TransactionService txService;

    @POST
    @Produces({ APPLICATION_JSON, TEXT_XML })
    public Transaction createTransaction() throws RepositoryException {
    	Session sess = getAuthenticatedSession();
    	return txService.beginTransaction(sess);
    }

    @GET
    @Path("/{txid}")
    public Transaction getTransaction(@PathParam("txid")
    final String txid) throws RepositoryException {
        return txService.getTransaction(txid);
    }

    @POST
    @Path("/{txid}/fcr:commit")
    @Produces({APPLICATION_JSON, TEXT_XML})
    public Transaction commit(@PathParam("txid")
    final String txid) throws RepositoryException {
        return txService.commit(txid);
    }

    @POST
    @Path("/{txid}/fcr:rollback")
    @Produces({APPLICATION_JSON, TEXT_XML})
    public Transaction rollback(@PathParam("txid")
    final String txid) throws RepositoryException {
        return txService.rollback(txid);
    }

    @POST
    @Path("/{txid}/{path: .*}/fcr:newhack")
    @Produces({TEXT_PLAIN})
    public Response createObjectInTransaction(@PathParam("txid")
    final String txid, @PathParam("path")
    final List<PathSegment> pathlist) throws RepositoryException {
        final Transaction tx = txService.getTransaction(txid);
        if (tx == null) {
            throw new RepositoryException("Transaction with id " + txid +
                    " is not available");
        }
        final String path = toPath(pathlist);
        objectService.createObject(tx.getSession(), path);
        tx.updateExpiryDate();
        return Response.ok(path).build();
    }

}
