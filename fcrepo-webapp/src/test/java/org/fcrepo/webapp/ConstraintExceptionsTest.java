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
package org.fcrepo.webapp;

import org.junit.Assert;
import org.fcrepo.kernel.api.exception.ConstraintViolationException;
import org.junit.Test;
import org.reflections.Reflections;

import java.io.File;
import java.util.Set;

/**
 * @author awoods
 * @since 6/25/15
 */
public class ConstraintExceptionsTest {

    @Test
    public void testConstraintRdfExists() {
        final Reflections reflections = new Reflections("org.fcrepo");
        final Set<Class<? extends ConstraintViolationException>> subTypes =
                reflections.getSubTypesOf(ConstraintViolationException.class);

        subTypes.add(ConstraintViolationException.class);

        for (final Class c : subTypes) {
            final File file = new File("src/main/webapp/static/constraints/" + c.getSimpleName() + ".rdf");
            Assert.assertTrue("Expected to find: " + file.getPath(), file.exists());
        }
    }

}
