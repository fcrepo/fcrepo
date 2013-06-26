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
package org.fcrepo.observer;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

/**
 * A very simple abstraction to prevent event-driven machinery
 * downstream from the repository from relying directly on a JCR
 * interface (Event).
 *
 * @author ajs6f
 * @date Feb 19, 2013
 */
public class FedoraEvent implements Event {

    private Event e;

    /**
     * @todo Add Documentation.
     */
    public FedoraEvent(final Event e) {
        checkArgument(e != null, "null cannot support a FedoraEvent!");
        this.e = e;
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public int getType() {
        return e.getType();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public String getPath() throws RepositoryException {
        return e.getPath();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public String getUserID() {
        return e.getUserID();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public String getIdentifier() throws RepositoryException {
        return e.getIdentifier();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public Map<?, ?> getInfo() throws RepositoryException {
        return e.getInfo();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public String getUserData() throws RepositoryException {
        return e.getUserData();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public long getDate() throws RepositoryException {
        return e.getDate();
    }

}
