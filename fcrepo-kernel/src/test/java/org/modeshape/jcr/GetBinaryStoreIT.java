/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.modeshape.jcr;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.TransientBinaryStore;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @todo Add Documentation.
 * @author Chris Beer
 * @date Apr 30, 2013
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/spring-test/repo.xml" })
public class GetBinaryStoreIT {

    @Inject
    javax.jcr.Repository repo;

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testApply() throws RepositoryException {
        final GetBinaryStore testObj = new GetBinaryStore();

        final BinaryStore binaryStore = testObj.apply(repo);

        assertThat(binaryStore, instanceOf(TransientBinaryStore.class));

    }

}
