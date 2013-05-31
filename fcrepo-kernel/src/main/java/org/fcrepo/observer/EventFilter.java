/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.observer;

import javax.jcr.observation.Event;

import com.google.common.base.Predicate;

/**
 * @todo Add Documentation.
 * @author eddies
 * @date Feb 7, 2013
 */
public interface EventFilter extends Predicate<Event> {

}
