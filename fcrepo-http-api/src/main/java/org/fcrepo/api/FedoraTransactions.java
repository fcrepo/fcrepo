package org.fcrepo.api;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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
import org.fcrepo.FedoraObject;
import org.fcrepo.Transaction;
import org.fcrepo.Transaction.State;
import org.fcrepo.services.ObjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Path("/rest/fcr:tx")
public class FedoraTransactions extends AbstractResource {

	@Autowired
	private ObjectService objectService;

	/* TODO: since transactions have to be available on all nodes, they have to be either persisted or written to a */
	/* distributed map or sth, not just this plain hashmap that follows */
	private static Map<String, Transaction> transactions = new ConcurrentHashMap<String, Transaction>();

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
	@Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
	public Transaction getTransaction(@PathParam("txid") final String txid) throws RepositoryException {
		Transaction tx = transactions.get(txid);
		if (tx == null) {
			throw new RepositoryException("Transaction with id " + txid + " is not available");
		}
		return tx;
	}

	@POST
	@Path("/{txid}/fcr:commit")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
	public Transaction commit(@PathParam("txid") final String txid) throws RepositoryException {
		Transaction tx = transactions.remove(txid);
		if (tx == null) {
			throw new RepositoryException("Transaction with id " + txid + " is not available");
		}
		tx.getSession().save();
		tx.setState(State.COMMITED);
		return tx;
	}

	@POST
	@Path("/{txid}/fcr:rollback")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_XML })
	public Transaction rollback(@PathParam("txid") final String txid) throws RepositoryException {
		Transaction tx = transactions.remove(txid);
		if (tx == null) {
			throw new RepositoryException("Transaction with id " + txid + " is not available");
		}
		tx.setState(State.ROLLED_BACK);
		return tx;
	}

	@POST
	@Path("/{txid}/{path: .*}/fcr:newhack")
	@Produces({MediaType.TEXT_PLAIN})
	public Response createObjectInTransaction(@PathParam("txid") final String txid, @PathParam("path") final List<PathSegment> pathlist)throws RepositoryException {
		Transaction tx = transactions.get(txid);
		if (tx == null) {
			throw new RepositoryException("Transaction with id " + txid + " is not available");
		}
		final String path = toPath(pathlist);
		final FedoraObject obj = objectService.createObject(tx.getSession(), path);
		tx.setState(State.DIRTY);
		return Response.ok(path).build();
	}

}
