/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo;

import static java.lang.reflect.Proxy.newProxyInstance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.jcr.Session;

/**
 * A dynamic proxy that wraps JCR sessions. It is aware of fcrepo transactions,
 * and turns mutating methods (e.g. logout, session) into no-ops. Those no-op'ed
 * methods should be called from the Transaction level instead.
 */
public class TxAwareSession implements InvocationHandler {

    private final String txId;

    private final Session session;

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
                new Class[] {TxSession.class},
                new TxAwareSession(session, txId));
    }

    @Override
    public Object invoke(final Object proxy, final Method method,
            final Object[] args) throws ReflectiveOperationException,
            IllegalArgumentException {
        if (method.getName().equals("logout") ||
                method.getName().equals("save")) {
            return null;
        } else if (method.getName().equals("getTxId")) {
            return txId;
        } else if (method.getName().equals("impersonate")) {
            return newInstance((Session) method.invoke(session, args), txId);
        } else {
            return method.invoke(session, args);
        }
    }
}
