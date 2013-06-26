/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.api;

import static org.mockito.Mockito.mock;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.services.NodeService;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Before;

public class FedoraVersionsTest {

    FedoraVersions testObj;

    NodeService mockNodes;

    Session mockSession;

    @Before
    public void setUp() throws LoginException, RepositoryException {

        testObj = new FedoraVersions();

        mockNodes = mock(NodeService.class);
        testObj.setNodeService(mockNodes);
        mockSession = TestHelpers.mockSession(testObj);
        testObj.setSession(mockSession);
        testObj.setUriInfo(TestHelpers.getUriInfoImpl());

    }

}
