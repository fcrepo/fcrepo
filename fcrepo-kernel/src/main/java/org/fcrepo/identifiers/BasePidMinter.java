/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.identifiers;

import com.google.common.base.Function;

/**
 * Minting FedoraObject unique identifiers
 *
 * @author ajs6f
 * @date Mar 25, 2013
 */
public abstract class BasePidMinter implements PidMinter {

    /**
     * @todo Add Documentation.
     */
    @Override
    public Function<Object, String> makePid() {
        return new Function<Object, String>() {

            /**
             * @todo Add Documentation.
             */
            @Override
            public String apply(final Object input) {
                return mintPid();
            }
        };
    }

}
