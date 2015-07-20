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
package org.fcrepo.kernel.api.observer.eventmappings;

import java.util.Iterator;
import java.util.function.Function;

import javax.jcr.observation.Event;

import org.fcrepo.kernel.api.observer.FedoraEvent;

/**
 * Maps {@link Iterator}s of {@link Event}s to {@link Iterator}s of {@link FedoraEvent}s according to some algorithm
 *
 * @author ajs6f
 * @since Feb 27, 2014
 */
public interface InternalExternalEventMapper extends Function<Iterator<Event>, Iterator<FedoraEvent>> {
}
