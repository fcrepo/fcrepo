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
package org.fcrepo.kernel.observer;

import javax.jcr.Session;
import javax.jcr.observation.Event;

import com.google.common.base.Function;

/**
 * Filter JCR events to remove extraneous events
 * @author eddies
 * @date Feb 7, 2013
 */
public interface EventFilter extends Function<Event, Event> {

    /**
     * Return a function to wrap Events, or transform to null if they should be
     * skipped
     *
     * @param session
     * @return
     */
    Function<Event, Event> getFilter(final Session session);
}
