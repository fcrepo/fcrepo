package org.fcrepo;

import javax.jcr.Session;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class TxAwareSession implements InvocationHandler {
    private Session session;

    public TxAwareSession(final Session session) {
        this.session = session;
    }

    public static Session newInstance(final Session session) {
        return (Session) java.lang.reflect.Proxy.newProxyInstance(session.getClass().getClassLoader(), new Class[] { Session.class }, new TxAwareSession(session));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("logout") || method.getName().equals("save")) {
            return null;
        } else {
            return method.invoke(session, args);
        }
    }
}
