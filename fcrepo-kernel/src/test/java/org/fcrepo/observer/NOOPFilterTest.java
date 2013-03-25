package org.fcrepo.observer;

import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class NOOPFilterTest {
    @Test
    public void testApply() throws Exception {
      assertTrue(new NOOPFilter().apply(null));
    }


}
