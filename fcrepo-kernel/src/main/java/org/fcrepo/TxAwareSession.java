package org.fcrepo;

import javax.jcr.Session;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class TxAwareSession implements InvocationHandler {
    private final String txId;
    private Session session;

    public TxAwareSession(final Session session, final String txID) {
        this.session = session;
        this.txId = txID;
    }

    public static Session newInstance(final Session session, final String txId) {
        return (Session) java.lang.reflect.Proxy.newProxyInstance(session.getClass().getClassLoader(), new Class[] { TxSession.class }, new TxAwareSession(session, txId));
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
