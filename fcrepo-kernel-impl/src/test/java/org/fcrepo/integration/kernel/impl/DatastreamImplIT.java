/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.integration.kernel.impl;

import javax.inject.Inject;
import javax.jcr.Repository;

import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.ObjectService;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.Assert.assertTrue;

/**
 * <p>DatastreamImplIT class.</p>
 *
 * @author ksclarke
 */
@ContextConfiguration({"/spring-test/repo.xml"})
public class DatastreamImplIT extends AbstractIT {

    @Inject
    Repository repo;

    @Inject
    DatastreamService datastreamService;

    @Inject
    ObjectService objectService;

    @Test
    public void toDo() {
        assertTrue(true);
    }

}
