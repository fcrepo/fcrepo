/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.TransientBinaryStore;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * <p>GetBinaryStoreIT class.</p>
 *
 * @author cbeer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/spring-test/repo.xml" })
public class GetBinaryStoreIT {

    @Inject
    javax.jcr.Repository repo;

    @Test
    public void testApply() {
        final GetBinaryStore testObj = new GetBinaryStore();

        final BinaryStore binaryStore = testObj.apply(repo);

        assertThat(binaryStore, instanceOf(TransientBinaryStore.class));

    }

}
