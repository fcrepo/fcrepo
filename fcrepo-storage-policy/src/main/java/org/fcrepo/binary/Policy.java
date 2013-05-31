/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.binary;

import javax.jcr.Node;

/**
 * @todo Add Documentation.
 * @author cbeer
 * @date Apr 25, 2013
 */
public interface Policy {
    /**
     * @todo Add Documentation.
     */
    String evaluatePolicy(Node n);
}
