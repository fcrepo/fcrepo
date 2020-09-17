/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.services;

import java.time.Instant;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.ContainmentIndex;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.services.MembershipService;
import org.fcrepo.kernel.impl.models.ResourceFactoryImpl;
import org.fcrepo.persistence.api.PersistentStorageSession;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;
import org.fcrepo.persistence.common.ResourceHeadersImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/containmentIndexTest.xml")
public class MembershipServiceImplTest {

    private final static Instant CREATED_DATE = Instant.parse("2019-11-12T10:00:30.0Z");

    private final static String CREATED_BY = "user1";

    private final static Instant LAST_MODIFIED_DATE = Instant.parse("2019-11-12T14:11:05.0Z");

    private final static String LAST_MODIFIED_BY = "user2";

    private final static String STATE_TOKEN = "stately_value";

    @Mock
    private PersistentStorageSessionManager pSessionManager;
    @Mock
    private PersistentStorageSession psSession;
    @Inject
    private MembershipService membershipService;
    @Inject
    private ContainmentIndex containmentIndex;

    private ResourceFactory resourceFactory;

    private FedoraResource membershipResc;

    private FedoraId membershipRescId;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        resourceFactory = new ResourceFactoryImpl();
        setField(resourceFactory, "persistentStorageSessionManager", pSessionManager);
        setField(resourceFactory, "containmentIndex", containmentIndex);

        membershipRescId = mintFedoraId();
    }

    // get membership for container with no members
    // get in tx, container no members
    // get in tx, container with newly added members
    // get in tx, container with existing members, no new
    // get in tx, container with existing members and new, no overly
    // get in tx,

    @Test
    public void getMembers_NoMembership() throws Exception {
        final var containerResc = resourceFactory.getResource(membershipRescId);

    }

    private FedoraId mintFedoraId() {
        return FedoraId.create(UUID.randomUUID().toString());
    }

    private static ResourceHeaders populateHeaders(final FedoraId fedoraId, final Resource ixModel) {
        headers = new ResourceHeadersImpl();
        resourceHeaders.setId(fedoraId);
        headers.setInteractionModel(ixModel.getURI());
        headers.setCreatedBy(CREATED_BY);
        headers.setCreatedDate(CREATED_DATE);
        headers.setLastModifiedBy(LAST_MODIFIED_BY);
        headers.setLastModifiedDate(LAST_MODIFIED_DATE);
        headers.setStateToken(STATE_TOKEN);
    }
}
