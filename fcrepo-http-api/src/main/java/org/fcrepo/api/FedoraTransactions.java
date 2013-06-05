package org.fcrepo.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;

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
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.Transaction;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Path("/fcr:tx")
public class FedoraTransactions extends AbstractResource {

    /*
     * TODO: since transactions have to be available on all nodes, they have to
     * be either persisted or written to a distributed map or sth, not just
     * this plain hashmap that follows
     */
    private static Map<String, Transaction> TRANSACTIONS =
            new ConcurrentHashMap<String, Transaction>();
    
    public static final long REAP_INTERVAL = 1000;

    @Scheduled(fixedRate=REAP_INTERVAL)
    public void removeAndRollbackExpired(){
        synchronized(TRANSACTIONS){
            Iterator<Entry<String, Transaction>> txs = TRANSACTIONS.entrySet().iterator();
            while (txs.hasNext()){
                Transaction tx = txs.next().getValue();
                if (tx.getExpires().getTime() <= System.currentTimeMillis()){
                	try {
						tx.rollback();
					} catch (RepositoryException e) {
						// TODO Not clear how to respond here
						e.printStackTrace();
					}
                    txs.remove();
                }
            }
        }
    }

    @POST
    @Produces({ APPLICATION_JSON, TEXT_XML })
    public Transaction createTransaction() throws RepositoryException {
    	Session sess = getAuthenticatedSession();
    	Transaction tx = new Transaction(sess);
    	TRANSACTIONS.put(tx.getId(), tx);
    	return tx;
    }

    @GET
    @Path("/{txid}")
    public Transaction getTransaction(@PathParam("txid")
    final String txid) throws RepositoryException {
        final Transaction tx = TRANSACTIONS.get(txid);
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
        final Transaction tx = TRANSACTIONS.remove(txid);
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
        final Transaction tx = TRANSACTIONS.remove(txid);
        if (tx == null) {
            throw new RepositoryException("Transaction with id " + txid +
                    " is not available");
        }
        tx.rollback();
        return tx;
    }

    @POST
    @Path("/{txid}/{path: .*}/fcr:newhack")
    @Produces({TEXT_PLAIN})
    public Response createObjectInTransaction(@PathParam("txid")
    final String txid, @PathParam("path")
    final List<PathSegment> pathlist) throws RepositoryException {
        final Transaction tx = TRANSACTIONS.get(txid);
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
