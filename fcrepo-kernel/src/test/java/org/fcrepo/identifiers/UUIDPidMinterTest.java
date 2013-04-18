
package org.fcrepo.identifiers;

import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UUIDPidMinterTest {

    @Test
    public void testMintPid() throws Exception {

        final PidMinter pidMinter = new UUIDPidMinter();

        final String pid = pidMinter.mintPid();

        assertTrue("PID wasn't a UUID", compile(
                "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
                .matcher(pid).find());

    }
}
