/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.identifiers;

import com.google.common.base.Function;

/**
 * Defines the behavior of a component that can accept responsibility
 * for the creation of Fedora PIDs. Do not implement this interface directly.
 * Subclass {@link BasePidMinter} instead.
 *
 * @author eddies
 * @date Feb 7, 2013
 */
public interface PidMinter {

    /**
     * @todo Document.
     */
    String mintPid();

    /**
     * @todo Document.
     */
    Function<Object, String> makePid();
}
