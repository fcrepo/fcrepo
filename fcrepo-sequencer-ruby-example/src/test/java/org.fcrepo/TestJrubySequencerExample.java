package org.fcrepo;

import org.junit.Test;
import org.fcrepo.FcrepoSequencerRubyExample;

import static junit.framework.Assert.assertTrue;


public class TestJrubySequencerExample {

    @Test
    public void testJrubySequencerJavaClass() {
        FcrepoSequencerRubyExample zz = new FcrepoSequencerRubyExample();
        Boolean b = zz.execute(null, null, null);
        assertTrue(b);
    }
}
