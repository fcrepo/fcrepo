package org.fcrepo.merritt.api;

import org.fcrepo.merritt.AbstractMerrittTest;
import org.junit.Test;

import java.io.IOException;

public class BagitServiceTest extends AbstractMerrittTest {
    @Test
    public void TestBagitService() throws IOException {
        getBagitPath("fedora/object_id/0");

    }
}
