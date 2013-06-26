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
package org.fcrepo.utils;

import java.util.Iterator;

import javax.jcr.observation.Event;

/**
 * @todo Add Documentation.
 * @author ajs6f
 * @date Apr 20, 2013
 */
public class EventIterator implements Iterator<Event>, Iterable<Event> {

    private javax.jcr.observation.EventIterator i;

    /**
     * @todo Add Documentation.
     */
    public EventIterator(final javax.jcr.observation.EventIterator i) {
        this.i = i;
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public boolean hasNext() {
        return i.hasNext();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public Event next() {
        return i.nextEvent();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public void remove() {
        i.remove();
    }

    /**
     * @todo Add Documentation.
     */
    @Override
    public Iterator<Event> iterator() {
        return this;
    }

}
