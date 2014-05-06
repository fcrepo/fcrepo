/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.kernel;

import org.slf4j.Logger;
import org.springframework.util.ClassUtils;

import javax.jcr.Session;
import javax.jcr.lock.LockManager;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.reflect.Proxy.newProxyInstance;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Wraps a session to ensure that all lock tokens are removed from the session
 * when logout() is invoked.
 *
 * The JCR spec isn't clear about what should happen when a session ends that
 * is the owner of open-scoped lock tokens.  The current ModeShape implementation
 * does not release those token upon logout, leaving them unable to be used
 * by other sessions.
 *
 * TODO: Remove this class When modeshape is updated to handle this. https://www.pivotaltracker.com/story/show/69574878
 *
 * @author Mike Durbin
 */

public class LockReleasingSession implements InvocationHandler {

    private static final Logger LOGGER = getLogger(LockReleasingSession.class);

    private final Session session;

    /**
     * Constructor that wraps a session.
     */
    public LockReleasingSession(final Session session) {
        this.session = session;
    }

    /**
     * Wrap a JCR session with this dynamic proxy
     */
    public static Session newInstance(final Session session) {
        LOGGER.trace("Wrapping session {} ({}).", session, session.getClass().getName());
        return (Session) newProxyInstance(session.getClass().getClassLoader(),
                ClassUtils.getAllInterfaces(session),
                new LockReleasingSession(session));
    }

    /**
     * Gets the wrapped session.
     */
    public Session getWrappedSession() {
        return this.session;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final String name = method.getName();

        if (name.equals("logout")) {
            LOGGER.trace("Intercepted session.logout()");
            if (session.isLive()) {  // this may be false if logout is called more than once on a session
                final LockManager lockManager = session.getWorkspace().getLockManager();
                for (String token : lockManager.getLockTokens()) {
                    LOGGER.trace("Removing lock token {}.", token);
                    lockManager.removeLockToken(token);
                }
            }
        }
        return delegate(method, args);
    }

    private Object delegate(final Method method, final Object[] args) throws Throwable {
        final Object invocationResult;
        try {
            invocationResult = method.invoke(session, args);
        } catch (final InvocationTargetException e) {
            LOGGER.debug("Throwing '{}', caught: {}", e.getCause().toString(), e);
            throw e.getCause();
        }
        return invocationResult;
    }
}
