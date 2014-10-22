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
package org.modeshape.jcr;

import org.modeshape.jcr.bus.ChangeBus;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 10/15/14
 */
public class SessionUtils {

    /**
     * No public constructor for utility classes
     */
    private SessionUtils() {

    }

    private static final Logger LOGGER = getLogger(SessionUtils.class);

    /**
     * Until MODE-2343 is fixed, we need to manually dispose of the ObservationManager listener.
     *
     * @param session
     */
    public static void unregisterObservationManager(final Session session) {
        try {
            getChangeBus(session).unregister(getObservationManager(session));
        } catch (final RepositoryException e) {
            LOGGER.info("Unable to dispose observation manager: {}", e);
        }
    }

    private static JcrObservationManager getObservationManager(final Session session) throws RepositoryException {
        return (JcrObservationManager) session.getWorkspace().getObservationManager();
    }

    private static ChangeBus getChangeBus(final Session session) {
        return ((JcrRepository)session.getRepository()).changeBus();
    }
}
