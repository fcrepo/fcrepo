package org.fcrepo;

import javax.jcr.Session;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * A dynamic proxy that wraps JCR sessions. It is aware of fcrepo transactions, and
 * turns mutating methods (e.g. logout, session) into no-ops. Those
 * no-op'ed methods should be called from the Transaction level instead.
 */
public class TxAwareSession implements InvocationHandler {
    private final String txId;
    private Session session;

    /**
     * @param session a JCR session
     * @param txID the transaction identifier
     */
    public TxAwareSession(final Session session, final String txID) {
        this.session = session;
        this.txId = txID;
    }

    /**
     * Wrap a JCR session with this dynamic proxy
     *
     * @param session a JCR session
     * @param txId the transaction identifier
     * @return a wrapped JCR session
     */
    public static Session newInstance(final Session session, final String txId) {
        return (Session) newProxyInstance(session.getClass().getClassLoader(),
                                                 new Class[]{TxSession.class},
                                                 new TxAwareSession(session, txId));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("logout") || method.getName().equals("save")) {
            return null;
        } else if (method.getName().equals("getTxId")) {
            return txId;
        } else {
            return method.invoke(session, args);
        }
    }
}
