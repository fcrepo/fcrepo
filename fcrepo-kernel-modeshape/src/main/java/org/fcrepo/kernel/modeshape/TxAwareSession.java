/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape;

import static com.google.common.reflect.Reflection.newProxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.jcr.Session;

import org.fcrepo.kernel.api.TxSession;

import com.google.common.reflect.AbstractInvocationHandler;

/**
 * A dynamic proxy that wraps JCR sessions. It is aware of fcrepo transactions,
 * and turns mutating methods (e.g. logout, session) into no-ops. Those no-op'ed
 * methods should be called from the Transaction level instead.
 *
 * @author awoods
 */
public class TxAwareSession extends AbstractInvocationHandler {

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
        return newProxy(TxSession.class, new TxAwareSession(session, txId));
    }

    @Override
    protected Object handleInvocation(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final String name = method.getName();
        if (name.equals("logout") || name.equals("save")) {
            return null;
        } else if (name.equals("getTxId")) {
            return txId;
        } else {
            final Object invocationResult;
            try {
                invocationResult = method.invoke(session, args);
            } catch (final InvocationTargetException e) {
                throw e.getCause();
            }
            if (name.equals("impersonate")) {
                return newInstance((Session) invocationResult, txId);
            }
            return invocationResult;
        }
    }
}
