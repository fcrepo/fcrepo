/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.identifiers;

import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date Mar 14, 2013
 */
public class UUIDPidMinterTest {

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testMintPid() throws Exception {

        final PidMinter pidMinter = new UUIDPidMinter();

        final String pid = pidMinter.mintPid();

        assertTrue("PID wasn't a UUID", compile(
                "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
                .matcher(pid).find());

    }
}
