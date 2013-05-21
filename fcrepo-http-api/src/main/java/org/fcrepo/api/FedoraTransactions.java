
package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.fcrepo.Transaction.State.DIRTY;
import static org.fcrepo.Transaction.State.ROLLED_BACK;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.Transaction;
import org.fcrepo.services.ObjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Path("/rest/fcr:tx")
public class FedoraTransactions extends AbstractResource {

    @Autowired
    private ObjectService objectService;

    /*
     * TODO: since transactions have to be available on all nodes, they have to
     * be either persisted or written to a
     */
    /* distributed map or sth, not just this plain hashmap that follows */
    private static Map<String, Transaction> transactions =
            new ConcurrentHashMap<String, Transaction>();

    @Scheduled(fixedRate=1000)
    public void removeAndRollbackExpired(){
        synchronized(transactions){
            Iterator<Entry<String, Transaction>> txs = transactions.entrySet().iterator();
            while (txs.hasNext()){
                Transaction tx = txs.next().getValue();
                if (tx.getExpires().getTime() > System.currentTimeMillis()){
                    txs.remove();
                }
            }
        }
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
    public Transaction createTransaction() throws RepositoryException {
    	Session sess = getAuthenticatedSession();
    	Transaction tx = new Transaction(sess);
    	transactions.put(tx.getId(), tx);
    	return tx;
    }

    @GET
    @Path("/{txid}")
    public Transaction getTransaction(@PathParam("txid")
    final String txid) throws RepositoryException {
        final Transaction tx = transactions.get(txid);
        if (tx == null) {
            throw new PathNotFoundException("Transaction is not available");
        }
        return tx;
    }

    @POST
    @Path("/{txid}/fcr:commit")
    @Produces({APPLICATION_JSON, TEXT_XML})
    public Transaction commit(@PathParam("txid")
    final String txid) throws RepositoryException {
        final Transaction tx = transactions.remove(txid);
        if (tx == null) {
            throw new RepositoryException("Transaction with id " + txid +
                    " is not available");
        }
        tx.commit();
        return tx;
    }

    @POST
    @Path("/{txid}/fcr:rollback")
    @Produces({APPLICATION_JSON, TEXT_XML})
    public Transaction rollback(@PathParam("txid")
    final String txid) throws RepositoryException {
        final Transaction tx = transactions.remove(txid);
        if (tx == null) {
            throw new RepositoryException("Transaction with id " + txid +
                    " is not available");
        }
        tx.setState(ROLLED_BACK);
        return tx;
    }

    @POST
    @Path("/{txid}/{path: .*}/fcr:newhack")
    @Produces({TEXT_PLAIN})
    public Response createObjectInTransaction(@PathParam("txid")
    final String txid, @PathParam("path")
    final List<PathSegment> pathlist) throws RepositoryException {
        final Transaction tx = transactions.get(txid);
        if (tx == null) {
            throw new RepositoryException("Transaction with id " + txid +
                    " is not available");
        }
        final String path = toPath(pathlist);
        objectService.createObject(tx.getSession(), path);
        tx.updateExpiryDate();
        tx.setState(DIRTY);
        return Response.ok(path).build();
    }

}
