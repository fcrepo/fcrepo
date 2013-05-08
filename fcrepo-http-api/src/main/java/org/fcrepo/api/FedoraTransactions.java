package org.fcrepo.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.fcrepo.AbstractResource;
import org.fcrepo.Transaction;
import org.fcrepo.Transaction.State;
import org.springframework.stereotype.Component;

import com.sun.jersey.spi.resource.Singleton;

@Component
@Path("/rest/fcr:tx")
public class FedoraTransactions extends AbstractResource {

	/* TODO: since transactions have to be available on all nodes, they have to be either persisted or written to a */
	/* distributed map or sth, not just this plain hashmap that follows */
	private static Map<String, Transaction> transactions = new ConcurrentHashMap<String, Transaction>();

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
	@Path("/{txid}/fcr:commmit")
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
	
}
