/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.observer;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date Mar 14, 2013
 */
public class NOOPFilterTest {

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testApply() throws Exception {
        assertTrue(new NOOPFilter().apply(null));
    }

}
