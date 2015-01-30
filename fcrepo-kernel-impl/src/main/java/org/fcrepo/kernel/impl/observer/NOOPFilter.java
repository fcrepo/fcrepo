/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.observer;

import javax.jcr.Session;
import javax.jcr.observation.Event;

import com.google.common.base.Predicate;
import org.fcrepo.kernel.observer.EventFilter;

/**
 * Simple {@link EventFilter} that does no filtering.
 *
 * @author eddies
 * @since Feb 7, 2013
 * @author ajs6f
 * @author barmintor
 * @since Dec 2013
 */
public class NOOPFilter implements EventFilter {

    @Override
    public Predicate<Event> getFilter(final Session session) {
        return this;
    }

    @Override
    public boolean apply(final Event event) {
        return true;
    }

}
