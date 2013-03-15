package org.fcrepo.observer;

import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class NOOPFilterTest {
    @Test
    public void testApply() throws Exception {
      assertTrue(new NOOPFilter().apply(null));
    }


}
